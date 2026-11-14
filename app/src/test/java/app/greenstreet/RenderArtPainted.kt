package app.greenstreet

import app.greenstreet.domain.Sky
import app.greenstreet.ui.art.Look
import app.greenstreet.ui.art.Moment
import app.greenstreet.ui.art.Pictures
import app.greenstreet.ui.art.YardLife
import app.greenstreet.ui.art.YardScene
import app.greenstreet.ui.art.placeScene
import app.greenstreet.ui.art.bed
import org.junit.Test
import java.io.File

/** Draws the painted scenes (assets/art) to app/build/art/painted (run with -Prender). */
class RenderArtPainted {
    private val out = File(System.getProperty("render.out") ?: "build/art", "painted").also { it.mkdirs() }

    @Test
    fun painted() {
        val assets = File("src/main/assets")
        Pictures.open = { p -> File(assets, p).takeIf { it.exists() }?.inputStream() }
        val days = listOf(
            "day" to Moment(11 * 60, Sky(sun = true), 6, 1),
            "rain" to Moment(15 * 60, Sky(rain = true), 10, 2),
            "night" to Moment(21 * 60, Sky(), 1, 3),
            "snow" to Moment(13 * 60, Sky(snow = true), 1, 4),
        )
        val people = File(assets, "content/people.tsv").readLines().drop(1).map { it.split('\t') }
        for (p in people) {
            val (id, kind) = p[0] to p[2]
            if (kind == "yard") continue
            val look = Look.of(p[8].split(',').associate { kv -> kv.split('=').let { it[0] to (it.getOrNull(1) ?: "1") } })
            for ((n, m) in days.take(if (id == "prokhor") 4 else 1))
                Png.write(File(out, "place_${kind}_$n.png"), placeScene(kind, id, look, m) { error("no picture for $kind") }.frame(), 2)
        }
        Png.write(File(out, "stroll.png"), placeScene("stroll", "lilya", Look.of(mapOf()), days[0].second) { error("no stroll") }.frame(), 2)
        // beds on the map: every stage of one variety, then one ripe bed of each family, on grass
        run {
            val content = app.greenstreet.data.Content.read({ p -> File(assets, "content/$p").takeIf { it.exists() }?.inputStream() })
            val vs = content.variety.values.groupBy { it.art }.values.map { it.first() }
            val b = app.greenstreet.ui.art.Buf(30 * (vs.size + 6), 40)
            for (y in 0 until b.h) for (x in 0 until b.w) b.p(x, y, app.greenstreet.ui.art.rp(app.greenstreet.ui.art.R.green, .45 + app.greenstreet.ui.art.hash(x, y) * .1, x, y))
            val v0 = vs.first()
            for (k in 0 until 4) b.bed(15 + k * 30, 28, v0, v0.days * (k + .5) / 4.0, false, k == 1, false, 0)
            b.bed(15 + 4 * 30, 28, v0, 1.0, false, false, true, 0)
            vs.forEachIndexed { i, v -> b.bed(15 + (i + 6) * 30, 28, v, v.days.toDouble(), true, false, false, 0) }
            Png.write(File(out, "beds.png"), b, 4)
        }
        val pots = listOf(app.greenstreet.ui.art.Pot("herb", app.greenstreet.ui.art.R.green, true), app.greenstreet.ui.art.Pot("flower", app.greenstreet.ui.art.R.jam, true))
        for (kind in listOf("well", "block", "house")) {
            if (Pictures.yard(kind) == null) continue
            for ((n, m) in days) Png.write(File(out, "yard_${kind}_$n.png"), YardScene(m, YardLife(seed = 3, kind = kind, eggs = 3, pots = pots)).frame(), 2)
            Png.write(File(out, "yard_${kind}_full.png"), YardScene(Moment(8 * 60, Sky(sun = true), 7, 20000),
                YardLife(seed = 3, kind = kind, hive = true, garden = true, decor = setOf("lights", "birdhouse", "feeder", "tyre", "gnome", "barrel"), pots = pots)).frame(), 2)
            Png.write(File(out, "yard_${kind}_guest.png"), YardScene(Moment(19 * 60, Sky(), 7, 20400),
                YardLife(seed = 7, kind = kind, decor = setOf("lights"), guest = mapOf("skin" to "1", "hair" to "grey", "cloth" to "lilac", "kerchief" to "lilac"), guestId = "nina")).frame(), 2)
        }
    }
}
