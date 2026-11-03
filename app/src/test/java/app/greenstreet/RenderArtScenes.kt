package app.greenstreet

import app.greenstreet.domain.Sky
import app.greenstreet.ui.art.FacadeScene
import app.greenstreet.ui.art.FishScene
import app.greenstreet.ui.art.G
import app.greenstreet.ui.art.Look
import app.greenstreet.ui.art.Moment
import app.greenstreet.ui.art.R
import app.greenstreet.ui.art.YardScene
import org.junit.Test
import java.io.File

/** Draws the side-view scenes to app/build/art (run with -Prender). */
class RenderArtScenes {
    private val out = File(System.getProperty("render.out") ?: "build/art").also { it.mkdirs() }
    private val bread = intArrayOf(0,1,1,1,0, 1,1,1,1,1, 1,0,1,0,1, 1,1,1,1,1, 0,0,0,0,0)

    @Test
    fun scenes() {
        val baker = Look.of(mapOf("skin" to "2", "hair" to "wood", "cloth" to "cream", "hat" to "toque", "beard" to "1", "apron" to "1"))
        val days = listOf(
            "day" to Moment(11 * 60, Sky(sun = true), 6, 1),
            "rain" to Moment(15 * 60, Sky(rain = true), 10, 2),
            "night" to Moment(21 * 60, Sky(), 1, 3),
            "snow" to Moment(13 * 60, Sky(snow = true), 1, 4),
        )
        for ((n, m) in days) {
            Png.write(File(out, "facade_$n.png"), FacadeScene("bakery", bread, G.ochre, G.plum, R.red, baker, m, true).frame(), 3)
            Png.write(File(out, "yard_$n.png"), YardScene(m).frame(), 3)
            Png.write(File(out, "fish_$n.png"), FishScene(m, true, "wait").frame(), 3)
        }
        // the courtyard through the years: morning and evening, year one to three, summer and New Year
        val pots = listOf(app.greenstreet.ui.art.Pot("herb", R.green, true), app.greenstreet.ui.art.Pot("flower", R.jam, true))
        val cases = listOf(
            "y1_morning" to (Moment(8 * 60, Sky(sun = true), 6, 20000) to app.greenstreet.ui.art.YardLife(seed = 3, weekday = 2, pots = pots)),
            "y2_evening" to (Moment(19 * 60, Sky(), 7, 20400) to app.greenstreet.ui.art.YardLife(fac = G.mint, seed = 7, year = 2, hive = true, balcony = true, weekday = 6, flags = setOf("gosha.lilya"), pots = pots)),
            "y3_day" to (Moment(16 * 60, Sky(sun = true), 8, 20800) to app.greenstreet.ui.art.YardLife(fac = G.salmon, seed = 11, year = 3, hive = true, balcony = true, garden = true, weekday = 7)),
            "newyear" to (Moment(17 * 60 + 40, Sky(snow = true), 12, java.time.LocalDate.of(2026, 12, 30).toEpochDay()) to app.greenstreet.ui.art.YardLife(seed = 5, weekday = 3)),
            "block_day" to (Moment(16 * 60, Sky(sun = true), 6, 20003) to app.greenstreet.ui.art.YardLife(fac = G.sky2, seed = 4, weekday = 6, kind = "block", pots = pots)),
            "block_night" to (Moment(21 * 60, Sky(), 10, 20103) to app.greenstreet.ui.art.YardLife(fac = G.salmon, seed = 8, year = 2, balcony = true, hive = true, kind = "block")),
            "guest_semyonych" to (Moment(19 * 60, Sky(), 7, 20400) to app.greenstreet.ui.art.YardLife(seed = 7, weekday = 6, guest = mapOf("skin" to "2", "hair" to "grey", "cloth" to "oak", "cap" to "1", "moustache" to "1"))),
            "guest_cat" to (Moment(12 * 60, Sky(sun = true), 7, 20400) to app.greenstreet.ui.art.YardLife(seed = 7, kind = "block", guest = mapOf("cat" to "ginger"))),
            "guest_house" to (Moment(9 * 60, Sky(sun = true), 7, 20005) to app.greenstreet.ui.art.YardLife(seed = 2, kind = "house", guest = mapOf("skin" to "1", "hair" to "dark", "cloth" to "navy", "glasses" to "1"))),
            "decor_well_eve" to (Moment(20 * 60, Sky(), 9, 20400) to app.greenstreet.ui.art.YardLife(seed = 7, weekday = 2, decor = setOf("lights", "birdhouse", "feeder", "tyre", "gnome", "barrel"))),
            "decor_block_day" to (Moment(12 * 60, Sky(sun = true), 7, 20400) to app.greenstreet.ui.art.YardLife(seed = 4, kind = "block", decor = setOf("lights", "birdhouse", "feeder", "tyre", "gnome", "barrel"))),
            "decor_house_winter" to (Moment(16 * 60 + 30, Sky(snow = true), 1, 20480) to app.greenstreet.ui.art.YardLife(seed = 2, kind = "house", decor = setOf("lights", "birdhouse", "feeder", "tyre", "gnome", "barrel"))),
            "house_summer" to (Moment(9 * 60, Sky(sun = true), 7, 20005) to app.greenstreet.ui.art.YardLife(seed = 2, weekday = 6, kind = "house", pots = pots)),
            "house_winter" to (Moment(16 * 60 + 30, Sky(snow = true), 1, java.time.LocalDate.of(2027, 1, 20).toEpochDay()) to app.greenstreet.ui.art.YardLife(seed = 5, year = 2, hive = true, balcony = true, kind = "house")),
            "house_y3" to (Moment(12 * 60, Sky(), 9, 20900) to app.greenstreet.ui.art.YardLife(seed = 1, year = 3, hive = true, balcony = true, garden = true, kind = "house")),
            "autumn" to (Moment(11 * 60, Sky(), 10, 20100) to app.greenstreet.ui.art.YardLife(fac = G.sky2, seed = 2, weekday = 5)),
        )
        for ((n, p) in cases) Png.write(File(out, "yard2_$n.png"), YardScene(p.first, p.second).frame(), 3)
    }
}
