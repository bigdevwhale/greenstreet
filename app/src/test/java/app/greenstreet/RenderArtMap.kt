package app.greenstreet

import app.greenstreet.data.District
import app.greenstreet.domain.Terrain
import app.greenstreet.ui.art.Buf
import app.greenstreet.ui.art.MapPainter
import app.greenstreet.ui.art.bed
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
/** Draws the Petrogradka test district to app/build/art (run with -Prender). */
class RenderArtMap {
    private val out = File(System.getProperty("render.out") ?: "build/art").also { it.mkdirs() }

    @Test
    fun petrogradka() {
        val json = javaClass.classLoader!!.getResource("petrogradka.osm.json")!!.readText()
        val t0 = System.currentTimeMillis()
        val d = District.parse(json, doubleArrayOf(59.9455, 30.3040, 59.9585, 30.3260))
        val t = Terrain(d)
        val t1 = System.currentTimeMillis()
        for (m in listOf(6, 10, 1)) {
            val b = MapPainter(t, m).paint()
            Png.write(File(out, "map_$m.png"), crop(b, 180, 20, 300, 260), 3)
            if (m == 6) Png.write(File(out, "map_whole.png"), b, 1)
        }
        println("district ${d.w}x${d.h}, ${d.buildings.size} buildings, terrain ${t1 - t0} ms, paint ${System.currentTimeMillis() - t1} ms for 3 months")
    }

    @Test
    fun beds() {
        val json = javaClass.classLoader!!.getResource("petrogradka.osm.json")!!.readText()
        val d = District.parse(json, doubleArrayOf(59.9455, 30.3040, 59.9585, 30.3260))
        val t = Terrain(d)
        val base = MapPainter(t, 7).paint()
        val x0 = 240; val y0 = 80; val w = 128; val h = 160
        val v = crop(base, x0, y0, w, h)
        val arts = listOf("flower" to "autumn", "root" to "jam", "tall" to "honey", "round" to "apple", "herb" to "green", "grain" to "gold", "bush" to "jam", "spike" to "lilac")
        var k = 0
        val used = ArrayList<IntArray>()
        for (j in 8 until h - 6 step 3) for (i in 6 until w - 6 step 3) {
            if (k >= 12 || !t.verge(x0 + i, y0 + j) || used.any { Math.hypot((it[0] - i).toDouble(), (it[1] - j).toDouble()) < 18 }) continue
            used += intArrayOf(i, j)
            val (art, ramp) = arts[k % arts.size]
            val va = app.greenstreet.data.Variety("v$k", "c", app.greenstreet.data.Place.VERGE, setOf(5), 20, 3, 0, 1, 1, art, ramp, "", app.greenstreet.data.Tx("", ""), app.greenstreet.data.Tx("", ""))
            val grown = listOf(0.0, 6.0, 11.0, 16.0, 20.0)[k % 5]
            v.bed(i, j, va, grown, k % 5 == 4, k % 3 == 0, k == 7, 0)
            k++
        }
        Png.write(File(out, "beds_map.png"), v, 6)
    }

    private fun crop(b: Buf, x: Int, y: Int, w: Int, h: Int): Buf {
        val o = Buf(w, h)
        for (j in 0 until h) for (i in 0 until w) o.d[j * w + i] = b.get(x + i, y + j)
        return o
    }
}
