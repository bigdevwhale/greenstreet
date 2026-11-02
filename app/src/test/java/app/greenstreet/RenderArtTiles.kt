package app.greenstreet

import app.greenstreet.data.District
import app.greenstreet.data.Tiles
import app.greenstreet.domain.Terrain
import app.greenstreet.ui.art.Buf
import app.greenstreet.ui.art.MapPainter
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Downloads real vector tiles around a point and draws them (run with -Prender; needs the network). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RenderArtTiles {
    private val out = File(System.getProperty("render.out") ?: "build/art").also { it.mkdirs() }

    @Test
    fun petrogradkaFromTiles() {
        val box = District.box(59.9572, 30.3135, 800.0)
        val t0 = System.currentTimeMillis()
        val blob = Tiles.download(box)
        val t1 = System.currentTimeMillis()
        val d = Tiles.parse(blob, box)
        val t = Terrain(d)
        val t2 = System.currentTimeMillis()
        val b = MapPainter(t, 6).paint()
        val t3 = System.currentTimeMillis()
        Png.write(File(out, "tiles_whole.png"), b, 1)
        val c = Buf(300, 260); for (j in 0 until 260) for (i in 0 until 300) c.d[j * 300 + i] = b.get(d.w / 2 - 150 + i, d.h / 2 - 130 + j)
        Png.write(File(out, "tiles_crop.png"), c, 3)
        println("tiles ${blob.size / 1024} KB in ${t1 - t0} ms; ${d.buildings.size} buildings, ${d.roads.size} roads, ${d.water.size} water, ${d.greens.size} greens, ${d.places.size} places; parse+terrain ${t2 - t1} ms; paint ${t3 - t2} ms; home=${t.homeKind(d.w / 2f, d.h / 2f)}")
        println(d.places.groupingBy { it.kind }.eachCount().entries.sortedByDescending { it.value }.take(25))
    }
}
