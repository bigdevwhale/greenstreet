package app.greenstreet.ui.art

import app.greenstreet.data.Content
import app.greenstreet.data.GoodKind

/** 16×16 pixel icons for everything that can lie in the bag, drawn from the item's kind and colour. */
object ItemIcons {
    private val cache = HashMap<String, Buf>()

    fun of(c: Content, key: String): Buf = cache.getOrPut(key) { draw(c, key) }

    /** A seed packet with the crop drawn on it. */
    fun packet(c: Content, key: String): Buf = cache.getOrPut("packet:$key") {
        val b = Buf(16, 16)
        for (y in 1 until 16) for (x in 2 until 14) b.p(x, y, rp(R.paper, .75 - y * .01 + if (x == 2) .1 else 0.0, x, y))
        b.r(2, 1, 12, 2, R.paper[2]); for (x in 2 until 14 step 2) b.p(x, 1, R.paper[1])
        outline(b, 2, 1, 12, 15)
        val v = c.variety[key]
        if (v != null) produce(b, v.art, rampOf(v.ramp), 3, 4, 10)
        b
    }

    private fun draw(c: Content, key: String): Buf {
        val b = Buf(16, 16)
        val v = c.variety[key] ?: c.crops[key]?.firstOrNull()
        if (v != null) { produce(b, v.art, rampOf(v.ramp), 2, 2, 12); return b }
        val g = c.good[key]
        when {
            g?.kind == GoodKind.FISH -> fish(b, FISH[key] ?: R.glass, key)
            g?.kind == GoodKind.FIND -> find(b, key)
            g?.kind == GoodKind.YARD -> yard(b, key)
            g?.kind == GoodKind.STAPLE -> staple(b, key)
            else -> product(b, key)
        }
        return b
    }

    private fun outline(b: Buf, x: Int, y: Int, w: Int, h: Int) {
        for (i in x - 1..x + w) { b.p(i, y - 1, R.INK, .7f); b.p(i, y + h, R.INK, .7f) }
        for (j in y until y + h) { b.p(x - 1, j, R.INK, .7f); b.p(x + w, j, R.INK, .7f) }
    }

    /** The harvested crop: a root, a head, a flower, a sheaf… inside a box at (x, y) of size s. */
    private fun produce(b: Buf, art: String, f: IntArray, x: Int, y: Int, s: Int) {
        val cx = x + s / 2; val cy = y + s / 2
        val leaf = R.green
        when (art) {
            "bulb", "root" -> {
                for (k in 0 until 3) b.line(cx, cy - 1, cx - 2 + k * 2, y, leaf[4 + k % 2])
                b.vol(cx - 3, cy - 1, 7, 7, ell(3.5, 3.5, 3.5, 3.5), sph(3.5, 3.5, 3.5, 3.5), f, R.INK)
                b.p(cx, y + s - 1, f[1])
            }
            "spike" -> { for (k in 0 until 3) b.line(cx - 1 + k, cy - 2, cx - 3 + k * 3, y, leaf[4]); b.vol(cx - 3, cy - 2, 7, 7, ell(3.5, 3.5, 3.5, 3.5), sph(3.5, 3.5, 3.5, 3.5), f, R.INK) }
            "flower", "bulbflower" -> {
                b.line(cx, cy + 1, cx, y + s - 1, leaf[4]); b.p(cx - 1, cy + 4, leaf[5]); b.p(cx + 1, cy + 3, leaf[5])
                for ((dx, dy) in listOf(0 to -3, -3 to 0, 3 to 0, 0 to 2, -2 to -2, 2 to -2, -2 to 2, 2 to 2)) b.ball(cx + dx, cy - 2 + dy, 1, f, null)
                b.ball(cx, cy - 2, 1, R.honey, null)
            }
            "grain" -> for (k in -2..2) { b.line(cx + k, y + s - 1, cx + k * 2, y + 3, f[2]); b.p(cx + k * 2, y + 2, f[4]); b.p(cx + k * 2, y + 3, f[3]); b.p(cx + k * 2 + 1, y + 4, f[3]) }
            "tall" -> { b.ball(cx, cy, 5, f, R.INK); b.ball(cx, cy, 2, R.wood, null) }
            "round", "vine" -> { b.vol(cx - 5, cy - 3, 11, 9, ell(5.5, 4.5, 5.5, 4.5), sph(5.5, 4.0, 5.5, 4.5), f, R.INK); b.r(cx, cy - 5, 1, 3, R.wood[2]); for (k in -3..3 step 3) b.line(cx + k, cy - 2, cx + k, cy + 4, f[1], .5f) }
            "bush" -> for ((dx, dy) in listOf(0 to 0, -3 to 2, 3 to 2, 0 to 4, -2 to -3, 2 to -3)) b.ball(cx + dx, cy + dy, 2, f, R.INK)
            "herb" -> for (k in 0 until 5) { b.line(cx, y + s - 1, cx - 4 + k * 2, y + 2, leaf[3]); b.ball(cx - 4 + k * 2, y + 3, 1, leaf, null) }
            "leafy" -> b.vol(cx - 5, cy - 4, 11, 10, ell(5.5, 5.0, 5.5, 5.0), { u, v -> sph(5.5, 4.5, 5.5, 5.0)(u, v) + (hash(u, v) - .5) * .3 }, leaf, R.INK)
            "sprout" -> { b.r(x + 1, cy + 2, s - 2, 3, R.wood[3]); for (k in 0 until s - 3 step 2) { b.line(x + 2 + k, cy + 2, x + 2 + k, cy - 2, leaf[5]); b.p(x + 2 + k, cy - 3, leaf[6]) } }
            else -> b.ball(cx, cy, 4, f, R.INK)
        }
    }

    private val FISH = mapOf(
        "roach" to R.glass, "perch" to R.green, "ruff" to G.oak, "bream" to R.brass, "pike" to G.oak, "crucian" to R.gold,
        "tench" to R.green, "smelt" to R.ice, "burbot" to R.wood, "ide" to R.glass, "zander" to R.stone, "carp" to R.honey,
        "rudd" to R.autumn, "gudgeon" to R.stone, "catfish" to R.iron,
    )

    private fun fish(b: Buf, f: IntArray, key: String) {
        val long = key in setOf("pike", "burbot", "catfish", "zander", "smelt", "gudgeon")
        val rx = if (long) 6.5 else 5.0; val ry = if (long) 2.6 else 3.4
        b.vol(1, 8 - ry.toInt() - 1, (rx * 2).toInt(), (ry * 2).toInt() + 1, ell(rx, ry, rx, ry), sph(rx, ry - .5, rx, ry), f, R.INK)
        val tx = (rx * 2).toInt() + 1
        for (j in -3..3) b.p(tx + kotlin.math.abs(j) / 2, 8 + j, f[2]); b.p(tx + 2, 5, R.INK); b.p(tx + 2, 11, R.INK)
        b.p(3, 7, R.INK); b.p(3, 6, R.cream[5])
        if (key == "perch") for (k in 0 until 3) b.line(5 + k * 3, 6, 5 + k * 3, 10, R.INK, .4f)
        if (key in setOf("perch", "rudd", "roach")) b.p(6, 11, R.red[4])
    }

    private fun find(b: Buf, k: String) {
        when (k) {
            "chanterelle" -> mushroom(b, R.honey, R.honey)
            "boletus", "porcini" -> mushroom(b, R.wood, R.cream)
            "suillus" -> mushroom(b, R.ginger, R.honey)
            "honey_fungus" -> { for (dx in listOf(-3, 0, 3)) { b.r(8 + dx, 8, 1, 6, R.cream[4]); b.ball(8 + dx, 7 + kotlin.math.abs(dx) / 3, 2, R.honey, R.INK) } }
            "russula" -> mushroom(b, R.red, R.cream)
            "morel" -> { b.r(7, 10, 3, 5, R.cream[4]); b.vol(5, 2, 7, 9, ell(3.5, 4.5, 3.5, 4.5), { u, v -> .3 + ((u + v) % 3) * .2 }, R.wood, R.INK) }
            "rowan", "rosehip", "wild_strawberry", "wild_raspberry", "elderberry", "sea_buckthorn", "bird_cherry", "ranetka" -> {
                val f = when (k) { "rowan" -> R.apple; "rosehip" -> R.autumn; "sea_buckthorn" -> R.honey; "elderberry", "bird_cherry" -> G.plum; "ranetka" -> R.red; else -> R.jam }
                b.line(8, 2, 8, 6, R.wood[2]); b.line(8, 3, 12, 2, R.green[4])
                for ((dx, dy) in listOf(0 to 0, -3 to 2, 3 to 2, 0 to 4, -2 to 6, 2 to 6)) b.ball(8 + dx, 7 + dy, if (k == "ranetka") 2 else 1, f, R.INK)
            }
            "maple_leaf" -> { for (j in 0 until 11) for (i in 0 until 11) { val a = Math.atan2(j - 5.0, i - 5.0); val r = Math.hypot(i - 5.0, j - 5.0); if (r < 3.5 + 2 * kotlin.math.abs(Math.sin(a * 2.5))) b.p(3 + i, 2 + j, rp(R.autumn, .4 + (i + j) * .03, i, j)) }; b.line(8, 9, 8, 15, R.wood[2]) }
            "pine_cone", "spruce_cone" -> b.vol(4, 2, 8, 12, ell(4.0, 6.0, 4.0, 6.0), { u, v -> .35 + ((u + v * 2) % 4) * .12 }, R.wood, R.INK)
            "acorn" -> { b.ball(8, 9, 3, R.honey, R.INK); b.rr(5, 4, 7, 3, R.wood, .4); b.p(8, 3, R.wood[1]) }
            "hazelnut", "conker" -> { b.ball(8, 8, 4, if (k == "conker") R.wood else R.honey, R.INK); b.p(6, 6, R.cream[5]) }
            "birch_bark" -> { b.rr(3, 4, 10, 8, R.cream, .8); for (k2 in 0 until 4) b.r(4 + k2 * 2, 6 + k2 % 2 * 3, 2, 1, R.INK) }
            "birch_sap" -> bottle(b, R.glass)
            "willow_twig" -> { b.line(4, 14, 12, 2, R.wood[2]); for (k2 in 0 until 4) b.ball(6 + k2 * 2, 11 - k2 * 3, 1, R.cream, null) }
            "nettle", "dandelion", "four_leaf_clover", "lime_blossom" -> {
                if (k == "four_leaf_clover") for ((dx, dy) in listOf(-2 to -2, 2 to -2, -2 to 2, 2 to 2)) b.ball(8 + dx, 7 + dy, 2, R.green, R.INK)
                else if (k == "dandelion") { b.line(8, 8, 8, 15, R.green[4]); b.ball(8, 6, 3, R.honey, R.INK) }
                else if (k == "lime_blossom") { b.vol(3, 4, 10, 8, ell(5.0, 4.0, 5.0, 4.0), sph(5.0, 3.5, 5.0, 4.0), R.green, R.INK); for (k2 in 0 until 4) b.p(5 + k2 * 2, 10, R.honey[4]) }
                else for (k2 in 0 until 3) b.vol(3 + k2 * 3, 3 + k2 * 2, 5, 8, ell(2.5, 4.0, 2.5, 4.0), sph(2.5, 3.5, 2.5, 4.0), R.green, R.INK)
            }
            "driftwood" -> b.vol(1, 6, 14, 5, ell(7.0, 2.5, 7.0, 2.5), { u, _ -> .45 + (u % 3) * .1 }, R.woodG, R.INK)
            "pebble" -> b.vol(3, 5, 10, 7, ell(5.0, 3.5, 5.0, 3.5), sph(5.0, 3.0, 5.0, 3.5), R.stone, R.INK)
            else -> b.ball(8, 8, 4, R.green, R.INK)
        }
    }

    private fun mushroom(b: Buf, cap: IntArray, stem: IntArray) {
        b.vol(6, 7, 4, 8, ell(2.0, 4.0, 2.0, 4.0), sph(2.0, 3.0, 2.0, 4.0), stem, R.INK)
        b.vol(2, 2, 12, 7, { u, v -> val a = (u - 5.5) / 6.0; val c = (v - 6.0) / 6.0; a * a + c * c <= 1 && v < 6 }, sph(6.0, 4.0, 6.0, 5.0), cap, R.INK)
    }

    private fun yard(b: Buf, k: String) {
        when (k) {
            "egg" -> b.vol(4, 2, 8, 12, ell(4.0, 6.0, 4.0, 6.0), sph(4.0, 5.0, 4.0, 6.0), R.cream, R.INK)
            "honey" -> jar(b, R.honey)
            "wax" -> b.vol(2, 4, 12, 9, { u, v -> u in 0..11 && v in 0..8 }, { u, v -> .7 - u * .02 - v * .03 + if ((u + v * 2) % 5 == 0) -.15 else 0.0 }, R.honey, R.INK)
            else -> { b.line(3, 14, 12, 2, R.cream[2]); for (j in 0 until 8) { b.p(6 + j, 10 - j, R.cream[4]); b.p(8 + j, 11 - j, R.cream[3]) } }
        }
    }

    private fun staple(b: Buf, k: String) {
        when (k) {
            "milk", "vinegar" -> bottle(b, if (k == "milk") R.cream else R.glass)
            "butter" -> b.vol(2, 6, 12, 6, { u, v -> u in 0..11 && v in 0..5 }, { u, v -> .8 - v * .06 - u * .01 }, R.honey, R.INK)
            "yeast" -> b.vol(3, 5, 10, 7, { u, v -> u in 0..9 && v in 0..6 }, { _, v -> .7 - v * .05 }, R.cream, R.INK)
            "tea_black", "spices" -> b.vol(3, 2, 10, 12, { u, v -> u in 0..9 && v in 0..11 }, { u, v -> .55 - u * .02 + if (v == 2) .2 else 0.0 }, if (k == "spices") R.red else R.green, R.INK)
            else -> sack(b, if (k == "sugar" || k == "salt" || k == "flour") R.cream else R.paper)
        }
    }

    private fun sack(b: Buf, r: IntArray) {
        b.vol(2, 4, 12, 11, { u, v -> val a = (u - 5.5) / 6.0; v >= 2 || kotlin.math.abs(a) < .5 }, { u, v -> .7 - u * .03 - v * .01 }, r, R.INK)
        b.r(6, 4, 4, 1, R.wood[2])
    }

    private fun bottle(b: Buf, r: IntArray) {
        b.vol(4, 5, 8, 10, { u, v -> u in 0..7 && v in 0..9 }, { u, v -> .6 - u * .04 + if (u == 1) .3 else 0.0 }, r, R.INK)
        b.r(6, 2, 4, 3, r[2]); b.r(6, 1, 4, 1, R.wood[2])
    }

    private fun jar(b: Buf, content: IntArray) {
        b.vol(3, 4, 10, 11, { u, v -> u in 0..9 && v in 0..10 }, { u, v -> .55 - u * .03 + (if (u == 1) .25 else 0.0) - v * .01 }, content, R.INK)
        b.r(3, 3, 10, 2, R.cream[3]); b.r(2, 2, 12, 1, R.red[3]); for (i in 2 until 14 step 2) b.p(i, 2, R.cream[4])
        b.r(5, 8, 6, 3, R.paper[4])
    }

    private val JAM = mapOf("jam_strawberry" to R.jam, "jam_raspberry" to R.jam, "jam_rowan" to R.apple, "jam_currant" to G.plum,
        "jam_gooseberry" to R.green, "jam_pumpkin" to R.autumn, "pickled_cucumbers" to R.green, "sauerkraut" to R.cream, "pickled_mushrooms" to R.wood,
        "salted_tomatoes" to R.red, "lecho" to R.autumn, "khrenovina" to R.red, "pickled_garlic" to R.cream, "salve_calendula" to R.honey,
        "zucchini_caviar" to R.honey, "poppy_filling" to R.iron, "cottage_cheese" to R.cream, "oil_sunflower" to R.gold)

    private fun product(b: Buf, k: String) {
        when {
            k in JAM && k != "oil_sunflower" -> jar(b, JAM.getValue(k))
            k == "oil_sunflower" || k == "kvass" || k == "mors" || k == "kompot" || k == "sbiten" -> bottle(b, when (k) { "kvass" -> R.wood; "mors" -> R.jam; "kompot" -> R.apple; "sbiten" -> R.honey; else -> R.gold })
            k.startsWith("tea_") || k == "kissel" -> { b.vol(2, 7, 10, 7, { u, v -> u in 0..9 && v in 0..6 }, { u, _ -> .85 - u * .04 }, R.cream, R.INK); b.r(4, 7, 6, 1, if (k == "kissel") R.jam[3] else R.honey[2]); b.line(12, 9, 14, 10, R.cream[3]); b.line(14, 10, 12, 12, R.cream[3]); for (j in 0 until 3) b.p(6 + j % 2, 5 - j, R.cream[5], .5f) }
            k.startsWith("bread") || k in setOf("kalach", "pirozhki", "gingerbread", "oat_cookies") -> b.vol(1, 5, 14, 9, ell(7.0, 4.5, 7.0, 4.5), { u, v -> sph(7.0, 3.5, 7.0, 4.5)(u, v) + if (k == "bread_rye") -.15 else 0.0 }, if (k == "gingerbread" || k == "oat_cookies") R.honey else R.ginger, R.INK)
            k == "bubliki" -> { b.ball(8, 8, 5, R.ginger, R.INK); b.ball(8, 8, 2, R.paper, null) }
            k.startsWith("pie") || k == "medovik" || k == "blini" || k == "syrniki" || k == "draniki" -> { b.vol(1, 7, 14, 6, ell(7.0, 3.0, 7.0, 3.0), sph(7.0, 2.0, 7.0, 3.0), if (k == "draniki") R.honey else R.ginger, R.INK); if (k.startsWith("pie")) for (i in 0 until 3) b.p(5 + i * 3, 8, R.wood[1]) }
            k.startsWith("cloth_") || k == "linen_cloth" -> { val r = when (k) { "cloth_blue" -> R.roofB; "cloth_red" -> R.red; "cloth_yellow" -> R.honey; else -> R.cream }; for (j in 0 until 4) b.vol(2, 4 + j * 2, 12, 3, { u, v -> u in 0..11 && v in 0..2 }, { _, v -> .7 - v * .15 }, r, R.INK) }
            k == "candle" -> { b.vol(6, 5, 4, 10, { u, v -> u in 0..3 && v in 0..9 }, { u, _ -> .8 - u * .1 }, R.cream, R.INK); b.p(8, 3, R.honey[5]); b.p(8, 4, R.autumn[4]) }
            k == "soap_herbal" -> b.vol(2, 5, 12, 7, { u, v -> u in 0..11 && v in 0..6 }, { u, v -> .7 - v * .05 + if ((u + v) % 4 == 0) .1 else 0.0 }, R.green, R.INK)
            k == "bouquet" || k == "dried_flowers" -> { for (i in -2..2) b.line(8, 15, 8 + i * 2, 7, R.green[4]); for ((i, r) in listOf(R.jam, R.honey, R.lilac, R.cream, R.apple).withIndex()) b.ball(4 + i * 2, 5 + (i % 2) * 2, 1, if (k == "dried_flowers") G.oak else r, null) }
            k == "wreath" -> { b.ball(8, 8, 6, R.autumn, R.INK); b.ball(8, 8, 3, R.paper, null); b.p(8, 2, R.red[4]) }
            k == "herbarium" -> { b.vol(2, 2, 12, 12, { u, v -> u in 0..11 && v in 0..11 }, { _, _ -> .8 }, R.paper, R.INK); b.line(8, 12, 8, 5, R.green[4]); b.ball(6, 6, 1, R.green, null); b.ball(10, 7, 1, R.green, null) }
            k == "lavender_sachet" -> { b.vol(3, 4, 10, 10, ell(5.0, 5.0, 5.0, 5.0), sph(5.0, 4.0, 5.0, 5.0), R.lilac, R.INK); b.r(6, 3, 4, 1, R.cream[4]) }
            k == "bird_feeder" -> { b.rr(3, 6, 10, 8, R.wood, .5); b.rr(2, 4, 12, 2, R.roofR, .6); b.ball(8, 10, 2, R.paper, null) }
            k == "seed_bomb" -> { b.ball(8, 8, 5, G.dirt, R.INK); for (i in 0 until 4) b.p(6 + i, 6 + (i % 2) * 3, R.green[5]) }
            k.startsWith("flour") -> sack(b, R.cream)
            k == "dried_mushrooms" || k == "dried_apples" -> { b.line(1, 4, 15, 4, R.wood[2]); for (i in 0 until 4) b.ball(3 + i * 3, 7, 1, if (k == "dried_apples") R.honey else R.wood, R.INK) }
            else -> { b.vol(1, 7, 14, 7, ell(7.0, 3.5, 7.0, 3.5), sph(7.0, 2.5, 7.0, 3.5), R.cream, R.INK); b.vol(3, 6, 10, 3, ell(5.0, 1.5, 5.0, 1.5), { _, _ -> .5 }, soupColour(k), null) }
        }
    }

    private fun soupColour(k: String) = when (k) { "borscht" -> R.jam; "shchi", "green_shchi", "okroshka" -> R.green; "ukha" -> R.honey; "pea_soup", "pumpkin_porridge", "baked_pumpkin" -> R.autumn; "mushroom_soup", "buckwheat_porridge" -> R.wood; "vinegret" -> R.jam; "stuffed_peppers", "vegetable_stew" -> R.red; else -> R.green }
}

private fun Buf.rr(x: Int, y: Int, w: Int, h: Int, r: IntArray, v: Double) { for (j in 0 until h) for (i in 0 until w) p(x + i, y + j, rp(r, v, x + i, y + j)) }
