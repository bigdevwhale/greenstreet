package app.greenstreet.ui.art

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** A pot on the player's windowsill: what grows in it, drawn small. */
data class Pot(val art: String, val ramp: IntArray, val ripe: Boolean)

/**
 * Everything that makes today's courtyard this courtyard: the kind of home and its colour (from the map), the
 * year of play and the story so far, who is out at this hour, the season and the festival, the player's own pots.
 */
data class YardLife(
    val fac: IntArray = G.ochre,
    val seed: Int = 0,
    val year: Int = 1,
    val eggs: Int = 2,
    val hive: Boolean = false,
    val balcony: Boolean = false,
    val garden: Boolean = false,
    val weekday: Int = 1,
    val festival: String? = null,
    val flags: Set<String> = emptySet(),
    val pots: List<Pot> = emptyList(),
    /** "well" (an old city courtyard), "block" (a block of flats), "house" (a house of one's own). */
    val kind: String = "well",
    /** Who came out to talk: their look, drawn front and centre; "cat" in it means Pryanik. */
    val guest: Map<String, String>? = null,
)

/** The yard at home. It changes with the kind of home, the hour, the weather, the season, the years and the stories. */
private const val GUEST_X = 46

class YardScene(val m: Moment, val life: YardLife = YardLife()) : Scene(128, 104) {
    override val key get() = listOf(m.minute / 10, m.sky, m.month, m.day, life)
    private val keep = BooleanArray(w * h)
    private val hour get() = m.minute / 60
    private val day get() = m.day.toInt()
    private val weekend get() = life.weekday >= 6
    private val newYear get() = (m.month == 12 && dayOfMonth() >= 20) || (m.month == 1 && dayOfMonth() <= 10)
    private fun dayOfMonth() = java.time.LocalDate.ofEpochDay(m.day).dayOfMonth

    /** Where the front-door lamp hangs; set by the backdrop. */
    private var lampX = 85
    private var lampY = 54

    private fun Buf.rr(x: Int, y: Int, w: Int, h: Int, r: IntArray, v: (Int, Int) -> Double) {
        for (j in 0 until h) for (i in 0 until w) p(x + i, y + j, rp(r, v(i, j), x + i, y + j))
    }

    override fun base(b: Buf) {
        when (life.kind) { "block" -> blockBackdrop(b); "house" -> houseBackdrop(b); else -> wellBackdrop(b) }
        ground(b)
        washing(b)
        coop(b)
        if (life.garden) sharedGarden(b) else planters(b)
        hives(b)
        season(b)
        b.r(96, 90, 22, 2, R.wood[5]); b.r(96, 92, 22, 1, R.wood[2]); b.r(97, 93, 1, 4, R.iron[1]); b.r(116, 93, 1, 4, R.iron[1]); b.r(96, 86, 22, 2, R.wood[4])
        people(b)
        cat(b)
    }

    // ---------- the three homes ----------

    /** An old city house around a courtyard-well: walls on three sides, an arch to the street. */
    private fun wellBackdrop(b: Buf) {
        val fac = life.fac
        b.skyBand(22, 0, 84, 14, m, 0)
        b.rr(22, 8, 84, 66, fac) { x, y -> .5 + (fbm(x * .2, y * .2) - .5) * .2 + if (hash(x + life.seed, y) < .02) -.15 else 0.0 }
        val trim = life.seed % 3
        b.rr(22, 8, 84, 2, fac) { _, j -> .82 - j * .3 }
        if (trim >= 1) for (x in 22 until 106 step 3) b.p(x, 10, fac[1])
        if (trim == 2) b.rr(22, 48, 84, 2, fac) { _, j -> .8 - j * .35 }
        val lit = m.dark > .12f
        for (row in 0 until 3) for (col in 0 until 5) {
            val wx = 27 + col * 16; val wy = 12 + row * 19
            if (row == 2 && (col == 0 || col == 3)) continue   // the arch and the front door
            val mine = row == 1 && col == 2
            val nina = row == 2 && col == 4
            if (mine && life.balcony) { balcony(b, wx, wy, lit); continue }
            val on = lit && (mine || hash(col * 7 + row + life.seed, day) < .55)
            b.r(wx - 1, wy - 1, 10, 14, R.cream[3])
            if (trim == 1) b.rr(wx - 1, wy - 3, 10, 2, fac) { _, j -> .85 - j * .2 }
            b.rr(wx, wy, 8, 12, if (on) R.honey else R.glass) { i, j -> if (on) .6 - j * .02 else .2 + if (i + j < 7) .3 else 0.0 }
            if (on) for (j in wy until wy + 12) for (i in wx until wx + 8) keep[j * w + i] = true
            b.r(wx + 4, wy, 1, 12, R.cream[2]); b.r(wx, wy + 5, 8, 1, R.cream[2])
            b.rr(wx - 1, wy + 12, 10, 1, R.stone) { _, _ -> .75 }
            for (i in -1 until 9) b.mul(wx + i, wy + 13, G.SHADOW, .4f)
            when {
                mine -> life.pots.take(3).forEachIndexed { k, pot -> pot(b, wx + 1 + k * 3, wy + 11, pot) }
                nina -> {
                    for (k in 0 until 3) { b.p(wx + 1 + k * 3, wy + 11, R.brick[3]); b.p(wx + 1 + k * 3, wy + 10, R.green[4]); if (m.month in 4..10) b.p(wx + 1 + k * 3, wy + 9, R.red[4]) }
                    if (hour in 9..19 && hash(day, 31) < .65) { b.ball(wx + 4, wy + 5, 2, R.skin, null); b.rr(wx + 2, wy + 2, 5, 2, R.lilac) { i, _ -> .6 - i * .05 } }
                }
                hash(col, row * 3 + life.seed) < .3 && m.month in 4..9 -> { b.p(wx + 1, wy + 11, R.green[5]); b.p(wx + 2, wy + 10, R.apple[4]); b.p(wx + 6, wy + 11, R.green[4]) }
            }
        }
        for (j in 0 until 20) for (i in 0 until 14) {
            val ax = 27 + i; val ay = 52 + j
            if (j < 4 && abs(i - 6.5) > Math.sqrt(max(0.0, 49.0 - (4 - j) * (4 - j) * 3.0))) continue
            b.p(ax, ay, rp(R.stone, .12 + j / 20.0 * .15 + if (i in 6..8 && j > 10) .35 else 0.0, ax, ay))
        }
        b.r(103, 8, 2, 66, R.iron[3]); b.r(104, 8, 1, 66, R.iron[5]); for (j in 10 until 74 step 12) b.r(102, j, 4, 1, R.iron[2])
        for (y in 0 until h) {
            for (x in 0 until 22) if (y <= 74 + (22 - x) * .6) b.p(x, y, rp(G.salmon, .32 + (fbm(x * .2, y * .2) - .5) * .15 + x * .006, x, y))
            for (x in 106 until w) if (y <= 74 + (x - 106) * .6) b.p(x, y, rp(G.salmon, .22 + (fbm(x * .2, y * .2) - .5) * .15, x, y))
        }
        for ((wx, wy) in listOf(6 to 16, 6 to 40, 112 to 18, 112 to 42)) {
            val on = lit && hash(wx, wy + day) < .5
            b.rr(wx, wy, 8, 12, if (on) R.honey else R.glass) { i, _ -> if (on) .55 else .15 + if (i < 3) .2 else 0.0 }
            if (on) for (j in wy until wy + 12) for (i in wx until wx + 8) keep[j * w + i] = true
        }
        b.rr(80, 58, 12, 16, G.bottle) { i, _ -> .45 - i * .02 }; b.r(85, 58, 1, 16, R.INK)
        b.rr(78, 55, 16, 3, R.iron) { i, _ -> .6 - i * .02 }; b.rr(84, 52, 3, 3, R.glass) { _, _ -> .85 }
        lampX = 85; lampY = 54
    }

    /** A block of flats across an open courtyard: panel seams, rows of small windows, birches, a swing. */
    private fun blockBackdrop(b: Buf) {
        b.skyBand(0, 0, w, 44, m, 0)
        val fac = IntArray(life.fac.size) { mix(life.fac[it], R.stone[it.coerceAtMost(R.stone.size - 1)], .45f) }
        b.rr(12, 3, 104, 71, fac) { x, y -> .55 + (fbm(x * .3, y * .3) - .5) * .08 - (if (y % 7 == 6) .12 else 0.0) - if ((x - 12) % 21 == 20) .1 else 0.0 }
        b.r(12, 3, 104, 1, fac[1])
        val lit = m.dark > .12f
        for (r in 0 until 9) for (c in 0 until 10) {
            val wx = 16 + c * 10; val wy = 6 + r * 7
            if ((r == 8 && c in 4..5) || (r == 7 && c in 4..5)) continue
            val mine = r == 4 && c == 6
            val nina = r == 8 && c == 2
            if (mine && life.balcony) {
                b.rr(wx - 1, wy - 1, 8, 7, R.glass) { i, j -> .6 + if ((i + j) % 5 == 0) .2 else 0.0 }
                for (k in 0 until 3) b.p(wx + k * 2, wy + 4, listOf(R.apple[4], R.green[5], R.honey[4])[k])
                if (lit) for (j in wy - 1 until wy + 6) for (i in wx - 1 until wx + 7) keep[j * w + i] = true
                continue
            }
            val on = lit && (mine || hash(c * 11 + r + life.seed, day) < .5)
            b.rr(wx, wy, 6, 4, if (on) R.honey else R.glass) { i, j -> if (on) .6 - j * .04 else .22 + if (i + j < 3) .3 else 0.0 }
            if (on) for (j in wy until wy + 4) for (i in wx until wx + 6) keep[j * w + i] = true
            b.r(wx, wy + 4, 6, 1, fac[4])
            if (mine) life.pots.take(2).forEachIndexed { k, pot -> pot(b, wx + 1 + k * 3, wy + 3, pot) }
            if (nina) for (k in 0 until 2) { b.p(wx + 1 + k * 3, wy + 3, R.green[4]); if (m.month in 4..10) b.p(wx + 1 + k * 3, wy + 2, R.red[4]) }
            if (r in 1..7 && c % 3 == 1 && hash(c, r + life.seed) < .35) b.rr(wx - 1, wy + 4, 8, 2, R.stone) { _, _ -> .6 }
        }
        b.rr(57, 62, 14, 12, G.bottle) { i, _ -> .4 - i * .02 }; b.r(63, 62, 1, 12, R.INK)
        b.rr(54, 59, 20, 3, R.stone) { _, j -> .7 - j * .15 }
        b.rr(72, 63, 2, 3, R.glass) { _, _ -> .85 }; b.p(72, 62, R.iron[2])
        lampX = 73; lampY = 64
        for (bx in listOf(6, 122)) {
            for (j in 0 until 40) { b.p(bx, 74 - j, if (j % 5 == 2) R.INK else R.cream[4]); b.p(bx + 1, 74 - j, R.cream[2]) }
            val ramp = when (m.month) { in 5..8 -> R.green; 9, 10 -> G.lime; else -> null }
            if (ramp != null) b.vol(bx - 7, 26, 16, 22, ell(8.0, 11.0, 8.0, 11.0), { u, v -> sph(8.0, 9.0, 8.0, 11.0)(u, v) + (fbm((bx + u) * .6, v * .6) - .5) * .5 }, ramp, mix(ramp[0], R.INK, .5f))
            else for (k in 0 until 6) b.line(bx, 50 - k * 3, bx + (if (k % 2 == 0) -6 else 6), 44 - k * 3, R.woodG[2])
        }
        b.r(30, 62, 1, 12, R.iron[3]); b.r(44, 62, 1, 12, R.iron[3]); b.r(30, 62, 15, 1, R.iron[3])
        b.line(36, 63, 36, 70, R.iron[4]); b.line(39, 63, 39, 70, R.iron[4]); b.r(35, 70, 5, 1, R.wood[4])
    }

    /** A house of its own: a log house with carved window frames, a picket fence, a well, the forest beyond. */
    private fun houseBackdrop(b: Buf) {
        b.skyBand(0, 0, w, 56, m, 0)
        for (x in 0 until w) {
            val top = 40 + (fbm(x * .08, 3.0) * 10).toInt()
            for (y in top until 66) b.p(x, y, rp(G.spruce, .25 + (y - top) * -.01 + fbm(x * .4, y * .4) * .25, x, y))
            if (x % 5 == 0) for (j in 0 until 4) b.p(x, top - j, G.spruce[2])
        }
        val logs = if (life.seed % 2 == 0) R.wood else R.woodG
        for (y in 38 until 74) for (x in 30 until 88) {
            var v = .5 + (if ((y - 38) % 3 == 2) -.22 else 0.0) + (fbm(x * .3, y * .5) - .5) * .12
            if (x < 32 || x > 85) v = .62 + if ((y - 38) % 3 == 1) .1 else -.1
            b.p(x, y, rp(logs, v, x, y))
        }
        val roof = listOf(G.rust, G.tin, G.roofGl, G.roofW)[life.seed % 4]
        for (j in 0 until 22) {
            val half = 4 + j * 1.55
            for (x in (59 - half).toInt()..(59 + half).toInt()) b.p(x, 17 + j, rp(roof, .7 - j * .015 + (if (x < 59) .1 else -.08) + if ((x + j) % 4 == 0) -.06 else 0.0, x, j))
        }
        b.rr(68, 14, 4, 9, R.brick) { i, _ -> .6 - i * .1 }
        b.ball(59, 30, 3, R.cream, R.INK); b.rr(57, 28, 5, 5, R.glass) { _, _ -> .3 }
        val lit = m.dark > .12f
        for ((k, wx) in listOf(36, 73).withIndex()) {
            val wy = 46
            val on = lit && (k == 1 || hash(day, 3) < .6)
            b.rr(wx - 3, wy - 4, 14, 16, R.cream) { _, j -> .9 - j * .01 }
            for (i in 0 until 14) if (i % 2 == 0) b.p(wx - 3 + i, wy - 5, R.roofB[3])
            b.rr(wx, wy, 8, 10, if (on) R.honey else R.glass) { i, j -> if (on) .6 - j * .02 else .22 + if (i + j < 6) .3 else 0.0 }
            if (on) for (j in wy until wy + 10) for (i in wx until wx + 8) keep[j * w + i] = true
            b.r(wx + 4, wy, 1, 10, R.cream[3]); b.r(wx, wy + 4, 8, 1, R.cream[3])
            b.rr(wx - 3, wy + 10, 14, 2, R.roofB) { _, _ -> .6 }
            if (k == 1) life.pots.take(3).forEachIndexed { n, pot -> pot(b, wx + 1 + n * 3, wy + 9, pot) }
        }
        b.rr(52, 56, 14, 18, R.wood) { _, j -> .45 + if (j > 13) (j - 13) * .08 else 0.0 }
        b.rr(55, 58, 8, 12, R.wood) { _, _ -> .25 }; b.p(61, 64, R.brass[4])
        b.rr(50, 53, 18, 3, roof) { i, _ -> .65 - i * .01 }
        lampX = 59; lampY = 55
        for (x in 0 until w) {
            if (x in 30..88) continue
            if (x % 3 != 2) for (y in 64 until 75) b.p(x, y, rp(R.cream, .55 + (if (y == 64) .2 else 0.0) - (x % 3) * .05, x, y))
            b.p(x, 67, R.wood[2]); b.p(x, 72, R.wood[2])
        }
        val apple = when (m.month) { in 5..8 -> R.green; 9, 10 -> R.autumn; else -> null }
        b.r(100, 46, 2, 20, R.wood[2])
        if (apple != null) {
            b.vol(91, 30, 20, 18, ell(10.0, 9.0, 10.0, 9.0), { u, v -> sph(10.0, 8.0, 10.0, 9.0)(u, v) + (fbm((91 + u) * .6, v * .6) - .5) * .5 }, apple, mix(apple[0], R.INK, .5f))
            if (m.month in 8..9) for (k in 0 until 8) b.p(93 + (hash(k, 3) * 16).toInt(), 33 + (hash(3, k) * 12).toInt(), R.apple[4])
        } else for (k in 0 until 6) b.line(101, 46, 92 + k * 4, 32 + (k % 2) * 3, R.woodG[2])
        // the well with its little roof
        b.rr(108, 66, 12, 8, R.wood) { _, j -> .5 - (j % 3) * .08 }
        b.r(108, 56, 1, 10, R.wood[2]); b.r(119, 56, 1, 10, R.wood[2]); b.rr(106, 53, 16, 3, roof) { i, _ -> .6 - i * .01 }
        b.r(113, 58, 1, 5, R.iron[3]); b.rr(112, 62, 3, 3, R.iron) { _, _ -> .5 }
        if (life.balcony) for (x in 4 until 28) {
            val top = 60 - Math.round(Math.sqrt(max(0.0, 144.0 - (x - 16.0) * (x - 16.0))) * .5).toInt()
            for (y in top until 66) b.p(x, y, rp(R.glass, .65 + if (x % 4 == 0) .2 else 0.0, x, y))
        }
    }

    private fun balcony(b: Buf, wx: Int, wy: Int, lit: Boolean) {
        b.rr(wx - 3, wy - 2, 14, 16, R.glass) { i, j -> .55 + (if ((i + j) % 9 == 0) .25 else 0.0) - j * .01 }
        for (i in wx - 3..wx + 10 step 4) b.r(i, wy - 2, 1, 16, R.cream[4])
        b.r(wx - 3, wy + 6, 14, 1, R.cream[4])
        b.rr(wx - 4, wy + 14, 16, 2, R.iron) { _, _ -> .4 }
        for (k in 0 until 4) { b.p(wx - 1 + k * 3, wy + 12, R.green[5]); b.p(wx - 1 + k * 3, wy + 11, R.green[4]); b.p(wx + k * 3, wy + 10, listOf(R.apple[4], R.honey[4], R.red[4], R.gold[4])[k]) }
        if (lit) for (j in wy - 2 until wy + 14) for (i in wx - 3 until wx + 11) { b.p(i, j, mix(b.get(i, j), G.WARM, .35f)); keep[j * w + i] = true }
    }

    private fun pot(b: Buf, x: Int, y: Int, p: Pot) {
        b.p(x, y, R.brick[3]); b.p(x + 1, y, R.brick[2])
        b.p(x, y - 1, R.green[5]); b.p(x + 1, y - 2, R.green[4])
        if (p.ripe) b.p(x, y - 2, p.ramp[min(4, p.ramp.size - 1)])
    }

    // ---------- the common foreground ----------

    private fun ground(b: Buf) {
        val country = life.kind == "house"
        val well = life.kind == "well"
        for (y in 74 until h) for (x in 0 until w) {
            if (well && x < 22 && y <= 74 + (22 - x) * .6) continue
            if (well && x >= 106 && y <= 74 + (x - 106) * .6) continue
            if (country) {
                val path = abs(x - 59 - (y - 74) * .2) < 5
                b.p(x, y, when {
                    m.winter && fbm(x * .1, y * .15) > .25 -> rp(R.snow, .55 + fbm(x * .3, y * .3) * .35, x, y)
                    path -> rp(G.dirt, .45 + fbm(x * .3, y * .3) * .25, x, y)
                    else -> rp(grassOf(m.month), .45 + (fbm(x * .1, y * .1) - .5) * .3 + if (hash(x, y) < .04) .2 else 0.0, x, y)
                })
                continue
            }
            val v = .45 + (fbm(x * .15, y * .3) - .5) * .2 + if (((x + (y shr 1)) and 3) == 0 && (y and 1) == 1) .08 else 0.0
            b.p(x, y, if (m.winter && fbm(x * .1, y * .15) > .35) rp(R.snow, .5 + fbm(x * .3, y * .3) * .4, x, y) else rp(R.stone, v, x, y))
        }
        for (x in 0 until w) b.mul(x, 74, G.SHADOW, .4f)
        if (!country && (m.sky.rain || m.month in 3..4)) for (i in 0 until 26) for (j in 0 until 3) if (abs(i - 13) / 13.0 + j / 3.0 < 1) b.p(50 + i, 92 + j, R.glass[4], .45f)
        if (!country && m.month in 5..8 && !m.sky.rain) {
            for (k in 0 until 4) { val cx = 56 + k * 5; b.r(cx, 98, 5, 1, R.cream[5]); b.r(cx, 102, 5, 1, R.cream[5]); b.r(cx, 98, 1, 5, R.cream[5]); b.r(cx + 5, 98, 1, 5, R.cream[5]) }
            b.p(73, 99, R.red[4])
        }
    }

    private fun washing(b: Buf) {
        if (m.sky.rain || m.sky.snow || m.winter) return
        val colours = listOf(R.cream, R.roofB, R.apple, R.lilac, R.green, R.honey, R.jam)
        if (life.kind == "block") {
            // washing hangs on the neighbours' balconies
            for (k in 0 until 3) { val bx = 26 + k * 30 + (hash(day, k) * 6).toInt(); val by = 13 + k * 14; b.rr(bx, by, 5, 4, colours[(hash(k, day) * colours.size).toInt()]) { i, _ -> .8 - i * .05 } }
            return
        }
        if (life.kind == "house") {
            // a short line between two posts over the hen run
            b.r(2, 50, 1, 14, R.wood[2]); b.r(28, 50, 1, 14, R.wood[2])
            for (x in 2..28) b.p(x, 50 + Math.round(sin((x - 2) / 26.0 * PI) * 2).toInt(), R.woodG[5])
            for (k in 0 until 2) { val sx = 6 + k * 11; b.rr(sx, 52, 7, 6 + k * 2, colours[(hash(k, day) * colours.size).toInt()]) { i, j -> .85 - i * .04 - j * .02 } }
            return
        }
        val ly = 30
        for (x in 22 until 106) b.p(x, ly + Math.round(sin((x - 22) / 84.0 * PI) * 4).toInt(), R.woodG[5])
        for (k in 0 until 4) {
            val sx = 30 + k * 17 + (hash(day, k) * 6).toInt()
            val c = colours[(hash(k, day) * colours.size).toInt()]
            val sy = ly + Math.round(sin((sx - 22) / 84.0 * PI) * 4).toInt()
            val hgt = 6 + (hash(day + k, 3) * 6).toInt()
            b.rr(sx, sy + 1, 9, hgt, c) { i, j -> .85 - i * .04 - j * .02 }; b.r(sx, sy + 1 + hgt, 9, 1, c[1])
        }
    }

    private fun coop(b: Buf) {
        b.rr(4, 70, 22, 14, R.wood) { i, j -> .5 + (if (j % 3 == 0) -.15 else 0.0) - i * .005 }
        // a gable roof: filled from the ridge down to the walls, lit on the left slope
        for (i in 0 until 24) {
            val top = 62 + Math.round(abs(i - 11.5) * .5).toInt()
            for (y in top until 71) b.p(3 + i, y, rp(if (m.winter && y < top + 2) R.snow else R.roofR, (if (i < 12) .68 else .48) - (y - top) * .02 + if ((i + y) % 3 == 0) -.06 else 0.0, i, y))
            b.p(3 + i, top - 1, R.INK, .5f)
        }
        b.rr(12, 75, 6, 9, R.wood) { _, _ -> .2 }; b.r(4, 84, 22, 1, G.SHADOW)
        for (k in 0 until min(3, life.eggs)) b.ball(7 + k * 3, 88, 1, R.cream, null)
        fun hen(x: Int, y: Int, red: Boolean) {
            b.rr(x, y, 4, 3, if (red) R.ginger else R.cream) { _, j -> .8 - j * .15 }
            b.p(x + 3, y - 1, if (red) R.ginger[4] else R.cream[5]); b.p(x + 4, y - 1, R.honey[4]); b.p(x + 3, y - 2, R.apple[3]); b.p(x + 1, y + 3, R.honey[2]); b.p(x + 3, y + 3, R.honey[2])
        }
        if (m.dark < .3f && !(m.winter && m.sky.snow)) for (k in 0 until 3) hen(16 + (hash(day, k * 5) * 30).toInt(), 88 + (hash(k, day) * 9).toInt(), k == 1)
        if (life.kind == "house") {
            // the dog's kennel by the coop, and the dog when it's light
            b.rr(28, 80, 9, 7, R.wood) { _, j -> .5 - j * .03 }; b.rr(27, 77, 11, 3, R.roofR) { i, _ -> .6 - i * .02 }; b.rr(31, 83, 3, 4, R.wood) { _, _ -> .15 }
            if (m.dark < .3f) { b.rr(38, 84, 6, 3, R.ginger) { _, j -> .5 - j * .1 }; b.p(43, 83, R.ginger[3]); b.p(44, 83, R.INK); b.p(37, 83, R.ginger[2]) }
        }
    }

    private fun planters(b: Buf) {
        val country = life.kind == "house"
        for (k in 0 until 3) {
            val px = 50 + k * 20
            if (country) b.rr(px, 80, 16, 4, G.dirt) { i, j -> .3 + j * .08 + if (i % 4 == 0) .1 else 0.0 }
            else { b.rr(px, 80, 16, 6, R.wood) { i, j -> .65 - j * .08 - if (i == 0 || i == 15) .15 else 0.0 }; b.rr(px + 1, 80, 14, 1, G.dirt) { _, _ -> .2 } }
            for (i in 0 until 7) {
                val hx = px + 2 + i * 2; val hh = if (m.winter || m.month == 11) 2 else 4 + (i * 3 + k) % 3
                for (j in 0 until hh) b.p(hx, 79 - j, rp(if (m.winter || m.month == 11) G.oak else R.green, .5 + j * .08, hx, j))
                if (m.month in 6..8 && i % 2 == 0) b.p(hx, 79 - hh, listOf(R.apple[4], R.honey[5], R.cream[5])[k])
                if (m.month in 9..10 && k == 1 && i == 3) b.ball(hx, 78, 2, R.autumn, R.INK)
            }
        }
    }

    private fun sharedGarden(b: Buf) {
        for (k in 0 until 4) {
            val px = 40 + k * 15; val py = 80 + (k % 2) * 6
            b.rr(px, py, 13, 5, R.wood) { i, j -> .7 - j * .1 - if (i == 0 || i == 12) .15 else 0.0 }
            for (i in 0 until 6) {
                val hx = px + 1 + i * 2; val hh = if (m.winter) 1 else 3 + (i + k) % 3
                for (j in 0 until hh) b.p(hx, py - 1 - j, rp(if (m.winter || m.month == 11) G.oak else R.green, .45 + j * .1, hx, j))
                if (m.month in 6..9 && (i + k) % 2 == 0) b.p(hx, py - 1 - hh, listOf(R.apple[4], R.honey[5], R.lilac[4], R.cream[5])[k])
            }
        }
        b.rr(108, 74, 18, 14, R.glass) { i, j -> .6 + if ((i * 3 + j) % 11 == 0) .25 else 0.0 }
        for (i in 108..126 step 6) b.r(i, 74, 1, 14, listOf(R.cream[4], R.honey[3], R.green[4], R.roofB[4])[(i / 6) % 4])
        b.r(108, 80, 18, 1, R.cream[4]); b.r(108, 73, 18, 1, R.wood[3])
        val ly = if (life.kind == "house") 44 else 22
        for (x in 22 until 106) if (x % 6 == 0) {
            val y = ly + Math.round(sin((x - 22) / 84.0 * PI) * 5).toInt()
            b.p(x, y, if (m.dark > .15f) G.WARM2 else R.cream[3]); if (m.dark > .15f) keep[y * w + x] = true
        }
    }

    private fun hives(b: Buf) {
        if (!life.hive) return
        if (life.kind == "house") for (k in 0 until 2) {
            val hx = 90 + k * 8
            b.rr(hx, 68, 6, 6, R.honey) { i, j -> .7 - i * .05 - j * .04 }; b.r(hx - 1, 67, 8, 1, R.wood[2]); b.p(hx + 2, 72, R.INK); b.r(hx, 74, 1, 2, R.wood[1]); b.r(hx + 5, 74, 1, 2, R.wood[1])
        } else {
            val top = if (life.kind == "block") 0 else 2
            for (k in 0 until 2) { val hx = 40 + k * 10; b.rr(hx, top, 7, 6, R.honey) { i, j -> .7 - i * .05 - j * .04 }; b.r(hx - 1, max(0, top - 1), 9, 1, R.wood[2]); b.p(hx + 3, top + 4, R.INK) }
        }
    }

    private fun season(b: Buf) {
        if (newYear) {
            val tx = 46; val ty = 74
            for (j in 0 until 18) { val ww = j / 3 + 1; for (i in -ww..ww) b.p(tx + i, ty - 18 + j, rp(G.spruce, .35 + (-i.toDouble() / (ww + 1)) * .25, i, j)) }
            b.r(tx, ty, 1, 3, R.wood[2]); b.p(tx, ty - 19, R.gold[5])
            for (k in 0 until 9) { val gx = tx + ((hash(k, 5) - .5) * (k + 2)).toInt(); val gy = ty - 16 + k * 2; b.p(gx, gy, listOf(R.red[4], R.gold[4], R.roofB[4])[k % 3]); keep[gy * w + gx] = true }
        }
        if (m.winter && !newYear) {
            val sx = 62 + (hash(day / 7, 2) * 20).toInt()
            b.ball(sx, 96, 4, R.snow, R.ice[1]); b.ball(sx, 89, 3, R.snow, R.ice[1]); b.p(sx, 89, R.autumn[3]); b.p(sx - 1, 88, R.INK); b.p(sx + 1, 88, R.INK)
            b.r(sx - 2, 85, 5, 1, R.red[3])
        }
        if (m.month == 4) {
            b.line(70, 99, 76, 86, R.wood[3]); b.r(74, 86, 5, 1, R.iron[3])
            b.rr(82, 95, 5, 5, R.roofB) { i, _ -> .6 - i * .05 }
            for (k in 0 until 2) b.rr(88 + k * 6, 94, 5, 6, R.iron) { i, j -> .3 + if (i + j < 3) .2 else 0.0 }
        }
        if (m.month in 9..11) {
            for (i in 0 until 30) b.p(8 + (hash(i, day) * 110).toInt(), 76 + (hash(day, i) * 27).toInt(), rp(R.autumn, .3 + hash(i, 2) * .6, i, 0))
            if (m.month == 10) for (j in 0 until 4) for (i in -6 + j..6 - j) b.p(36 + i, 99 - j, rp(R.autumn, .3 + hash(i, j) * .6, i, j))
        }
    }

    private fun people(b: Buf) {
        // a neighbour who came out to talk stands in front, and the yard is otherwise quiet
        life.guest?.let { g -> if ("cat" !in g) { b.person(GUEST_X, 102, Look.of(g)); return } }
        if (hour in 7..10 && !(m.winter && m.sky.snow && hash(day, 9) < .5)) {
            val x = 60 + (hash(day, 4) * 30).toInt()
            b.person(x, 102, Look.of(mapOf("skin" to "2", "hair" to "grey", "cloth" to "oak", "cap" to "1", "moustache" to "1")))
            b.line(x + 6, 102, x + 9, 84, R.wood[3]); b.r(x + 4, 101, 6, 2, R.honey[2])
        }
        if ((hour in 15..19 || (weekend && hour in 10..19)) && !m.sky.rain && hash(day, 12) < .8) kid(b, 84 + (hash(day, 8) * 10).toInt(), 101)
        if (hour in 18..22 && !m.sky.rain && hash(day, 21) < .7) {
            sitter(b, 104, 89, G.navy, R.iron)
            b.vol(108, 82, 5, 9, ell(2.5, 4.5, 2.5, 4.5), sph(2.5, 4.0, 2.5, 4.5), R.wood, R.INK); b.r(110, 74, 1, 9, R.wood[1])
            if ("gosha.lilya" in life.flags || "lilya.met_gosha" in life.flags) sitter(b, 113, 89, R.lilac, R.honey)
        }
    }

    private fun kid(b: Buf, x: Int, y: Int) {
        b.r(x - 1, y - 4, 1, 4, R.iron[2]); b.r(x + 1, y - 4, 1, 4, R.iron[2])
        b.rr(x - 2, y - 9, 5, 5, R.apple) { i, _ -> .7 - i * .08 }
        b.ball(x, y - 12, 2, R.skin, R.INK); b.p(x - 3, y - 12, R.wood[3]); b.p(x + 3, y - 12, R.wood[3])
        if (m.month in 5..9) { b.r(x + 3, y - 8, 1, 4, R.iron[3]); b.p(x + 3, y - 9, R.honey[4]) }
    }

    private fun sitter(b: Buf, x: Int, y: Int, coat: IntArray, hair: IntArray) {
        b.rr(x - 2, y - 7, 5, 7, coat) { i, _ -> .65 - i * .07 }
        b.r(x - 2, y, 5, 2, coat[1]); b.r(x + 2, y + 2, 1, 3, R.iron[1])
        b.ball(x, y - 10, 2, R.skin, R.INK); b.rr(x - 2, y - 13, 5, 2, hair) { i, _ -> .6 - i * .05 }
    }

    private fun cat(b: Buf) {
        // the warm and dry spots of each kind of yard: a bench, the coop roof, a planter, a step, a sill
        val roof = 10 to 59
        val dry = when (life.kind) { "house" -> 54 to 68; "block" -> 58 to 70; else -> 88 to 72 }
        val spots = when {
            m.winter -> listOf(105 to 85, roof)
            m.sky.rain -> listOf(roof, dry)
            life.kind == "house" -> listOf(102 to 85, roof, 54 to 68, 60 to 77)
            life.kind == "block" -> listOf(102 to 85, roof, 60 to 77)
            else -> listOf(102 to 85, roof, 60 to 77, 30 to 56)
        }
        val (x, y) = if (life.guest?.contains("cat") == true) GUEST_X - 5 to 96 else spots[(hash(day, 77) * 10).toInt() % spots.size]
        b.vol(x, y, 10, 5, ell(5.0, 3.0, 5.0, 2.6), sph(5.0, 2.5, 5.0, 2.5), R.ginger, R.INK)
        b.p(x + 1, y + 1, R.ginger[5]); b.p(x + 3, y + 1, R.INK); b.r(x + 8, y + 3, 4, 1, R.ginger[2])
    }

    override fun anim(b: Buf, t: Int) {
        b.precipitation(m, t)
        b.night(m, keep)
        if (m.dark > .15f) b.lamplight(lampX, lampY, 14, .7f)
        // smoke from the stove in the cold months
        if (life.kind == "house" && (m.winter || m.month in listOf(3, 10, 11))) for (k in 0 until 5) {
            val y = 12 - k * 3 - (t % 3)
            b.p(70 + (t / 4 + k) % 2, y, R.cream[4], .6f - k * .1f)
        }
    }
}
