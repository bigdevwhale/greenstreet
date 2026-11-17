package app.greenstreet

import app.greenstreet.data.District
import app.greenstreet.data.Green
import app.greenstreet.data.Poly
import app.greenstreet.data.Road
import app.greenstreet.domain.K
import app.greenstreet.domain.Terrain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** With no water near home the game digs ponds to fish in (#31, #14); with water about it leaves the map be. */
class PondTest {
    // ~1 km square, home in the middle; a park east of home crossed by a path, streets round it
    private fun district(water: List<Poly>): District {
        val roads = listOf(
            Road("footway", floatArrayOf(20f, 312f, 600f, 312f), false, false, "", ""),
            Road("footway", floatArrayOf(312f, 20f, 312f, 600f), false, false, "", ""),
            Road("path", floatArrayOf(400f, 250f, 560f, 250f), false, false, "", ""),
        )
        val park = Green("park", Poly(listOf(floatArrayOf(380f, 180f, 580f, 180f, 580f, 300f, 380f, 300f))))
        return District(59.95, 30.30, 59.959, 30.318, 1.6, emptyList(), roads, water, listOf(park), emptyList(), emptyList(), emptyList())
    }

    @Test
    fun digsPondsWhereThereIsNoWater() {
        val t = Terrain(district(emptyList()))
        assertTrue("ponds dug", t.ponds.isNotEmpty())
        assertTrue("one of them a lake", t.ponds.any { it.big })
        for (p in t.ponds) {
            assertEquals(K.WATER, t.at(p.x, p.y))
            // somewhere public to stand within reach of the rod (the map's 6-pixel check)
            val bank = (-p.ry - 6..p.ry + 6).any { j -> (-p.rx - 6..p.rx + 6).any { i -> t.open[(p.y + j) * t.w + p.x + i] && t.nearWater(p.x + i, p.y + j, 6) } }
            assertTrue("a bank to fish from", bank)
        }
    }

    @Test
    fun leavesRealWaterAlone() {
        val lake = Poly(listOf(floatArrayOf(150f, 150f, 250f, 150f, 250f, 250f, 150f, 250f)))
        assertTrue(Terrain(district(listOf(lake))).ponds.isEmpty())
    }
}
