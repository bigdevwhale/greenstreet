package app.greenstreet.ui.art

import app.greenstreet.data.Variety
import app.greenstreet.domain.Bed
import app.greenstreet.domain.K
import app.greenstreet.domain.Terrain
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** The colour ramp a variety's fruit or flower is drawn with. */
fun rampOf(name: String): IntArray = when (name) {
    "jam" -> R.jam; "apple" -> R.apple; "honey" -> R.honey; "gold" -> R.gold; "autumn" -> R.autumn; "cream" -> R.cream
    "lilac" -> R.lilac; "roofB" -> R.roofB; "enamel" -> R.enamel; "green" -> R.green; "oak" -> G.oak; "ginger" -> R.ginger
    else -> R.honey
}

/** A bed as it grows: wooden edge, soil, then the crop by its drawing family. (x, y) is the bed's centre. */
fun Buf.bed(x: Int, y: Int, v: Variety, grown: Double, ripe: Boolean, wet: Boolean, wild: Boolean, t: Int) {
    val st = if (ripe) 4 else min(3, (grown / v.days * 4).toInt())
    val f0 = rampOf(v.ramp)
    // a soft shadow, then the frame: an inked box of light planks round dark tilled soil, so it reads from far off
    for (i in -6..7) mul(x + i, y + 6, G.SHADOW, .45f)
    for (j in -1..5) for (i in -6..6) {
        val edge = i == -6 || i == 6 || j == -1 || j == 5
        val plank = i == -5 || i == 5 || j == 0 || j == 4
        p(x + i, y + j, when {
            edge -> R.INK
            plank -> rp(R.wood, if (j == 0) .9 else if (j == 4) .45 else .65, x + i, y + j)
            else -> rp(G.dirt, (if (wet) .05 else .2) + (if ((j % 2) == 1) .0 else .12) + hash(x + i, y + j) * .08, x + i, y + j)
        })
    }
    if (wet && (t / 3) % 4 == 0) { p(x + 2, y + 1, R.glass[5]); p(x - 3, y + 3, R.glass[4]) }
    // a garden stake with a little flag in the plant's colour: the bed is found by it from the far zoom
    for (k in 0 until 9) { p(x + 7, y + 4 - k, R.cream[5]); p(x + 8, y + 4 - k, R.INK) }
    p(x + 6, y - 4, R.INK)
    for (j in 0 until 4) for (i in 0 until 5) {
        val edge = j == 0 || j == 3 || i == 4
        p(x + 8 + i, y - 5 + j, if (edge) R.INK else rp(f0, .85 - j * .12, x + i, j))
    }
    p(x + 7, y - 5, R.INK)
    val l = R.green
    val f = rampOf(v.ramp)
    fun lf(dx: Int, dy: Int, s: Double) = p(x + dx, y + dy, rp(l, s, x + dx, y + dy))
    when (st) {
        0 -> for (q in listOf(-3, 0, 3)) { p(x + q, y + 2, G.dirt[0]); p(x + q, y + 1, R.cream[3]) }
        1 -> for (q in listOf(-3, 0, 3)) { lf(q, 1, .75); lf(q, 0, .85); lf(q - 1, -1, .95); lf(q + 1, -1, .9) }
        2 -> for (q in listOf(-3, 0, 3)) { for (k in 0 until 3) lf(q, 1 - k, .6 + k * .1); lf(q - 1, -1, .85); lf(q + 1, -2, .95); lf(q - 1, -3, .9) }
        3 -> {
            vol(x - 3, y - 5, 7, 5, ell(3.5, 2.8, 3.5, 2.6), { u, w -> sph(3.5, 2.5, 3.5, 2.5)(u, w) + (hash(u + x, w + y) - .5) * .3 }, l, mix(l[0], R.INK, .5f))
            if (v.art == "tall") for (k in 0 until 5) lf(0, -6 - k, .6)
        }
        else -> {
            when (v.art) {
                "bulb", "root" -> { for (q in listOf(-2, 0, 2)) { lf(q, -3, .7); lf(q, -2, .6); lf(q + 1, -4, .85) }; ball(x - 2, y, 1, f, null); ball(x + 2, y, 1, f, null) }
                "spike" -> { for (q in listOf(-2, 0, 2)) for (k in 0 until 5) lf(q + if (k > 3) 1 else 0, -k, .5 + k * .08); for (i in -2..2) p(x + i, y, rp(f, .7, x + i, y)) }
                "flower", "bulbflower", "herb" -> for (q in listOf(-2, 2)) {
                    for (k in 0 until 4) lf(q, -k, .5)
                    val fy = y - 5
                    if (v.art == "herb") { lf(q - 1, -4, .8); lf(q + 1, -5, .85); lf(q, -6, .9) } else {
                        p(x + q, fy - 1, rp(f, .85, 0, 0)); p(x + q - 1, fy, rp(f, .7, 0, 0)); p(x + q + 1, fy, rp(f, .75, 0, 0)); p(x + q, fy + 1, rp(f, .6, 0, 0)); p(x + q, fy, R.honey[4])
                    }
                }
                "grain" -> for (q in -3..3 step 2) { for (k in 0 until 5) p(x + q, y - k, rp(f, .35 + k * .06, x, y)); p(x + q, y - 5, f[min(4, f.size - 1)]); p(x + q, y - 6, f[3]); p(x + q + 1, y - 6, f[2]) }
                "tall" -> { for (k in 0 until 9) lf(0, -k, .55); lf(-1, -3, .7); lf(1, -5, .7); ball(x, y - 11, 2, f, R.INK); p(x, y - 11, R.wood[1]); p(x - 1, y - 11, R.wood[2]) }
                "round", "vine" -> { lf(-3, 0, .7); lf(3, -1, .7); lf(-2, -1, .8); vol(x - 3, y - 3, 7, 5, ell(3.2, 2.2, 3.2, 2.2), sph(3.2, 2.2, 3.2, 2.2), f, R.INK); p(x, y - 3, R.wood[2]) }
                "bush" -> { vol(x - 4, y - 7, 9, 7, ell(4.5, 3.5, 4.5, 3.4), sph(4.5, 3.0, 4.5, 3.4), l, mix(l[0], R.INK, .5f)); for (k in 0 until 6) p(x - 3 + (hash(k, x) * 7).toInt(), y - 6 + (hash(y, k) * 5).toInt(), f[3]) }
                else -> { vol(x - 3, y - 5, 7, 5, ell(3.5, 2.8, 3.5, 2.6), sph(3.5, 2.5, 3.5, 2.5), l, mix(l[0], R.INK, .5f)); for (k in 0 until 3) p(x - 2 + k * 2, y - 3, f[3]) }
            }
            val bob = (t / 3) % 2
            val sy = y - (if (v.art == "tall") 15 else 8) - bob
            p(x + 3, sy, G.WARM2); p(x + 2, sy + 1, R.gold[4]); p(x + 4, sy + 1, R.gold[4]); p(x + 3, sy + 2, R.gold[3])
        }
    }
    if (wild) for ((q, k) in listOf(-5 to 0, 5 to -1, -4 to -3, 4 to -4)) { p(x + q, y + k, G.oak[3]); p(x + q, y + k - 1, G.oak[4]) }
}

/** The player: knitted hat, scarf, green coat, a canvas bag. Feet at (x, y). */
fun Buf.walker(x: Int, y: Int, step: Int, left: Boolean) {
    for (i in -3..3) mul(x + i, y, G.SHADOW, .35f)
    val rows = listOf("..kkkk..", ".krrrrk.", "krRrrrrk", "kkffffkk", ".kfefek.", ".kffffk.", ".kssssk.", "kccssccb", "kcccccbk", "kcccccbk", ".kcccck.",
        if (step == 1) ".kpk.kpk" else ".kpkkpk.", if (step == 1) "..kk.kk." else ".kk..kk.")
    val pal = mapOf('k' to R.INK, 'r' to R.red[3], 'R' to R.red[5], 'f' to R.skin[4], 'e' to R.INK, 's' to R.honey[4], 'c' to R.enamel[3], 'b' to R.wood[4], 'p' to R.iron[2])
    rows.forEachIndexed { j, row -> for (i in 0 until 8) { val ch = row[if (left) 7 - i else i]; if (ch != '.') p(x - 4 + i, y - 13 + j, if (ch == 'c' && i < 3) R.enamel[4] else pal.getValue(ch)) } }
}

/** A neighbour's place on the map: a little signboard with their icon. */
fun Buf.sign(x: Int, y: Int, icon: IntArray, open: Boolean) {
    r(x - 4, y - 11, 9, 8, R.INK)
    for (j in 0 until 6) for (i in 0 until 7) p(x - 3 + i, y - 10 + j, rp(if (open) R.cream else R.stone, .85 - j * .05, i, j))
    for (j in 0 until 5) for (i in 0 until 5) if (icon[j * 5 + i] != 0) p(x - 2 + i, y - 10 + j, if (open) R.red[2] else R.iron[3])
    r(x, y - 3, 1, 3, R.iron[2])
}

/** Night, glints and weather over the visible window of the map. [ox], [oy]: the window's corner on the map. */
fun Buf.streetLife(tr: Terrain, ox: Int, oy: Int, m: Moment, t: Int, windows: List<Int>, px: Int, py: Int, front: BooleanArray? = null) {
    val tk = t / 4
    for (y in 0 until h) for (x in 0 until w) {
        val mx = ox + x; val my = oy + y
        if (tr.at(mx, my) == K.WATER && !m.winter && hash(mx + tk * 3, my) < .006) { p(x, y, G.water[6], .8f); p(x + 1, y, G.water[5], .6f) }
    }
    if (m.sky.rain) {
        for (i in d.indices) d[i] = mixMulPublic(d[i], rgb(0x8c9aa8), .25f)
        for (i in 0 until 90) { val x = ((hash(i, 1) * w + t * 3) % w).toInt(); val y = ((hash(i, 2) * h + t * 11) % h).toInt(); p(x, y, R.glass[4], .45f); p(x, y + 1, R.glass[4], .35f); p(x - 1, y + 2, R.glass[4], .2f) }
    }
    if (m.sky.snow) for (i in 0 until 70) { val x = ((hash(i, 3) * w + kotlin.math.sin(t / 8.0 + i) * 4 + w) % w).toInt(); val y = ((hash(i, 4) * h + t * 2) % h).toInt(); p(x, y, R.snow[5]) }
    if (m.sky.fog) for (y in 0 until h) for (x in 0 until w) p(x, y, rgb(0xc8ccd0), (.25 + fbm((x + ox) * .03 + t * .01, (y + oy) * .05) * .3).toFloat())
    val a = m.dark
    if (a <= 0f) return
    for (i in d.indices) { var c = d[i]; if (m.dusk) c = mixMulPublic(c, rgb(0xf2a26a), .2f); d[i] = mixMulPublic(c, rgb(0x3a4a88), a * .8f) }
    if (a < .15f) return
    for (wi in windows) {
        val x = wi % tr.w - ox; val y = wi / tr.w - oy
        if (x < 0 || y < 0 || x >= w || y >= h || hash(wi, m.day.toInt()) < .35) continue
        if (front != null && front[y * w + x]) continue
        p(x, y, if (hash(wi, 3) < .2) G.WARM2 else G.WARM, min(1f, a * 1.5f))
    }
    for (l in tr.lamps) {
        val lx = l[0] - ox; val ly = l[1] - 7 - oy
        if (lx < -12 || ly < -12 || lx > w + 12 || ly > h + 12) continue
        for (j in -10..10) for (i in -10..10) { val dd = hypot(i.toDouble(), j * 1.1) / 10; if (dd >= 1) continue; val s = (1 - dd) * (1 - dd); if (s * 16 < bay(lx + i, ly + j) * .6) continue; scr(lx + i, ly + j, G.WARM, (s * a * .9).toFloat()) }
        p(lx, ly, G.WARM2)
    }
    for (j in -7..7) for (i in -7..7) { val dd = hypot(i.toDouble(), j.toDouble()) / 7; if (dd < 1) scr(px + i, py - 6 + j, G.WARM, ((1 - dd) * (1 - dd) * a * .5).toFloat()) }
}

fun mixMulPublic(o: Int, c: Int, a: Float): Int {
    fun one(sh: Int): Int { val s = Buf.ch(c, sh) / 255f; val v = Buf.ch(o, sh) / 255f; return ((v + (v * s - v) * a) * 255).toInt().coerceIn(0, 255) }
    return (o and 0xFF000000.toInt()) or (one(16) shl 16) or (one(8) shl 8) or one(0)
}

/** The bed's state as the street shows it. */
fun Buf.bedOf(b: Bed, v: Variety, ripe: Boolean, today: Long, x: Int, y: Int, t: Int) = bed(x, y, v, b.grown, ripe, b.lastTend == today, b.wild, t)

/** The 5×5 icons of the neighbours' places. */
object Icons {
    private fun of(vararg rows: String) = IntArray(25) { i -> if (rows[i / 5][i % 5] == '#') 1 else 0 }
    val bakery = of(" ### ", "#####", "#.#.#", "#####", "     ")
    val pharmacy = of("  #  ", "  #  ", "#####", "  #  ", "  #  ")
    val cafe = of("#### ", "#####", "#### ", "#### ", " ##  ")
    val school = of("  #  ", " ### ", " ### ", "#####", "  #  ")
    val station = of("#   #", "## ##", "# # #", "#   #", "#   #")
    val library = of("##.##", "#.#.#", "##.##", "#.#.#", "##.##")
    val market = of(" # # ", "  #  ", " ### ", "#####", " ### ")
    val florist = of(" # # ", "#####", " ### ", "  #  ", "  #  ")
    val hardware = of("###  ", " #   ", " ### ", "   # ", "   ##")
    val post = of("#####", "##.##", "#.#.#", "#...#", "#####")
    val salon = of("#   #", " # # ", "  #  ", " # # ", "## ##")
    val sports = of(" ### ", "# # #", "#####", "# # #", " ### ")
    val park = of("  #  ", " ### ", "#####", "  #  ", "  #  ")
    val water = of("     ", "## ##", "#####", "     ", "## ##")
    val garage = of(" ### ", "#   #", "#####", "## ##", "     ")
    val theatre = of("#####", "#.#.#", "#####", "#...#", " ### ")
    val vet = of("# # #", "     ", " ### ", "#####", " ### ")
    val tailor = of("#   #", " # # ", "  #  ", " # # ", "#   #")
    val home = of("  #  ", " ### ", "#####", "#.#.#", "#####")
    fun forKind(kind: String): IntArray = when (kind) {
        "bakery" -> bakery; "pharmacy" -> pharmacy; "cafe" -> cafe; "school" -> school; "station" -> station; "library" -> library
        "market" -> market; "florist" -> florist; "hardware" -> hardware; "post" -> post; "salon" -> salon; "sports" -> sports
        "park" -> park; "water" -> water; "garage" -> garage; "theatre" -> theatre; "vet" -> vet; "tailor" -> tailor; else -> home
    }
}

/** Keeps a camera window inside the map. */
fun clampCam(c: Int, size: Int, view: Int) = max(0, min(size - view, c))
