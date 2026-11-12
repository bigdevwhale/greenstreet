package app.greenstreet.data

import android.content.Context
import app.greenstreet.domain.GameState
import app.greenstreet.domain.Sky
import org.json.JSONObject
import java.io.File

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
        val o = runCatching { JSONObject(skiesFile.readText()) }.getOrDefault(JSONObject())
        val old = o.optJSONObject(day.toString())
        if (ahead && old != null) return
        val keep = old != null && !old.optBoolean("a")
        fun b(k: String, v: Boolean) = v || keep && old!!.optBoolean(k)
        o.put(day.toString(), JSONObject().put("r", b("r", sky.rain)).put("s", b("s", sky.snow)).put("f", b("f", sky.frost)).put("g", b("g", sky.fog))
            .put("u", b("u", sky.sun)).put("h", b("h", sky.heat)).put("t", sky.temp).apply { if (ahead) put("a", true) })
        o.keys().asSequence().toList().filter { (it.toLongOrNull() ?: 0) < day - 60 }.forEach { o.remove(it) }
        write(skiesFile, o.toString())
    }

    fun sky(day: Long): Sky? {
        val o = runCatching { JSONObject(skiesFile.readText()) }.getOrNull()?.optJSONObject(day.toString()) ?: return null
        return Sky(o.optBoolean("r"), o.optBoolean("s"), o.optBoolean("f"), o.optBoolean("g"), o.optBoolean("u"), o.optBoolean("h"), o.optDouble("t", 10.0))
    }

    private fun write(f: File, text: String) {
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
    }
}
