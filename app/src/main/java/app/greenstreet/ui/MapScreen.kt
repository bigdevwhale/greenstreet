package app.greenstreet.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.greenstreet.BuildConfig
import app.greenstreet.container
import app.greenstreet.data.Place
import app.greenstreet.domain.Geo
import app.greenstreet.domain.metres
import app.greenstreet.ui.art.Buf
import app.greenstreet.ui.art.Icons
import app.greenstreet.ui.art.MapPainter
import app.greenstreet.ui.art.Moment
import app.greenstreet.ui.art.bedOf
import app.greenstreet.ui.art.clampCam
import app.greenstreet.ui.art.sign
import app.greenstreet.ui.art.streetLife
import app.greenstreet.ui.art.walker
import app.greenstreet.ui.components.Kind
import app.greenstreet.ui.components.Panel
import app.greenstreet.ui.components.pixelBox
import app.greenstreet.ui.components.PixelButton
import app.greenstreet.ui.theme.C
import app.greenstreet.ui.theme.T
import app.greenstreet.work.Tracker
import app.greenstreet.work.WalkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.math.roundToInt

/** View widths in map pixels, from close up to the whole quarter; the height is always 5/4 of it. */
private val ZOOMS = intArrayOf(80, 128, 192, 288)

/** The painted district, kept per month: painting takes a second, so it is done once and off the main thread. */
object MapCache {
    /** Bump when the map art changes so old pictures on disk are repainted. */
    private const val ART = 3
    var month = -1
    var buf: Buf? = null
    var windows: List<Int> = emptyList()

    /** The painted map for a month: from the disk if it was painted before, else painted now and saved. */
    fun load(ctx: android.content.Context, t: app.greenstreet.domain.Terrain, month: Int, px: Int = t.w / 2, py: Int = t.h / 2, quick: () -> Unit = {}) {
        val d = t.d
        val f = java.io.File(ctx.cacheDir, "map_${ART}_${month}_${"%.4f_%.4f".format(java.util.Locale.US, d.south, d.west)}.bin")
        runCatching {
            java.io.DataInputStream(java.util.zip.GZIPInputStream(f.inputStream().buffered())).use { inp ->
                val w = inp.readInt(); val h = inp.readInt()
                if (w != t.w || h != t.h) error("size")
                val b = Buf(w, h)
                for (i in b.d.indices) b.d[i] = inp.readInt()
                val n = inp.readInt()
                windows = List(n) { inp.readInt() }
                buf = b; this.month = month
                return
            }
        }
        // first the window around the player, so the street shows at once; then the whole district behind it
        val near = MapPainter(t, month, px - 170, py - 210, px + 170, py + 210)
        buf = near.paint(); windows = near.windows; this.month = month
        quick()
        val p = MapPainter(t, month)
        val b = p.paint()
        buf = b; windows = p.windows; this.month = month
        runCatching {
            ctx.cacheDir.listFiles()?.filter { it.name.startsWith("map_") }?.forEach { it.delete() }
            java.io.DataOutputStream(java.util.zip.GZIPOutputStream(f.outputStream().buffered())).use { out ->
                out.writeInt(b.w); out.writeInt(b.h)
                for (c in b.d) out.writeInt(c)
                out.writeInt(p.windows.size); p.windows.forEach { out.writeInt(it) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapScreen(open: (Sheet) -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ver by c.version.collectAsState()
    val here by c.here.collectAsState()
    val walking by WalkService.walking.collectAsState()
    val ru = ru()
    // touch the version now: a delegated state is only subscribed to when read, and the early return below skips it
    ver.let { }
    val t = c.terrain
    val d = c.district
    val s = c.state
    if (t == null || d == null || s == null) { Loading(); return }
    val now = c.now()
    var ready by remember { mutableIntStateOf(if (MapCache.month == now.month) 1 else 0) }
    val me = here ?: s.home ?: return
    val mx = d.x(me.lon).roundToInt(); val my = d.y(me.lat).roundToInt()
    LaunchedEffect(now.month) {
        if (MapCache.month != now.month) {
            withContext(Dispatchers.Default) { MapCache.load(ctx, t, now.month, mx, my) { ready++ } }
            ready++
        }
    }
    var pan by remember { mutableStateOf(IntOffset.Zero) }
    val focus by c.focus.collectAsState()
    LaunchedEffect(focus) {
        val f = focus ?: return@LaunchedEffect
        pan = IntOffset((d.x(f.lon) - mx).roundToInt(), (d.y(f.lat) - my).roundToInt()); c.focus.value = null
    }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { var last = 0L; while (true) withFrameMillis { ms -> if (ms - last >= 110) { last = ms; tick++ } } }
    var zoom by remember { mutableIntStateOf(1) }
    val lt = LocalDateTime.now()
    val moment = Moment(lt.hour * 60 + lt.minute, now.sky, now.month, now.day)

    Column(Modifier.fillMaxSize()) {
        // the map takes all the room the panel below leaves; its height in map pixels follows the box
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
            val boxW = maxWidth
            val vw = ZOOMS[zoom]
            val vh = (vw * maxHeight.value / maxWidth.value).roundToInt().coerceIn(vw / 2, vw * 3).coerceAtMost(t.h)
            val view = remember(vw, vh) { Buf(vw, vh) }
            val under = remember(vw, vh) { IntArray(vw * vh) }
            val front = remember(vw, vh) { BooleanArray(vw * vh) }
            val bmp = remember(vw, vh) { Bitmap.createBitmap(vw, vh, Bitmap.Config.ARGB_8888) }
            val img = remember(bmp) { bmp.asImageBitmap() }
            if (MapCache.month != now.month || ready < 0) { LaunchedEffect(Unit) { c.stage.value = "paint" }; Loading(Modifier.align(Alignment.Center)) }
            Canvas(Modifier.fillMaxSize()
                .pointerInput(vw, vh) {
                    var acc = 1f
                    detectTransformGestures { _, drag, z, _ ->
                        val k = size.width / vw.toFloat()
                        pan = IntOffset(pan.x - (drag.x / k).roundToInt(), pan.y - (drag.y / k).roundToInt())
                        acc *= z
                        if (acc > 1.35f && zoom > 0) { zoom--; acc = 1f }
                        if (acc < .74f && zoom < ZOOMS.size - 1) { zoom++; acc = 1f }
                    }
                }
                .pointerInput(ver) {
                    detectTapGestures(
                        onTap = { o ->
                            val k = size.width / vw.toFloat()
                            val cx = clampCam(mx - vw / 2 + pan.x, t.w, vw); val cy = clampCam(my - vh * 11 / 20 + pan.y, t.h, vh)
                            val ax = cx + o.x / k; val ay = cy + o.y / k
                            val bed = s.beds.filter { c.content.variety[it.variety]?.place == Place.VERGE }.minByOrNull { Math.hypot(d.x(it.lon) - ax.toDouble(), d.y(it.lat) - ay.toDouble()) }
                            if (bed != null && Math.hypot(d.x(bed.lon) - ax.toDouble(), d.y(bed.lat) - ay.toDouble()) < 7) { open(Sheet.BedInfo(bed.id)); return@detectTapGestures }
                            val who = c.places.entries.minByOrNull { Math.hypot(it.value.x - ax.toDouble(), it.value.y - 6 - ay.toDouble()) }
                            if (who != null && Math.hypot(who.value.x - ax.toDouble(), who.value.y - 6 - ay.toDouble()) < 9) open(Sheet.Place(who.key))
                        },
                        onLongPress = { o ->
                            if (!BuildConfig.DEMO) return@detectTapGestures
                            val k = size.width / vw.toFloat()
                            val cx = clampCam(mx - vw / 2 + pan.x, t.w, vw); val cy = clampCam(my - vh * 11 / 20 + pan.y, t.h, vh)
                            val ax = cx + o.x / k; val ay = cy + o.y / k
                            pan = IntOffset.Zero
                            c.fix(Geo(d.lat(ay), d.lon(ax)), 5f)
                        },
                    )
                }) {
                // read the frame tick before any early return, or the canvas never learns to redraw
                val frame = tick + ready
                val base = MapCache.buf ?: return@Canvas
                frame.let { }
                val cx = clampCam(mx - vw / 2 + pan.x, t.w, vw); val cy = clampCam(my - vh * 11 / 20 + pan.y, t.h, vh)
                for (y in 0 until vh) System.arraycopy(base.d, (cy + y) * base.w + cx, view.d, y * vw, vw)
                val objs = ArrayList<Pair<Int, () -> Unit>>()
                for (b in s.beds) {
                    val v = c.content.variety[b.variety] ?: continue
                    if (v.place != Place.VERGE) continue
                    val bx = d.x(b.lon).roundToInt() - cx; val by = d.y(b.lat).roundToInt() - cy
                    if (bx < -10 || by < -20 || bx > vw + 10 || by > vh + 10) continue
                    objs += by + 3 to { view.bedOf(b, v, c.game.ripe(b), now.day, bx, by, tick) }
                }
                for ((id, sp) in c.places) {
                    val p = c.content.person[id] ?: continue
                    val sx = sp.x.roundToInt() - cx; val sy = sp.y.roundToInt() - cy
                    if (sx < -6 || sy < -2 || sx > vw + 6 || sy > vh + 12) continue
                    objs += sy to { view.sign(sx, sy, Icons.forKind(p.kind), s.bonds[id]?.isOpen == true) }
                }
                s.home?.let { h -> val hx = d.x(h.lon).roundToInt() - cx; val hy = d.y(h.lat).roundToInt() - cy; objs += hy to { view.sign(hx, hy, Icons.home, true) } }
                objs += my - cy to { view.walker(mx - cx, my - cy, if (walking) (tick / 2) % 2 else 0, false) }
                System.arraycopy(view.d, 0, under, 0, under.size)
                objs.sortedBy { it.first }.forEach { it.second() }
                // whatever stands in front of the houses must not be painted over by their lit windows
                for (i in under.indices) front[i] = view.d[i] != under[i]
                view.streetLife(t, cx, cy, moment, tick, MapCache.windows, mx - cx, my - cy, front)
                bmp.setPixels(view.d, 0, vw, 0, 0, vw, vh)
                drawImage(img, srcOffset = IntOffset.Zero, srcSize = IntSize(vw, vh), dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.roundToInt(), (vh * size.width / vw).roundToInt()), filterQuality = FilterQuality.None)
            }
            val ccx = clampCam(mx - vw / 2 + pan.x, t.w, vw); val ccy = clampCam(my - vh * 11 / 20 + pan.y, t.h, vh)
            val placed = ArrayList<FloatArray>()
            val order = c.places.entries.sortedByDescending { (id, _) -> (if (s.bonds[id]?.isOpen == true) 2 else 0) + (if (c.game.request(s, id) != null) 1 else 0) }
            for ((id, sp) in order) {
                val p = c.content.person[id] ?: continue
                val sx = sp.x - ccx; val sy = sp.y - ccy
                if (sx < 0 || sy < 10 || sx > vw || sy > vh) continue
                val opened = s.bonds[id]?.isOpen == true
                val asks = opened && c.game.request(s, id) != null
                val label = if (opened) p.name(ru).substringBefore(' ') + if (asks) " !" else "" else "?"
                val lw = label.length * 8.6f + 10f
                val bw = boxW.value
                val lx = (bw * (sx / vw) - lw / 2).coerceIn(2f, bw - lw - 2f)
                val ly = bw * (sy / vw) - 46f
                if (placed.any { r -> lx < r[0] + r[2] && lx + lw > r[0] && ly < r[1] + 20f && ly + 20f > r[1] }) continue
                placed += floatArrayOf(lx, ly, lw)
                Text(label, style = T.Small.copy(color = if (asks) C.light else if (opened) C.ink else C.muted),
                    modifier = Modifier.offset(x = lx.dp, y = ly.dp).pixelBox(if (asks) C.red else if (opened) C.light else C.paper2, bevel = null, lip = null)
                        .padding(horizontal = 5.dp, vertical = 1.dp), maxLines = 1)
            }
            Column(Modifier.align(Alignment.TopStart).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PixelButton("+", { if (zoom > 0) zoom-- }, kind = Kind.GHOST, enabled = zoom > 0)
                PixelButton("−", { if (zoom < ZOOMS.size - 1) zoom++ }, kind = Kind.GHOST, enabled = zoom < ZOOMS.size - 1)
            }
            if (pan != IntOffset.Zero) PixelButton(w(ru, "К себе", "To me"), { pan = IntOffset.Zero }, Modifier.align(Alignment.TopEnd).padding(8.dp), Kind.GHOST)
        }
        // what can be done right here: a compact block that never needs scrolling
        val near = s.beds.filter { c.content.variety[it.variety]?.place == Place.VERGE && Geo(it.lat, it.lon).metres(me) <= c.game.reach(s) }
        var questOpen by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxWidth().heightIn(max = 340.dp).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ver.let { }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(placeName(c, me, ru), style = T.Heading, modifier = Modifier.weight(1f), maxLines = 1)
                Text(w(ru, "сегодня %.1f км", "today %.1f km").format(s.kmToday).replace('.', if (ru) ',' else '.'), style = T.Small)
            }
            app.greenstreet.domain.Quests.current(s)?.let { q ->
                Panel(Modifier.fillMaxWidth(), fill = C.light, onClick = { questOpen = !questOpen }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(w(ru, "задание · ", "task · ") + app.greenstreet.domain.Quests.from(q.from) { id -> c.content.person[id]?.name }(ru), style = T.Eyebrow)
                            Text(q.title(ru) + if (q.goal > 1) "  ${q.progress(s).coerceAtMost(q.goal)}/${q.goal}" else "", style = T.Body, maxLines = 1)
                        }
                        Text(if (questOpen) "▴" else "▾", style = T.Heading)
                    }
                    if (questOpen) {
                        Text(q.how(ru), style = T.Small.copy(color = C.ink))
                        Text(w(ru, "Награда: ", "Reward: ") + rewards(c, q.reward, ru), style = T.Small)
                    }
                }
            }
            nextStep(c, me, ru)?.let { (text, geo) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text, style = T.Small.copy(color = C.ink), modifier = Modifier.weight(1f), maxLines = 2)
                    if (geo != null) PixelButton(w(ru, "Показать", "Show"), { c.focus.value = geo }, kind = Kind.GHOST)
                }
            }
            for (b in near.take(2)) {
                val v = c.content.variety.getValue(b.variety)
                Text("${v.name(ru)} · ${bedState(c, b, now.day, ru)}", style = T.Small.copy(color = C.ink), maxLines = 1)
            }
            // sowing, when standing by a free verge
            val spot = t.nearestVerge(mx, my, 3)
            if (spot != null) {
                val sowable = s.seeds.keys.mapNotNull { c.content.variety[it] }.filter { it.place == Place.VERGE && c.game.canSow(s, now, it) }
                val tooClose = s.beds.any { Geo(it.lat, it.lon).metres(Geo(d.lat(spot[1].toFloat()), d.lon(spot[0].toFloat()))) < 8 }
                when {
                    tooClose -> Text(w(ru, "Рядом уже есть грядка: между грядками — не меньше восьми шагов.", "There's a bed close by: beds need eight paces between them."), style = T.Small)
                    sowable.isEmpty() -> Text(w(ru, "Газон свободен, но сеять сейчас нечего: семена дают соседи и рынок.", "The verge is free, but there's nothing to sow now: neighbours and the market give seeds."), style = T.Small)
                    else -> FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(w(ru, "Посеять:", "Sow:"), style = T.Body, modifier = Modifier.align(Alignment.CenterVertically))
                        sowable.forEach { v ->
                            val hot = c.game.demand(s, now) == v.crop
                            PixelButton((if (hot) "★ " else "") + "${v.name(ru)} ×${s.seeds[v.key]} · ${v.days}" + w(ru, " дн.", " d"), {
                                c.game.sow(s, now, v.key, Geo(d.lat(spot[1] + .5f), d.lon(spot[0] + .5f))); c.changed()
                            }, kind = Kind.LAMP)
                        }
                    }
                }
            }
            // the actions: they wrap onto a second line rather than hide past the edge
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                near.filter { c.game.ripe(it) }.forEach { b ->
                    PixelButton(w(ru, "Собрать: ", "Pick: ") + c.content.name(c.content.variety.getValue(b.variety).crop)(ru).lowercase(), { open(Sheet.Harvest(b.id)) }, kind = Kind.LAMP)
                }
                c.places.keys.filter { c.near(it, me) }.forEach { id ->
                    PixelButton(w(ru, "Зайти: ", "Visit: ") + c.content.person.getValue(id).name(ru), { open(Sheet.Visit(id)) })
                }
                if (t.nearWater(mx, my, 6)) PixelButton(w(ru, "Закинуть удочку", "Cast a line"), { open(Sheet.Fish(isBigWater(t, mx, my))) }, kind = Kind.GHOST)
                if (c.near("zina", me)) PixelButton(if (now.saturday) w(ru, "Ярмарка", "The fair") else w(ru, "Семенной ряд", "Seed stall"), { open(Sheet.Market) }, kind = Kind.GHOST)
                if (c.atHome(me)) PixelButton(w(ru, "Во двор", "Into the courtyard"), { open(Sheet.Tab(1)) }, kind = Kind.GHOST)
                PixelButton(if (walking) w(ru, "Закончить прогулку", "End the walk") else w(ru, "Выйти на прогулку", "Go for a walk"),
                    { if (walking) WalkService.stop(ctx) else WalkService.start(ctx) }, kind = if (walking) Kind.GHOST else Kind.PRIMARY)
            }
            val tended by Tracker.lastTended.collectAsState()
            if (System.currentTimeMillis() - tended.first < 6000 && tended.second > 0)
                Text(w(ru, "Грядок проведано: %d. Полито.", "Beds looked after: %d. Watered.").format(tended.second), style = T.Body.copy(color = C.green))
        }
    }
}

private fun isBigWater(t: app.greenstreet.domain.Terrain, x: Int, y: Int): Boolean {
    var n = 0
    for (j in -30..30 step 3) for (i in -30..30 step 3) if (t.at(x + i, y + j) == app.greenstreet.domain.K.WATER) n++
    return n > 120
}

fun bedState(c: app.greenstreet.AppContainer, b: app.greenstreet.domain.Bed, today: Long, ru: Boolean): String {
    val v = c.content.variety.getValue(b.variety)
    if (c.game.ripe(b)) return w(ru, "урожай готов", "ready to pick") + if (b.wild) w(ru, " · одичала", " · gone wild") else ""
    val parts = ArrayList<String>()
    parts += w(ru, "%d из %d дн.", "%d of %d days").format(b.grown.toInt(), v.days)
    if (b.wild) parts += w(ru, "одичала", "gone wild")
    parts += if (b.lastTend == today) w(ru, "полита", "watered") else w(ru, "не полита", "not watered")
    return parts.joinToString(" · ")
}

/** The street name nearest to the player, or a general word. */
fun placeName(c: app.greenstreet.AppContainer, at: Geo, ru: Boolean): String {
    if (c.atHome(at)) return w(ru, "У дома", "At home")
    c.places.keys.firstOrNull { c.near(it, at) }?.let { return c.content.person.getValue(it).role(ru).replaceFirstChar { ch -> ch.uppercase() } }
    val d = c.district ?: return ""
    val x = d.x(at.lon); val y = d.y(at.lat)
    var best: String? = null; var bd = 14.0
    for (r in d.roads) {
        if (r.name.isBlank()) continue
        var i = 0
        while (i + 1 < r.pts.size) { val dd = Math.hypot((r.pts[i] - x).toDouble(), (r.pts[i + 1] - y).toDouble()); if (dd < bd) { bd = dd; best = r.name }; i += 2 }
    }
    return best ?: w(ru, "В районе", "In the district")
}

/** The most useful thing to do next and where: a ripe bed, a request you can fulfil, seeds waiting, a place not yet visited. */
fun nextStep(c: app.greenstreet.AppContainer, me: Geo, ru: Boolean): Pair<String, Geo?>? {
    val s = c.state ?: return null
    val now = c.now()
    val reach = c.game.reach(s)
    s.beds.filter { c.content.variety[it.variety]?.place == Place.VERGE && c.game.ripe(it) }
        .map { it to Geo(it.lat, it.lon) }.filter { it.second.metres(me) > reach }.minByOrNull { it.second.metres(me) }?.let { (b, g) ->
            return w(ru, "Созрела грядка: ", "A bed is ripe: ") + c.content.name(c.content.variety.getValue(b.variety).crop)(ru).lowercase() + " · " + where(me, g, ru) to g
        }
    val people = c.places.keys.filter { s.bonds[it]?.isOpen == true }
    people.mapNotNull { id -> c.game.request(s, id)?.takeIf { c.game.canFulfil(s, it.needs) }?.let { id to c.placeGeo(id) } }
        .filter { it.second != null && !c.near(it.first, me) }.minByOrNull { it.second!!.metres(me) }?.let { (id, g) ->
            return w(ru, "Можно отнести: %s ждёт то, что у вас есть · ", "You can deliver: %s is waiting for what you have · ").format(c.content.person.getValue(id).name(ru)) + where(me, g!!, ru) to g
        }
    people.filter { c.game.seedsToday(s, now, it) != null && !c.near(it, me) }.mapNotNull { id -> c.placeGeo(id)?.let { id to it } }.minByOrNull { it.second.metres(me) }?.let { (id, g) ->
        return w(ru, "Семена на сегодня ждут у: %s · ", "Today's seeds are waiting at %s's · ").format(c.content.person.getValue(id).name(ru)) + where(me, g, ru) to g
    }
    c.places.keys.filter { s.bonds[it]?.isOpen != true }.mapNotNull { id -> c.placeGeo(id)?.let { id to it } }.minByOrNull { it.second.metres(me) }?.let { (id, g) ->
        return w(ru, "Здесь ещё не были: %s · ", "Not visited yet: %s · ").format(c.content.person.getValue(id).role(ru)) + where(me, g, ru) to g
    }
    return null
}
