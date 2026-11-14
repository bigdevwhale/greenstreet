package app.greenstreet.ui.art

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.InputStream
import java.util.zip.InflaterInputStream
import kotlin.math.abs

/**
 * Pictures painted ahead of time (tools/art) and shipped in assets/art: the people, the places they stand in front of,
 * the courtyards. Scenes use them at twice the old resolution when they are there, and fall back to drawing in code
 * when they are not (unit tests, a missing file).
 */
object Pictures {
    /** Opens an asset by path, or null. Set by the app at start, and by render tests. */
    @Volatile var open: ((String) -> InputStream?)? = null

    private val bufs = HashMap<String, Buf?>()
    private val backdrops = HashMap<String, Backdrop?>()

    /** art/art.tsv: per picture, where the feet stand, where the ground starts, where the lamp hangs, the sill. */
    private val meta: Map<String, Map<String, Int>> by lazy {
        val text = runCatching { open?.invoke("art/art.tsv")?.use { it.readBytes().decodeToString() } }.getOrNull() ?: return@lazy emptyMap()
        val lines = text.lines().filter { it.isNotBlank() }
        val head = lines.first().split('\t')
        lines.drop(1).associate { l ->
            val f = l.split('\t')
            f[0] to head.indices.drop(1).mapNotNull { i -> f.getOrNull(i)?.toIntOrNull()?.let { head[i] to it } }.toMap()
        }
    }

    @Synchronized fun buf(path: String): Buf? = bufs.getOrPut(path) {
        runCatching { open?.invoke(path)?.use { PngIn.read(it) } }.getOrNull()
    }

    fun person(id: String): Buf? = buf("art/people/$id.png")

    /** A street front by the neighbour's kind of place, or the park path ("stroll"). */
    fun place(name: String): Backdrop? = backdrop("places/$name")

    /** A courtyard: "well", "block" or "house". */
    fun yard(kind: String): Backdrop? = backdrop("yard/$kind")

    @Synchronized private fun backdrop(name: String): Backdrop? = backdrops.getOrPut(name) {
        val pic = buf("art/$name.png") ?: return@getOrPut null
        val lit = buf("art/$name.lit.png")
        Backdrop(name, pic, lit?.d?.let { d -> IntArray(d.size) { (d[it] shr 8) and 0xFFFF } }, meta[name] ?: emptyMap())
    }
}

/**
 * A painted background. Transparent pixels are sky, drawn live behind it. [windows] numbers each window pane, so a
 * random few light up at night; [meta] has feetX/feetY (where a person stands), ground (where the ground line is),
 * lampX/lampY, sillX/sillY (the player's own windowsill).
 */
class Backdrop(val name: String, val pic: Buf, val windows: IntArray?, val meta: Map<String, Int>) {
    val w get() = pic.w
    val h get() = pic.h

    /** How far down the sky shows through. */
    val skyH: Int = run {
        var last = -1
        for (y in 0 until pic.h) for (x in 0 until pic.w) if (pic.d[y * pic.w + x] ushr 24 == 0) last = y
        last + 1
    }
    val feetX get() = meta["feetX"] ?: (w / 2)
    val feetY get() = meta["feetY"] ?: (h * 88 / 100)
    val ground get() = meta["ground"] ?: feetY
}

/** Paints a backdrop: today's sky behind, the picture, then some windows lit if it's dark (marked in [keep]). */
fun Buf.backdrop(bd: Backdrop, m: Moment, keep: BooleanArray, seed: Int) {
    if (bd.skyH > 0) skyBand(0, 0, w, bd.skyH, m, 0)
    blit(bd.pic, 0, 0)
    val win = bd.windows ?: return
    if (m.dark <= .12f) return
    for (i in d.indices) {
        val id = win.getOrNull(i) ?: 0
        if (id == 0 || hash(id * 7 + seed, m.day.toInt()) >= .55) continue
        val c = d[i]
        val lum = (Buf.ch(c, 16) + Buf.ch(c, 8) + Buf.ch(c, 0)) / 765.0
        d[i] = rp(R.honey, .4 + lum * .5, i % w, i / w)
        keep[i] = true
    }
}

/** Snow, puddles and fallen leaves on the ground below [top]. */
fun Buf.groundSeason(m: Moment, top: Int, seed: Int) {
    if (top >= h) return
    if (m.winter) for (y in top until h) for (x in 0 until w)
        if (fbm(x * .05, y * .1) > .42) p(x, y, rp(R.snow, .5 + fbm(x * .15, y * .15) * .4, x, y))
    if (m.sky.rain) for (k in 0 until 3) {
        val px = (hash(k, seed) * (w - 50)).toInt() + 10; val py = top + 6 + (hash(seed, k) * (h - top - 12).coerceAtLeast(1)).toInt(); val pw = 30 + k * 8
        for (i in 0 until pw) for (j in 0 until 5) if (abs(i - pw / 2.0) / (pw / 2.0) + j / 5.0 < 1) p(px + i, py + j, R.glass[4], .45f)
    }
    if (m.month in 9..10) for (i in 0 until 30) {
        val x = (hash(i, seed + m.day.toInt()) * w).toInt(); val y = top + 2 + (hash(m.day.toInt(), i + seed) * (h - top - 2)).toInt()
        r(x, y, 2, 2, rp(R.autumn, .3 + hash(i, 2) * .6, i, 0))
    }
}

/** Lays [s] over this buffer with its top left at (x, y), by alpha. */
fun Buf.blit(s: Buf, x: Int, y: Int) {
    for (j in 0 until s.h) {
        val yy = y + j
        if (yy < 0 || yy >= h) continue
        for (i in 0 until s.w) {
            val xx = x + i
            if (xx < 0 || xx >= w) continue
            val c = s.d[j * s.w + i]
            val a = c ushr 24
            if (a == 0) continue
            if (a == 255) d[yy * w + xx] = c else p(xx, yy, c, a / 255f)
        }
    }
}

/** This buffer with every pixel doubled. */
fun Buf.up2(): Buf {
    val o = Buf(w * 2, h * 2)
    for (y in 0 until h) for (x in 0 until w) {
        val c = d[y * w + x]
        if (c ushr 24 == 0) continue
        val i = y * 2 * o.w + x * 2
        o.d[i] = c; o.d[i + 1] = c; o.d[i + o.w] = c; o.d[i + o.w + 1] = c
    }
    return o
}

/** A person drawn in code, doubled to stand among painted pictures. */
fun figure(l: Look): Buf = Buf(16, 36).also { it.person(8, 35, l) }.up2()

/** A painted neighbour if there is one, else drawn from their look. */
fun figureOf(id: String?, l: Look): Buf = id?.let { Pictures.person(it) } ?: figure(l)

/** Stands a figure with its feet centred at (cx, by). */
fun Buf.stand(f: Buf, cx: Int, by: Int) = blit(f, cx - f.w / 2, by - f.h)

/** A neighbour in front of their painted place (a shop, the park path…): sky, weather and night drawn live. */
class PlaceScene(val bd: Backdrop, val who: Buf?, val m: Moment, val seed: Int) : Scene(bd.w, bd.h) {
    override val key get() = listOf(bd.name, seed, m.minute / 10, m.sky, m.month, m.day)
    private val keep = BooleanArray(w * h)

    override fun base(b: Buf) {
        b.backdrop(bd, m, keep, seed)
        b.groundSeason(m, bd.ground, seed)
        who?.let { b.stand(it, bd.feetX, bd.feetY) }
    }

    override fun anim(b: Buf, t: Int) {
        b.precipitation(m, t)
        b.precipitation(m, t + 37)
        b.night(m, keep)
        val lx = bd.meta["lampX"]; val ly = bd.meta["lampY"]
        if (lx != null && ly != null && m.dark > .15f) b.lamplight(lx, ly, 26, .6f)
    }
}

/** A whole painted picture with something drawn live over it each frame. */
class PictureScene(val name: String, val pic: Buf, val over: (Buf, Int) -> Unit = { _, _ -> }) : Scene(pic.w, pic.h) {
    override val key get() = name
    override fun base(b: Buf) = b.blit(pic, 0, 0)
    override fun anim(b: Buf, t: Int) = over(b, t)
}

/** A painted picture from art/intro if it's there, else [fallback]. */
fun introScene(name: String, over: (Buf, Int) -> Unit = { _, _ -> }, fallback: () -> Scene): Scene =
    Pictures.buf("art/intro/$name.png")?.let { PictureScene(name, it, over) } ?: fallback()

/** A neighbour's place: painted if the picture is there, otherwise drawn in code by [fallback]. */
fun placeScene(place: String, id: String?, look: Look, m: Moment, fallback: () -> Scene): Scene =
    Pictures.place(place)?.let { PlaceScene(it, figureOf(id, look), m, (id ?: place).hashCode()) } ?: fallback()

/** Reads the 8-bit RGBA, non-interlaced PNGs tools/art writes. Android and the JVM both, without platform imaging. */
object PngIn {
    fun read(input: InputStream): Buf {
        val s = DataInputStream(input.buffered())
        s.readFully(ByteArray(8))
        var w = 0; var h = 0
        val idat = ByteArrayOutputStream()
        while (true) {
            val len = s.readInt()
            val type = String(ByteArray(4).also { s.readFully(it) }, Charsets.US_ASCII)
            val data = ByteArray(len).also { s.readFully(it) }
            s.readInt()
            when (type) {
                "IHDR" -> {
                    w = (data[0].toInt() and 0xFF shl 24) or (data[1].toInt() and 0xFF shl 16) or (data[2].toInt() and 0xFF shl 8) or (data[3].toInt() and 0xFF)
                    h = (data[4].toInt() and 0xFF shl 24) or (data[5].toInt() and 0xFF shl 16) or (data[6].toInt() and 0xFF shl 8) or (data[7].toInt() and 0xFF)
                    require(data[8].toInt() == 8 && data[9].toInt() == 6 && data[12].toInt() == 0) { "only 8-bit RGBA, not interlaced" }
                }
                "IDAT" -> idat.write(data)
                "IEND" -> break
            }
        }
        val raw = InflaterInputStream(idat.toByteArray().inputStream()).readBytes()
        val stride = w * 4
        val prev = ByteArray(stride); val cur = ByteArray(stride)
        val b = Buf(w, h)
        var o = 0
        for (y in 0 until h) {
            val f = raw[o++].toInt()
            for (i in 0 until stride) {
                val x = raw[o++].toInt() and 0xFF
                val a = if (i >= 4) cur[i - 4].toInt() and 0xFF else 0
                val up = prev[i].toInt() and 0xFF
                val c = if (i >= 4) prev[i - 4].toInt() and 0xFF else 0
                val v = when (f) {
                    1 -> x + a; 2 -> x + up; 3 -> x + (a + up) / 2
                    4 -> { val p = a + up - c; val pa = abs(p - a); val pb = abs(p - up); val pc = abs(p - c); x + if (pa <= pb && pa <= pc) a else if (pb <= pc) up else c }
                    else -> x
                }
                cur[i] = v.toByte()
            }
            for (x in 0 until w) {
                val k = x * 4
                b.d[y * w + x] = (cur[k + 3].toInt() and 0xFF shl 24) or (cur[k].toInt() and 0xFF shl 16) or (cur[k + 1].toInt() and 0xFF shl 8) or (cur[k + 2].toInt() and 0xFF)
            }
            cur.copyInto(prev)
        }
        return b
    }
}
