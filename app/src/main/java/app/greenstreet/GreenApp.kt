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
import app.greenstreet.domain.Kept
import app.greenstreet.domain.Geo
import app.greenstreet.domain.Morning
import app.greenstreet.domain.Now
import app.greenstreet.domain.Quest
import app.greenstreet.domain.K
import app.greenstreet.domain.Quests
import app.greenstreet.domain.Schedule
import app.greenstreet.domain.Whereabouts
import app.greenstreet.domain.Terrain
import app.greenstreet.domain.Town
import app.greenstreet.domain.Weather
import app.greenstreet.domain.metres
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

/** Half the side of the district square around home, metres. */
const val DISTRICT_HALF = 800.0

/** A place counts as open when the public ways pass within this many map pixels (about 20 m). */
private const val REACH_PX = 12
/** How much nearer, in map pixels (about 100 m), a fenced-off place must be to win over an open one. */
private const val FENCE_PX = 60.0
/** A strolling neighbour stops and waits once the player is this close, metres (GPS alone can be 30 m off)… */
private const val WAIT_M = 80.0
/** …and walks on only when the player is this far again, so a jumpy fix at the edge doesn't set them off. */
private const val LEAVE_M = 120.0
/** How far a neighbour's place may be moved from where the map first put it, metres. */
const val MOVE_M = 150.0
/** How many ponds the player may dig (#48). */
const val MAX_PONDS = 2
/** Worse than this many metres a fix is too unsure to walk, tend or set home with (#26, #45). */
const val WEAK_GPS = 40f

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
    /** How sure the last position is, metres: past [WEAK_GPS] it neither walks, tends nor counts (#26). */
    val hereAcc = MutableStateFlow(0f)
    /** Neighbour id → the place they keep (a spot on the district map). */
    val places = HashMap<String, Spot>()
    var morning: Morning? = null
    /** Bumped when a new [morning] is ready, so the screen opens its card even when the nights ran after start-up. */
    val dawn = MutableStateFlow(0)
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

    fun now(at: LocalDateTime = Clock.now()): Now {
        val today = at.toLocalDate()
        val f = forecastFor(today)
        // the day's record first: it keeps the rain already fallen, which an evening forecast knows nothing of
        val own = state?.ownSky == true
        val sky = (if (own) null else store.sky(today.toEpochDay()) ?: f?.today) ?: Weather.simulated(today, today.monthValue)
        val tomorrow = (if (own) null else f?.tomorrow) ?: Weather.simulated(today.plusDays(1), today.plusDays(1).monthValue)
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
     *
     * A place once taken is kept in the save ([GameState.places]) and comes back as it was, even when the map
     * has been downloaded again since and the shop is gone from it; only newcomers are placed afresh.
     */
    private fun bind(d: District, home: Geo) {
        places.clear()
        val s = state
        val ter = terrain
        val hx = d.x(home.lon); val hy = d.y(home.lat)
        fun open(p: Spot) = ter == null || ter.reachable(p.x.toInt(), p.y.toInt(), REACH_PX)
        fun dist(p: Spot) = Math.hypot((p.x - hx).toDouble(), (p.y - hy).toDouble()) + if (open(p)) 0.0 else FENCE_PX
        val taken = HashSet<Spot>()
        // places the player marked out of reach (#41): never offered again
        val shut = s?.flags?.filter { it.startsWith("shut:") }?.mapNotNull { f -> f.removePrefix("shut:").split(',').takeIf { it.size == 2 }?.let { (a, b) -> a.toDoubleOrNull()?.let { la -> b.toDoubleOrNull()?.let { lo -> Spot(d.x(lo), d.y(la), "", "") } } } }.orEmpty()
        fun isShut(p: Spot) = shut.any { Math.hypot((it.x - p.x).toDouble(), (it.y - p.y).toDouble()) * d.mpp < 25 }
        taken += d.places.filter(::isShut)
        val town = content.people.filter { !it.yard }.map { it.id }.toSet()
        s?.places?.forEach { (id, k) ->
            if (id !in town) return@forEach
            val at = Spot(d.x(k.lon), d.y(k.lat), k.kind, k.name)
            places[id] = at
            // the shop it stands for (or outside of) is no one else's
            d.places.filter { it.kind == k.kind && it.name == k.name && it !in taken }
                .minByOrNull { Math.hypot((it.x - at.x).toDouble(), (it.y - at.y).toDouble()) }
                ?.takeIf { Math.hypot((it.x - at.x).toDouble(), (it.y - at.y).toDouble()) <= 400 }?.let { taken += it }
        }
        var fresh = false
        fun settle(id: String, p: Spot) {
            val out = if (ter == null || open(p)) p else ter.nearestOpen(p.x.toInt(), p.y.toInt(), 400)?.let { Spot(it[0].toFloat(), it[1].toFloat(), p.kind, p.name) } ?: p
            places[id] = out
            s?.places?.put(id, Kept(d.lat(out.y), d.lon(out.x), out.kind, out.name)); fresh = true
        }
        for (p in content.people.filter { !it.yard && it.id !in places }) {
            val match = d.places.filter { s -> p.osm.any { (k, v) -> s.kind == "$k=$v" } && s !in taken }.minByOrNull(::dist)
            if (match != null) { settle(p.id, match); taken += match }
        }
        for (p in content.people.filter { !it.yard && it.id !in places }) {
            val any = d.places.filter { it !in taken && (it.kind.startsWith("shop=") || it.kind.startsWith("amenity=")) }.minByOrNull(::dist)
            if (any != null) { settle(p.id, any); taken += any }
        }
        if (fresh && s != null) store.save(s)
        pools.clear(); legs.clear()
    }

    fun placeGeo(id: String): Geo? {
        val d = district ?: return null
        val s = places[id] ?: return state?.home
        return Geo(d.lat(s.y), d.lon(s.x))
    }

    /** Where a neighbour is now: at work, out walking or asleep. */
    fun whereabouts(id: String, t: LocalDateTime = Clock.now()): Whereabouts =
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
    fun spotNow(id: String, t: LocalDateTime = Clock.now()): Spot? {
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
    private val waiting = HashSet<String>()

    /** Is the player close enough to a strolling neighbour that they stop and wait? Once waiting, they wait till the player is well away. */
    private fun waits(id: String): Boolean {
        val me = here.value ?: return false
        val g = lastSpot[id]?.let(::geoOf) ?: return false
        val stay = g.metres(me) <= if (id in waiting) LEAVE_M else WAIT_M
        if (stay) waiting += id else waiting -= id
        return stay
    }

    /**
     * Works the strolls out at a new fix: the map does it every second, but with the screen off nobody does, and
     * a neighbour the player was walking up to would have counted the whole time as walking on, and be gone.
     */
    private fun stepStrolls() {
        for (id in places.keys) if (whereabouts(id) == Whereabouts.OUT) spotNow(id)
    }

    /** The neighbour whose place the player is moving on the map, or null. */
    val moving = MutableStateFlow<String?>(null)

    /** Where the map first put a neighbour's place: moves are measured from here. */
    fun origin(id: String): Geo? = state?.places?.get(id)?.let { Geo(it.olat, it.olon) }

    /**
     * Moves a neighbour's place to map point (x, y), for places the player can't get to (behind a fence, in a
     * locked yard): no further than [MOVE_M] from where the map first put it, and onto ground the public ways
     * reach (a spot a little off them goes to the nearest pavement). False if it can't go there.
     */
    fun movePlace(id: String, x: Float, y: Float): Boolean {
        val d = district ?: return false; val ter = terrain ?: return false; val s = state ?: return false
        val old = s.places[id] ?: return false
        val xi = x.toInt(); val yi = y.toInt()
        val at = if (ter.reachable(xi, yi, 3)) intArrayOf(xi, yi) else ter.nearestOpen(xi, yi, 12) ?: return false
        val sp = Spot(at[0].toFloat(), at[1].toFloat(), old.kind, old.name)
        val g = Geo(d.lat(sp.y), d.lon(sp.x))
        if (Geo(old.olat, old.olon).metres(g) > MOVE_M) return false
        places[id] = sp
        s.places[id] = Kept(g.lat, g.lon, old.kind, old.name, old.olat, old.olon)
        pools.remove(id); legs.remove(id)
        store.save(s); version.value++
        return true
    }

    fun geoOf(s: Spot): Geo? { val d = district ?: return null; return Geo(d.lat(s.y), d.lon(s.x)) }

    /** The ponds the player dug (#47, #48): where the map has too little water, a dam where they stood. */
    fun ponds(): List<Geo> = state?.flags?.filter { it.startsWith("pond:") }?.mapNotNull { f ->
        f.removePrefix("pond:").split(',').takeIf { it.size == 2 }?.let { (a, b) -> a.toDoubleOrNull()?.let { la -> b.toDoubleOrNull()?.let { lo -> Geo(la, lo) } } }
    }.orEmpty()

    /** By one of the player's ponds, within 25 m. */
    fun byOwnPond(at: Geo? = here.value): Boolean = at != null && ponds().any { it.metres(at) <= 25 }

    /** Why a pond can't be dug at [at] now, or null if it can. */
    fun pondBlock(at: Geo?): String? {
        if (at == null) return "gps"
        if (hereAcc.value > WEAK_GPS) return "gps"
        if (ponds().size >= MAX_PONDS) return "max"
        if (ponds().any { it.metres(at) < 300 }) return "close"
        val d = district ?: return "map"; val t = terrain ?: return "map"
        if (!t.reachable(d.x(at.lon).toInt(), d.y(at.lat).toInt(), 3)) return "shut"
        return null
    }

    /** Digs a pond where the player stands (#48): it fishes like a river. */
    fun digPond(at: Geo): Boolean {
        if (pondBlock(at) != null) return false
        val s = state ?: return false
        s.flags += "pond:%.6f,%.6f".format(java.util.Locale.ROOT, at.lat, at.lon)
        changed(); return true
    }

    /**
     * The player can't get to a neighbour's place at all (a locked kindergarten yard, #41): as in Postround, it is
     * marked out of reach and the neighbour moves to the nearest fitting open place; the bond and its progress stay.
     */
    fun shutPlace(id: String): Boolean {
        val d = district ?: return false; val s = state ?: return false
        val old = places[id] ?: return false
        s.flags += "shut:%.6f,%.6f".format(java.util.Locale.ROOT, d.lat(old.y), d.lon(old.x))
        s.places.remove(id)
        s.home?.let { bind(d, it) }
        store.save(s); version.value++
        return places[id] != null
    }

    /** Can the player talk to a neighbour now: near their place while it's open, near them while they walk, or home with the yard's people awake. */
    fun near(id: String, at: Geo? = here.value): Boolean {
        val p = content.person[id] ?: return false
        if (at == null) return false
        if (p.yard) return whereabouts(id) == Whereabouts.WORK && atHome(at)
        val st = whereabouts(id)
        val g = spotNow(id)?.let(::geoOf) ?: return false
        return g.metres(at) <= 40
    }

    /** Home within 40 m, a little more while the GPS wanders indoors (#46). */
    fun atHome(at: Geo? = here.value): Boolean { val h = state?.home ?: return false; return at != null && h.metres(at) <= 40 + hereAcc.value.coerceIn(0f, 40f) }

    /**
     * Home moved to where the player stands (#45, #46): a step within the district keeps the map and everyone in
     * it; further away, the district is downloaded again around the new home, as at the start.
     */
    suspend fun moveHome(at: Geo) {
        val s = state ?: return
        val box = s.box
        val near = s.home?.let { it.metres(at) <= 500 } == true && box != null && at.lat in box[0]..box[2] && at.lon in box[1]..box[3]
        if (near) { s.home = at; store.save(s); district?.let { bind(it, at) }; changed() } else setHome(at)
    }

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
        pendingSky?.let { (town, own) -> s.town = town; s.ownSky = own; pendingSky = null }
        s.places.clear()
        loadDistrict()
        changed()
    }

    /** The whole game in one file to carry to another phone: the save, the district map and the skies on record. */
    fun exportSave(): ByteArray? = state?.let { store.pack(it) }

    /**
     * Puts a carried game in place of this one. The map comes with it, so home, the beds and every neighbour's
     * place are where they were; a file without the map has it downloaded again for the same box, and the
     * neighbours still keep the places written in the save. Heavy: call off the main thread.
     */
    suspend fun importSave(bytes: ByteArray) {
        var p = withContext(Dispatchers.IO) { Store.unpack(bytes) }
        val s = p.state
        if (p.tiles == null && p.json == null) {
            // nothing on this phone is touched until the map is here: a failed download leaves the old game as it was
            val box = s.box ?: District.box(s.home!!.lat, s.home!!.lon, DISTRICT_HALF)
            stage.value = "download"
            val tiles = try { withContext(Dispatchers.IO) { Tiles.download(box) } } catch (e: Exception) { stage.value = ""; throw e }
            s.box = box; p = p.copy(tiles = tiles)
        }
        withContext(Dispatchers.IO) { store.install(p) }
        withContext(Dispatchers.Main) {
            state = s; district = null; terrain = null; morning = null; pastOn = null
            version.value++
        }
        withContext(Dispatchers.Default) { loadDistrict() }
        withContext(Dispatchers.Main) { changed(); wake() }
    }

    /**
     * Runs the nights missed since the last open. Nights still to run wait for the weather first (a few seconds
     * at most): the rain that fell while the app was closed has to be on record before the beds are judged.
     */
    fun wake() {
        val s = state ?: return
        val today = LocalDate.now()
        if (s.day != 0L && s.day < today.toEpochDay() && pastOn != today && skyAt() != null) {
            if (waking) return
            waking = true
            scope.launch {
                withTimeoutOrNull(12_000) { fetchWeather() }
                withContext(Dispatchers.Main) { waking = false; nights() }
            }
            return
        }
        nights()
    }

    @Volatile private var waking = false

    private fun nights() {
        val s = state ?: return
        val now = now()
        // only a real forecast goes on record: a made-up sky would stick to the day (see Store.remember)
        forecastFor(now.date)?.let { store.remember(now.day, it.today) }
        val m = game.catchUp(s, now) { day ->
            val date = LocalDate.ofEpochDay(day)
            (store.sky(day) ?: Weather.simulated(date, date.monthValue)) to (store.sky(day + 1) ?: Weather.simulated(date.plusDays(1), date.plusDays(1).monthValue))
        }
        if (m.grew + m.ripe + m.wild + m.mail.size + m.requests.size > 0 || m.festival != null || m.newYear) { morning = m; dawn.value++ }
        changed()
    }

    /** The forecast in memory, if it was fetched on [day]: one from yesterday (the app left open overnight) says nothing about today. */
    private fun forecastFor(day: LocalDate) =
        forecast?.takeIf { java.time.Instant.ofEpochMilli(it.at).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == day }

    /** The day the weather that already fell was last fetched (see [Weather.past]). */
    @Volatile private var pastOn: LocalDate? = null
    @Volatile private var pastAt = 0L

    /** Where the weather is asked for: the town picked, or home; none for the game's own weather. */
    fun skyAt(): Geo? {
        val s = state ?: return null
        if (s.ownSky) return null
        return s.town?.let { Geo(it.lat, it.lon) } ?: s.home
    }

    /**
     * The weather picked in the settings: [town] (home when null), or the game's [own]. The skies on record
     * from today on were seen elsewhere, so they go, and the new place's weather is fetched at once.
     */
    fun setSky(town: Town?, own: Boolean) {
        val s = state ?: run { pendingSky = town to own; return }
        s.town = town; s.ownSky = own
        store.forget(LocalDate.now().toEpochDay())
        forecast = null; pastOn = null; pastAt = 0
        changed()
        refreshWeather()
    }

    /** The weather picked during the welcome, before there is a game to keep it in. */
    var pendingSky: Pair<Town?, Boolean>? = null
        private set

    fun refreshWeather() {
        if (skyAt() == null || waking) return
        val fresh = System.currentTimeMillis() - 3 * 3600_000L
        if (forecastFor(LocalDate.now())?.let { it.at > fresh } == true && pastAt > fresh) return
        scope.launch { fetchWeather() }
    }

    /**
     * Fetches the forecast (MET Norway) and what has already fallen (Open-Meteo), and puts both on record.
     * Either may fail on its own; the other is still kept.
     */
    private suspend fun fetchWeather() = withContext(Dispatchers.IO) {
        val h = skyAt() ?: return@withContext
        val today = LocalDate.now()
        val ahead = async { runCatching { Weather.fetch(h.lat, h.lon) }.getOrNull() }
        val past = async { runCatching { Weather.past(h.lat, h.lon) }.getOrNull() }
        past.await()?.let { days ->
            days.forEach { (d, sky) -> store.remember(d.toEpochDay(), sky) }
            pastOn = today; pastAt = System.currentTimeMillis()
        }
        ahead.await()?.let { f ->
            forecast = f; store.remember(today.toEpochDay(), f.today); store.remember(today.toEpochDay() + 1, f.tomorrow, ahead = true)
        }
        version.value++
    }

    /** A new position from the GPS: distance walked, beds tended on the way. Returns how many beds were tended. */
    @Synchronized
    fun fix(at: Geo, accuracy: Float): Int {
        here.value = at
        hereAcc.value = accuracy
        stepStrolls()
        val s = state ?: return 0
        if (accuracy > WEAK_GPS) return 0
        var find: Find? = null
        // the way since the last fix, if it's a step and not a jump across town
        val from = lastFix?.takeIf { it.metres(at) <= 300.0 }
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
        val tended = game.pass(s, now(), at, from, accuracy.toDouble()).size
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
        .setSmallIcon(R.drawable.ic_notify)
        .setContentTitle(if (ru) "Находка по дороге" else "A find on the way")
        .setContentText(f.text(ru))
        .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(f.text(ru)))
        .setContentIntent(open).setAutoCancel(true)
        .build()
    runCatching { m.notify(100 + (System.currentTimeMillis() / 1000 % 50).toInt(), n) }
}

/** The time of day the app runs on: real, except in the screenshot build, where it's always half past noon today. */
object Clock {
    fun now(): java.time.LocalDateTime =
        if (BuildConfig.SHOTS) java.time.LocalDate.now().atTime(12, 30) else java.time.LocalDateTime.now()
}

class GreenApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        app.greenstreet.ui.art.Pictures.open = { path -> runCatching { assets.open(path) }.getOrNull() }
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
