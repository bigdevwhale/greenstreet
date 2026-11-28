package app.greenstreet

import app.greenstreet.domain.Fixes
import app.greenstreet.domain.Fixes.Use
import app.greenstreet.domain.Geo
import org.junit.Assert.assertEquals
import org.junit.Test

/** #58, #60: cell towers and lone far fixes don't throw the player about. */
class FixesTest {
    private val home = Geo(59.95, 30.31)
    /** [m] metres north of home. */
    private fun north(m: Double) = Geo(home.lat + m / 111_320.0, home.lon)

    @Test
    fun aCellTowerWaitsForTheSatellitesToFallSilent() {
        val f = Fixes(WEAK_GPS)
        assertEquals(Use.WALK, f.judge(home, 8f, 0))
        // a kilometre off and 900 m sure, between two satellite fixes
        assertEquals(Use.DROP, f.judge(north(1000.0), 900f, 5_000))
        assertEquals(Use.WALK, f.judge(north(5.0), 8f, 10_000))
        // the satellites quiet for half a minute: now it may move the player, but never walks
        assertEquals(Use.SHOW, f.judge(north(1000.0), 900f, 45_000))
        // a worse guess outside the shown circle doesn't toss the player about…
        assertEquals(Use.DROP, f.judge(north(3000.0), 1500f, 50_000))
        // …until the shown one is stale
        assertEquals(Use.SHOW, f.judge(north(3000.0), 1500f, 61_000))
    }

    @Test
    fun aLoneFarFixIsDroppedThreeInARowAreARealMove() {
        val f = Fixes(WEAK_GPS)
        assertEquals(Use.WALK, f.judge(home, 10f, 0))
        assertEquals(Use.WALK, f.judge(north(10.0), 10f, 4_000))
        // 600 m in four seconds
        assertEquals(Use.DROP, f.judge(north(600.0), 10f, 8_000))
        assertEquals(Use.WALK, f.judge(north(15.0), 10f, 12_000))
        assertEquals(Use.DROP, f.judge(north(800.0), 10f, 16_000))
        assertEquals(Use.DROP, f.judge(north(802.0), 10f, 20_000))
        assertEquals(Use.JUMP, f.judge(north(805.0), 10f, 24_000))
        assertEquals(Use.WALK, f.judge(north(810.0), 10f, 28_000))
    }

    @Test
    fun aRememberedFixComingLateIsDropped() {
        val f = Fixes(WEAK_GPS)
        assertEquals(Use.WALK, f.judge(home, 10f, 100_000))
        assertEquals(Use.DROP, f.judge(north(50.0), 10f, 40_000))
    }

    @Test
    fun aWalkAfterAGapCounts() {
        val f = Fixes(WEAK_GPS)
        assertEquals(Use.WALK, f.judge(home, 10f, 0))
        // GPS lost under the arches for two minutes, 150 m further on
        assertEquals(Use.WALK, f.judge(north(150.0), 10f, 120_000))
    }
}
