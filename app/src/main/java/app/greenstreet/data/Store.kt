package app.greenstreet.data

import android.content.Context
import app.greenstreet.domain.GameState
import app.greenstreet.domain.Sky
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Saves the game as JSON in the app's files, atomically (write aside, then rename). Also keeps the raw
 * district map and a short log of the skies, so missed nights are replayed with the real weather.
 */
class Store(context: Context) {
    private val dir = context.filesDir
    private val stateFile = File(dir, "state.json")
    private val districtFile = File(dir, "district.json")
    private val tilesFile = File(dir, "district.bin")
    private val skiesFile = File(dir, "skies.json")

    fun load(): GameState? = runCatching { GameState.fromJson(JSONObject(stateFile.readText())) }.getOrNull()

    @Synchronized
    fun save(s: GameState) = write(stateFile, s.toJson().toString())

    fun district(): String? = districtFile.takeIf { it.exists() }?.readText()
    fun saveDistrict(json: String) = write(districtFile, json)

    /** The district as vector tiles (the normal case); the JSON above is only the demo's bundled Overpass dump. */
    fun tiles(): ByteArray? = tilesFile.takeIf { it.exists() }?.readBytes()
    fun saveTiles(b: ByteArray) { val tmp = File(dir, "district.bin.tmp"); tmp.writeBytes(b); if (!tmp.renameTo(tilesFile)) { tilesFile.delete(); tmp.renameTo(tilesFile) } }
    fun hasDistrict() = tilesFile.exists() || districtFile.exists()

    /**
     * Remembers the sky of a day (epoch day → sky), keeping the last 60 days. A forecast only covers the hours
     * still ahead, so an evening one knows nothing of the morning's rain: what was once seen for the day is
     * kept (rain, snow and the rest stay on), only the temperature takes the latest word. [ahead] marks a guess
     * for a day still to come (tomorrow's forecast): it fills a gap but never adds to a day already on record.
     */
    @Synchronized
    fun remember(day: Long, sky: Sky, ahead: Boolean = false) {
        val o = skies()
        val old = o.optJSONObject(day.toString())
        if (ahead && old != null) return
        val keep = old != null && !old.optBoolean("a")
        fun b(k: String, v: Boolean) = v || keep && old!!.optBoolean(k)
        o.put(day.toString(), JSONObject().put("r", b("r", sky.rain)).put("s", b("s", sky.snow)).put("f", b("f", sky.frost)).put("g", b("g", sky.fog))
            .put("u", b("u", sky.sun)).put("h", b("h", sky.heat)).put("t", sky.temp).apply { if (ahead) put("a", true) })
        o.keys().asSequence().toList().filter { (it.toLongOrNull() ?: 0) < day - 60 }.forEach { o.remove(it) }
        write(skiesFile, o.toString())
    }

    /** The skies on record, read from disk once and then kept in memory (the screen asks for today's on every frame). */
    private var skiesCache: JSONObject? = null
    private fun skies(): JSONObject = skiesCache ?: runCatching { JSONObject(skiesFile.readText()) }.getOrDefault(JSONObject()).also { skiesCache = it }

    @Synchronized
    fun sky(day: Long): Sky? {
        val o = skies().optJSONObject(day.toString()) ?: return null
        return Sky(o.optBoolean("r"), o.optBoolean("s"), o.optBoolean("f"), o.optBoolean("g"), o.optBoolean("u"), o.optBoolean("h"), o.optDouble("t", 10.0))
    }

    /** The game packed to carry to another phone: a zip of the save, the district map and the skies on record. */
    @Synchronized
    fun pack(s: GameState): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            fun put(name: String, b: ByteArray) { z.putNextEntry(ZipEntry(name)); z.write(b); z.closeEntry() }
            put(MARK, JSONObject().put("app", "app.greenstreet").put("v", 1).toString().toByteArray())
            put("state.json", s.toJson().toString().toByteArray())
            tilesFile.takeIf { it.exists() }?.let { put("district.bin", it.readBytes()) }
            if (!tilesFile.exists()) districtFile.takeIf { it.exists() }?.let { put("district.json", it.readBytes()) }
            skiesFile.takeIf { it.exists() }?.let { put("skies.json", it.readBytes()) }
        }
        return out.toByteArray()
    }

    /** Puts an unpacked game in place of this phone's: the map it came with replaces the old one. */
    @Synchronized
    fun install(p: Packed) {
        if (p.tiles != null) { saveTiles(p.tiles); districtFile.delete() }
        else if (p.json != null) { saveDistrict(p.json); tilesFile.delete() }
        if (p.skies != null) write(skiesFile, p.skies) else skiesFile.delete()
        skiesCache = null
        write(stateFile, p.state.toJson().toString())
    }

    /** What a carried file holds; [tiles] or [json] is the district map, or neither when it came without one. */
    data class Packed(val state: GameState, val tiles: ByteArray?, val json: String?, val skies: String?)

    companion object {
        private const val MARK = "greenstreet.json"
        private const val MAX = 64L shl 20

        /** Reads a carried file, touching nothing; throws [IllegalArgumentException] when it isn't a Green Street save. */
        fun unpack(bytes: ByteArray): Packed {
            val got = HashMap<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    if (e.isDirectory) continue
                    val buf = ByteArrayOutputStream(); val chunk = ByteArray(1 shl 16)
                    while (true) { val n = z.read(chunk); if (n < 0) break; buf.write(chunk, 0, n); require(buf.size() <= MAX) { "file too large" } }
                    got[e.name] = buf.toByteArray()
                }
            }
            require(MARK in got && "state.json" in got) { "not a Green Street save" }
            val state = runCatching { GameState.fromJson(JSONObject(got.getValue("state.json").decodeToString())) }.getOrNull()
            require(state?.home != null) { "the save is damaged" }
            val skies = got["skies.json"]?.decodeToString()?.takeIf { runCatching { JSONObject(it) }.isSuccess }
            return Packed(state!!, got["district.bin"], got["district.json"]?.decodeToString(), skies)
        }
    }

    private fun write(f: File, text: String) {
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
    }
}
