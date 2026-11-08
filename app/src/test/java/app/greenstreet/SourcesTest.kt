package app.greenstreet

import app.greenstreet.data.Content
import app.greenstreet.data.GoodKind
import app.greenstreet.data.Need
import app.greenstreet.domain.Game
import app.greenstreet.domain.Geo
import app.greenstreet.domain.Now
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Everything a request, a notice or a recipe asks for must be something the player can get: a crop, a find,
 * a fish, a staple from the market, an egg, or a dish cooked from those. Prints what can't be had and who asks.
 */
class SourcesTest {
    private val dir = File("src/main/assets/content")
    private val content = Content.read({ p -> File(dir, p).takeIf { it.exists() }?.inputStream() })
    private val game = Game(content)

    /** What the game hands out: crops, finds and fish on the way, the market in every month, the hen house. */
    private fun obtainable(): Set<String> {
        val have = HashSet<String>()
        content.varieties.forEach { have += it.key }
        content.goods.filter { it.kind in setOf(GoodKind.FIND, GoodKind.FISH) }.forEach { have += it.key }
        for (m in 1..12) game.market(Now(java.time.LocalDate.of(2027, m, 15))).forEach { have += it.first }
        val s = game.newGame(Now(java.time.LocalDate.of(2027, 5, 1)), Geo(59.95, 30.31))
        repeat(4) { s.eggs = 2; game.collectEggs(s) }
        have += s.bag.keys
        while (true) {
            val more = content.recipes.filter { it.makes !in have && it.needs.all { n -> ok(n, have) } }.map { it.makes }
            if (more.isEmpty()) break
            have += more
        }
        return have
    }

    private fun ok(n: Need, have: Set<String>) = have.any { content.matches(n.key, it) }

    @Test
    fun everythingAskedForCanBeHad() {
        val have = obtainable()
        val missing = HashMap<String, MutableList<String>>()
        for ((id, list) in content.requests) list.forEachIndexed { i, r ->
            r.needs.filter { !ok(it, have) }.forEach { missing.getOrPut(it.key) { ArrayList() } += "$id#${i + 1}" }
        }
        content.board.forEach { b -> b.needs.filter { !ok(it, have) }.forEach { missing.getOrPut(it.key) { ArrayList() } += "board:${b.key}" } }
        content.recipes.forEach { r -> r.needs.filter { !ok(it, have) }.forEach { missing.getOrPut(it.key) { ArrayList() } += "recipe:${r.key}" } }
        println("asked for but never handed out: ${missing.size}")
        for ((k, who) in missing.entries.sortedByDescending { it.value.size }) println("  $k ×${who.size}: ${who.take(12).joinToString()}${if (who.size > 12) " …" else ""}")
        assertTrue("asked for but never handed out: ${missing.keys}", missing.isEmpty())
    }
}
