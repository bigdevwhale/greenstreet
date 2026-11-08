package app.greenstreet

import app.greenstreet.data.Content
import app.greenstreet.data.Place
import app.greenstreet.domain.Game
import app.greenstreet.domain.Geo
import app.greenstreet.domain.Now
import app.greenstreet.domain.Quests
import app.greenstreet.domain.Sky
import app.greenstreet.domain.metres
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * The first two weeks of an ordinary player, day by day: what they get back for their walks. Measures pacing
 * (first harvest, empty days) and holds the bars it was tuned to; run it with -i and read the table.
 *
 * The player walks 1.5 km on five days of seven past all their beds, sows whatever they can on the way and on
 * the windowsill, meets a new neighbour every other walk, takes seeds, gives a present from the bag, fulfils
 * requests they can, cooks what they can, collects eggs, and sells the harvest on Saturdays.
 */
class FirstWeeksTest {
    private val dir = File("src/main/assets/content")
    private val content = Content.read({ p -> File(dir, p).takeIf { it.exists() }?.inputStream() })
    private val game = Game(content)

    private class Day(val d: Int, val walk: Boolean) {
        val got = ArrayList<String>()
        var finds = 0
        var sown = 0
        var harvests = 0
        var outdoor = 0
        var verge = 0
        var sill = 0
        var nextRipe = "-"
        var coins = 0
        /** Something that ends a loop: a harvest, a request done, a task done, a dish, a new neighbour. */
        val payoff get() = harvests + got.count { !it.startsWith("seeds") }
    }

    private fun play(start: LocalDate, days: Int = 14): List<Day> {
        val home = Geo(59.95, 30.31)
        val s = game.newGame(Now(start), home)
        val rnd = java.util.Random(11)
        val town = content.people.filter { !it.yard }.map { it.id }
        var met = 0
        val beds = (0 until 16).map { Geo(home.lat + 0.0004 * (it + 1), home.lon) }  // ~45 m apart along the route
        val out = ArrayList<Day>()
        for (d in 0 until days) {
            val date = start.plusDays(d.toLong())
            val m = date.monthValue
            val sky = Sky(rain = d % 4 == 2, snow = m in listOf(12, 1, 2) && d % 5 == 1,
                temp = when (m) { 12, 1, 2 -> -5.0; 3, 11 -> 2.5; 4, 10 -> 8.0; else -> 16.0 })
            val now = Now(date, 18, sky, sky)
            game.catchUp(s, now) { sky to sky }
            val walk = d % 7 !in listOf(2, 5)
            val day = Day(d, walk)
            val req0 = s.counts["request"] ?: 0; val cook0 = s.counts["cook"] ?: 0
            fun settle() = Quests.settle(s) { game.give(s, it) }.forEach { day.got += "task:${it.key}" }

            // at home, morning: the yard, eggs, the windowsill, the kitchen
            if (game.visit(s, now, "semyonych")) day.got += "met:semyonych"
            game.collectEggs(s)
            game.tendHome(s, now)
            for (b in s.beds.filter { game.ripe(it) && content.variety[it.variety]?.place == Place.SILL }.toList()) { game.harvest(s, now, b.id, rnd.nextDouble()); day.harvests++ }
            for (k in s.seeds.keys.toList()) { val v = content.variety[k] ?: continue; while (v.place == Place.SILL && game.canSow(s, now, v)) { game.sow(s, now, k, home); day.sown++ } }
            settle()

            if (walk) {
                var pos = 0
                repeat(150) { i ->
                    game.walked(s, now, 10.0, setOf("street", "park", "lawn"), rnd)?.let { day.finds++ }
                    if (i % 9 == 0 && pos < beds.size) {
                        val at = beds[pos++]
                        game.pass(s, now, at)
                        s.beds.firstOrNull { Geo(it.lat, it.lon).metres(at) < 1 && game.ripe(it) }?.let { game.harvest(s, now, it.id, rnd.nextDouble()); day.harvests++; day.outdoor++ }
                        if (s.beds.none { Geo(it.lat, it.lon).metres(at) < 1 }) {
                            val v = s.seeds.filterValues { it > 0 }.keys.mapNotNull { content.variety[it] }
                                .filter { it.place == Place.VERGE && game.canSow(s, now, it) }.minByOrNull { it.days }
                            if (v != null && game.sow(s, now, v.key, at) != null) day.sown++
                        }
                    }
                    s.km += 0.01; s.kmToday += 0.01
                }
                // a new neighbour every other walk, then a round of the ones already met (three a walk)
                if (met < town.size && (met == 0 || d % 2 == 0)) { if (game.visit(s, now, town[met])) day.got += "met:${town[met]}"; met++ }
                for (id in town.take(met).shuffled(rnd).take(3)) {
                    game.visit(s, now, id)
                    game.takeSeeds(s, now, id)?.let { day.got += "seeds:${it.first}" }
                    s.bag.filterValues { it > 0 }.keys.firstOrNull()?.let { game.gift(s, now, id, it) }
                    if (game.request(s, id)?.let { game.canFulfil(s, it.needs) } == true) game.fulfil(s, now, id)
                    game.returnLost(s, id)
                }
                for (id in s.bonds.keys.toList()) if (game.request(s, id)?.let { game.canFulfil(s, it.needs) } == true) game.fulfil(s, now, id)
            }
            // home in the evening: cook what can be cooked, sell on Saturdays
            for (r in game.known(s)) if (game.cook(s, now, r.key)) break
            if (now.saturday) for (k in s.bag.keys.filter { content.variety[it] != null }.toList()) game.sell(s, now, k, s.bag[k] ?: 0)
            settle()

            repeat((s.counts["request"] ?: 0) - req0) { day.got += "request" }
            repeat((s.counts["cook"] ?: 0) - cook0) { day.got += "cook" }
            day.verge = s.beds.count { content.variety[it.variety]?.place == Place.VERGE }
            day.sill = s.beds.count { content.variety[it.variety]?.place == Place.SILL }
            day.nextRipe = s.beds.filter { !game.ripe(it) }.minOfOrNull { game.outlook(s, it, now).nights }?.toString() ?: "-"
            day.coins = s.coins
            out += day
        }
        return out
    }

    private fun report(title: String, days: List<Day>): List<Day> {
        println("\n== $title ==")
        println("day walk finds sown harvest(out) beds(v/s) ripe-in coins  what happened")
        for (x in days) println("%3d  %-4s %5d %4d %7s %10s %7s %5d  %s".format(x.d + 1, if (x.walk) "yes" else "-", x.finds, x.sown, "${x.harvests}(${x.outdoor})",
            "${x.verge}/${x.sill}", x.nextRipe, x.coins, x.got.joinToString(" ")))
        val first = days.firstOrNull { it.harvests > 0 }?.d?.plus(1)
        val empty = days.count { it.payoff == 0 }
        val outdoor = days.firstOrNull { it.outdoor > 0 }?.d?.plus(1)
        println("first harvest: day ${first ?: "none in ${days.size}"}; first from the verge: day ${outdoor ?: "none"}; from the verge: ${days.sumOf { it.outdoor }}; harvests: ${days.sumOf { it.harvests }}; days with no payoff: $empty of ${days.size}; finds: ${days.sumOf { it.finds }}")
        return days
    }

    @Test
    fun firstTwoWeeks() {
        // the bars this pacing was tuned to: something picked within the first week, the verge by day 10
        for ((title, start) in listOf("start 1 May" to LocalDate.of(2027, 5, 1), "start 1 September" to LocalDate.of(2027, 9, 1))) {
            val days = report(title, play(start))
            assertTrue("$title: a harvest in the first week", days.take(7).any { it.harvests > 0 })
            assertTrue("$title: a verge harvest by day 10", days.take(10).any { it.outdoor > 0 })
            assertTrue("$title: payoff on every walk but a couple", days.count { it.walk && it.payoff == 0 } <= 2)
        }
        val winter = report("start 10 January", play(LocalDate.of(2027, 1, 10)))
        assertTrue("winter: the windowsill gives within the first week", winter.take(7).any { it.harvests > 0 })
        report("start 1 May, 30 days", play(LocalDate.of(2027, 5, 1), 30).drop(14))
    }
}
