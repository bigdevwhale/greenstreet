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
fun Buf.sign(x: Int, y: Int, icon: IntArray, open: Boolean, shut: Boolean = false) {
    r(x - 4, y - 11, 9, 8, R.INK)
    for (j in 0 until 6) for (i in 0 until 7) p(x - 3 + i, y - 10 + j, rp(if (open) R.cream else R.stone, .85 - j * .05, i, j))
    for (j in 0 until 5) for (i in 0 until 5) if (icon[j * 5 + i] != 0) p(x - 2 + i, y - 10 + j, if (open) R.red[2] else R.iron[3])
    r(x, y - 3, 1, 3, R.iron[2])
    // closed: a rolled-down shutter over the board
    if (shut) for (j in 0 until 6) for (i in 0 until 7) p(x - 3 + i, y - 10 + j, if (j % 2 == 0) R.iron[2] else R.iron[1], .75f)
}

/**
 * A neighbour out for a walk, drawn small for the map but recognisable: their own coat, hair, hat or kerchief,
 * beard or glasses, and the thing they carry for their trade. Feet at (x, y).
 */
fun Buf.villager(x: Int, y: Int, l: Look, step: Int, kind: String = "") {
    val e = l.extras
    val skirt = "long" in e || "bun" in e || "earrings" in e || e.any { it.startsWith("kerchief") }
    val half = if (kind in setOf("bakery", "market", "garage", "hardware")) 4 else if (kind in setOf("library", "salon")) 2 else 3
    val tall = if (kind in setOf("station", "library", "theatre", "park")) 2 else 0
    val top = y - 12 - tall          // the coat's top row
    for (i in -half - 1..half + 1) mul(x + i, y, G.SHADOW, .4f)
    // legs, one stepping
    val legs = if (skirt) l.skin[2] else R.iron[1]
    r(x - 2, y - 4, 2, 4, R.INK); r(x + 1, y - 4, 2, 4, R.INK)
    p(x - 2, y - 3, legs); p(x + 2, y - 3, legs)
    if (step == 1) { p(x - 2, y - 1, R.INK); p(x - 3, y - 1, R.INK) } else { p(x + 2, y - 1, R.INK); p(x + 3, y - 1, R.INK) }
    // the coat or dress, outlined and lit from the left; a skirt flares at the hem
    val rows = 8 + tall + if (skirt) 1 else 0
    for (j in 0 until rows) {
        val flare = if (skirt && j >= rows - 3) j - (rows - 3) + 1 else 0
        val hw = half + flare - (if (j == 0) 1 else 0)
        for (i in -hw - 1..hw + 1) {
            val edge = i == -hw - 1 || i == hw + 1 || j == rows - 1
            val shade = if (i < -hw / 2) 4 else if (i <= hw / 2) 3 else 2
            p(x + i, top + j, if (edge) R.INK else l.cloth[shade.coerceAtMost(l.cloth.size - 1)])
        }
    }
    r(x - half, top - 1, half * 2 + 1, 1, R.INK)
    if ("apron" in e) for (j in 2 until rows - 1) for (i in -half + 1..half - 1) p(x + i, top + j, R.cream[if (i < 0) 5 else 4])
    p(x - half - 1, top + 5, l.skin[3]); p(x + half + 1, top + 5, l.skin[2])   // hands
    val hy0 = top - 6                 // the head's top row
    r(x - 3, hy0 - 1, 7, 7, R.INK)
    for (j in 0 until 5) for (i in 0 until 5) p(x - 2 + i, hy0 + j, l.skin[if (i < 2) 4 else 3])
    p(x - 1, hy0 + 2, R.INK); p(x + 1, hy0 + 2, R.INK)
    val hr = l.hair
    r(x - 2, hy0, 5, 1, hr[3]); p(x - 2, hy0 + 1, hr[2]); p(x + 2, hy0 + 1, hr[2])
    if ("long" in e) { r(x - 3, hy0 + 1, 1, 6, hr[3]); r(x + 3, hy0 + 1, 1, 6, hr[2]) }
    if ("bun" in e) { r(x - 1, hy0 - 3, 3, 2, hr[3]); p(x, hy0 - 4, R.INK) }
    if ("curls" in e) for (k in -2..2 step 2) p(x + k, hy0 - 1, hr[4])
    if ("pigtails" in e) { p(x - 4, hy0 + 3, hr[3]); p(x + 4, hy0 + 3, hr[3]) }
    if ("beard" in e) { r(x - 2, hy0 + 3, 5, 2, hr[3]); p(x - 1, hy0 + 5, hr[2]); p(x, hy0 + 5, hr[3]); p(x + 1, hy0 + 5, hr[2]) }
    if ("moustache" in e) r(x - 1, hy0 + 3, 3, 1, hr[2])
    if ("glasses" in e) { p(x - 1, hy0 + 2, R.glass[5]); p(x + 1, hy0 + 2, R.glass[5]); p(x, hy0 + 2, R.INK) }
    if ("earrings" in e) { p(x - 3, hy0 + 3, R.gold[4]); p(x + 3, hy0 + 3, R.gold[4]) }
    when {
        e.any { it.startsWith("hat=toque") } -> { r(x - 2, hy0 - 4, 5, 4, R.cream[5]); r(x - 3, hy0 - 1, 7, 1, R.cream[3]); r(x - 3, hy0 - 5, 7, 1, R.INK) }
        e.any { it.startsWith("kerchief") } && l.hat != null -> { r(x - 3, hy0 - 1, 7, 3, l.hat[3]); r(x - 3, hy0 - 1, 7, 1, l.hat[4]); p(x - 3, hy0 + 2, l.hat[2]); p(x + 3, hy0 + 2, l.hat[2]) }
        "cap" in e || e.any { it == "hat=cap" } -> { val c = l.hat ?: G.navy; r(x - 3, hy0 - 2, 7, 2, c[3]); r(x - 3, hy0, 8, 1, R.INK) }
    }
    val hr2 = hr
    // what they carry for their trade
    val hx = x + half + 2; val hy = top + 4
    when (kind) {
        "bakery" -> { r(hx, hy - 1, 4, 2, R.honey[3]); p(hx + 1, hy - 1, R.honey[5]); p(hx + 3, hy, R.honey[2]) }
        "water" -> { for (k in 0 until 9) p(hx + k / 2, hy - k, R.wood[2]); p(hx + 4, hy - 9, R.cream[5]); p(hx + 5, hy - 6, R.glass[4]) }
        "vet" -> { r(hx + 3, y - 3, 4, 2, R.wood[3]); p(hx + 7, y - 4, R.wood[3]); p(hx + 7, y - 5, R.wood[4]); p(hx + 3, y - 1, R.wood[1]); p(hx + 6, y - 1, R.wood[1]); for (k in 0 until 4) p(hx + k / 2, hy + k, R.red[3]) }
        "post" -> { r(hx - 1, hy - 1, 4, 4, R.wood[3]); p(hx, hy - 2, R.wood[2]); p(hx + 1, hy, R.cream[5]) }
        "florist" -> { for ((dx, dy) in listOf(0 to -3, 1 to -2, -1 to -2, 2 to -3)) p(hx + dx, hy + dy, listOf(R.red[4], R.autumn[4], R.lilac[4], R.honey[5])[(dx + 2) % 4]); p(hx, hy - 1, R.green[3]); p(hx, hy, R.green[2]) }
        "school", "library" -> { r(hx, hy - 1, 3, 4, if (kind == "school") R.roofB[3] else R.red[2]); r(hx, hy - 1, 3, 1, R.cream[5]) }
        "sports" -> { r(hx, hy - 1, 3, 3, R.cream[5]); p(hx + 1, hy, R.INK) }
        "park" -> { for (k in 0 until 10) p(hx, hy + 3 - k, R.wood[2]); r(hx - 1, hy - 7, 3, 1, R.iron[3]) }
        "garage", "hardware" -> { r(hx, hy, 4, 3, if (kind == "hardware") R.red[3] else R.iron[3]); r(hx + 1, hy - 1, 2, 1, R.INK) }
        "market" -> { r(hx - 1, hy, 5, 3, R.wood[3]); p(hx, hy - 1, R.apple[4]); p(hx + 2, hy - 1, R.green[4]) }
        "cafe" -> { r(hx, hy, 2, 3, R.cream[5]); p(hx + 2, hy + 1, R.cream[4]); p(hx, hy - 2, R.cream[3]) }
        "pharmacy" -> { for (k in 0 until 3) { p(hx + k, hy - 1, R.green[3 + k % 2]); p(hx + k, hy, R.green[2]) } }
        "tailor" -> { r(hx, hy, 3, 3, R.enamel[4]); p(hx + 1, hy + 1, R.enamel[2]) }
        "theatre" -> { for (k in 0 until 7) p(x - half + k, top + 1 + k / 3, R.red[4]) }
        "salon" -> { r(x - 3, hy0 - 2, 7, 2, hr2[4]); p(x - 4, hy0, hr2[3]); p(x + 4, hy0, hr2[3]) }
        "station" -> { r(hx, hy - 1, 3, 4, R.iron[3]); p(hx + 1, hy, R.honey[5]) }
    }
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
