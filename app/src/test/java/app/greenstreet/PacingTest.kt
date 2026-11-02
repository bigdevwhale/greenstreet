package app.greenstreet

import app.greenstreet.data.Content
import app.greenstreet.domain.Game
import app.greenstreet.domain.Geo
import app.greenstreet.domain.Now
import app.greenstreet.domain.Sky
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Plays the content for four years with a keen player — visits everyone daily, brings what is asked —
 * and checks that the stories last: requests and notes must keep coming into the third year.
 */
class PacingTest {
    private val dir = File("src/main/assets/content")
    private val content = Content.read({ p -> File(dir, p).takeIf { it.exists() }?.inputStream() })
    private val game = Game(content)

    @Test
    fun storiesLastThreeYears() {
        if (content.people.isEmpty() || content.requests.values.all { it.isEmpty() }) return
        val start = LocalDate.of(2026, 9, 1)
        val s = game.newGame(Now(start), Geo(59.95, 30.31))
        val lastReq = HashMap<String, Long>()
        val lastNote = HashMap<String, Long>()
        val byYear = HashMap<String, IntArray>()
        var seen = 0
        for (d in 0 until 365 * 4) {
            val date = start.plusDays(d.toLong())
            val m = date.monthValue
            val sky = Sky(rain = d % 4 == 0, frost = m in listOf(11, 12, 1, 2, 3) && d % 3 == 0, snow = m in listOf(12, 1, 2) && d % 5 == 0,
                temp = when (m) { 12, 1, 2 -> -6.0; 3, 11 -> 2.0; 4, 10 -> 8.0; else -> 16.0 })
            val now = Now(date, 12, sky)
            game.catchUp(s, now) { sky to sky }
            for (p in content.people) {
                // a keen player opens a neighbour in the first weeks, one every few days
                if (s.bonds[p.id]?.isOpen != true && d < content.people.indexOf(p) * 4) continue
                game.visit(s, now, p.id)
                if (d % 7 == content.people.indexOf(p) % 7) {
                    val love = p.loves.firstOrNull { content.good[it] != null || content.products.contains(it) || content.variety[it] != null }
                    if (love != null) { s.add(s.bag, love, 1); game.gift(s, now, p.id, love) }
                }
                val r = game.request(s, p.id)
                if (r != null && now.day - (s.bonds[p.id]!!.reqShown) >= 3) {
                    r.needs.forEach { n -> s.add(s.bag, concrete(n.key), n.qty) }
                    if (game.fulfil(s, now, p.id) != null) {
                        lastReq[p.id] = d.toLong()
                        byYear.getOrPut(p.id) { IntArray(8) }[(d / 365).coerceAtMost(3)]++
                    }
                }
            }
            s.inbox.drop(seen).forEach { mail ->
                val id = mail.key.substringBefore(':'); lastNote[id] = d.toLong()
                byYear.getOrPut(id) { IntArray(8) }[4 + (d / 365).coerceAtMost(3)]++
            }
            seen = s.inbox.size
        }
        println("who          requests y1/y2/y3/y4   notes y1/y2/y3/y4   last req day, last note day")
        var short = 0
        for (p in content.people) {
            val y = byYear[p.id] ?: IntArray(8)
            val total = content.requests[p.id]?.size ?: 0
            val notes = content.notes[p.id]?.size ?: 0
            println("%-12s %3d/%3d/%3d/%3d of %3d   %3d/%3d/%3d/%3d of %3d   %4d %4d".format(p.id, y[0], y[1], y[2], y[3], total, y[4], y[5], y[6], y[7], notes,
                lastReq[p.id] ?: -1, lastNote[p.id] ?: -1))
            if (total + notes > 0 && maxOf(lastReq[p.id] ?: 0, lastNote[p.id] ?: 0) < 365 * 2 + 200) short++
        }
        for (p in content.people) {
            val b = s.bonds[p.id] ?: continue
            val n = content.notes[p.id]?.getOrNull(b.note) ?: continue
            println("stuck note ${p.id}:${n.n} gap=${n.gap} months=${n.months} hearts=${n.hearts}/${game.hearts(b.points)} when=${n.`when`.tokens} needs=${n.flags} missing=${n.flags.filter { !game.flag(s, Now(start.plusDays(1459)), it) }}")
        }
        assertTrue("$short characters run out of story before the end of year three", short == 0)
    }

    /** A real bag key for a need: a variety for a species, a member for a group. */
    private fun concrete(k: String): String = content.crops[k]?.first()?.key ?: content.groups[k]?.firstOrNull()?.let { concrete(it) } ?: k
}
