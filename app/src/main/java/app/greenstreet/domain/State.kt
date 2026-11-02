package app.greenstreet.domain

import org.json.JSONArray
import org.json.JSONObject

/** A point on the map, in degrees. */
data class Geo(val lat: Double, val lon: Double)

/** A bed sown on a lawn (or a pot on the sill, a box on the balcony, a bed in the shared garden). */
class Bed(
    val id: Long,
    val variety: String,
    val lat: Double,
    val lon: Double,
    val sown: Long,
    var grown: Double = 0.0,
    var tended: Int = 0,
    var lastTend: Long = -1,
    var idle: Int = 0,
    var wild: Boolean = false,
    /** Sown by the wind from the tram: grows on its own, comes up wild. */
    var self: Boolean = false,
    var harvests: Int = 0,
)

/** The player's ties with one neighbour. */
class Bond(
    var open: Long = -1,
    var points: Int = 0,
    /** Index (0-based) of the next request in the chain. */
    var req: Int = 0,
    /** The day the current request appeared, or -1 while none is showing. */
    var reqShown: Long = -1,
    /** Day the last request was fulfilled (or the bond opened). */
    var reqDone: Long = -1,
    var note: Int = 0,
    var noteDone: Long = -1,
    var visited: Long = -1,
    var gifted: Long = -1,
    var seeded: Long = -1,
    /** What the player found out about tastes: key → "love" / "like" / "dislike" / "plain". */
    val known: MutableMap<String, String> = HashMap(),
) {
    val isOpen get() = open >= 0
}

/** A note in the letterbox. */
data class Mail(val key: String, val day: Long, var read: Boolean = false)

/** A notice pinned on the district board. */
data class Pinned(val key: String, val day: Long)

/** Everything the player has done, saved as JSON. */
class GameState {
    /** The epoch day the game started; years of play count from here. */
    var start: Long = 0
    var home: Geo? = null
    /** The district box (south, west, north, east) the map was downloaded for. */
    var box: DoubleArray? = null
    /** The last day the night has been run for. */
    var day: Long = 0
    var coins: Int = 15
    var km: Double = 0.0
    var kmToday: Double = 0.0
    var eggs: Int = 2
    var nextId: Long = 1
    val beds: MutableList<Bed> = ArrayList()
    val bag: MutableMap<String, Int> = LinkedHashMap()
    val seeds: MutableMap<String, Int> = LinkedHashMap()
    val recipes: MutableSet<String> = LinkedHashSet()
    val tools: MutableMap<String, Int> = HashMap()
    val bonds: MutableMap<String, Bond> = HashMap()
    val inbox: MutableList<Mail> = ArrayList()
    val board: MutableList<Pinned> = ArrayList()
    val boardDone: MutableSet<String> = HashSet()
    val flags: MutableSet<String> = LinkedHashSet()
    val album: MutableSet<String> = LinkedHashSet()
    val ribbons: MutableSet<String> = LinkedHashSet()
    /** Running totals for ribbons: "harvest", "harvest:<crop>", "fish", "fish:<key>", "cook", "wild", "request"… */
    val counts: MutableMap<String, Int> = HashMap()
    /** Festivals already celebrated, as "<key>:<year>". */
    val fests: MutableSet<String> = HashSet()
    /** Groats taken at the fair on [fairDay]. */
    var fair: Int = 0
    var fairDay: Long = -1

    fun tool(k: String) = tools[k] ?: 1
    fun bond(id: String) = bonds.getOrPut(id) { Bond() }
    fun add(map: MutableMap<String, Int>, k: String, n: Int) {
        val v = (map[k] ?: 0) + n
        if (v <= 0) map.remove(k) else map[k] = v
    }
    fun count(k: String, n: Int = 1) { counts[k] = (counts[k] ?: 0) + n }

    fun toJson(): JSONObject = JSONObject().apply {
        put("v", 1); put("start", start); put("day", day); put("coins", coins); put("km", km); put("kmToday", kmToday)
        put("eggs", eggs); put("nextId", nextId); put("fair", fair); put("fairDay", fairDay)
        home?.let { put("home", JSONObject().put("lat", it.lat).put("lon", it.lon)) }
        box?.let { put("box", JSONArray(it.toList())) }
        put("beds", JSONArray().apply {
            beds.forEach { b ->
                put(JSONObject().put("id", b.id).put("v", b.variety).put("lat", b.lat).put("lon", b.lon).put("sown", b.sown)
                    .put("grown", b.grown).put("tended", b.tended).put("lastTend", b.lastTend).put("idle", b.idle)
                    .put("wild", b.wild).put("self", b.self).put("harvests", b.harvests))
            }
        })
        put("bag", JSONObject(bag as Map<*, *>)); put("seeds", JSONObject(seeds as Map<*, *>)); put("tools", JSONObject(tools as Map<*, *>))
        put("counts", JSONObject(counts as Map<*, *>))
        put("recipes", JSONArray(recipes)); put("flags", JSONArray(flags)); put("album", JSONArray(album)); put("ribbons", JSONArray(ribbons))
        put("fests", JSONArray(fests)); put("boardDone", JSONArray(boardDone))
        put("bonds", JSONObject().apply {
            bonds.forEach { (id, b) ->
                put(id, JSONObject().put("open", b.open).put("points", b.points).put("req", b.req).put("reqShown", b.reqShown)
                    .put("reqDone", b.reqDone).put("note", b.note).put("noteDone", b.noteDone).put("visited", b.visited)
                    .put("gifted", b.gifted).put("seeded", b.seeded).put("known", JSONObject(b.known as Map<*, *>)))
            }
        })
        put("inbox", JSONArray().apply { inbox.forEach { put(JSONObject().put("k", it.key).put("d", it.day).put("r", it.read)) } })
        put("board", JSONArray().apply { board.forEach { put(JSONObject().put("k", it.key).put("d", it.day)) } })
    }

    companion object {
        fun fromJson(o: JSONObject): GameState = GameState().apply {
            start = o.optLong("start"); day = o.optLong("day"); coins = o.optInt("coins", 15); km = o.optDouble("km", 0.0)
            kmToday = o.optDouble("kmToday", 0.0); eggs = o.optInt("eggs"); nextId = o.optLong("nextId", 1)
            fair = o.optInt("fair"); fairDay = o.optLong("fairDay", -1)
            o.optJSONObject("home")?.let { home = Geo(it.getDouble("lat"), it.getDouble("lon")) }
            o.optJSONArray("box")?.let { a -> box = DoubleArray(a.length()) { a.getDouble(it) } }
            o.optJSONArray("beds")?.let { a ->
                for (i in 0 until a.length()) {
                    val b = a.getJSONObject(i)
                    beds += Bed(b.getLong("id"), b.getString("v"), b.getDouble("lat"), b.getDouble("lon"), b.getLong("sown"),
                        b.optDouble("grown", 0.0), b.optInt("tended"), b.optLong("lastTend", -1), b.optInt("idle"),
                        b.optBoolean("wild"), b.optBoolean("self"), b.optInt("harvests"))
                }
            }
            fun ints(k: String, into: MutableMap<String, Int>) = o.optJSONObject(k)?.let { j -> j.keys().forEach { into[it] = j.getInt(it) } }
            ints("bag", bag); ints("seeds", seeds); ints("tools", tools); ints("counts", counts)
            fun strs(k: String, into: MutableSet<String>) = o.optJSONArray(k)?.let { a -> for (i in 0 until a.length()) into += a.getString(i) }
            strs("recipes", recipes); strs("flags", flags); strs("album", album); strs("ribbons", ribbons); strs("fests", fests); strs("boardDone", boardDone)
            o.optJSONObject("bonds")?.let { j ->
                j.keys().forEach { id ->
                    val b = j.getJSONObject(id)
                    bonds[id] = Bond(b.optLong("open", -1), b.optInt("points"), b.optInt("req"), b.optLong("reqShown", -1), b.optLong("reqDone", -1),
                        b.optInt("note"), b.optLong("noteDone", -1), b.optLong("visited", -1), b.optLong("gifted", -1), b.optLong("seeded", -1)).also { bond ->
                        b.optJSONObject("known")?.let { k -> k.keys().forEach { key -> bond.known[key] = k.getString(key) } }
                    }
                }
            }
            o.optJSONArray("inbox")?.let { a -> for (i in 0 until a.length()) a.getJSONObject(i).let { inbox += Mail(it.getString("k"), it.getLong("d"), it.optBoolean("r")) } }
            o.optJSONArray("board")?.let { a -> for (i in 0 until a.length()) a.getJSONObject(i).let { board += Pinned(it.getString("k"), it.getLong("d")) } }
        }
    }
}
