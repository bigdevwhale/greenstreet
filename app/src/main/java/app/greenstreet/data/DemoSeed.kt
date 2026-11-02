package app.greenstreet.data

import app.greenstreet.AppContainer
import app.greenstreet.domain.Geo
import app.greenstreet.domain.Now
import java.time.LocalDate

/**
 * The demo build (-Pdemo): home on Petrogradka, St Petersburg, a district bundled in the APK, a few weeks of play
 * already behind — beds along the way to the station, neighbours met, a request or two waiting.
 */
object DemoSeed {
    val HOME = Geo(59.9572, 30.3135)
    /** The box of the bundled Overpass dump. */
    val BOX = doubleArrayOf(59.9455, 30.3040, 59.9585, 30.3260)

    fun seedIfEmpty(c: AppContainer) {
        if (c.state != null) return
        val json = runCatching { c.app.assets.open("demo/petrogradka.osm.json").bufferedReader().use { it.readText() } }.getOrNull() ?: return
        // the bundled dump covers a fixed box; store it as if downloaded around home
        c.store.saveDistrict(json)
        val start = LocalDate.now().minusDays(24)
        val s = c.game.newGame(Now(start), HOME)
        s.coins = 64
        c.adopt(s, BOX)
        val g = c.game
        val now = c.now()
        for (id in listOf("prokhor", "agafya", "kuzmich", "zina", "semyonych", "nina", "asya", "mila", "sych")) {
            g.visit(s, Now(start.plusDays(2)), id)
            s.bond(id).points = listOf(25, 48, 14, 30, 60, 52, 40, 9, 20)[listOf("prokhor", "agafya", "kuzmich", "zina", "semyonych", "nina", "asya", "mila", "sych").indexOf(id)]
        }
        listOf("radish_breakfast", "rye_winter", "calendula_orange", "chamomile_pharmacy", "onion_sets", "dill_umbrella").forEach { k -> if (c.content.variety[k] != null) s.seeds[k] = 3 }
        listOf("egg" to 4, "rowan" to 3, "roach" to 2, "honey_fungus" to 3, "bread_rye" to 1).forEach { (k, n) -> s.bag[k] = n }
        // a few beds on the verges near home, at different stages
        val t = c.terrain
        val d = c.district
        if (t != null && d != null) {
            val hx = d.x(HOME.lon).toInt(); val hy = d.y(HOME.lat).toInt()
            val kinds = listOf("radish_breakfast" to 1.0, "rye_winter" to .5, "calendula_orange" to .7, "chamomile_pharmacy" to .3, "dill_umbrella" to 1.0)
            var k = 0
            for (r in 6..60 step 3) for (a in 0 until 24) {
                if (k >= kinds.size) break
                val x = hx + (r * Math.cos(a * Math.PI / 12)).toInt(); val y = hy + (r * Math.sin(a * Math.PI / 12)).toInt()
                if (!t.verge(x, y)) continue
                val g = Geo(d.lat(y + .5f), d.lon(x + .5f))
                if (s.beds.any { b -> Math.hypot((d.x(b.lon) - x).toDouble(), (d.y(b.lat) - y).toDouble()) < 8 }) continue
                val v = c.content.variety[kinds[k].first] ?: run { k++; null } ?: continue
                s.beds += app.greenstreet.domain.Bed(s.nextId++, v.key, g.lat, g.lon, now.day - 20, grown = v.days * kinds[k].second, tended = (v.tends * kinds[k].second).toInt() + 1, lastTend = now.day - 1)
                k++
            }
        }
        c.here.value = HOME
        c.changed()
    }
}
