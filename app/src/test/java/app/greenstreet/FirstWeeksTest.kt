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
        /** Coins earned today by source: fair, requests, finds, tasks, other. */
        val earned = HashMap<String, Int>()
        var spent = 0
        val bought = ArrayList<String>()
        /** Units of harvest picked, sold at the fair, and lying in the bag tonight. */
        var picked = 0
        var sold = 0
        var held = 0
        /** Requests showing that the bag can't fill. */
        var waiting = 0
        /** Today's errand: -1 none offered, 0 offered and not done, 1 done. */
        var errand = -1
        /** Something that ends a loop: a harvest, a request done, a task done, a dish, a new neighbour. */
        val payoff get() = harvests + got.count { !it.startsWith("seeds") }
    }

    /** Upgrades in the order a sensible player buys them, then the yard's things from Mikhalych. */
    private val wishes = listOf("cart", "mulch", "coop", "sill", "satchel", "cover", "compost", "stall")

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
            val cMorning = s.coins
            game.catchUp(s, now) { sky to sky }
            val morningCoins = s.coins - cMorning
            val walk = d % 7 !in listOf(2, 5)
            val day = Day(d, walk)
            val req0 = s.counts["request"] ?: 0; val cook0 = s.counts["cook"] ?: 0
            fun <T> earn(src: String, f: () -> T): T { val c0 = s.coins; val r = f(); if (s.coins != c0) day.earned.merge(src, s.coins - c0, Int::plus); return r }
            /** Everything the neighbours are asking for right now. */
            fun wanted() = s.bonds.keys.mapNotNull { game.request(s, it) }.flatMap { it.needs }
            fun wants(k: String) = wanted().any { content.matches(it.key, k) }
            fun settle() = earn("tasks") { Quests.settle(s) { game.give(s, it) } }.forEach { day.got += "task:${it.key}" }
            fun harvest(id: Long) { earn("other") { game.harvest(s, now, id, rnd.nextDouble()) }?.let { day.picked += it.n } }
            val c0 = s.coins

            // at home, morning: the yard, eggs, the windowsill, the kitchen
            if (game.visit(s, now, "semyonych")) day.got += "met:semyonych"
            game.collectEggs(s)
            game.tendHome(s, now)
            for (b in s.beds.filter { game.ripe(it) && content.variety[it.variety]?.place == Place.SILL }.toList()) { harvest(b.id); day.harvests++ }
            for (k in s.seeds.keys.toList()) { val v = content.variety[k] ?: continue; while (v.place == Place.SILL && game.canSow(s, now, v)) { game.sow(s, now, k, home); day.sown++ } }
            settle()

            if (walk) {
                var pos = 0
                repeat(150) { i ->
                    earn("finds") { game.walked(s, now, 10.0, if (d % 3 == 0) setOf("street", "water") else setOf("street", "park", "lawn"), rnd) }?.let { day.finds++ }
                    if (i % 9 == 0 && pos < beds.size) {
                        val at = beds[pos++]
                        game.pass(s, now, at)
                        s.beds.firstOrNull { Geo(it.lat, it.lon).metres(at) < 1 && game.ripe(it) }?.let { harvest(it.id); day.harvests++; day.outdoor++ }
                        if (s.beds.none { Geo(it.lat, it.lon).metres(at) < 1 }) {
                            val v = s.seeds.filterValues { it > 0 }.keys.mapNotNull { content.variety[it] }
                                .filter { it.place == Place.VERGE && game.canSow(s, now, it) }.sortedBy { it.days }.let { l -> l.firstOrNull { wants(it.key) } ?: l.firstOrNull() }
                            if (v != null && game.sow(s, now, v.key, at) != null) day.sown++
                        }
                    }
                    s.km += 0.01; s.kmToday += 0.01
                }
                // by the water: a cast or two (the fishing sheet's catch, minus the float)
                if (d % 3 == 0) repeat(2) {
                    val fish = content.goods.filter { it.kind == app.greenstreet.data.GoodKind.FISH && (it.months.isEmpty() || m in it.months) }
                    if (fish.isNotEmpty() && rnd.nextDouble() < .6) { val f = fish[rnd.nextInt(fish.size)]; s.add(s.bag, f.key, 1); s.album += f.key; s.count("fish") }
                }
                // at the market once Aunt Zina is known: buy what a request needs and the stalls have
                if (s.bonds["zina"]?.isOpen == true) for ((k, price) in game.market(now)) {
                    if (content.variety[k] == null && wants(k) && (s.bag[k] ?: 0) < 3 && s.coins >= price + 40) { s.coins -= price; s.add(s.bag, k, 1); day.spent += price }
                }
                // a new neighbour every other walk, then a round of the ones already met (three a walk)
                if (met < town.size && (met == 0 || d % 2 == 0)) { if (game.visit(s, now, town[met])) day.got += "met:${town[met]}"; met++ }
                // today's errand, if it names someone: they come first
                app.greenstreet.domain.Errands.today(game, s, now)  // chooses it, if not yet
                val first = s.flags.firstOrNull { it.startsWith("errand:${now.day}:visit:") || it.startsWith("errand:${now.day}:gift:") }?.split(':')?.getOrNull(4)
                for (id in (listOfNotNull(first) + town.take(met).shuffled(rnd).take(3)).distinct()) {
                    game.visit(s, now, id)
                    game.takeSeeds(s, now, id)?.let { day.got += "seeds:${it.first}" }
                    s.bag.filterValues { it > 0 }.keys.firstOrNull { !wants(it) }?.let { game.gift(s, now, id, it) }
                    if (game.request(s, id)?.let { game.canFulfil(s, it.needs) } == true) earn("requests") { game.fulfil(s, now, id) }
                    earn("other") { game.returnLost(s, id) }
                }
                for (id in s.bonds.keys.toList()) if (game.request(s, id)?.let { game.canFulfil(s, it.needs) } == true) earn("requests") { game.fulfil(s, now, id) }
            }
            // home in the evening: cook what can be cooked, sell on Saturdays
            val menu = game.known(s).sortedByDescending { wants(it.makes) }
            for (r in menu) if (game.cook(s, now, r.key)) break
            if (now.saturday) for (k in s.bag.keys.filter { content.variety[it] != null }.toList()) {
                val keep = wanted().filter { content.matches(it.key, k) }.sumOf { it.qty }
                val n = (s.bag[k] ?: 0) - keep
                val v = if (n > 0) game.sell(s, now, k, n) else 0; if (v > 0) { day.earned.merge("fair", v, Int::plus); day.sold += n }
            }
            settle()
            // the household: the next upgrade on the list as soon as it's affordable, then the yard's things
            while (true) {
                val k = wishes.firstOrNull { game.nextPrice(s, it) != null } ?: break
                val price = game.nextPrice(s, k)!!
                if (s.coins < price) break
                game.buyUpgrade(s, k); day.spent += price; day.bought += k
            }
            if (wishes.all { game.nextPrice(s, it) == null })
                for (w in app.greenstreet.domain.Wares.all.filter { it.once }) if (!game.owns(s, w.key) && s.coins >= w.price) { game.buyWare(s, now, w.key); day.spent += w.price; day.bought += w.key }
            if (app.greenstreet.domain.Errands.today(game, s, now) != null) day.errand = 0
            earn("errands") { app.greenstreet.domain.Errands.settle(game, s, now) }?.let { day.errand = 1; day.got += "errand:${it.key.removePrefix("errand.")}" }
            if (morningCoins != 0) day.earned.merge("other", morningCoins, Int::plus)
            val unexplained = s.coins - c0 + day.spent - day.earned.values.sum()
            if (unexplained != 0) day.earned.merge("other", unexplained, Int::plus)
            day.held = s.bag.filterKeys { content.variety[it] != null }.values.sum()
            day.waiting = s.bonds.keys.count { id -> game.request(s, id)?.let { !game.canFulfil(s, it.needs) } == true }
            if (d == days - 1 && days >= 60) for (id in s.bonds.keys) game.request(s, id)?.takeIf { !game.canFulfil(s, it.needs) }?.let { r ->
                println("stuck $id since day ${s.bonds[id]!!.reqShown - now.day + d + 1}: " + r.needs.joinToString { n -> "${n.key}×${n.qty} (have ${s.bag[n.key] ?: 0})" })
            }

            repeat((s.counts["request"] ?: 0) - req0) { day.got += "request" }
            repeat((s.counts["cook"] ?: 0) - cook0) { day.got += "cook" }
            day.verge = s.beds.count { content.variety[it.variety]?.place == Place.VERGE }
            day.sill = s.beds.count { content.variety[it.variety]?.place == Place.SILL }
            day.nextRipe = s.beds.filter { !game.ripe(it) }.minOfOrNull { game.outlook(s, it, now).nights }?.toString() ?: "-"
            day.coins = s.coins
            out += day
        }
        println("after ${days} days the opening task is: ${Quests.current(s)?.key ?: "all done"}")
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
        val offered = days.count { it.errand >= 0 }
        println("errands done: ${days.count { it.errand == 1 }} of $offered offered (on walks: ${days.count { it.walk && it.errand == 1 }} of ${days.count { it.walk && it.errand >= 0 }})")
        println("first harvest: day ${first ?: "none in ${days.size}"}; first from the verge: day ${outdoor ?: "none"}; from the verge: ${days.sumOf { it.outdoor }}; harvests: ${days.sumOf { it.harvests }}; days with no payoff: $empty of ${days.size}; finds: ${days.sumOf { it.finds }}")
        return days
    }

    /** Ten-day stretches: what came in from where, what went on the household, how much harvest piled up. */
    private fun economy(title: String, days: List<Day>) {
        println()
        println("== $title: the economy ==")
        println("days    fair  req finds tasks other | spent coins | picked sold held | waiting  bought")
        for (chunk in days.chunked(if (days.size > 120) 30 else 10)) {
            fun e(k: String) = chunk.sumOf { it.earned[k] ?: 0 }
            println("%3d-%-3d %4d %4d %5d %5d %5d | %5d %5d | %6d %4d %4d | %7d  %s".format(chunk.first().d + 1, chunk.last().d + 1,
                e("fair"), e("requests"), e("finds"), e("tasks"), e("other"), chunk.sumOf { it.spent }, chunk.last().coins,
                chunk.sumOf { it.picked }, chunk.sumOf { it.sold }, chunk.last().held, chunk.last().waiting, chunk.flatMap { it.bought }.joinToString(" ")))
        }
        val offered = days.count { it.errand >= 0 }
        println("errands done: ${days.count { it.errand == 1 }} of $offered offered, first offered on day ${days.firstOrNull { it.errand >= 0 }?.d?.plus(1)}")
        val all = days.flatMap { it.earned.entries }.groupBy({ it.key }, { it.value }).mapValues { it.value.sum() }
        val upgrades = days.flatMap { it.bought }.filter { it in wishes }
        println("earned ${all.values.sum()} (${all.entries.sortedByDescending { it.value }.joinToString { "${it.key} ${it.value}" }}), spent ${days.sumOf { it.spent }}, " +
            "upgrades ${upgrades.size} of ${app.greenstreet.domain.Upgrades.all.sumOf { it.max }}, all bought by day ${days.lastOrNull { d -> d.bought.any { it in wishes } }?.d?.plus(1)}")
    }

    @Test
    fun threeMonthsOfGroats() {
        // the groats should last: the household fills up over the first year, not the first season
        for ((title, start) in listOf("start 1 May" to LocalDate.of(2027, 5, 1), "start 1 September" to LocalDate.of(2027, 9, 1))) {
            val days = play(start, 365)
            economy("$title, a year", days)
            val last = days.lastOrNull { d -> d.bought.any { it in wishes } }?.d ?: 365
            val all = days.flatMap { it.bought }.count { it in wishes } == app.greenstreet.domain.Upgrades.all.sumOf { it.max }
            assertTrue("$title: the household takes most of the first year (done by ${last + 1})", !all || last >= 180)
            assertTrue("$title: something is bought in the first month", days.take(30).any { d -> d.bought.isNotEmpty() })
        }
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
