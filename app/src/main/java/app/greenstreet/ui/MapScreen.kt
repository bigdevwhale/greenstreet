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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.sp
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
import app.greenstreet.domain.Whereabouts
import app.greenstreet.ui.art.villager
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
import app.greenstreet.ui.components.Segments
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
    private const val ART = 4
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
    // off the street's map (a rough fix far away, or the player away from home, #60): the camera stays on home,
    // which would otherwise be pinned to the map's edge with neither the player nor home in sight
    val offMap = mx < 0 || my < 0 || mx >= t.w || my >= t.h
    val home = s.home
    val fx = if (offMap && home != null) d.x(home.lon).roundToInt() else mx
    val fy = if (offMap && home != null) d.y(home.lat).roundToInt() else my
    LaunchedEffect(now.month) {
        if (MapCache.month != now.month) {
            withContext(Dispatchers.Default) { MapCache.load(ctx, t, now.month, fx, fy) { ready++ } }
            ready++
        }
    }
    var pan by remember { mutableStateOf(IntOffset.Zero) }
    val focus by c.focus.collectAsState()
    LaunchedEffect(focus) {
        val f = focus ?: return@LaunchedEffect
        pan = IntOffset((d.x(f.lon) - fx).roundToInt(), (d.y(f.lat) - fy).roundToInt()); c.focus.value = null
    }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { var last = 0L; while (true) withFrameMillis { ms -> if (ms - last >= 110) { last = ms; tick++ } } }
    var zoom by remember { mutableIntStateOf(1) }
    val lt = app.greenstreet.Clock.now()
    val moment = Moment(lt.hour * 60 + lt.minute, now.sky, now.month, now.day)
    // the neighbours walk: where everyone is is worked out again every second
    var clock by remember { mutableStateOf(app.greenstreet.Clock.now()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); clock = app.greenstreet.Clock.now() } }
    val status = c.places.keys.associateWith { c.whereabouts(it, clock) }
    val walkers = status.filterValues { it == Whereabouts.OUT }.keys.mapNotNull { id -> c.spotNow(id, clock)?.let { id to it } }.toMap()
    // the tap handler lives across recompositions: it reads where the walkers are now, not where they were when it was made
    val walkersNow by rememberUpdatedState(walkers)
    // moving a neighbour's place: the next tap on the map says where to
    val moving by c.moving.collectAsState()
    val movingNow by rememberUpdatedState(moving)
    var moveMsg by remember { mutableStateOf<String?>(null) }
    // the whole ring has to be in sight to choose a spot in it: zoom right out
    LaunchedEffect(moving) { if (moving != null) { moveMsg = null; zoom = ZOOMS.size - 1 } }
    // the panel under the map folds away, and the map takes the whole screen
    var panelOpen by rememberSaveable { mutableStateOf(true) }

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
            // read `ready` first: it is the state that recomposes this when the map is done; MapCache.month is a plain field
            val mapLoading = ready < 0 || MapCache.month != now.month
            if (mapLoading) { LaunchedEffect(Unit) { c.stage.value = "paint" }; Loading(Modifier.align(Alignment.Center)) }
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
                // keyed on the zoom and the player's spot too: a handler kept from before a zoom read taps at the old scale and missed the beds
                .pointerInput(ver, vw, vh, fx, fy) {
                    detectTapGestures(
                        onTap = { o ->
                            val k = size.width / vw.toFloat()
                            val cx = clampCam(fx - vw / 2 + pan.x, t.w, vw); val cy = clampCam(fy - vh * 11 / 20 + pan.y, t.h, vh)
                            val ax = cx + o.x / k; val ay = cy + o.y / k
                            // a finger is about 24 dp wide whatever the zoom: far out, a bed is only a few map pixels
                            val finger = 24.dp.toPx() / k
                            movingNow?.let { id ->
                                if (c.movePlace(id, ax, ay)) { c.moving.value = null; moveMsg = w(ru, "Готово: место передвинуто.", "Done: the place has moved.") }
                                else moveMsg = w(ru, "Сюда нельзя: не дальше %d м от исходного места и рядом с тротуаром или дорожкой.", "Not there: within %d m of where it was, and by a pavement or path.").format(app.greenstreet.MOVE_M.toInt())
                                return@detectTapGestures
                            }
                            val bed = s.beds.filter { c.content.variety[it.variety]?.place == Place.VERGE }.minByOrNull { Math.hypot(d.x(it.lon) - ax.toDouble(), d.y(it.lat) - ay.toDouble()) }
                            if (bed != null && Math.hypot(d.x(bed.lon) - ax.toDouble(), d.y(bed.lat) - ay.toDouble()) < maxOf(7f, finger)) { open(Sheet.BedInfo(bed.id)); return@detectTapGestures }
                            val walker = walkersNow.entries.minByOrNull { Math.hypot(it.value.x - ax.toDouble(), it.value.y - 6 - ay.toDouble()) }
                            if (walker != null && Math.hypot(walker.value.x - ax.toDouble(), walker.value.y - 6 - ay.toDouble()) < maxOf(8f, finger)) { open(Sheet.Place(walker.key)); return@detectTapGestures }
                            val who = c.places.entries.minByOrNull { Math.hypot(it.value.x - ax.toDouble(), it.value.y - 6 - ay.toDouble()) }
                            if (who != null && Math.hypot(who.value.x - ax.toDouble(), who.value.y - 6 - ay.toDouble()) < maxOf(9f, finger)) open(Sheet.Place(who.key))
                        },
                        onLongPress = { o ->
                            if (!BuildConfig.DEMO) return@detectTapGestures
                            val k = size.width / vw.toFloat()
                            val cx = clampCam(fx - vw / 2 + pan.x, t.w, vw); val cy = clampCam(fy - vh * 11 / 20 + pan.y, t.h, vh)
                            val ax = cx + o.x / k; val ay = cy + o.y / k
                            pan = IntOffset.Zero
                            val tended = c.fix(Geo(d.lat(ay), d.lon(ax)), 5f, manual = true)
                            if (tended > 0) Tracker.lastTended.value = System.currentTimeMillis() to tended
                        },
                    )
                }) {
                // read the frame tick before any early return, or the canvas never learns to redraw
                val frame = tick + ready
                val base = MapCache.buf ?: return@Canvas
                frame.let { }
                val cx = clampCam(fx - vw / 2 + pan.x, t.w, vw); val cy = clampCam(fy - vh * 11 / 20 + pan.y, t.h, vh)
                for (y in 0 until vh) System.arraycopy(base.d, (cy + y) * base.w + cx, view.d, y * vw, vw)
                val objs = ArrayList<Pair<Int, () -> Unit>>()
                for (b in s.beds) {
                    val v = c.content.variety[b.variety] ?: continue
                    if (v.place != Place.VERGE) continue
                    val bx = d.x(b.lon).roundToInt() - cx; val by = d.y(b.lat).roundToInt() - cy
                    if (bx < -10 || by < -20 || bx > vw + 10 || by > vh + 10) continue
                    objs += by + 3 to { view.bedOf(b, v, c.game.ripe(b), now.day, bx, by, tick) }
                }
                // the player's own ponds: small blue ovals with a darker rim
                for (g in c.ponds()) {
                    val px = d.x(g.lon).roundToInt() - cx; val py = d.y(g.lat).roundToInt() - cy
                    if (px < -8 || py < -6 || px > vw + 8 || py > vh + 6) continue
                    objs += py - 4 to {
                        for (j in -4..4) for (i in -7..7) {
                            val e = (i * i) / 49.0 + (j * j) / 16.0
                            if (e <= 1.0) view.p(px + i, py + j, if (e > .62) 0xFF2F5E8C.toInt() else if (e < .2 && i < 0) 0xFF9CD3F2.toInt() else 0xFF4F8FC9.toInt())
                        }
                    }
                }
                for ((id, sp) in c.places) {
                    val p = c.content.person[id] ?: continue
                    val sx = sp.x.roundToInt() - cx; val sy = sp.y.roundToInt() - cy
                    if (sx < -6 || sy < -2 || sx > vw + 6 || sy > vh + 12) continue
                    objs += sy to { view.sign(sx, sy, Icons.forKind(p.kind), s.bonds[id]?.isOpen == true, status[id] != Whereabouts.WORK) }
                }
                for ((id, sp) in walkers) {
                    val p = c.content.person[id] ?: continue
                    val sx = sp.x.roundToInt() - cx; val sy = sp.y.roundToInt() - cy
                    if (sx < -6 || sy < -2 || sx > vw + 6 || sy > vh + 16) continue
                    val look = app.greenstreet.ui.art.Look.of(p.look)
                    val moving = c.walkingNow(id)
                    objs += sy to { view.villager(sx, sy, look, if (moving) ((tick / 3) + id.length) % 2 else 0, p.kind, id) }
                }
                // the ring a moved place must stay in
                moving?.let { id -> c.origin(id) }?.let { o ->
                    val ox = d.x(o.lon) - cx; val oy = d.y(o.lat) - cy
                    val rr = (app.greenstreet.MOVE_M / d.mpp).toFloat()
                    val n = (rr * 7).toInt()
                    // dashes, dark with a light core, so they show on lawns and roofs alike
                    for (q in 0 until n) if ((q / 6) % 3 != 2) {
                        val a = q * 2 * Math.PI / n
                        val px = (ox + rr * Math.cos(a)).roundToInt(); val py = (oy + rr * Math.sin(a)).roundToInt()
                        for (j in -1..1) for (i in -1..1) view.p(px + i, py + j, 0xFF2B1A14.toInt(), .8f)
                    }
                    for (q in 0 until n) if ((q / 6) % 3 != 2) {
                        val a = q * 2 * Math.PI / n
                        view.p((ox + rr * Math.cos(a)).roundToInt(), (oy + rr * Math.sin(a)).roundToInt(), 0xFFFFE9A8.toInt())
                    }
                }
                s.home?.let { h -> val hx = d.x(h.lon).roundToInt() - cx; val hy = d.y(h.lat).roundToInt() - cy; objs += hy to { view.sign(hx, hy, Icons.home, true) } }
                System.arraycopy(view.d, 0, under, 0, under.size)
                objs.sortedBy { it.first }.forEach { it.second() }
                // whatever stands in front of the houses must not be painted over by their lit windows
                for (i in under.indices) front[i] = view.d[i] != under[i]
                view.streetLife(t, cx, cy, moment, tick, MapCache.windows, mx - cx, my - cy, front)
                // you, last and unshaded by the night, so you're always the clearest thing on the map
                view.walker(mx - cx, my - cy, if (walking) (tick / 2) % 2 else 0, false, tick)
                bmp.setPixels(view.d, 0, vw, 0, 0, vw, vh)
                drawImage(img, srcOffset = IntOffset.Zero, srcSize = IntSize(vw, vh), dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.roundToInt(), (vh * size.width / vw).roundToInt()), filterQuality = FilterQuality.None)
            }
            val ccx = clampCam(fx - vw / 2 + pan.x, t.w, vw); val ccy = clampCam(fy - vh * 11 / 20 + pan.y, t.h, vh)
            val placed = ArrayList<FloatArray>()
            val order = c.places.entries.sortedByDescending { (id, _) -> (if (s.bonds[id]?.isOpen == true) 2 else 0) + (if (c.game.request(s, id) != null) 1 else 0) }
            // a closed place keeps its owner's name, pale, so you know whose it is
            for ((id, place) in order) {
                if (mapLoading) break
                if (status[id] != Whereabouts.OUT || s.bonds[id]?.isOpen != true) continue
                val p = c.content.person[id] ?: continue
                val sx = place.x - ccx; val sy = place.y - ccy
                if (sx < 0 || sy < 10 || sx > vw || sy > vh) continue
                val label = p.name(ru).substringBefore(' ')
                val lw = label.length * 8.6f + 10f
                val bw = boxW.value
                val lx = (bw * (sx / vw) - lw / 2).coerceIn(2f, bw - lw - 2f)
                val ly = bw * (sy / vw) - 46f
                if (placed.any { r -> lx < r[0] + r[2] && lx + lw > r[0] && ly < r[1] + 20f && ly + 20f > r[1] }) continue
                placed += floatArrayOf(lx, ly, lw)
                Text(label, style = T.Small.copy(color = C.muted), modifier = Modifier.offset(x = lx.dp, y = ly.dp)
                    .pixelBox(C.paper2.copy(alpha = .8f), bevel = null, lip = null).padding(horizontal = 7.dp, vertical = 3.dp), maxLines = 1)
            }
            for ((id, place) in order) {
                if (mapLoading) break
                val p = c.content.person[id] ?: continue
                val st = status[id]
                val sp = walkers[id] ?: place
                val sx = sp.x - ccx; val sy = sp.y - ccy - if (walkers[id] != null) 9 else 0
                if (sx < 0 || sy < 10 || sx > vw || sy > vh) continue
                val opened = s.bonds[id]?.isOpen == true
                val awake = st != Whereabouts.ASLEEP
                val asks = opened && awake && c.game.request(s, id) != null
                val label = if (!opened) "?" else p.name(ru).substringBefore(' ') + (if (asks) " !" else "") + if (!awake) " zz" else ""
                val lw = label.length * 8.6f + 10f
                val bw = boxW.value
                val lx = (bw * (sx / vw) - lw / 2).coerceIn(2f, bw - lw - 2f)
                val ly = bw * (sy / vw) - 46f
                if (placed.any { r -> lx < r[0] + r[2] && lx + lw > r[0] && ly < r[1] + 20f && ly + 20f > r[1] }) continue
                placed += floatArrayOf(lx, ly, lw)
                Text(label, style = T.Small.copy(color = if (asks) C.light else if (opened && awake) C.ink else C.muted),
                    modifier = Modifier.offset(x = lx.dp, y = ly.dp).pixelBox(if (asks) C.red else if (opened && awake) C.light else C.paper2, bevel = null, lip = null)
                        .padding(horizontal = 5.dp, vertical = 1.dp), maxLines = 1)
            }
            if (!mapLoading) Column(Modifier.align(Alignment.TopStart).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PixelButton("+", { if (zoom > 0) zoom-- }, kind = Kind.GHOST, enabled = zoom > 0)
                PixelButton("−", { if (zoom < ZOOMS.size - 1) zoom++ }, kind = Kind.GHOST, enabled = zoom < ZOOMS.size - 1)
            }
            moving?.let { id ->
                val who = c.content.person[id]
                // a neighbour not met yet goes by their trade, as on their card
                val whoName = who?.let { if (s.bonds[id]?.isOpen == true) it.name(ru) else it.role(ru) } ?: ""
                Panel(Modifier.align(Alignment.TopCenter).padding(start = 56.dp, end = 8.dp, top = 8.dp).fillMaxWidth(), fill = C.light) {
                    Text(w(ru, "Нажмите на карте, куда передвинуть место: ", "Tap the map where the place should go: ") + whoName +
                        w(ru, ". Не дальше %d м — в пределах кольца.", ". Within %d m — inside the ring.").format(app.greenstreet.MOVE_M.toInt()), style = T.Small.copy(color = C.ink))
                    moveMsg?.let { Text(it, style = T.Small.copy(color = C.red)) }
                    PixelButton(w(ru, "Отмена", "Cancel"), { c.moving.value = null }, kind = Kind.GHOST)
                }
            }
            if (moving == null) moveMsg?.let { m -> LaunchedEffect(m) { kotlinx.coroutines.delay(3000); moveMsg = null }
                Text(m, style = T.Small.copy(color = C.green), modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).pixelBox(C.light, bevel = null, lip = null).padding(horizontal = 8.dp, vertical = 4.dp)) }
            if (offMap && moving == null && moveMsg == null) Text(w(ru, "Вы сейчас за пределами карты своей улицы. Она показана от дома.", "You're off your street's map right now. It's shown from home."),
                style = T.Small.copy(color = C.ink), modifier = Modifier.align(Alignment.TopCenter)
                    // below the "To me" button when the map is moved
                    .padding(start = 56.dp, end = 8.dp, top = if (pan != IntOffset.Zero) 56.dp else 8.dp)
                    .pixelBox(C.light, bevel = null, lip = null).padding(horizontal = 8.dp, vertical = 4.dp))
            if (pan != IntOffset.Zero && moving == null) PixelButton(w(ru, "К себе", "To me"), { pan = IntOffset.Zero }, Modifier.align(Alignment.TopEnd).padding(8.dp), Kind.GHOST)
            if (!mapLoading) PixelButton(if (panelOpen) w(ru, "Скрыть ▾", "Hide ▾") else w(ru, "Описание ▴", "Details ▴"), { panelOpen = !panelOpen },
                Modifier.align(Alignment.BottomEnd).padding(8.dp), Kind.GHOST)
        }
        // what can be done right here: a compact block that never needs scrolling
        val near = s.beds.filter { c.content.variety[it.variety]?.place == Place.VERGE && Geo(it.lat, it.lon).metres(me) <= c.game.reach(s) }
        var questOpen by remember { mutableStateOf(false) }
        var digAsk by remember { mutableStateOf(false) }
        if (digAsk) androidx.compose.material3.AlertDialog(
            onDismissRequest = { digAsk = false },
            title = { Text(w(ru, "Выкопать пруд здесь?", "Dig a pond here?"), style = T.Heading) },
            text = { Text(w(ru, "Если рядом нет воды или она за забором, можно выкопать свой пруд там, где вы стоите. В нём ловится та же рыба, что в реке. Прудов можно выкопать не больше %d, не ближе 300 м друг к другу.",
                "If there's no water near, or it's behind a fence, dig a pond of your own where you stand. It has the same fish as a river. Up to %d ponds, at least 300 m apart.").format(app.greenstreet.MAX_PONDS), style = T.Body) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { c.here.value?.let { c.digPond(it) }; digAsk = false }) { Text(w(ru, "Выкопать", "Dig"), style = T.Body.copy(color = C.green)) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { digAsk = false }) { Text(w(ru, "Отмена", "Cancel"), style = T.Body) } },
        )
        if (panelOpen) Column(Modifier.fillMaxWidth().heightIn(max = 340.dp).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ver.let { }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(placeName(c, me, ru), style = T.Heading, modifier = Modifier.weight(1f), maxLines = 1)
                Text(w(ru, "сегодня %.1f км", "today %.1f km").format(s.kmToday).replace('.', if (ru) ',' else '.'), style = T.Small)
            }
            // the opening task, or once those are done, today's errand
            val errand = app.greenstreet.domain.Quests.current(s) == null
            (app.greenstreet.domain.Quests.current(s) ?: app.greenstreet.domain.Errands.today(c.game, s, now))?.let { q ->
                val finished = errand && app.greenstreet.domain.Errands.done(s, now)
                Panel(Modifier.fillMaxWidth(), fill = if (finished) C.paper2 else C.light, onClick = { questOpen = !questOpen }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text((if (errand) w(ru, "на сегодня · ", "for today · ") else w(ru, "задание · ", "task · ")) +
                                app.greenstreet.domain.Quests.from(q.from) { id -> c.content.person[id]?.name }(ru) + if (finished) w(ru, " · сделано ✓", " · done ✓") else "", style = T.Eyebrow)
                            Text(q.title(ru) + if (q.goal > 1 && !finished) "  ${progressText(q.progress(s).coerceAtMost(q.goal), q.unit, ru)}/${progressText(q.goal, q.unit, ru)}" else "",
                                style = T.Body.copy(color = if (finished) C.muted else C.ink), maxLines = 1)
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
                Text("${v.name(ru)} · ${bedState(c, b, now, ru)}", style = T.Small.copy(color = C.ink), maxLines = 1)
            }
            // sowing, when standing by a free verge
            // within ~15 m: the GPS dot wanders that much, and the verge is wherever the player is standing
            val spot = t.nearestVerge(mx, my, (15 / d.mpp).roundToInt())
            if (spot != null) {
                // quickest first, each with the day it would be ripe, so nobody fills every bed with winter rye by mistake
                val sowable = s.seeds.keys.mapNotNull { c.content.variety[it] }.filter { it.place == Place.VERGE && c.game.canSow(s, now, it) }.sortedBy { it.days }
                val tooClose = c.game.tooClose(s, Geo(d.lat(spot[1] + .5f), d.lon(spot[0] + .5f)))
                when {
                    tooClose -> Text(w(ru, "Рядом уже есть грядка. Между грядками — не меньше 25 метров, иначе им тесно.", "There's a bed close by. Beds need 25 metres between them, or they crowd each other."), style = T.Small)
                    sowable.isEmpty() -> Text(nothingToSow(c, now, ru), style = T.Small)
                    // the packets in a row, each with the day it would be ripe: tap one to sow it here
                    else -> Column {
                        Text(w(ru, "Посеять здесь — нажмите на пакетик", "Sow here — tap a packet") +
                            if (c.game.cold(now)) w(ru, " (зимой растёт вдвое дольше)", " (winter: twice as long)") else "", style = T.Small)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            sowable.forEach { v ->
                                val hot = c.game.demand(s, now) == v.crop
                                SeedPacket(c, v, s.seeds[v.key], ru, dayMonth(c.game.ripeOn(v, now), ru), mark = if (hot) "★" else null, variety = false) {
                                    c.game.sow(s, now, v.key, Geo(d.lat(spot[1] + .5f), d.lon(spot[0] + .5f))); c.changed()
                                }
                            }
                        }
                    }
                }
            }
            // ripe beds here: the one thing worth a big button besides the walk
            near.filter { c.game.ripe(it) }.forEach { b ->
                PixelButton(w(ru, "Собрать: ", "Pick: ") + c.content.name(c.content.variety.getValue(b.variety).crop)(ru).lowercase(), { open(Sheet.Harvest(b.id)) }, Modifier.fillMaxWidth(), kind = Kind.LAMP)
            }
            // what this spot offers: small chips, each with its picture
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                c.places.keys.filter { c.near(it, me) }.forEach { id ->
                    val met = s.bonds[id]?.isOpen == true
                    val who = c.content.person.getValue(id)
                    val label = when {
                        !met && status[id] == Whereabouts.OUT -> w(ru, "Познакомиться", "Say hello")
                        !met -> w(ru, "Зайти: ", "Visit: ") + who.role(ru)
                        status[id] == Whereabouts.OUT -> w(ru, "Поговорить", "Talk") + ": " + who.name(ru)
                        else -> w(ru, "Зайти", "Visit") + ": " + who.name(ru)
                    }
                    app.greenstreet.ui.components.ActionChip(label, { open(Sheet.Visit(id)) }, leading = { app.greenstreet.ui.components.Avatar(portrait(c, id), 28.dp) })
                }
                // a pond the player dug fishes like a river (#47, #48)
                val ownPond = c.byOwnPond(me)
                if (t.nearWater(mx, my, 6) || ownPond) app.greenstreet.ui.components.ActionChip(w(ru, "Удочка", "Fish"), { open(Sheet.Fish(ownPond || isBigWater(t, mx, my) || t.byBigPond(mx, my))) }, "walk")
                // no water close by: a pond can be dug where you stand, like home at the start
                if (!t.nearWater(mx, my, 30) && !ownPond && c.pondBlock(c.here.value) == null) app.greenstreet.ui.components.ActionChip(w(ru, "Выкопать пруд", "Dig a pond"), { digAsk = true }, "walk")
                if (c.near("mikhalych", me)) app.greenstreet.ui.components.ActionChip(w(ru, "Хозтовары", "Hardware"), { open(Sheet.Shop) }, "tools")
                if (c.near("zina", me)) app.greenstreet.ui.components.ActionChip(if (now.saturday) w(ru, "Ярмарка", "The fair") else w(ru, "Семена", "Seeds"), { open(Sheet.Market) }, if (now.saturday) "fair" else "market")
                if (c.atHome(me)) app.greenstreet.ui.components.ActionChip(w(ru, "Во двор", "Courtyard"), { open(Sheet.Tab(1)) }, "tab_yard")
            }
            // the walk: the one main action, across the whole width; it says why when it can't start (#26)
            val tracker = remember { app.greenstreet.work.Tracker.of(ctx) }
            var walkWhy by remember { mutableStateOf<String?>(null) }
            val askWalk = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { got ->
                tracker.refresh()
                if (got.values.any { it } && tracker.allowed) { WalkService.start(ctx); walkWhy = null }
                else walkWhy = w(ru, "Без геолокации прогулка не начнётся: разрешите её в настройках телефона.", "A walk can't start without location: allow it in the phone's settings.")
            }
            PixelButton(if (walking) w(ru, "Закончить прогулку", "End the walk") else w(ru, "Выйти на прогулку", "Go for a walk"), {
                when {
                    walking -> WalkService.stop(ctx)
                    !tracker.allowed || (android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) ->
                        askWalk.launch(buildList { add(android.Manifest.permission.ACCESS_FINE_LOCATION); add(android.Manifest.permission.ACCESS_COARSE_LOCATION); if (android.os.Build.VERSION.SDK_INT >= 33) add(android.Manifest.permission.POST_NOTIFICATIONS) }.toTypedArray())
                    !tracker.locationOn -> walkWhy = w(ru, "Геолокация в телефоне выключена: включите её в шторке, и прогулка начнётся.", "Location is off on the phone: turn it on in the quick settings and the walk will start.")
                    else -> { WalkService.start(ctx); walkWhy = null }
                }
            }, Modifier.fillMaxWidth(), kind = if (walking) Kind.GHOST else Kind.PRIMARY)
            walkWhy?.let { Text(it, style = T.Small.copy(color = C.red)) }
            // as in Fogwalk: a weak signal is said plainly, since steps then don't count
            val acc by c.hereAcc.collectAsState()
            if (acc > app.greenstreet.WEAK_GPS) Text(w(ru, "Слабый сигнал GPS (±%d м): шаги и грядки сейчас не засчитываются. Выйдите на открытое место, подальше от высоких стен.",
                "Weak GPS signal (±%d m): steps and beds don't count right now. Step into the open, away from tall walls.").format(acc.toInt()), style = T.Small.copy(color = C.red))
            if ("pause" in s.flags) Text(w(ru, "Игра на паузе: дни на улице не идут. Снять паузу – в настройках.", "The game is paused: no days pass on the street. Lift it in Settings."), style = T.Small.copy(color = C.ochre))
            val tended by Tracker.lastTended.collectAsState()
            if (System.currentTimeMillis() - tended.first < 6000 && tended.second > 0)
                Text(w(ru, "Грядок проведано: %d. Полито.", "Beds looked after: %d. Watered.").format(tended.second), style = T.Body.copy(color = C.green))
        }
    }
}

/** A seed packet to sow with a tap, its name written under it so nobody has to tell rye from wheat by the picture; on the street, under the map, only the crop and the date, to keep the walk button in sight. */
@Composable
fun SeedPacket(c: app.greenstreet.AppContainer, v: app.greenstreet.data.Variety, count: Int?, ru: Boolean, sub: String? = null, mark: String? = null, variety: Boolean = true, onClick: () -> Unit) {
    // the crop in dark letters ("Рожь"), the variety under it, pale ("«Северянка»"): a long name never breaks mid-word
    val full = v.name(ru)
    val cut = full.indexOf(if (ru) " «" else " '")
    val crop = if (cut > 0) full.substring(0, cut) else full
    val sort = if (cut > 0) full.substring(cut + 1) else null
    val ell = androidx.compose.ui.text.style.TextOverflow.Ellipsis
    Column(Modifier.width(90.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ItemSlot(app.greenstreet.ui.art.ItemIcons.packet(c.content, v.key), count, false, mark = mark, onClick = onClick)
        Text(crop, style = T.Small.copy(color = C.ink, lineHeight = 16.sp), textAlign = TextAlign.Center, maxLines = 2, overflow = ell, modifier = Modifier.padding(top = 2.dp))
        if (sort != null && variety) Text(sort, style = T.Small.copy(lineHeight = 16.sp), textAlign = TextAlign.Center, maxLines = 1, overflow = ell)
        if (sub != null) Text(sub, style = T.Small.copy(color = C.ink), maxLines = 1)
    }
}

private fun isBigWater(t: app.greenstreet.domain.Terrain, x: Int, y: Int): Boolean {
    var n = 0
    for (j in -30..30 step 3) for (i in -30..30 step 3) if (t.at(x + i, y + j) == app.greenstreet.domain.K.WATER) n++
    return n > 120
}

/** Why nothing can go on this verge now: every verge bed is taken, or there are no seeds for outdoors. */
fun nothingToSow(c: app.greenstreet.AppContainer, now: app.greenstreet.domain.Now, ru: Boolean): String {
    val s = c.state ?: return ""
    val mine = s.seeds.filterValues { it > 0 }.keys.mapNotNull { c.content.variety[it] }.filter { it.place == Place.VERGE }
    if (mine.isEmpty()) return w(ru, "Газон свободен, но семян для улицы у вас нет: их дают соседи и продаёт рынок.",
        "The verge is free, but you have no seeds for outdoors: neighbours give them and the market sells them.")
    return w(ru, "Больше грядок у дороги сейчас не завести: их уже %d из %d. Соберите урожай или перекопайте старую.",
        "No more verge beds for now: you have %d of %d. Pick a harvest or dig up an old one.")
        .format(s.beds.count { c.content.variety[it.variety]?.place == Place.VERGE }, c.game.capacity(s, now, Place.VERGE))
}

/** A task's progress: whole steps, or tenths ("0,7") when counted in parts. */
fun progressText(n: Int, unit: Int, ru: Boolean): String =
    if (unit == 1) "$n" else "${n / unit}" + (if (n % unit == 0) "" else (if (ru) "," else ".") + "${n % unit}")

/** "15 авг." / "15 Aug". */
fun dayMonth(d: java.time.LocalDate, ru: Boolean): String =
    "${d.dayOfMonth} " + if (ru) listOf("янв.", "февр.", "марта", "апр.", "мая", "июня", "июля", "авг.", "сент.", "окт.", "нояб.", "дек.")[d.monthValue - 1]
    else java.time.Month.of(d.monthValue).name.take(3).lowercase().replaceFirstChar { it.uppercase() }

/** When a bed ripens, in words: "ripe in ~5 days", "ripe by morning", "ready to pick". */
fun ripeIn(o: app.greenstreet.domain.Game.Outlook, ru: Boolean): String = when (o.nights) {
    0 -> w(ru, "урожай готов", "ready to pick")
    1 -> w(ru, "созреет к утру", "ripe by morning")
    else -> w(ru, "созреет через ~%d дн.", "ripe in ~%d days").format(o.nights)
}

/** Has the bed had its water today, in words. */
fun wateredWord(o: app.greenstreet.domain.Game.Outlook, ru: Boolean): String = when {
    o.watered -> w(ru, "сегодня полита", "watered today")
    o.rain && o.snow -> w(ru, "польёт снег", "the snow waters it")
    o.rain -> w(ru, "польёт дождь", "the rain waters it")
    else -> w(ru, "сегодня не полита", "not watered today")
}

/** One line for lists: when it ripens and whether it has been watered today. */
fun bedState(c: app.greenstreet.AppContainer, b: app.greenstreet.domain.Bed, now: app.greenstreet.domain.Now, ru: Boolean): String {
    val s = c.state ?: return ""
    val o = c.game.outlook(s, b, now)
    if (o.nights == 0) return ripeIn(o, ru) + if (b.wild) w(ru, " · одичала", " · gone wild") else ""
    val parts = arrayListOf(ripeIn(o, ru))
    if (b.wild) parts += w(ru, "одичала", "gone wild")
    parts += wateredWord(o, ru)
    return parts.joinToString(" · ")
}

/** The bed card: how far it has grown, when it ripens, has it been watered, and what to watch out for. */
@Composable
fun BedDetails(c: app.greenstreet.AppContainer, b: app.greenstreet.domain.Bed, now: app.greenstreet.domain.Now, ru: Boolean) {
    val s = c.state ?: return
    val v = c.content.variety.getValue(b.variety)
    val o = c.game.outlook(s, b, now)
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // ripening: the main question, in big letters
        Text(ripeIn(o, ru).replaceFirstChar { it.uppercase() } + if (o.nights > 1) w(ru, ", если поливать каждый день", " if watered every day") else "",
            style = T.Body.copy(color = if (o.nights == 0) C.green else C.ink))
        Text(w(ru, "Рост: %d из %d дн.", "Growth: %d of %d days").format(b.grown.toInt().coerceAtMost(v.days), v.days), style = T.Small)
        Segments((b.grown / v.days).toFloat())
        if (!o.indoors && o.nights != 0 && c.game.cold(now)) Text(w(ru, "Зима: сейчас растёт вдвое медленнее.", "Winter: growing at half pace for now."), style = T.Small)
        if (!b.wild && v.tends > 0) {
            Text(w(ru, "Поливов: %d из %d нужных", "Waterings: %d of %d needed").format(b.tended.coerceAtMost(v.tends), v.tends), style = T.Small, modifier = Modifier.padding(top = 2.dp))
            Segments(b.tended.toFloat() / v.tends)
        }
        if (o.nights == 0) return@Column
        // today's water: a coloured box, so it reads at a glance
        val (fill, text) = when {
            o.indoors && o.watered -> C.greenLight to w(ru, "Сегодня полита: вы были дома.", "Watered today: you were home.")
            o.indoors -> C.paper2 to w(ru, "Сегодня не полита. Горшки поливаются сами, когда вы дома.", "Not watered today. Pots get watered whenever you're home.")
            o.watered -> C.greenLight to w(ru, "Сегодня полита: вы прошли рядом.", "Watered today: you walked by.")
            o.rain && o.snow -> C.greenLight to w(ru, "Сегодня полита: идёт снег, к ночи он польёт.", "Watered today: it's snowing, the snow does it by night.")
            o.rain -> C.greenLight to w(ru, "Сегодня полита: идёт дождь, к ночи он польёт.", "Watered today: it's raining, the rain does it by night.")
            else -> C.paper2 to w(ru, "Сегодня ещё не полита. Чтобы полить, пройдите в %d м от грядки — больше ничего не нужно.",
                "Not watered yet today. To water it, walk within %d m of the bed — nothing else needed.").format(c.game.reach(s).toInt())
        }
        Text(text, style = T.Small.copy(color = if (fill == C.greenLight) C.light else C.ink),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).pixelBox(fill, bevel = null, lip = null).padding(horizontal = 8.dp, vertical = 5.dp))
        // what can go wrong tonight
        if (b.wild) Text(w(ru, "Одичала: урожай будет вдвое меньше." + if (c.game.owns(s, "hoe")) " Прополите — станет как была." else " Прополоть можно тяпкой.",
            "Gone wild: it will give half the harvest." + if (c.game.owns(s, "hoe")) " Weed it to set it right." else " A hoe can weed it."), style = T.Small.copy(color = C.red))
        else if (o.wildIn != null && !o.watered && !o.rain && o.wildIn <= 2)
            Text((if (o.wildIn == 1) w(ru, "Без полива сегодня к утру одичает.", "Without water today it goes wild by morning.")
                else w(ru, "Без полива одичает через %d дн.", "Goes wild in %d days without water.").format(o.wildIn)), style = T.Small.copy(color = C.red))
    }
}

/** The street name nearest to the player, or a general word. */
fun placeName(c: app.greenstreet.AppContainer, at: Geo, ru: Boolean): String {
    if (c.atHome(at)) return w(ru, "У дома", "At home")
    c.places.keys.firstOrNull { c.whereabouts(it) == Whereabouts.WORK && c.near(it, at) }?.let { return c.content.person.getValue(it).role(ru).replaceFirstChar { ch -> ch.uppercase() } }
    val d = c.district ?: return ""
    val x = d.x(at.lon); val y = d.y(at.lat)
    var best: String? = null; var bd = 14.0
    for (r in d.roads) {
        if (r.name.isBlank()) continue
        var i = 0
        while (i + 1 < r.pts.size) { val dd = Math.hypot((r.pts[i] - x).toDouble(), (r.pts[i + 1] - y).toDouble()); if (dd < bd) { bd = dd; best = r.name }; i += 2 }
    }
    return best ?: w(ru, "На прогулке", "Out for a walk")
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
    people.mapNotNull { id -> c.game.request(s, id)?.takeIf { c.game.canFulfil(s, it.needs) }?.let { id to c.spotNow(id)?.let { sp -> c.geoOf(sp) } } }
        .filter { it.second != null && !c.near(it.first, me) }.minByOrNull { it.second!!.metres(me) }?.let { (id, g) ->
            return w(ru, "Можно отнести: %s ждёт то, что у вас есть · ", "You can deliver: %s is waiting for what you have · ").format(c.content.person.getValue(id).name(ru)) + where(me, g!!, ru) to g
        }
    people.filter { c.game.seedsToday(s, now, it) != null && !c.near(it, me) }.mapNotNull { id -> c.spotNow(id)?.let { sp -> c.geoOf(sp) }?.let { id to it } }.minByOrNull { it.second.metres(me) }?.let { (id, g) ->
        return w(ru, "Семена на сегодня ждут у: %s · ", "Today's seeds are waiting at %s's · ").format(c.content.person.getValue(id).name(ru)) + where(me, g, ru) to g
    }
    c.places.keys.filter { s.bonds[it]?.isOpen != true }.mapNotNull { id -> c.spotNow(id)?.let { sp -> c.geoOf(sp) }?.let { id to it } }.minByOrNull { it.second.metres(me) }?.let { (id, g) ->
        return w(ru, "Здесь ещё не были: %s · ", "Not visited yet: %s · ").format(c.content.person.getValue(id).role(ru)) + where(me, g, ru) to g
    }
    return null
}
