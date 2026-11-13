package app.greenstreet

import app.greenstreet.domain.Weather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Real org.json comes with Robolectric; the plain JVM has only stubs. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeatherTest {
    /** Open-Meteo hourly answer: [codes] per hour from midnight of [start]. */
    private fun meteo(start: LocalDate, codes: List<Int>): String {
        val times = codes.indices.joinToString(",") { "\"${start.atStartOfDay().plusHours(it.toLong())}\"" }
        val temps = codes.indices.joinToString(",") { "5.0" }
        return """{"hourly":{"time":[$times],"weather_code":[${codes.joinToString(",")}],"temperature_2m":[$temps]}}"""
    }

    @Test fun pastRainWhileTheAppWasClosedIsOnRecord() {
        val y = LocalDate.of(2026, 11, 12)
        // yesterday: rain at 3 am only; today: rain at 8 am, clear after
        val codes = List(24) { if (it == 3) 61 else 3 } + List(24) { if (it == 8) 80 else 0 }
        val days = Weather.parsePast(meteo(y, codes), LocalDateTime.of(2026, 11, 13, 20, 30))
        assertTrue(days.getValue(y).rain)
        assertTrue(days.getValue(y.plusDays(1)).rain)
    }

    @Test fun hoursStillAheadDontCount() {
        val d = LocalDate.of(2026, 11, 13)
        val codes = List(24) { if (it == 22) 63 else 3 }
        assertFalse(Weather.parsePast(meteo(d, codes), LocalDateTime.of(2026, 11, 13, 9, 0)).getValue(d).rain)
    }

    @Test fun snowIsSnow() {
        val d = LocalDate.of(2026, 12, 1)
        val s = Weather.parsePast(meteo(d, List(24) { 73 }), LocalDateTime.of(2026, 12, 2, 0, 0)).getValue(d)
        assertTrue(s.snow); assertFalse(s.rain); assertEquals(5.0, s.temp, 1e-9)
    }

    @Test fun forecastRainAtNightCounts() {
        val zone: ZoneId = ZoneOffset.UTC
        val d = LocalDate.of(2026, 11, 13)
        val json = """{"properties":{"timeseries":[
            {"time":"2026-11-13T22:00:00Z","data":{"instant":{"details":{"air_temperature":4.0}},"next_1_hours":{"summary":{"symbol_code":"lightrain"}}}},
            {"time":"2026-11-13T23:00:00Z","data":{"instant":{"details":{"air_temperature":4.0}},"next_1_hours":{"summary":{"symbol_code":"cloudy"}}}}
        ]}}"""
        val f = Weather.parse(json, d, zone, d.atTime(21, 30).toInstant(ZoneOffset.UTC).toEpochMilli())
        assertTrue(f.today.rain)
    }
}
