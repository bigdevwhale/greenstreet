package app.greenstreet.data

import java.io.InputStream

/** A pair of texts, Russian and English. */
data class Tx(val ru: String, val en: String) {
    operator fun invoke(ru: Boolean) = if (ru) this.ru.ifBlank { en } else en.ifBlank { this.ru }

    companion object {
        val EMPTY = Tx("", "")
    }
}

enum class Place { VERGE, SILL, BALCONY, GARDEN }

/** One variety of a crop: what is sown, how long it takes, what it gives. */
data class Variety(
    val key: String,
    val crop: String,
    val place: Place,
    val sow: Set<Int>,
    val days: Int,
    val tends: Int,
    val regrow: Int,
    val yield: Int,
    val price: Int,
    val art: String,
    val ramp: String,
    val unlock: String,
    val name: Tx,
    val about: Tx,
)

enum class GoodKind { YARD, FISH, FIND, STAPLE }

data class Good(
    val key: String,
    val kind: GoodKind,
    val months: Set<Int>,
    val where: String,
    val time: String,
    val weather: String,
    val price: Int,
    val rare: Int,
    val name: Tx,
    val about: Tx,
)

/** `key:qty`, where key may be a crop species, a variety, a good, a product or a group. */
data class Need(val key: String, val qty: Int)

data class Recipe(
    val key: String,
    val needs: List<Need>,
    val makes: String,
    val qty: Int,
    val station: String,
    val unlock: String,
    val price: Int,
    val name: Tx,
    val about: Tx,
)

sealed interface Reward {
    data class Coins(val n: Int) : Reward
    data class Seeds(val variety: String, val n: Int) : Reward
    data class Item(val key: String, val n: Int) : Reward
    data class Recipe(val key: String) : Reward
    data class Tool(val key: String) : Reward
}

data class Person(
    val id: String,
    val yard: Boolean,
    val kind: String,
    /** OSM tags matched in order: listOf("shop" to "bakery", …). */
    val osm: List<Pair<String, String>>,
    val seeds: List<String>,
    val likes: Set<String>,
    val loves: Set<String>,
    val dislikes: Set<String>,
    val look: Map<String, String>,
    val name: Tx,
    val role: Tx,
    val about: Tx,
    /** Working hours: from [open] to [close] o'clock, not on [off] days (ISO, 1 = Monday). */
    val open: Int = 9,
    val close: Int = 19,
    val off: Set<Int> = emptySet(),
    /** Their own words at the first meeting, and what they are good for. */
    val intro: Tx = Tx("", ""),
    val perk: Tx = Tx("", ""),
    /** The notebook: what the player learns about them, by hearts. */
    val facts: List<Fact> = emptyList(),
)

/** A line in the player's notebook about a neighbour, opened at [hearts]. */
data class Fact(val hearts: Int, val text: Tx)

/** What has to hold for a line or a note: seasons, weather, time, years, hearts, festivals, flags. */
data class When(val tokens: List<String>) {
    val years: Set<Int>
        get() = tokens.mapNotNull {
            when (it) {
                "y1" -> 1; "y2" -> 2; "y3" -> 3; else -> null
            }
        }.toSet()

    companion object {
        val ANY = When(emptyList())
        fun of(s: String) = if (s.isBlank()) ANY else When(s.split(',').map { it.trim() }.filter { it.isNotEmpty() })
    }
}

data class Line(val who: String, val months: Set<Int>, val `when`: When, val hearts: Int, val text: Tx)

data class Request(
    val who: String,
    val n: Int,
    val gap: Int,
    val months: Set<Int>,
    val `when`: When,
    val hearts: Int,
    val needs: List<Need>,
    val rewards: List<Reward>,
    val ask: Tx,
    val thanks: Tx,
    val sets: String,
) {
    val key get() = "$who:$n"
}

data class Note(
    val who: String,
    val n: Int,
    val gap: Int,
    val months: Set<Int>,
    val `when`: When,
    val hearts: Int,
    val flags: List<String>,
    val rewards: List<Reward>,
    val text: Tx,
    val sets: String,
) {
    val key get() = "$who:$n"
}

data class Festival(
    val key: String,
    /** "MM-DD" or empty when [rule] decides the day. */
    val md: String,
    val rule: String,
    /** 1, 2, 3, or 0 for "any year". */
    val year: Int,
    val name: Tx,
    val notice: Tx,
    val gift: List<Reward>,
    val ribbon: String,
)

data class Notice(val key: String, val months: Set<Int>, val needs: List<Need>, val rewards: List<Reward>, val text: Tx)

data class Ribbon(val key: String, val kind: String, val arg: String, val name: Tx, val about: Tx)

/** Everything the game knows that isn't the player's progress. */
class Content(
    val varieties: List<Variety>,
    val goods: List<Good>,
    val recipes: List<Recipe>,
    val people: List<Person>,
    val lines: Map<String, List<Line>>,
    val requests: Map<String, List<Request>>,
    val notes: Map<String, List<Note>>,
    val festivals: List<Festival>,
    /** "MM-DD" → line. */
    val almanac: Map<String, Tx>,
    val board: List<Notice>,
    val ribbons: List<Ribbon>,
) {
    val variety = varieties.associateBy { it.key }
    val good = goods.associateBy { it.key }
    val person = people.associateBy { it.id }
    val crops: Map<String, List<Variety>> = varieties.groupBy { it.crop }
    val recipesFor: Map<String, List<Recipe>> = recipes.groupBy { it.makes }
    val products: Set<String> = recipes.map { it.makes }.toSet()

    /** Display name of anything that can sit in the bag. */
    fun name(key: String): Tx = variety[key]?.name ?: good[key]?.name ?: recipesFor[key]?.firstOrNull()?.name
        ?: crops[key]?.firstOrNull()?.let { v -> Tx(v.name.ru.substringBefore(" «").trim(), v.name.en.substringBefore(" '").trim()) }
        ?: GROUP_NAMES[key] ?: Tx(key, key)

    fun price(key: String): Int = variety[key]?.price ?: good[key]?.price ?: recipesFor[key]?.firstOrNull()?.price
        ?: crops[key]?.minOfOrNull { it.price } ?: 1

    /** The species a bag key belongs to: a variety's crop, else the key itself. */
    fun speciesOf(key: String): String = variety[key]?.crop ?: key

    /** Does a bag key satisfy a need key (a species, a variety, a group or the same key)? */
    fun matches(need: String, have: String): Boolean {
        if (need == have) return true
        if (variety[have]?.crop == need) return true
        val g = groups[need] ?: return false
        return have in g || speciesOf(have) in g
    }

    val groups: Map<String, Set<String>> by lazy {
        val fish = goods.filter { it.kind == GoodKind.FISH }.map { it.key }.toSet()
        val finds = goods.filter { it.kind == GoodKind.FIND }.map { it.key }.toSet()
        mapOf(
            "anyfish" to fish,
            "anymush" to setOf("chanterelle", "boletus", "suillus", "honey_fungus", "porcini", "russula", "morel"),
            "anyberry" to BERRIES,
            "anyflower" to FLOWERS,
            "anyherb" to HERBS,
            "anyveg" to crops.keys.filter { it !in FLOWERS && it !in HERBS && it !in BERRIES && it !in NONFOOD }.toSet(),
            "anyjam" to products.filter { it.startsWith("jam_") }.toSet(),
            "anytea" to products.filter { it.startsWith("tea_") }.toSet(),
            "anybread" to setOf("bread_rye", "bread_wheat", "kalach", "bubliki"),
            "anycloth" to setOf("linen_cloth", "cloth_blue", "cloth_red", "cloth_yellow"),
            "anyfind" to finds,
        )
    }

    companion object {
        val FLOWERS = setOf("chamomile", "calendula", "cornflower", "nasturtium", "marigold", "sweetpea", "aster", "cosmos", "tulip", "daffodil", "crocus", "lavender", "hyacinth", "amaryllis")
        val HERBS = setOf("dill", "parsley", "coriander", "mint", "basil", "thyme", "valerian", "catnip", "sorrel")
        val BERRIES = setOf("strawberry", "raspberry", "currant", "gooseberry", "honeysuckle", "wild_strawberry", "wild_raspberry", "rowan", "rosehip", "elderberry", "sea_buckthorn", "bird_cherry")
        val NONFOOD = setOf("flax", "woad", "madder", "lupine", "catnip", "valerian") + FLOWERS
        val GROUP_NAMES = mapOf(
            "anyfish" to Tx("любая рыба", "any fish"), "anymush" to Tx("любые грибы", "any mushrooms"),
            "anyberry" to Tx("любые ягоды", "any berries"), "anyflower" to Tx("любые цветы", "any flowers"),
            "anyherb" to Tx("любая зелень", "any herbs"), "anyveg" to Tx("любые овощи", "any vegetables"),
            "anyjam" to Tx("любое варенье", "any jam"), "anytea" to Tx("любой чай", "any tea"),
            "anybread" to Tx("любой хлеб", "any bread"), "anycloth" to Tx("любая ткань", "any cloth"),
            "anyfind" to Tx("любая находка", "any find"),
        )

        /** Reads the content from a directory-like source: [open] returns a file by its relative path or null. */
        fun read(open: (String) -> InputStream?, ids: List<String>? = null): Content {
            fun rows(path: String): List<Map<String, String>> {
                val text = open(path)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }?.replace("\r", "") ?: return emptyList()
                val lines = text.split('\n').filter { it.isNotBlank() }
                if (lines.isEmpty()) return emptyList()
                val head = lines[0].split('\t')
                return lines.drop(1).map { l -> val c = l.split('\t'); head.indices.associate { head[it] to c.getOrElse(it) { "" } } }
            }
            fun Map<String, String>.tx(c: String) = Tx(this["${c}_ru"].orEmpty(), this["${c}_en"].orEmpty())
            fun Map<String, String>.int(c: String, d: Int = 0) = this[c]?.trim()?.toIntOrNull() ?: d

            val varieties = rows("crops.tsv").map {
                Variety(it.getValue("key"), it.getValue("crop"), Place.valueOf(it.getValue("place").uppercase()), months(it["sow"]),
                    it.int("days", 30), it.int("tends", 10), it.int("regrow"), it.int("yield", 1), it.int("price", 1),
                    it["art"].orEmpty(), it["ramp"].orEmpty(), it["unlock"].orEmpty(), it.tx("name"), it.tx("about"))
            }
            val goods = rows("goods.tsv").map {
                Good(it.getValue("key"), GoodKind.valueOf(it.getValue("kind").uppercase()), months(it["months"]), it["where"].orEmpty(),
                    it["time"].orEmpty(), it["weather"].orEmpty(), it.int("price", 1), it.int("rare"), it.tx("name"), it.tx("about"))
            }
            val recipes = rows("recipes.tsv").map {
                Recipe(it.getValue("key"), needs(it["needs"]), it.getValue("makes"), it.int("qty", 1), it["station"].orEmpty(),
                    it["unlock"].orEmpty(), it.int("price", 1), it.tx("name"), it.tx("about"))
            }
            val acq = rows("acquaint.tsv").associateBy { it["id"].orEmpty() }
            val facts = rows("facts.tsv").groupBy { it["who"].orEmpty() }
            val people = rows("people.tsv").map {
                val h = it["hours"].orEmpty()
                val span = h.substringBefore('/').split('-').mapNotNull { x -> x.trim().toIntOrNull() }
                val id = it.getValue("id")
                Person(it.getValue("id"), it["home"] == "yard", it["kind"].orEmpty(),
                    it["osm"].orEmpty().split('|').filter { s -> '=' in s }.map { s -> s.substringBefore('=') to s.substringAfter('=') },
                    list(it["seeds"]), list(it["likes"]).toSet(), list(it["loves"]).toSet(), list(it["dislikes"]).toSet(),
                    list(it["look"]).associate { s -> s.substringBefore('=') to s.substringAfter('=', "1") },
                    it.tx("name"), it.tx("role"), it.tx("about"),
                    span.getOrElse(0) { 9 }, span.getOrElse(1) { 19 }, months(h.substringAfter('/', "")),
                    acq[id]?.tx("intro") ?: Tx("", ""), acq[id]?.tx("perk") ?: Tx("", ""),
                    facts[id].orEmpty().map { f -> Fact(f.int("hearts"), Tx(f["ru"].orEmpty(), f["en"].orEmpty())) }.sortedBy { f -> f.hearts })
            }
            val lines = HashMap<String, List<Line>>()
            val reqs = HashMap<String, List<Request>>()
            val notes = HashMap<String, List<Note>>()
            for (id in ids ?: people.map { it.id }) {
                val r = rows("people/$id.tsv")
                lines[id] = r.filter { it["t"] == "line" }.map { Line(id, months(it["months"]), When.of(it["when"].orEmpty()), it.int("hearts"), it.tx("text")) }
                reqs[id] = r.filter { it["t"] == "req" }.map {
                    Request(id, it.int("n"), it.int("gap"), months(it["months"]), When.of(it["when"].orEmpty()), it.int("hearts"),
                        needs(it["needs"]), rewards(it["reward"]), it.tx("text"), it.tx("reply"), it["sets"].orEmpty())
                }.sortedBy { it.n }
                notes[id] = r.filter { it["t"] == "note" }.map {
                    Note(id, it.int("n"), it.int("gap"), months(it["months"]), When.of(it["when"].orEmpty()), it.int("hearts"),
                        list(it["needs"]), rewards(it["reward"]), it.tx("text"), it["sets"].orEmpty())
                }.sortedBy { it.n }
            }
            val festivals = rows("festivals.tsv").map {
                Festival(it.getValue("key"), it["md"].orEmpty(), it["rule"].orEmpty(), it["year"]?.toIntOrNull() ?: 0,
                    it.tx("name"), it.tx("notice"), rewards(it["gift"]), it["ribbon"].orEmpty())
            }
            val almanac = rows("almanac.tsv").associate { it.getValue("md") to it.tx("text") }
            val board = rows("board.tsv").map { Notice(it.getValue("key"), months(it["months"]), needs(it["needs"]), rewards(it["reward"]), it.tx("text")) }
            val ribbons = rows("ribbons.tsv").map { Ribbon(it.getValue("key"), it["kind"].orEmpty(), it["arg"].orEmpty(), it.tx("name"), it.tx("about")) }
            return Content(varieties, goods, recipes, people, lines, reqs, notes, festivals, almanac, board, ribbons)
        }

        fun months(s: String?): Set<Int> = s.orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
        fun list(s: String?): List<String> = s.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        fun needs(s: String?): List<Need> = list(s).mapNotNull { p ->
            val k = p.substringBefore(':')
            val n = p.substringAfter(':', "1").toIntOrNull() ?: return@mapNotNull null
            Need(k, n)
        }

        fun rewards(s: String?): List<Reward> = list(s).mapNotNull { p ->
            val b = p.split(':')
            when (b[0]) {
                "coins" -> b.getOrNull(1)?.toIntOrNull()?.let { Reward.Coins(it) }
                "seeds" -> if (b.size == 3) Reward.Seeds(b[1], b[2].toIntOrNull() ?: 1) else null
                "item" -> if (b.size == 3) Reward.Item(b[1], b[2].toIntOrNull() ?: 1) else null
                "recipe" -> b.getOrNull(1)?.let { Reward.Recipe(it) }
                "tool" -> b.getOrNull(1)?.let { Reward.Tool(it) }
                else -> null
            }
        }
    }
}
