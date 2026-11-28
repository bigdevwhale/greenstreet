package app.greenstreet.domain

/**
 * Sorts raw positions before they touch the game (#58, #60), after Fogwalk's Track and Dot. A phone mixes
 * satellite fixes with coarse cell ones, hands a remembered fix in after a newer one, and now and then throws
 * a good-looking fix hundreds of metres off; taken as they come, they put the player off the map and out of home.
 *
 * - a fix older than the last one is dropped;
 * - a rough fix (worse than [weak] metres) never walks: it only moves the player while the satellites have been
 *   silent for [GPS_FRESH_MS], and then only if it's no worse than the one shown, lands inside its circle, or the
 *   shown one is stale ([STALE_MS]), so the cell towers don't toss the player about;
 * - a lone good fix further off than a walker could have gone is dropped; three in a row mean a real move
 *   ([Use.JUMP]: the player is there now, but didn't walk the way).
 */
class Fixes(private val weak: Float) {
    enum class Use { DROP, SHOW, WALK, JUMP }

    private class Raw(val at: Geo, val acc: Float, val time: Long)

    private var shown: Raw? = null
    private var good: Raw? = null
    private var rejects = 0

    /** What a fix at [at], [acc] metres sure, taken at [time] ms (a monotonic clock), may do. */
    fun judge(at: Geo, acc: Float, time: Long): Use {
        val s = shown
        if (s != null && time < s.time) return Use.DROP
        val raw = Raw(at, acc, time)
        val g = good
        if (acc > weak) {
            if (g != null && time - g.time < GPS_FRESH_MS) return Use.DROP
            if (s != null && time - s.time <= STALE_MS && acc > s.acc && s.at.metres(at) > s.acc) return Use.DROP
            shown = raw
            return Use.SHOW
        }
        if (g != null) {
            val possible = g.acc + acc + (time - g.time) / 1000.0 * MAX_SPEED + SLACK_M
            if (g.at.metres(at) > possible) {
                if (++rejects < JUMP_FIXES) return Use.DROP
                rejects = 0; good = raw; shown = raw
                return Use.JUMP
            }
        }
        rejects = 0; good = raw; shown = raw
        return Use.WALK
    }

    /** Forgets what came before: the next fix is taken whatever it is (the demo's long-press, a new home). */
    fun reset() { shown = null; good = null; rejects = 0 }

    companion object {
        /** Satellites heard this recently keep the cell towers from moving the player, ms. */
        const val GPS_FRESH_MS = 30_000L
        /** A shown fix this old gives way to any newer one, ms. */
        const val STALE_MS = 15_000L
        /** Faster than this between two fixes, m/s, is a glitch, not a walker (a bike or a tram included). */
        const val MAX_SPEED = 12.0
        const val SLACK_M = 15.0
        /** So many far fixes in a row mean the player really is there. */
        const val JUMP_FIXES = 3
    }
}
