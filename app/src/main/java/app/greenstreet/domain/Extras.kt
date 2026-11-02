package app.greenstreet.domain

import app.greenstreet.data.Tx

/** One thing to build or buy for the household, in levels; each level costs more. */
class Upgrade(val key: String, val prices: List<Int>, val name: Tx, val effect: (Int) -> Tx) {
    val max get() = prices.size
}

/**
 * What the groats are for. Each upgrade changes one rule, so buying is a choice: more eggs or more beds,
 * beds that forgive a week away or a stall that sells dearer.
 */
object Upgrades {
    val all = listOf(
        Upgrade("cart", listOf(80, 260), Tx("Тачка и лопата", "A barrow and a spade"))
            { l -> Tx("ещё ${2 * l} места под грядки у дорог", "${2 * l} more bed places by the roads") },
        Upgrade("coop", listOf(60, 200), Tx("Новый насест", "A new perch"))
            { l -> Tx("куры несут на $l ${if (l == 1) "яйцо" else "яйца"} в день больше", "hens lay $l more egg${if (l == 1) "" else "s"} a day") },
        Upgrade("mulch", listOf(70, 180), Tx("Мульча из листьев", "Leaf mulch"))
            { l -> Tx("грядки дичают через ${3 + 2 * l} дней без присмотра, а не через 3", "beds go wild after ${3 + 2 * l} days alone, not 3") },
        Upgrade("satchel", listOf(90), Tx("Корзинка грибника", "A forager's basket"))
            { _ -> Tx("находки по дороге попадаются чаще", "finds turn up more often on the way") },
        Upgrade("cover", listOf(120), Tx("Укрывной спанбонд", "Frost fleece"))
            { _ -> Tx("заморозки больше не трогают грядки", "frost no longer touches the beds") },
        Upgrade("sill", listOf(110), Tx("Полка на окно", "A window shelf"))
            { _ -> Tx("на подоконнике 6 горшков, а не 4", "6 pots on the sill instead of 4") },
        Upgrade("compost", listOf(160, 420), Tx("Компостный ящик", "A compost box"))
            { l -> Tx("каждая грядка даёт на $l больше урожая", "every bed yields $l more") },
        Upgrade("stall", listOf(240), Tx("Свой прилавок на ярмарке", "Your own stall at the fair"))
            { _ -> Tx("на ярмарке всё продаётся на четверть дороже", "everything sells a quarter dearer at the fair") },
    )

    fun of(key: String) = all.first { it.key == key }
}

/** Something that turned up on the walk. [key] is a bag key, a variety (seeds), "coins", a neighbour id (a lost thing) or "" for a moment. */
class Find(val kind: String, val key: String, val n: Int, val text: Tx)

object Extras {
    /** The kind of spot the player stands in, as the content's "where" words: water, wood, park, lawn, street. */
    fun Terrain.whereAt(x: Int, y: Int): Set<String> = where(this, x, y)
}

private fun where(t: Terrain, x: Int, y: Int): Set<String> = with(t) {
    var water = 0; var wood = 0; var lawn = 0
    for (j in -9..9) for (i in -9..9) when (at(x + i, y + j)) { K.WATER -> water++; K.WOOD -> wood++; K.LAWN -> lawn++ }
    val out = linkedSetOf("street")
    if (water > 4) out += "water"
    if (wood > 20) { out += "wood"; out += "park" }
    if (lawn > 30) { out += "lawn"; out += "park" }
    out
}

/** Neighbours' lost things: what each one could drop. */
object Lost {
    val things = listOf(
        Tx("варежку с оленями", "a mitten with deer on it"), Tx("связку ключей с брелоком-ёжиком", "a bunch of keys on a hedgehog fob"),
        Tx("очки в потёртом футляре", "glasses in a worn case"), Tx("библиотечную книжку", "a library book"),
        Tx("кошелёк-«таблетку»", "a little coin purse"), Tx("клетчатый шарф", "a checked scarf"),
        Tx("перчатку на меху", "a fur-lined glove"), Tx("список покупок, подписанный по имени", "a shopping list signed with a name"),
        Tx("фотографию в целлофане", "a photo in a plastic sleeve"), Tx("зонт-трость", "a walking-stick umbrella"),
    )
}

/** Small moments on the way: nothing to take, something to see. Months empty = any time. */
class Moment2(val months: Set<Int>, val text: Tx)

object Moments {
    val all = listOf(
        Moment2(emptySet(), Tx("Из открытой форточки пахнет жареной картошкой. Кто-то ужинает.", "Fried potatoes through an open window. Someone's having supper.")),
        Moment2(emptySet(), Tx("На подоконнике первого этажа спит толстый серый кот. Не Пряник, но явно знакомы.", "A fat grey cat asleep on a ground-floor sill. Not Pryanik, but they clearly know each other.")),
        Moment2(emptySet(), Tx("Мальчишка гоняет по двору мяч об стену. Тук. Тук. Тук.", "A boy kicks a ball against a wall. Thud. Thud. Thud.")),
        Moment2(emptySet(), Tx("Две старушки на лавочке обсуждают погоду так, будто от неё зависит судьба мира.", "Two old ladies on a bench discuss the weather as if the fate of the world hung on it.")),
        Moment2(emptySet(), Tx("Голуби дерутся за горбушку. Воробей уносит её у всех из-под носа.", "Pigeons fight over a crust. A sparrow steals it from under all their beaks.")),
        Moment2(emptySet(), Tx("Кто-то мелом нарисовал на асфальте классики до самого угла.", "Someone has chalked hopscotch on the pavement all the way to the corner.")),
        Moment2(emptySet(), Tx("Из подвальной мастерской доносится радио и стук молотка.", "A radio and a hammer from a basement workshop.")),
        Moment2(setOf(3, 4), Tx("С крыш капает. Под водосточной трубой — целое озеро.", "Dripping from the roofs. Under a drainpipe — a whole lake.")),
        Moment2(setOf(4, 5), Tx("У подъезда кто-то вынес на солнце рассаду в молочных пакетах.", "Someone has put seedlings in milk cartons out in the sun by a doorway.")),
        Moment2(setOf(5, 6), Tx("Тополиный пух собрался у бордюра в маленькие сугробы.", "Poplar fluff has gathered by the kerb in little drifts.")),
        Moment2(setOf(5, 6, 7), Tx("Под окнами цветёт сирень, и кто-то уже обломал веточку.", "Lilac in bloom under the windows, and someone's already broken off a sprig.")),
        Moment2(setOf(6, 7, 8), Tx("Поливальная машина прошла по улице. Пахнет мокрым асфальтом.", "The street sprinkler has gone by. It smells of wet tarmac.")),
        Moment2(setOf(7, 8), Tx("На балконе третьего этажа сохнут полотенца с подсолнухами.", "Sunflower towels drying on a third-floor balcony.")),
        Moment2(setOf(9, 10), Tx("Дворник сгребает листья в кучу. Ребёнок тут же в неё прыгает.", "The janitor rakes the leaves into a pile. A child jumps straight in.")),
        Moment2(setOf(10, 11), Tx("В окнах уже горит свет, хотя только пять часов.", "Lights already on in the windows, and it's only five.")),
        Moment2(setOf(12, 1), Tx("Кто-то слепил у подъезда снеговика с морковкой набок.", "Someone's made a snowman by the doorway, carrot askew.")),
        Moment2(setOf(12, 1, 2), Tx("Под ногами скрипит снег. Скрип-скрип — единственный звук на улице.", "Snow creaks underfoot. Creak, creak — the only sound in the street.")),
        Moment2(setOf(12), Tx("В окне напротив наряжают ёлку. Кто-то машет вам рукой.", "Across the way they're trimming a tree. Someone waves at you.")),
    )
}
