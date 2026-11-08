package app.greenstreet

import app.greenstreet.data.Content
import app.greenstreet.domain.Errands
import app.greenstreet.domain.Game
import app.greenstreet.domain.Geo
import app.greenstreet.domain.Now
import app.greenstreet.domain.Quests
import app.greenstreet.domain.Sources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Today's errand and where to get what the neighbours ask for. */
class ErrandsTest {
    private val dir = File("src/main/assets/content")
    private val content = Content.read({ p -> File(dir, p).takeIf { it.exists() }?.inputStream() })
    private val game = Game(content)
    private val day = LocalDate.of(2027, 6, 7)

    @Test
    fun oneErrandADayAfterTheOpening() {
        val s = game.newGame(Now(day), Geo(59.95, 30.31))
        val now = Now(day)
        s.bond("mila").open = 0; s.bond("prokhor").open = 0
        assertNull("not while the opening tasks run", Errands.today(game, s, now))
        Quests.all.forEach { s.flags += "quest.${it.key}" }
        val q = Errands.today(game, s, now)
        assertNotNull(q)
        s.bag["egg"] = 3; s.kmToday = 0.4
        assertEquals("the same all day", q!!.key, Errands.today(game, s, now)!!.key)
        // a fortnight of errands: different ones, each paid once when done
        val kinds = HashSet<String>()
        for (d in 0 until 14) {
            val n = Now(day.plusDays(d.toLong()))
            val e = Errands.today(game, s, n)!!
            kinds += e.key
            assertNull("not paid before it's done", Errands.settle(game, s, n).takeIf { e.progress(s) < e.goal })
            // do it the blunt way: everything an errand can ask for
            s.kmToday = 3.0; s.counts["tend_today"] = 9; s.counts["finds_today"] = 3
            s.counts["harvest_day"] = n.day.toInt(); s.counts["request_day"] = n.day.toInt()
            s.bonds.values.forEach { it.visited = n.day; it.gifted = n.day }
            val coins = s.coins
            assertNotNull("${e.key} pays", Errands.settle(game, s, n))
            assertTrue(s.coins > coins)
            assertNull("only once", Errands.settle(game, s, n))
            s.kmToday = 0.0; s.counts.remove("tend_today"); s.counts.remove("finds_today")
        }
        println("errands over a fortnight: $kinds")
        assertTrue("errands vary", kinds.size >= 3)
    }

    @Test
    fun everyNeedHasAHint() {
        val s = game.newGame(Now(day), Geo(59.95, 30.31))
        val now = Now(day)
        val seen = HashMap<String, Int>()
        for ((_, list) in content.requests) for (r in list) for (n in r.needs) {
            val h = Sources.hint(game, s, now, n)
            assertTrue("${n.key}: ${h.ru}", h.ru.isNotBlank() && h.en.isNotBlank())
            seen.merge(h.en.substringBefore(',').substringBefore(" (").take(28), 1, Int::plus)
        }
        println("hints: " + seen.entries.sortedByDescending { it.value }.joinToString { "${it.key}: ${it.value}" })
        assertTrue("almost every need has a real source", (seen["seeds come from requests "] ?: 0) < 20)
    }
}
