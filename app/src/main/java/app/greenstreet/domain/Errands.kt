package app.greenstreet.domain

import app.greenstreet.data.Place
import app.greenstreet.data.Reward
import app.greenstreet.data.Tx

/**
 * Today's errand: once the opening tasks are done, one small thing a day from a neighbour already met, always
 * doable on one walk — pass by your beds, look in on someone, bring a present, pick a ripe bed, help with a
 * request that can be done now, or just walk and see what turns up. It is chosen once a day and kept in the
 * state, so it doesn't change as the bag does; it pays a little of that neighbour's liking and a token groat or three.
 */
object Errands {
    /** Liking the neighbour gives for a done errand (a visit gives 1, a liked present 3). */
    const val POINTS = 2

    private fun tag(day: Long) = "errand:$day:"

    /** Today's errand, chosen now if it hasn't been yet; null while the opening tasks run or nobody is met. */
    fun today(g: Game, s: GameState, now: Now): Quest? {
        if (Quests.current(s) != null) return null
        val saved = s.flags.firstOrNull { it.startsWith(tag(now.day)) } ?: choose(g, s, now)?.also { f ->
            s.flags.removeAll { it.startsWith("errand:") && !it.startsWith(tag(now.day)) }
            s.flags += f
        } ?: return null
        val (kind, from, target, goal) = saved.removePrefix(tag(now.day)).split(':').let { listOf(it[0], it[1], it[2], it[3]) }
        return quest(g, s, now, kind, from, target, goal.toInt())
    }

    /** Is today's errand done and paid? */
    fun done(s: GameState, now: Now) = "errand.done:${now.day}" in s.flags

    /** Pays today's errand if it's finished; returns it just then, null otherwise. */
    fun settle(g: Game, s: GameState, now: Now): Quest? {
        val q = today(g, s, now) ?: return null
        if (done(s, now) || q.progress(s) < q.goal) return null
        s.flags.removeAll { it.startsWith("errand.done:") }
        s.flags += "errand.done:${now.day}"
        q.reward.forEach { g.give(s, it) }
        s.bond(q.from).points += POINTS
        s.count("errands")
        return q
    }

    private fun choose(g: Game, s: GameState, now: Now): String? {
        val met = s.bonds.filter { it.value.isOpen && g.c.person[it.key]?.yard == false }.keys.sorted()
        if (met.isEmpty()) return null
        val rnd = java.util.Random(now.day * 7919 + s.start)
        fun pick(l: List<String>) = l[rnd.nextInt(l.size)]
        val verge = s.beds.filter { g.c.variety[it.variety]?.place == Place.VERGE }
        val options = ArrayList<String>()
        // help with a request the bag can fill now: the best errand there is
        met.filter { id -> g.request(s, id)?.let { g.canFulfil(s, it.needs) } == true }.takeIf { it.isNotEmpty() }?.let { options += "help:${pick(it)}:-:1"; options += options.last() }
        if (verge.size >= 2) options += "tend:${pick(met)}:-:${minOf(verge.size, 2 + rnd.nextInt(3))}"
        if (verge.any { g.outlook(s, it, now).nights <= 1 }) options += "pick:${pick(met)}:-:1"
        // the neighbour seen least lately
        options += met.minBy { s.bonds[it]?.visited ?: -1 }.let { "visit:$it:$it:1" }
        if (s.bag.any { it.value > 0 }) { val id = pick(met); options += "gift:$id:$id:1" }
        options += "walk:${pick(met)}:-:${10 + 5 * rnd.nextInt(3)}"
        options += "find:${pick(met)}:-:2"
        return tag(now.day) + pick(options)
    }

    private fun quest(g: Game, s: GameState, now: Now, kind: String, from: String, target: String, goal: Int): Quest {
        val day = now.day
        // the liking is the point; the groats are a token, or a daily errand would buy the household by spring
        val coins = listOf(Reward.Coins(if (kind == "help") 6 else 3))
        val who = g.c.person[target]?.name ?: Tx(target, target)
        val (title, how, progress) = when (kind) {
            "help" -> Triple(Tx("Помочь с просьбой", "Help with a request"),
                Tx("У вас в сумке есть всё, о чём просит сосед. Отнесите сегодня.", "Your bag holds everything a neighbour has asked for. Take it over today."),
                { st: GameState -> if ((st.counts["request_day"] ?: -1) == day.toInt()) 1 else 0 })
            "tend" -> Triple(Tx("Обойти грядки", "Round of the beds"),
                Tx("Пройдите сегодня мимо своих грядок у дороги: %d штук.".format(goal), "Walk past %d of your verge beds today.".format(goal)),
                { st: GameState -> st.counts["tend_today"] ?: 0 })
            "pick" -> Triple(Tx("Собрать урожай", "Pick a harvest"),
                Tx("Одна из грядок созрела или созреет к утру. Соберите урожай сегодня или завтра утром.", "A bed is ripe or will be by morning. Pick it today or first thing tomorrow."),
                { st: GameState -> if ((st.counts["harvest_day"] ?: -1) == day.toInt()) 1 else 0 })
            "visit" -> Triple(Tx("Проведать соседа", "Look in on a neighbour"),
                Tx("Давно не заходили: ${who.ru}. Загляните сегодня.", "You haven't been by in a while: ${who.en}. Look in today."),
                { st: GameState -> if (st.bonds[target]?.visited == day) 1 else 0 })
            "gift" -> Triple(Tx("Маленький подарок", "A small present"),
                Tx("Порадуйте сегодня подарком: ${who.ru}. Лучше тем, что любит.", "Bring a present today: ${who.en}. Something they like is best."),
                { st: GameState -> if (st.bonds[target]?.gifted == day) 1 else 0 })
            "walk" -> Triple(Tx("Прогулка", "A walk"),
                Tx("Пройдите сегодня %s км. Всё, что попадётся, — ваше.".format(km(goal, true)), "Walk %s km today. Whatever turns up is yours.".format(km(goal, false))),
                { st: GameState -> (st.kmToday * 10).toInt() })
            else -> Triple(Tx("Что под ногами", "What's underfoot"),
                Tx("Найдите сегодня на прогулке %d вещи.".format(goal), "Find %d things on today's walk.".format(goal)),
                { st: GameState -> st.counts["finds_today"] ?: 0 })
        }
        return Quest("errand.$kind", from, title, how, goal, coins, unit = if (kind == "walk") 10 else 1, progress = progress)
    }

    private fun km(tenths: Int, ru: Boolean) = (if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}${if (ru) ',' else '.'}${tenths % 10}")
}
