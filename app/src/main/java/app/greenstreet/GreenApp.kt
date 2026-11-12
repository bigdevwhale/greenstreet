package app.greenstreet

import android.app.Application
import android.content.Context
import app.greenstreet.data.Content
import app.greenstreet.data.District
import app.greenstreet.data.Spot
import app.greenstreet.data.Store
import app.greenstreet.data.Tiles
import app.greenstreet.domain.Errands
import app.greenstreet.domain.Extras
import app.greenstreet.domain.Find
import app.greenstreet.domain.Forecast
import app.greenstreet.domain.Game
import app.greenstreet.domain.GameState
import app.greenstreet.domain.Geo
import app.greenstreet.domain.Morning
import app.greenstreet.domain.Now
import app.greenstreet.domain.Quest
import app.greenstreet.domain.K
import app.greenstreet.domain.Quests
import app.greenstreet.domain.Schedule
import app.greenstreet.domain.Whereabouts
import app.greenstreet.domain.Terrain
import app.greenstreet.domain.Weather
import app.greenstreet.domain.metres
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

/** Half the side of the district square around home, metres. */
const val DISTRICT_HALF = 800.0

/** A place counts as open when the public ways pass within this many map pixels (about 20 m). */
private const val REACH_PX = 12
/** How much nearer, in map pixels (about 100 m), a fenced-off place must be to win over an open one. */
private const val FENCE_PX = 60.0
/** A strolling neighbour stops and waits while the player is this close, metres. */
private const val WAIT_M = 60.0

/** Holds the game for the whole app: content, the player's state, the district and the weather. */
class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val content: Content = Content.read({ path -> runCatching { app.assets.open("content/$path") }.getOrNull() })
    val game = Game(content)
    val store = Store(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var state: GameState? = store.load()
        private set
    /** Bumped after every change so the screens redraw. */
    val version = MutableStateFlow(0)
    var district: District? = null
        private set
    var terrain: Terrain? = null
        private set
    var forecast: Forecast? = null
        private set
    /** The player's last known position. */
    val here = MutableStateFlow<Geo?>(null)
    /** Neighbour id → the place they keep (a spot on the district map). */
    val places = HashMap<String, Spot>()
    var morning: Morning? = null
    /** A place the map should pan to (asked from the neighbours list). */
    val focus = MutableStateFlow<Geo?>(null)
    /** What the district loading is doing now: download, parse, terrain, bind, paint, or empty. */
    val stage = MutableStateFlow("")

    private var lastFix: Geo? = null

    /** The opening task just finished, for a moment's banner. */
    val questDone = MutableStateFlow<Quest?>(null)
    /** Something just found on the walk, for a moment's banner. */
    val found = MutableStateFlow<Find?>(null)
    /** True while the app is on screen: finds then show as a banner, otherwise as a notification. */
    @Volatile var visible = false

    fun changed() {
        state?.let { s ->
            Quests.settle(s) { game.give(s, it) }.lastOrNull()?.let { questDone.value = it }
            Errands.settle(game, s, now())?.let { questDone.value = it }
            store.save(s)
        }
        version.value++
    }

    fun now(at: LocalDateTime = LocalDateTime.now()): Now {
        val today = at.toLocalDate()
        val f = forecastFor(today)
        val sky = f?.today ?: store.sky(today.toEpochDay()) ?: Weather.simulated(today, today.monthValue)
        val tomorrow = f?.tomorrow ?: Weather.simulated(today.plusDays(1), today.plusDays(1).monthValue)
        return Now(today, at.hour, sky, tomorrow, south = (state?.home?.lat ?: 1.0) < 0)
    }

    /** Loads the stored district and binds the neighbours to places. Heavy: call off the main thread. */
    fun loadDistrict() {
        val s = state ?: return android.util.Log.i("GreenStreet", "load: no state").let { }
        val home = s.home ?: return android.util.Log.i("GreenStreet", "load: no home").let { }
        val tiles = store.tiles()
        val json = if (tiles == null) store.district() ?: return android.util.Log.i("GreenStreet", "load: no district file").let { } else null
        android.util.Log.i("GreenStreet", "load: box=${s.box?.toList()}")
        val box = s.box ?: District.box(home.lat, home.lon, DISTRICT_HALF)
        stage.value = "parse"
        val d = if (tiles != null) Tiles.parse(tiles, box) else District.parse(json!!, box)
        district = d
        stage.value = "terrain"
        terrain = Terrain(d)
        stage.value = "bind"
        bind(d, home)
    }

    /**
     * Each town neighbour takes the nearest place of their kind, or else the nearest unclaimed shop. A place
     * the public ways don't reach (behind a fence, on closed land) loses to an open one unless that is much
     * further; if it still wins, its keeper stands on the nearest pavement outside instead.
     */
    private fun bind(d: District, home: Geo) {
        places.clear()
        val ter = terrain
        val hx = d.x(home.lon); val hy = d.y(home.lat)
        fun open(p: Spot) = ter == null || ter.reachable(p.x.toInt(), p.y.toInt(), REACH_PX)
        fun dist(p: Spot) = Math.hypot((p.x - hx).toDouble(), (p.y - hy).toDouble()) + if (open(p)) 0.0 else FENCE_PX
        fun settle(id: String, p: Spot) {
            val out = if (ter == null || open(p)) p else ter.nearestOpen(p.x.toInt(), p.y.toInt(), 400)?.let { Spot(it[0].toFloat(), it[1].toFloat(), p.kind, p.name) } ?: p
            places[id] = out
        }
        val taken = HashSet<Spot>()
        for (p in content.people.filter { !it.yard }) {
            val match = d.places.filter { s -> p.osm.any { (k, v) -> s.kind == "$k=$v" } && s !in taken }.minByOrNull(::dist)
            if (match != null) { settle(p.id, match); taken += match }
        }
        for (p in content.people.filter { !it.yard && it.id !in places }) {
            val any = d.places.filter { it !in taken && (it.kind.startsWith("shop=") || it.kind.startsWith("amenity=")) }.minByOrNull(::dist)
            if (any != null) { settle(p.id, any); taken += any }
        }
        pools.clear(); legs.clear()
    }

    fun placeGeo(id: String): Geo? {
        val d = district ?: return null
        val s = places[id] ?: return state?.home
        return Geo(d.lat(s.y), d.lon(s.x))
    }

    /** Where a neighbour is now: at work, out walking or asleep. */
    fun whereabouts(id: String, t: LocalDateTime = LocalDateTime.now()): Whereabouts =
        content.person[id]?.let { Schedule.status(it, t) } ?: Whereabouts.ASLEEP

    private var walkFor: Terrain? = null
    private var walkPts = IntArray(0)

    /** Paths and pavements to stroll on: every fourth walkway pixel, as x,y pairs. */
    private fun walks(t: Terrain): IntArray {
        if (walkFor === t) return walkPts
        val out = ArrayList<Int>()
        for (y in 2 until t.h - 2 step 4) for (x in 2 until t.w - 2 step 4) if (t.at(x, y) == K.WALK && t.open[y * t.w + x]) { out += x; out += y }
        walkPts = out.toIntArray(); walkFor = t
        return walkPts
    }

    /**
     * Where to find a neighbour right now, in map pixels: their place in working hours, a spot on a path
     * not far from it while out walking (changing every forty minutes), null while asleep.
     */
    fun spotNow(id: String, t: LocalDateTime = LocalDateTime.now()): Spot? {
        val p = content.person[id] ?: return null
        if (p.yard) return null
        val place = places[id] ?: return null
        return when (Schedule.status(p, t)) {
            Whereabouts.WORK -> place
            Whereabouts.ASLEEP -> null
            Whereabouts.OUT -> stroll(id, place, t)
        }
    }

    /** Where a strolling neighbour is facing: true when walking left. */
    fun walkingLeft(id: String): Boolean = strollState[id]?.left ?: false

    /** Is a strolling neighbour on the move this second (or stopped to look around)? */
    fun walkingNow(id: String): Boolean = strollState[id]?.moving ?: false

    private class Leg(val key: Long, val path: IntArray)
    private class StrollState(var left: Boolean, var moving: Boolean)
    private val legs = HashMap<String, Leg>()
    private val strollState = HashMap<String, StrollState>()

    /** A waypoint of a neighbour's stroll: a pavement or path spot within a couple of hundred metres of their place. */
    private fun waypoint(id: String, place: Spot, k: Long): IntArray? {
        val ter = terrain ?: return null
        val pts = walks(ter)
        if (pts.isEmpty()) return null
        val pool = pools.getOrPut(id) {
            fun within(r: Int) = (0 until pts.size / 2).filter { i -> Math.hypot(pts[2 * i] - place.x.toDouble(), pts[2 * i + 1] - place.y.toDouble()) in 12.0..r.toDouble() }
            within(110).ifEmpty { within(240) }.ifEmpty { (0 until pts.size / 2).toList() }.toIntArray()
        }
        val i = pool[Math.floorMod((id.hashCode() * 31L + k * 7919L).toInt(), pool.size)]
        return intArrayOf(pts[2 * i], pts[2 * i + 1])
    }
    private val pools = HashMap<String, IntArray>()

    /**
     * A path along the public walkways from a to b (breadth-first over the terrain, in a box around them);
     * across the roadway and through arches if the pavements alone don't join; straight if nothing does.
     */
    private fun route(ter: Terrain, a: IntArray, b: IntArray): IntArray =
        route(ter, a, b) { _, c -> c == K.WALK || c == K.DIRT || c == K.STEPS || c == K.BRIDGE }
            ?: route(ter, a, b) { _, _ -> true }
            ?: run {
                val n = maxOf(1, Math.hypot((b[0] - a[0]).toDouble(), (b[1] - a[1]).toDouble()).toInt())
                IntArray(2 * (n + 1)) { j -> val f = (j / 2) / n.toDouble(); if (j % 2 == 0) (a[0] + (b[0] - a[0]) * f).toInt() else (a[1] + (b[1] - a[1]) * f).toInt() }
            }

    private fun route(ter: Terrain, a: IntArray, b: IntArray, allow: (Int, Int) -> Boolean): IntArray? {
        val pad = 40
        val x0 = maxOf(0, minOf(a[0], b[0]) - pad); val y0 = maxOf(0, minOf(a[1], b[1]) - pad)
        val x1 = minOf(ter.w - 1, maxOf(a[0], b[0]) + pad); val y1 = minOf(ter.h - 1, maxOf(a[1], b[1]) + pad)
        val bw = x1 - x0 + 1; val bh = y1 - y0 + 1
        fun ok(x: Int, y: Int): Boolean { val i = y * ter.w + x; return ter.open[i] && allow(i, ter.at(x, y)) }
        val prev = IntArray(bw * bh) { -2 }
        val q = IntArray(bw * bh); var qh = 0; var qt = 0
        val start = (a[1] - y0) * bw + (a[0] - x0); val goal = (b[1] - y0) * bw + (b[0] - x0)
        prev[start] = -1; q[qt++] = start
        val dx = intArrayOf(1, -1, 0, 0, 1, 1, -1, -1); val dy = intArrayOf(0, 0, 1, -1, 1, -1, 1, -1)
        while (qh < qt) {
            val c = q[qh++]
            if (c == goal) break
            val cx = c % bw; val cy = c / bw
            for (d in 0 until 8) {
                val nx = cx + dx[d]; val ny = cy + dy[d]
                if (nx < 0 || ny < 0 || nx >= bw || ny >= bh) continue
                val n = ny * bw + nx
                if (prev[n] != -2 || !ok(nx + x0, ny + y0)) continue
                prev[n] = c; q[qt++] = n
            }
        }
        if (prev[goal] == -2) return null
        val out = ArrayList<Int>()
        var c = goal
        while (c >= 0) { out += c; c = prev[c] }
        out.reverse()
        return IntArray(out.size * 2) { j -> val cc = out[j / 2]; if (j % 2 == 0) cc % bw + x0 else cc / bw + y0 }
    }

    /**
     * A neighbour strolling: every six minutes they set off from one waypoint to the next along the paths at
     * an easy pace (about a metre a second), then stop and look about until it's time to go on. When the
     * player comes near, they wait where they are, and walk on from there once the player has gone.
     */
    private fun stroll(id: String, place: Spot, t: LocalDateTime): Spot? {
        val ter = terrain ?: return null
        val real = t.toLocalDate().toEpochDay() * 86400L + t.toLocalTime().toSecondOfDay()
        // the time they've stood waiting is taken off their walk, so they pick up where they stopped
        val last = seen[id]
        val waiting = last != null && real > last && real - last < 120 && waits(id)
        if (waiting) held[id] = (held[id] ?: 0L) + (real - last!!)
        if (last == null || real > last) seen[id] = real
        val secs = real - (held[id] ?: 0L)
        val k = secs / 360
        val leg = legs[id]?.takeIf { it.key == k } ?: run {
            val a = waypoint(id, place, k) ?: return place
            val b = waypoint(id, place, k + 1) ?: return place
            Leg(k, route(ter, a, b)).also { legs[id] = it }
        }
        val n = leg.path.size / 2
        val step = ((secs - k * 360) * 0.65).toInt()
        val i = minOf(step, n - 1)
        val st = strollState.getOrPut(id) { StrollState(false, false) }
        st.moving = step < n - 1 && !waiting
        if (i + 3 < n) st.left = leg.path[2 * (i + 3)] < leg.path[2 * i]
        return Spot(leg.path[2 * i].toFloat(), leg.path[2 * i + 1].toFloat(), "", "").also { lastSpot[id] = it }
    }

    /** Last time each stroll was worked out, seconds; seconds each neighbour has stood waiting; where they were. */
    private val seen = HashMap<String, Long>()
    private val held = HashMap<String, Long>()
    private val lastSpot = HashMap<String, Spot>()

    /** Is the player close enough to a strolling neighbour that they stop and wait? */
    private fun waits(id: String): Boolean {
        val me = here.value ?: return false
        val g = lastSpot[id]?.let(::geoOf) ?: return false
        return g.metres(me) <= WAIT_M
    }

    fun geoOf(s: Spot): Geo? { val d = district ?: return null; return Geo(d.lat(s.y), d.lon(s.x)) }

    /** Can the player talk to a neighbour now: near their place while it's open, near them while they walk, or home with the yard's people awake. */
    fun near(id: String, at: Geo? = here.value): Boolean {
        val p = content.person[id] ?: return false
        if (at == null) return false
        if (p.yard) return whereabouts(id) == Whereabouts.WORK && atHome(at)
        val st = whereabouts(id)
        val g = spotNow(id)?.let(::geoOf) ?: return false
        return g.metres(at) <= 40
    }

    fun atHome(at: Geo? = here.value): Boolean { val h = state?.home ?: return false; return at != null && h.metres(at) <= 40 }

    /** Takes a ready state (the demo) and a district box it was saved for. */
    fun adopt(s: GameState, box: DoubleArray) {
        state = s; s.box = box
        var t0 = System.currentTimeMillis()
        val json = store.district() ?: return
        android.util.Log.i("GreenStreet", "read ${json.length} in ${System.currentTimeMillis() - t0} ms"); t0 = System.currentTimeMillis()
        val d = District.parse(json, box)
        android.util.Log.i("GreenStreet", "parse ${System.currentTimeMillis() - t0} ms"); t0 = System.currentTimeMillis()
        district = d; terrain = Terrain(d)
        android.util.Log.i("GreenStreet", "terrain ${System.currentTimeMillis() - t0} ms"); t0 = System.currentTimeMillis()
        s.home?.let { bind(d, it) }
        android.util.Log.i("GreenStreet", "bind ${System.currentTimeMillis() - t0} ms")
        store.save(s)
    }

    /** Sets home where the player stands and downloads the district around it. */
    suspend fun setHome(at: Geo) {
        val box = District.box(at.lat, at.lon, DISTRICT_HALF)
        stage.value = "download"
        val tiles = try { withContext(Dispatchers.IO) { Tiles.download(box) } } catch (e: Exception) { stage.value = ""; throw e }
        store.saveTiles(tiles)
        val s = state ?: game.newGame(now(), at).also { state = it }
        s.home = at; s.box = box
        loadDistrict()
        changed()
    }

    /** Runs the nights missed since the last open and refreshes the weather. */
    fun wake() {
        val s = state ?: return
        val now = now()
        // only a real forecast goes on record: a made-up sky would stick to the day (see Store.remember)
        forecastFor(now.date)?.let { store.remember(now.day, it.today) }
        val m = game.catchUp(s, now) { day ->
            val date = LocalDate.ofEpochDay(day)
            (store.sky(day) ?: Weather.simulated(date, date.monthValue)) to (store.sky(day + 1) ?: Weather.simulated(date.plusDays(1), date.plusDays(1).monthValue))
        }
        if (m.grew + m.ripe + m.wild +m.mail.size + m.requests.size > 0 || m.festival != null || m.newYear) morning = m
        changed()
    }

    /** The forecast in memory, if it was fetched on [day]: one from yesterday (the app left open overnight) says nothing about today. */
    private fun forecastFor(day: LocalDate) =
        forecast?.takeIf { java.time.Instant.ofEpochMilli(it.at).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == day }

    fun refreshWeather() {
        val h = state?.home ?: return
        if (forecastFor(LocalDate.now())?.let { System.currentTimeMillis() - it.at < 3 * 3600_000L } == true) return
        scope.launch(Dispatchers.IO) {
            runCatching { Weather.fetch(h.lat, h.lon) }.getOrNull()?.let { f ->
                val today = LocalDate.now().toEpochDay()
                forecast = f; store.remember(today, f.today); store.remember(today + 1, f.tomorrow, ahead = true); version.value++
            }
        }
    }

    /** A new position from the GPS: distance walked, beds tended on the way. Returns how many beds were tended. */
    @Synchronized
    fun fix(at: Geo, accuracy: Float): Int {
        here.value = at
        val s = state ?: return 0
        if (accuracy > 40f) return 0
        var find: Find? = null
        lastFix?.let {
            val d = it.metres(at)
            if (d in 3.0..300.0) {
                s.km += d / 1000; s.kmToday += d / 1000
                val t = terrain; val dd = district
                val where = if (t != null && dd != null) with(Extras) { t.whereAt(dd.x(at.lon).toInt(), dd.y(at.lat).toInt()) } else setOf("street")
                find = game.walked(s, now(), d, where)
            }
        }
        lastFix = at
        find?.let { f -> found.value = f; changed(); if (!visible) notifyFind(f) }
        val tended = game.pass(s, now(), at).size
        if (tended > 0 || abs(s.kmToday * 1000 % 50) < 5) changed()
        return tended
    }
}

/** A find while the phone is in a pocket: a quiet notification that opens the game. */
private fun AppContainer.notifyFind(f: Find) {
    val m = app.getSystemService(android.app.NotificationManager::class.java) ?: return
    if (m.getNotificationChannel("finds") == null) m.createNotificationChannel(android.app.NotificationChannel("finds", app.getString(R.string.channel_finds), android.app.NotificationManager.IMPORTANCE_DEFAULT))
    val ru = app.resources.configuration.locales[0].language == "ru"
    val open = android.app.PendingIntent.getActivity(app, 2, android.content.Intent(app, MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP),
        android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
    val n = androidx.core.app.NotificationCompat.Builder(app, "finds")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(if (ru) "Находка по дороге" else "A find on the way")
        .setContentText(f.text(ru))
        .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(f.text(ru)))
        .setContentIntent(open).setAutoCancel(true)
        .build()
    runCatching { m.notify(100 + (System.currentTimeMillis() / 1000 % 50).toInt(), n) }
}

class GreenApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch {
          runCatching {
            if (BuildConfig.DEMO) app.greenstreet.data.DemoSeed.seedIfEmpty(container) else container.loadDistrict()
            if (container.terrain == null) container.loadDistrict()
          }.onFailure { android.util.Log.e("GreenStreet", "district", it) }
            // the demo has no real GPS: the player stands at home until moved by a long press
            if (BuildConfig.DEMO && container.here.value == null) container.here.value = container.state?.home
            container.version.value++
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as GreenApp).container
