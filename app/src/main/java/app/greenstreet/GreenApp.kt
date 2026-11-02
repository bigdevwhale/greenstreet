package app.greenstreet

import android.app.Application
import android.content.Context
import app.greenstreet.data.Content
import app.greenstreet.data.District
import app.greenstreet.data.Spot
import app.greenstreet.data.Store
import app.greenstreet.data.Tiles
import app.greenstreet.domain.Extras
import app.greenstreet.domain.Find
import app.greenstreet.domain.Forecast
import app.greenstreet.domain.Game
import app.greenstreet.domain.GameState
import app.greenstreet.domain.Geo
import app.greenstreet.domain.Morning
import app.greenstreet.domain.Now
import app.greenstreet.domain.Quest
import app.greenstreet.domain.Quests
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
            store.save(s)
        }
        version.value++
    }

    fun now(at: LocalDateTime = LocalDateTime.now()): Now {
        val f = forecast
        val today = at.toLocalDate()
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

    /** Each town neighbour takes the nearest place of their kind, or else the nearest unclaimed shop. */
    private fun bind(d: District, home: Geo) {
        places.clear()
        val hx = d.x(home.lon); val hy = d.y(home.lat)
        fun dist(p: Spot) = Math.hypot((p.x - hx).toDouble(), (p.y - hy).toDouble())
        val taken = HashSet<Spot>()
        for (p in content.people.filter { !it.yard }) {
            val match = d.places.filter { s -> p.osm.any { (k, v) -> s.kind == "$k=$v" } && s !in taken }.minByOrNull(::dist)
            if (match != null) { places[p.id] = match; taken += match }
        }
        for (p in content.people.filter { !it.yard && it.id !in places }) {
            val any = d.places.filter { it !in taken && (it.kind.startsWith("shop=") || it.kind.startsWith("amenity=")) }.minByOrNull(::dist)
            if (any != null) { places[p.id] = any; taken += any }
        }
    }

    fun placeGeo(id: String): Geo? {
        val d = district ?: return null
        val s = places[id] ?: return state?.home
        return Geo(d.lat(s.y), d.lon(s.x))
    }

    /** Is the player near enough to a neighbour's place to visit (yard neighbours: at home)? */
    fun near(id: String, at: Geo? = here.value): Boolean {
        val g = placeGeo(id) ?: return false
        return at != null && g.metres(at) <= 40
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
        store.remember(now.day, now.sky)
        val m = game.catchUp(s, now) { day ->
            val date = LocalDate.ofEpochDay(day)
            (store.sky(day) ?: Weather.simulated(date, date.monthValue)) to (store.sky(day + 1) ?: Weather.simulated(date.plusDays(1), date.plusDays(1).monthValue))
        }
        if (m.grew + m.ripe + m.wild + m.frost + m.mail.size + m.requests.size > 0 || m.festival != null || m.newYear) morning = m
        changed()
    }

    fun refreshWeather() {
        val h = state?.home ?: return
        if (forecast != null && System.currentTimeMillis() - forecast!!.at < 3 * 3600_000L) return
        scope.launch(Dispatchers.IO) {
            runCatching { Weather.fetch(h.lat, h.lon) }.getOrNull()?.let { f ->
                forecast = f; store.remember(LocalDate.now().toEpochDay(), f.today); version.value++
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
