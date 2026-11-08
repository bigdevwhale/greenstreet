package app.greenstreet

import app.greenstreet.data.Content
import app.greenstreet.domain.Game
import app.greenstreet.domain.Geo
import app.greenstreet.domain.Now
import app.greenstreet.domain.Schedule
import app.greenstreet.domain.Whereabouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/** The neighbours' days, the first-meeting texts and notebook, and Mikhalych's shop. */
class ScheduleTest {
    private val dir = File("src/main/assets/content")
    private val content = Content.read({ p -> File(dir, p).takeIf { it.exists() }?.inputStream() })
    private val game = Game(content)

    @Test
    fun days() {
        val prokhor = content.person.getValue("prokhor")   // 7–19, off on Mondays
        val mon = LocalDateTime.of(2026, 11, 2, 12, 0)       // a Monday
        assertEquals(Whereabouts.OUT, Schedule.status(prokhor, mon))
        assertEquals(Whereabouts.WORK, Schedule.status(prokhor, mon.plusDays(1)))
        assertEquals(Whereabouts.OUT, Schedule.status(prokhor, mon.plusDays(1).withHour(20)))
        assertEquals(Whereabouts.ASLEEP, Schedule.status(prokhor, mon.withHour(23)))
        assertEquals(Whereabouts.ASLEEP, Schedule.status(prokhor, mon.withHour(3)))
        assertEquals(Whereabouts.WORK, Schedule.status(content.person.getValue("pryanik"), mon.withHour(3)))
        assertEquals(Whereabouts.WORK, Schedule.status(content.person.getValue("semyonych"), mon.withHour(20)))
        assertEquals(Whereabouts.ASLEEP, Schedule.status(content.person.getValue("semyonych"), mon.withHour(23)))
        assertEquals(LocalDateTime.of(2026, 11, 3, 7, 0), Schedule.nextOpen(prokhor, mon))
        // every town neighbour works some hours on most days, and everyone sleeps at three in the morning but the cat
        for (p in content.people.filter { !it.yard }) {
            val week = (0 until 7).count { d -> (0 until 24).any { h -> Schedule.status(p, mon.plusDays(d.toLong()).withHour(h)) == Whereabouts.WORK } }
            assertTrue("${p.id} works", week >= 5)
            assertEquals("${p.id} sleeps", Whereabouts.ASLEEP, Schedule.status(p, mon.withHour(3)))
        }
    }

    @Test
    fun acquaintance() {
        for (p in content.people) {
            assertTrue("${p.id} intro", p.intro.ru.isNotBlank() && p.intro.en.isNotBlank())
            assertTrue("${p.id} perk", p.perk.ru.isNotBlank())
            assertEquals("${p.id} facts", listOf(1, 2, 4, 6, 8), p.facts.map { it.hearts })
        }
    }

    @Test
    fun shop() {
        val s = game.newGame(Now(LocalDate.of(2027, 5, 3)), Geo(59.95, 30.31))
        s.coins = 500
        val now = Now(LocalDate.of(2027, 5, 3))
        assertEquals("", game.buyWare(s, now, "gnome"))
        assertEquals(null, game.buyWare(s, now, "gnome"))
        assertTrue(game.buyWare(s, now, "surprise")!!.isNotEmpty())
        game.buyWare(s, now, "feed")
        val v = content.varieties.first { it.place == app.greenstreet.data.Place.VERGE && 5 in it.sow }
        s.add(s.seeds, v.key, 1)
        val b = game.sow(s, now, v.key, Geo(59.95, 30.31))!!
        assertTrue(game.feed(s, b.id))
        assertEquals(1.0, b.grown, 0.0)
        assertEquals(0, game.stock(s, "feed"))
        b.wild = true
        assertTrue(!game.weed(s, b.id))
        game.buyWare(s, now, "hoe")
        assertTrue(game.weed(s, b.id) && !b.wild)
    }
}
