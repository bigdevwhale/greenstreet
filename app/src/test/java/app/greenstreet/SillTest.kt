package app.greenstreet

import app.greenstreet.data.Content
import app.greenstreet.domain.Game
import app.greenstreet.domain.Geo
import app.greenstreet.domain.Now
import app.greenstreet.domain.Sky
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** #59: pots watered on opening the courtyard before last night has run (it waits for the weather) still grow. */
class SillTest {
    private val dir = File("src/main/assets/content")
    private val content = Content.read({ p -> File(dir, p).takeIf { it.exists() }?.inputStream() })
    private val game = Game(content)

    @Test
    fun potsWateredBeforeTheNightRunsStillGrow() {
        val start = LocalDate.of(2026, 11, 20)
        val s = game.newGame(Now(start), Geo(59.95, 30.31))
        s.seeds["microgreens_pea"] = 3
        game.catchUp(s, Now(start)) { Sky() to Sky() }
        val bed = game.sow(s, Now(start), "microgreens_pea", Geo(59.95, 30.31))!!
        var ripeOn: LocalDate? = null
        for (d in 1 until 10) {
            val now = Now(start.plusDays(d.toLong()), 9)
            // the courtyard opens first, the missed night runs a few seconds later
            game.tendHome(s, now)
            game.catchUp(s, now) { Sky() to Sky() }
            if (ripeOn == null && game.ripe(bed)) ripeOn = now.date
        }
        // five nights at November's pace, as when the night runs first
        assertEquals(start.plusDays(5), ripeOn)
    }
}
