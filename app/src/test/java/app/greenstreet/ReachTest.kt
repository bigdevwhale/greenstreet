package app.greenstreet

import app.greenstreet.data.Content
import app.greenstreet.data.District
import app.greenstreet.data.Poly
import app.greenstreet.data.Road
import app.greenstreet.data.Spot
import app.greenstreet.domain.Bed
import app.greenstreet.domain.Game
import app.greenstreet.domain.Geo
import app.greenstreet.domain.Now
import app.greenstreet.domain.Sky
import app.greenstreet.domain.Terrain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/** Neighbours stay where the public can walk (issue #2); a bed says plainly when it ripens and whether it's watered (#3). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReachTest {
    private fun district(roads: List<Road>, closed: List<Poly> = emptyList(), places: List<Spot> = emptyList()) =
        District(59.95, 30.30, 59.9518, 30.3036, 1.6, emptyList(), roads, emptyList(), emptyList(), emptyList(), emptyList(), places, closed)

    private fun square(x0: Float, y0: Float, x1: Float, y1: Float) = Poly(listOf(floatArrayOf(x0, y0, x1, y0, x1, y1, x0, y1)))

    @Test
    fun fencedGroundIsOffTheWays() {
        // a public street along the top; a guarded yard below it with its own driveway and a private lane
        val street = Road("footway", floatArrayOf(5f, 20f, 115f, 20f), false, false, "", "")
        val inside = Road("footway", floatArrayOf(60f, 70f, 100f, 70f), false, false, "", "")
        val lane = Road("service", floatArrayOf(30f, 20f, 30f, 100f), false, false, "", "", closed = true)
        val t = Terrain(district(listOf(street, inside, lane), closed = listOf(square(50f, 50f, 110f, 110f))))
        assertTrue(t.open[20 * t.w + 40])
        assertFalse("the yard's paths are shut", t.open[70 * t.w + 80])
        assertFalse("the private lane is shut", t.open[60 * t.w + 30])
        assertTrue("the street crossing the lane stays open", t.open[20 * t.w + 30])
        assertTrue(t.reachable(40, 25, 12))
        assertFalse("a shop in the yard can't be reached", t.reachable(80, 80, 12))
        val out = t.nearestOpen(80, 80, 400)
        assertNotNull(out)
        assertEquals("its keeper waits on the street", 20f, out!![1].toFloat(), 2f)
    }

    @Test
    fun petrogradkaPlacesCanBeReached() {
        val json = javaClass.classLoader!!.getResource("petrogradka.osm.json")!!.readText()
        val d = District.parse(json, doubleArrayOf(59.9455, 30.3040, 59.9585, 30.3260))
        val t = Terrain(d)
        assertTrue("the public ways cover the district", t.open.count { it } > t.n / 20)
        val fenced = d.places.filter { !t.reachable(it.x.toInt(), it.y.toInt(), 12) }
        println("public ways ${t.open.count { it } * 100 / t.n}% of the district; places off the ways: ${fenced.size} of ${d.places.size}: " +
            fenced.joinToString { "${it.kind} '${it.name}'" })
        for (p in d.places) {
            val ok = t.reachable(p.x.toInt(), p.y.toInt(), 12) || t.nearestOpen(p.x.toInt(), p.y.toInt(), 400)?.let { t.reachable(it[0], it[1], 0) } == true
            assertTrue("${p.kind} ${p.name} at ${p.x},${p.y}", ok)
        }
    }

    private val dir = File("src/main/assets/content")
    private val content = Content.read({ p -> File(dir, p).takeIf { it.exists() }?.inputStream() })
    private val game = Game(content)

    @Test
    fun bedOutlook() {
        val v = content.variety.getValue("radish_breakfast")   // 25 real days, 15 waterings: 7 and 4 in the game
        assertEquals(7, v.days); assertEquals(4, v.tends)
        val day = LocalDate.of(2027, 6, 10)
        val s = game.newGame(Now(day), Geo(59.95, 30.31))
        val b = Bed(1, v.key, 59.95, 30.31, day.toEpochDay() - 10, grown = 3.0, tended = 3, lastTend = day.toEpochDay() - 1, idle = 1)
        s.beds += b
        var o = game.outlook(s, b, Now(day, 12, Sky()))
        assertEquals(4, o.nights)
        assertFalse(o.watered); assertFalse(o.rain)
        assertEquals("two more dry nights and it goes wild", 2, o.wildIn)
        o = game.outlook(s, b, Now(day, 12, Sky(rain = true)))
        assertTrue("rain waters it", o.rain)
        b.lastTend = day.toEpochDay()
        assertTrue(game.outlook(s, b, Now(day)).watered)
        // in March a tended night gives only 0.6 of a day
        assertEquals(7, game.outlook(s, b, Now(LocalDate.of(2027, 3, 10), 12, Sky(temp = 5.0))).nights)
        // outdoors in the cold it grows at half pace
        assertEquals(8, game.outlook(s, b, Now(LocalDate.of(2027, 1, 10), 12, Sky(temp = -5.0))).nights)
        // enough growth but too few waterings: the waterings decide
        b.grown = 7.0; b.tended = 2
        assertEquals(2, game.outlook(s, b, Now(day)).nights)
        b.tended = 4
        assertEquals(0, game.outlook(s, b, Now(day)).nights)
    }

    @Test
    fun walkingByANewBedCountsOnDayOne() {
        val day = LocalDate.of(2027, 6, 10)
        val at = Geo(59.95, 30.31)
        val s = game.newGame(Now(day), at)
        val v = content.variety.getValue("radish_breakfast")
        s.seeds[v.key] = 1
        val b = game.sow(s, Now(day), v.key, at)!!
        assertTrue("sowing waters it", game.outlook(s, b, Now(day)).watered)
        assertEquals(null, s.counts["tend"])
        assertEquals("walking by counts on the sowing day", 1, game.pass(s, Now(day), at).size)
        assertEquals("Aunt Klava's 'Water on the way' moves on the sowing day", 1, s.counts["tend"])
        assertEquals("but only once a day", 0, game.pass(s, Now(day), at).size)
        val back = app.greenstreet.domain.GameState.fromJson(s.toJson()).beds.single()
        assertEquals("the walk past is saved", day.toEpochDay(), back.passed)
    }

    @Test
    fun winterSowing() {
        val jan = LocalDate.of(2027, 1, 12)
        val cold = Sky(snow = true, temp = -6.0)
        val s = game.newGame(Now(jan), Geo(59.95, 30.31))
        val v = content.variety.getValue("radish_breakfast")   // best sown April to August
        s.seeds[v.key] = 1
        assertTrue("any month will do", game.canSow(s, Now(jan, 12, cold), v))
        val b = game.sow(s, Now(jan, 12, cold), v.key, Geo(59.95, 30.31))!!
        game.catchUp(s, Now(jan.plusDays(4), 12, cold)) { cold to cold }
        assertEquals("snow waters it, the cold halves the pace", 2.0, b.grown, 1e-9)
        // there is always something for the verge at the market, even in deep winter
        for (m in 1..12) assertTrue("month $m", game.seasonal(m, setOf("market")).isNotEmpty())
        assertTrue(game.market(Now(jan)).any { (k, _) -> content.variety[k]?.place == app.greenstreet.data.Place.VERGE })
    }
}
