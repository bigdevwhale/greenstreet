package app.greenstreet.domain

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Today's and tomorrow's sky at home, as the game needs it. */
data class Forecast(val today: Sky, val tomorrow: Sky, val at: Long)

object Weather {
    private const val AGENT = "GreenStreet/1.0 github.com/bigdevwhale/greenstreet"

    /** MET Norway's forecast for the next two days. Only the rounded coordinates are sent. */
    fun fetch(lat: Double, lon: Double, zone: ZoneId = ZoneId.systemDefault(), now: Long = System.currentTimeMillis()): Forecast {
        val url = "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.2f&lon=%.2f".format(Locale.US, lat, lon)
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 7000; c.readTimeout = 10_000
        c.setRequestProperty("User-Agent", AGENT)
        val body = try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
        return parse(body, Instant.ofEpochMilli(now).atZone(zone).toLocalDate(), zone, now)
    }

    fun parse(json: String, today: LocalDate, zone: ZoneId, now: Long): Forecast {
        val ts = JSONObject(json).getJSONObject("properties").getJSONArray("timeseries")
        class Acc { var min = 99.0; var max = -99.0; var rain = false; var snow = false; var fog = false; var clear = 0; var n = 0 }
        val days = mapOf(today to Acc(), today.plusDays(1) to Acc())
        for (i in 0 until ts.length()) {
            val e = ts.getJSONObject(i)
            val t = Instant.parse(e.getString("time")).atZone(zone)
            val a = days[t.toLocalDate()] ?: continue
            val d = e.getJSONObject("data")
            val temp = d.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature")
            a.min = minOf(a.min, temp); a.max = maxOf(a.max, temp)
            val sym = d.optJSONObject("next_1_hours")?.optJSONObject("summary")?.optString("symbol_code")
                ?: d.optJSONObject("next_6_hours")?.optJSONObject("summary")?.optString("symbol_code") ?: ""
            // rain or snow at any hour waters the beds; the look of the day (fog, sun) is taken from daylight
            if ("snow" in sym || "sleet" in sym) a.snow = true
            else if ("rain" in sym) a.rain = true
            if (t.hour in 6..21) {
                a.n++
                if (sym.startsWith("fog")) a.fog = true
                if (sym.startsWith("clearsky") || sym.startsWith("fair")) a.clear++
            }
        }
        fun sky(a: Acc?): Sky = if (a == null || a.n == 0 && a.min > 90) Sky() else Sky(
            rain = a.rain, snow = a.snow, frost = a.min <= 0.0, fog = a.fog, sun = a.clear * 2 > a.n, heat = a.max >= 25,
            temp = if (a.min > 90) 10.0 else (a.min + a.max) / 2,
        )
        return Forecast(sky(days[today]), sky(days[today.plusDays(1)]), now)
    }

    /**
     * What the sky actually did on the last [days] days and today so far, by Open-Meteo (MET Norway only looks
     * ahead): rain that fell while the app was closed still waters the beds. Only the rounded coordinates are sent.
     */
    fun past(lat: Double, lon: Double, days: Int = 7, zone: ZoneId = ZoneId.systemDefault(), now: Long = System.currentTimeMillis()): Map<LocalDate, Sky> {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&hourly=weather_code,temperature_2m&past_days=%d&forecast_days=1&timezone=%s"
            .format(Locale.US, lat, lon, days, java.net.URLEncoder.encode(zone.id, "UTF-8"))
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 7000; c.readTimeout = 10_000
        c.setRequestProperty("User-Agent", AGENT)
        val body = try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
        return parsePast(body, Instant.ofEpochMilli(now).atZone(zone).toLocalDateTime())
    }

    /** Open-Meteo's hourly answer (local times, WMO weather codes) → the sky of each day, counting only hours already gone by [now]. */
    fun parsePast(json: String, now: java.time.LocalDateTime): Map<LocalDate, Sky> {
        val h = JSONObject(json).getJSONObject("hourly")
        val time = h.getJSONArray("time"); val code = h.getJSONArray("weather_code"); val temp = h.getJSONArray("temperature_2m")
        class Acc { var min = 99.0; var max = -99.0; var rain = false; var snow = false; var fog = false; var clear = 0; var n = 0 }
        val days = sortedMapOf<LocalDate, Acc>()
        for (i in 0 until time.length()) {
            val t = java.time.LocalDateTime.parse(time.getString(i))
            if (t.isAfter(now)) break
            if (code.isNull(i)) continue
            val w = code.getInt(i)
            val a = days.getOrPut(t.toLocalDate()) { Acc() }
            if (!temp.isNull(i)) temp.getDouble(i).let { a.min = minOf(a.min, it); a.max = maxOf(a.max, it) }
            when (w) {
                in 51..67, in 80..82, in 95..99 -> a.rain = true
                in 71..77, 85, 86 -> a.snow = true
            }
            if (t.hour in 6..21) {
                a.n++
                if (w == 45 || w == 48) a.fog = true
                if (w <= 1) a.clear++
            }
        }
        return days.mapValues { (_, a) ->
            Sky(rain = a.rain, snow = a.snow, frost = a.min <= 0.0, fog = a.fog, sun = a.n > 0 && a.clear * 2 > a.n, heat = a.max >= 25,
                temp = if (a.min > 90) 10.0 else (a.min + a.max) / 2)
        }
    }

    /** A plausible sky for a day with no forecast on record: the same for everyone, by the month. */
    fun simulated(date: LocalDate, month: Int): Sky {
        val r = hash(date.toEpochDay().toInt(), 77)
        val r2 = hash(date.toEpochDay().toInt(), 13)
        val base = doubleArrayOf(-7.0, -6.0, -1.0, 6.0, 12.0, 16.0, 19.0, 17.0, 11.0, 5.0, 0.0, -4.0)[month - 1] + (r2 - .5) * 8
        val winter = month in listOf(12, 1, 2)
        return Sky(
            rain = !winter && r < .3, snow = winter && r < .3, frost = base < 1, fog = month in 9..11 && r in .3..0.4,
            sun = r > .65, heat = base > 24, temp = base,
        )
    }

    private fun hash(i: Int, j: Int): Double = Terrain.hash(i, j)
}
