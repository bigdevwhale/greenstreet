package app.greenstreet.domain

import app.greenstreet.data.Content
import app.greenstreet.data.GoodKind
import app.greenstreet.data.Need
import app.greenstreet.data.Place
import app.greenstreet.data.Tx

/**
 * Where to get what a neighbour asks for, in a line the player can act on: it's in the bag, it's growing and
 * ripens in N days, plant the seeds you have, who gives the seeds, the market, a find by the water, the hen
 * house, or a dish and what goes into it.
 */
object Sources {
    /** How many of the bag's things answer a need. */
    fun have(c: Content, s: GameState, key: String): Int = s.bag.entries.filter { c.matches(key, it.key) }.sumOf { it.value }

    fun hint(g: Game, s: GameState, now: Now, n: Need): Tx {
        val c = g.c
        if (have(c, s, n.key) >= n.qty) return Tx("есть в сумке — можно отнести", "in your bag — ready to take")
        return when {
            n.key == "egg" || n.key == "feather" -> Tx("курятник во дворе", "the hen house in the yard")
            n.key in Game.BEEKEEPER -> Tx("пасечник на рынке у тёти Зины", "the beekeeper at Aunt Zina's market")
            c.good[n.key]?.kind == GoodKind.STAPLE -> Tx("рынок у тёти Зины", "Aunt Zina's market")
            n.key == "anyfish" || c.good[n.key]?.kind == GoodKind.FISH -> fish(c, n.key, now)
            n.key == "anymush" -> Tx("грибы попадаются на прогулке в парке и в роще, чаще после дождя", "mushrooms turn up on walks in the park and the trees, more after rain")
            n.key == "anyfind" -> Tx("любая находка с прогулки", "anything found on a walk")
            c.good[n.key]?.kind == GoodKind.FIND -> find(c, n.key, now)
            n.key in c.products || n.key in setOf("anyjam", "anytea", "anybread", "anycloth") -> cook(c, s, n.key)
            else -> grow(g, s, now, n.key)
        }
    }

    private fun fish(c: Content, key: String, now: Now): Tx {
        val g = c.good[key] ?: return Tx("рыбалка: подойдите к воде и закиньте удочку", "fishing: go to the water and cast")
        val off = g.months.isNotEmpty() && now.month !in g.months
        return Tx("рыбалка у воды" + if (off) ", клюёт в " + months(g.months, true) else "", "fishing by the water" + if (off) ", bites in " + months(g.months, false) else "")
    }

    private fun find(c: Content, key: String, now: Now): Tx {
        val g = c.good.getValue(key)
        val where = when (g.where) {
            "water", "river", "canal", "lake", "pond" -> Tx("у воды", "by the water"); "wood" -> Tx("в роще", "among the trees")
            "park" -> Tx("в парке", "in the park"); "lawn" -> Tx("на газонах", "on the grass"); "street" -> Tx("на улицах", "in the streets")
            else -> Tx("на прогулке", "on a walk")
        }
        val off = g.months.isNotEmpty() && now.month !in g.months
        val weather = when (g.weather) { "rain" -> Tx(", после дождя", ", after rain"); "frost" -> Tx(", в мороз", ", in frost"); "snow" -> Tx(", в снег", ", in snow"); else -> Tx("", "") }
        return Tx("находка " + where.ru + weather.ru + if (off) ", бывает в " + months(g.months, true) else "",
            "found " + where.en + weather.en + if (off) ", turns up in " + months(g.months, false) else "")
    }

    private fun cook(c: Content, s: GameState, key: String): Tx {
        val r = c.recipes.firstOrNull { c.matches(key, it.makes) } ?: return Tx("приготовить на кухне", "cook it in the kitchen")
        val parts = r.needs.joinToString { c.name(it.key).ru.lowercase() + " ×" + it.qty }
        val partsEn = r.needs.joinToString { c.name(it.key).en.lowercase() + " ×" + it.qty }
        val dish = if (r.makes == key) "" else c.name(r.makes).ru.lowercase() + ": "
        val dishEn = if (r.makes == key) "" else c.name(r.makes).en.lowercase() + ": "
        val known = r.makes in s.recipes || r.unlock == "start" || r.key in s.recipes
        return Tx("кухня во дворе, $dish$parts" + if (known) "" else " (рецепт ещё узнаете)", "the yard kitchen, $dishEn$partsEn" + if (known) "" else " (recipe still to learn)")
    }

    private fun grow(g: Game, s: GameState, now: Now, key: String): Tx {
        val c = g.c
        fun fits(v: String) = c.matches(key, v)
        // growing already: the bed that ripens first
        s.beds.filter { fits(it.variety) }.minByOrNull { g.outlook(s, it, now).nights }?.let { b ->
            val n = g.outlook(s, b, now).nights
            return if (n == 0) Tx("грядка созрела — соберите", "a bed is ripe — pick it")
            else Tx("растёт на грядке, созреет через ~$n дн.", "growing on a bed, ripe in ~$n days")
        }
        s.seeds.filter { it.value > 0 && fits(it.key) }.keys.mapNotNull { c.variety[it] }.minByOrNull { it.days }?.let { v ->
            val on = g.ripeOn(v, now)
            val where = if (v.place == Place.VERGE) Tx("у дороги", "by a road") else Tx("дома", "at home")
            return Tx("семена в сумке — посадите ${where.ru}, созреет к ${on.dayOfMonth}.${"%02d".format(on.monthValue)}",
                "seeds in your bag — sow them ${where.en}, ripe by ${on.dayOfMonth}/${on.monthValue}")
        }
        val givers = c.people.filter { p -> p.seeds.any { fits(it) } }
        givers.sortedByDescending { s.bonds[it.id]?.isOpen == true }.firstOrNull()?.let { p ->
            val met = s.bonds[p.id]?.isOpen == true
            return Tx("семена даёт ${p.name.ru}" + if (met) "" else " (ещё не знакомы)", "${p.name.en} gives the seeds" + if (met) "" else " (not met yet)")
        }
        if (c.varieties.any { it.unlock == "market" && fits(it.key) }) return Tx("семена на рынке у тёти Зины", "seeds at Aunt Zina's market")
        if (c.goods.any { it.kind == GoodKind.FIND && fits(it.key) }) return Tx("находка на прогулке", "found on walks")
        return Tx("семена дают за просьбы и на праздниках", "seeds come from requests and festivals")
    }

    private fun months(ms: Set<Int>, ru: Boolean): String {
        val r = listOf("янв.", "февр.", "март", "апр.", "май", "июнь", "июль", "авг.", "сент.", "окт.", "нояб.", "дек.")
        val e = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        return ms.sorted().joinToString(", ") { (if (ru) r else e)[it - 1] }
    }
}
