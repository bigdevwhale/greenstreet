package app.greenstreet

import app.greenstreet.data.Content
import app.greenstreet.domain.Game
import app.greenstreet.domain.Geo
import app.greenstreet.domain.Now
import app.greenstreet.domain.Sky
import app.greenstreet.domain.Upgrades
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Walks a year of daily 3 km rounds and checks what turns up on the way, the weekly demand and the upgrades. */
class FindsTest {
    private val dir = File("src/main/assets/content")
    private val content = Content.read({ p -> File(dir, p).takeIf { it.exists() }?.inputStream() })
    private val game = Game(content)

    @Test
    fun aYearOfWalks() {
        val start = LocalDate.of(2026, 9, 1)
        val s = game.newGame(Now(start), Geo(59.95, 30.31))
        listOf("semyonych", "zina", "mila").forEach { s.bond(it).open = 0 }
        val rnd = java.util.Random(7)
        val kinds = HashMap<String, Int>()
        val goods = HashSet<String>()
        var days = 0
        for (d in 0 until 365) {
            val date = start.plusDays(d.toLong())
            val now = Now(date, 9 + d % 10, Sky(rain = d % 4 == 0))
            game.catchUp(s, now) { Sky() to Sky() }
            var today = 0
            val where = when (d % 3) { 0 -> setOf("street", "park", "lawn"); 1 -> setOf("street", "water"); else -> setOf("street", "wood", "park") }
            repeat(300) { game.walked(s, now, 10.0, where, rnd)?.let { f -> kinds.merge(f.kind, 1, Int::plus); if (f.kind == "good") goods += f.key; today++ } }
            if (today in 3..6) days++
            // carry lost things back the same day
            for (id in s.bonds.keys.toList()) game.returnLost(s, id)
        }
        println("finds: $kinds, ${goods.size} kinds of forage: $goods; days with 3–6 finds: $days, coins ${s.coins}")
        assertTrue("finds every day", days > 300)
        for (k in listOf("good", "seeds", "coins", "lost", "moment")) assertTrue("kind $k turns up", (kinds[k] ?: 0) > 10)
        assertTrue("forage varies with the seasons", goods.size >= 15)
    }

    @Test
    fun demandAndUpgrades() {
        val s = game.newGame(Now(LocalDate.of(2027, 7, 3)), Geo(59.95, 30.31))
        val weeks = (0 until 52).mapNotNull { game.demand(s, Now(LocalDate.of(2027, 1, 2).plusWeeks(it.toLong()))) }.toSet()
        println("demand over a year: ${weeks.size} crops: $weeks")
        assertTrue("demand changes through the year", weeks.size >= 8)
        s.coins = 10000
        for (u in Upgrades.all) repeat(u.max) { assertTrue(game.buyUpgrade(s, u.key)) }
        for (u in Upgrades.all) { assertEquals(u.max, game.up(s, u.key)); assertTrue(!game.buyUpgrade(s, u.key)) }
        println("all upgrades cost ${10000 - s.coins}")
    }
}
