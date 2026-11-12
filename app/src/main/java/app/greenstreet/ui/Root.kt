package app.greenstreet.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.greenstreet.AppContainer
import app.greenstreet.container
import app.greenstreet.data.Place
import app.greenstreet.data.Reward
import app.greenstreet.data.Tx
import app.greenstreet.domain.Geo
import app.greenstreet.domain.metres
import app.greenstreet.ui.art.FacadeScene
import app.greenstreet.ui.art.FishScene
import app.greenstreet.ui.art.G
import app.greenstreet.ui.art.Icons
import app.greenstreet.ui.art.Look
import app.greenstreet.ui.art.Moment
import app.greenstreet.ui.art.PixelView
import app.greenstreet.ui.art.R
import app.greenstreet.ui.art.YardScene
import app.greenstreet.ui.components.Kind
import app.greenstreet.ui.components.Panel
import app.greenstreet.ui.components.PixelButton
import app.greenstreet.ui.components.pixelBox
import app.greenstreet.ui.theme.C
import app.greenstreet.ui.theme.T
import app.greenstreet.work.Tracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** What opens over the tabs. */
sealed interface Sheet {
    data class BedInfo(val id: Long) : Sheet
    data class Harvest(val id: Long) : Sheet
    data class Place(val id: String) : Sheet
    data class Visit(val id: String) : Sheet
    data class Fish(val big: Boolean) : Sheet
    data object Market : Sheet
    data object Board : Sheet
    data class Letter(val key: String) : Sheet
    data class Tab(val n: Int) : Sheet
    data object Morning : Sheet
    data object Settings : Sheet
    data object Shop : Sheet
}

@Composable
fun ru(): Boolean = LocalConfiguration.current.locales[0].language == "ru"
fun w(ru: Boolean, a: String, b: String) = if (ru) a else b

/** Money in words: «1 грош», «3 гроша», «5 грошей»; "1 groat", "5 groats". */
fun money(n: Int, ru: Boolean): String {
    if (!ru) return "$n " + if (n == 1) "groat" else "groats"
    val m10 = n % 10; val m100 = n % 100
    return "$n " + when { m10 == 1 && m100 != 11 -> "грош"; m10 in 2..4 && m100 !in 12..14 -> "гроша"; else -> "грошей" }
}

/** A copper groat for the top line. */
val coinIcon: app.greenstreet.ui.art.Buf by lazy {
    val b = app.greenstreet.ui.art.Buf(9, 9)
    for (y in 0 until 9) for (x in 0 until 9) {
        val r = Math.hypot(x - 4.0, y - 4.0)
        if (r <= 4.3) b.p(x, y, when { r > 3.4 -> 0xFF8A4A1C.toInt(); x + y < 6 -> 0xFFFCE09A.toInt(); else -> 0xFFF0B04A.toInt() })
    }
    b.p(4, 3, 0xFF8A4A1C.toInt()); b.p(4, 4, 0xFF8A4A1C.toInt()); b.p(4, 5, 0xFF8A4A1C.toInt()); b.p(3, 4, 0xFF8A4A1C.toInt()); b.p(5, 4, 0xFF8A4A1C.toInt())
    b
}

/** Today's courtyard: the house from the map, the year, the stories, the pots on the sill. */
fun yardLife(c: AppContainer, guest: String? = null): app.greenstreet.ui.art.YardLife {
    val s = c.state ?: return app.greenstreet.ui.art.YardLife()
    val now = c.now()
    val t = c.terrain; val d = c.district; val home = s.home
    val block = if (t != null && d != null && home != null) t.blocks.minByOrNull { Math.hypot(it.cx - d.x(home.lon).toDouble(), it.cy - d.y(home.lat).toDouble()) } else null
    val fac = block?.let { G.FAC[(app.greenstreet.ui.art.hash(it.index, 3) * G.FAC.size).toInt()] } ?: G.ochre
    val pots = s.beds.filter { c.content.variety[it.variety]?.place == app.greenstreet.data.Place.SILL }.map {
        val v = c.content.variety.getValue(it.variety); app.greenstreet.ui.art.Pot(v.art, app.greenstreet.ui.art.rampOf(v.ramp), c.game.ripe(it))
    }
    return app.greenstreet.ui.art.YardLife(fac, block?.index ?: 0, c.game.year(s, now.day), s.eggs, c.game.flag(s, now, "hive"), c.game.flag(s, now, "balcony"),
        c.game.flag(s, now, "garden"), now.date.dayOfWeek.value, c.game.festival(s, now)?.key, s.flags.toSet(), pots,
        if (t != null && d != null && home != null) t.homeKind(d.x(home.lon), d.y(home.lat)) else "well",
        guest?.let { c.content.person[it]?.look },
        s.flags.filter { it.startsWith("own.") }.map { it.removePrefix("own.") }.toSet())
}

fun moment(c: AppContainer): Moment {
    val n = c.now(); val t = LocalDateTime.now()
    return Moment(t.hour * 60 + t.minute, n.sky, n.month, n.day)
}

@Composable
fun Root() {
    val ctx = LocalContext.current
    val c = ctx.container
    val ver by c.version.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    LaunchedEffect(Unit) { c.wake(); c.refreshWeather(); if (c.morning != null) sheet = Sheet.Morning }
    DisposableEffect(Unit) { val tr = Tracker.of(ctx); tr.start(); onDispose { tr.stop() } }
    ver.let { }
    if (c.state?.home == null || c.terrain == null && !c.store.hasDistrict()) { Onboarding(); return }
    Column(Modifier.fillMaxSize().background(C.paper)) {
        TopLine(c) { sheet = Sheet.Settings }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> MapScreen { sh -> if (sh is Sheet.Tab) tab = sh.n else sheet = sh }
                1 -> YardScreen { sh -> if (sh is Sheet.Tab) tab = sh.n else sheet = sh }
                2 -> PeopleScreen { sh -> if (sh is Sheet.Tab) tab = sh.n else sheet = sh }
                else -> BagScreen()
            }
        }
        Tabs(tab, badges(c)) { tab = it }
    }
    sheet?.let { sh -> SheetView(sh, { sheet = it }) { sheet = null } }
    QuestBanner(c)
    FindBanner(c)
}

@Composable
private fun FindBanner(c: AppContainer) {
    val f by c.found.collectAsState()
    val ru = ru()
    val find = f ?: return
    LaunchedEffect(find) { delay(5500); if (c.found.value === find) c.found.value = null }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(12.dp), contentAlignment = Alignment.TopCenter) {
        Panel(Modifier.fillMaxWidth().clickable { c.found.value = null }, fill = C.light) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (find.kind) {
                    "good" -> app.greenstreet.ui.art.ItemIcons.of(c.content, find.key)
                    "seeds" -> app.greenstreet.ui.art.ItemIcons.packet(c.content, find.key)
                    "coins" -> coinIcon
                    else -> null
                }
                if (icon != null) app.greenstreet.ui.components.BufImage(icon, Modifier.padding(end = 10.dp), if (icon === coinIcon) 3.dp else 2.dp)
                Column(Modifier.weight(1f)) {
                    Text(if (find.kind == "moment") w(ru, "по дороге", "on the way") else w(ru, "находка", "a find"), style = T.Eyebrow)
                    Text(find.text(ru), style = T.Body)
                }
            }
        }
    }
}

@Composable
private fun TopLine(c: AppContainer, settings: () -> Unit) {
    val ru = ru()
    c.version.collectAsState().value  // repaint after every change: the container itself never changes
    val s = c.state ?: return
    val n = c.now()
    val d = n.date
    val months = if (ru) listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
    else listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val sky = n.sky
    val wx = when { sky.snow -> w(ru, "снег", "snow"); sky.rain -> w(ru, "дождь", "rain"); sky.fog -> w(ru, "туман", "fog"); sky.sun -> w(ru, "ясно", "clear"); else -> w(ru, "облачно", "cloudy") }
    Row(Modifier.fillMaxWidth().background(C.greenDeep).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(if (ru) "${d.dayOfMonth} ${months[d.monthValue - 1]}" else "${months[d.monthValue - 1]} ${d.dayOfMonth}", style = T.Heading.copy(color = C.light))
            Text("$wx, %+.0f°".format(sky.temp) + " · " + w(ru, "год ", "year ") + c.game.year(s, n.day), style = T.Small.copy(color = C.paper2))
        }
        app.greenstreet.ui.components.BufImage(coinIcon, Modifier.padding(end = 6.dp, bottom = 4.dp), 2.4.dp)
        Text("${s.coins}", style = T.Heading.copy(color = C.lamp))
        Box(Modifier.padding(start = 10.dp).clickable(onClick = settings).padding(4.dp)) {
            app.greenstreet.ui.components.BufImage(gearIcon, Modifier, 2.4.dp)
        }
    }
}

@Composable
private fun Tabs(tab: Int, badges: List<Boolean>, pick: (Int) -> Unit) {
    val ru = ru()
    val names = listOf(w(ru, "Улица", "Street"), w(ru, "Двор", "Yard"), w(ru, "Соседи", "Neighbours"), w(ru, "Сумка", "Bag"))
    Row(Modifier.fillMaxWidth().background(C.bg).navigationBarsPadding().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        names.forEachIndexed { i, n ->
            Box(Modifier.weight(1f).background(if (i == tab) C.ochreDeep else C.bg).clickable { pick(i) }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    app.greenstreet.ui.components.BufImage(tabIcon(i), Modifier.width(28.dp), 2.dp)
                    Text(n, style = T.Small.copy(color = if (i == tab) C.lamp else C.paper2))
                }
                if (badges.getOrElse(i) { false }) Box(Modifier.align(Alignment.TopEnd).padding(end = 14.dp, top = 2.dp).width(10.dp).height(10.dp).background(C.red))
            }
        }
    }
}

/** Something waits on a tab: ripe beds on the street, eggs or ripe pots at home, unread notes, new requests. */
private fun badges(c: AppContainer): List<Boolean> {
    val s = c.state ?: return listOf(false, false, false, false)
    val verge = s.beds.filter { c.content.variety[it.variety]?.place == Place.VERGE }
    val home = s.beds.filter { c.content.variety[it.variety]?.place != Place.VERGE }
    return listOf(
        verge.any { c.game.ripe(it) },
        s.eggs > 0 || home.any { c.game.ripe(it) },
        s.inbox.any { !it.read } || s.bonds.values.any { it.reqShown == c.now().day },
        false,
    )
}

/** Little pictures for the tabs: a street sign, a house, two faces, a sack. */
private fun tabIcon(i: Int): app.greenstreet.ui.art.Buf {
    val b = app.greenstreet.ui.art.Buf(14, 14)
    fun f(x: Int, y: Int, w: Int, h: Int, c: Int) = b.r(x, y, w, h, c)
    when (i) {
        0 -> { f(1, 2, 12, 10, R.paper[3]); f(2, 3, 10, 8, R.grassSummer[3]); f(2, 6, 10, 1, R.stone[5]); f(6, 3, 1, 8, R.stone[5]); f(9, 8, 2, 2, G.water[3]); f(3, 4, 1, 1, R.red[4]) }
        1 -> { f(2, 6, 10, 7, G.ochre[3]); for (k in 0 until 6) f(1 + k, 6 - k, 12 - 2 * k, 1, R.roofR[3]); f(6, 9, 2, 4, R.wood[1]); f(3, 8, 2, 2, R.honey[4]); f(9, 8, 2, 2, R.honey[4]) }
        2 -> { f(2, 2, 4, 4, R.skin[4]); f(2, 1, 4, 1, R.wood[2]); f(1, 7, 6, 6, R.red[3]); f(8, 2, 4, 4, R.skin[4]); f(8, 1, 4, 1, R.enamel[3]); f(7, 7, 6, 6, R.roofB[3]) }
        else -> { f(4, 2, 6, 2, R.wood[2]); f(2, 4, 10, 8, R.wood[4]); f(2, 4, 10, 1, R.wood[6]); f(6, 6, 2, 2, R.gold[4]) }
    }
    return b
}

/** A banner over everything for a moment when an opening task is done, with what it gave. */
@Composable
private fun QuestBanner(c: AppContainer) {
    val q by c.questDone.collectAsState()
    val ru = ru()
    val done = q ?: return
    LaunchedEffect(done) { delay(4500); c.questDone.value = null }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(12.dp), contentAlignment = Alignment.TopCenter) {
        Panel(Modifier.fillMaxWidth().clickable { c.questDone.value = null }, fill = C.lamp) {
            Text(w(ru, "Задание выполнено", "Task done"), style = T.Eyebrow.copy(color = C.ink))
            Text(done.title(ru), style = T.Heading)
            Text(w(ru, "Получено: ", "You get: ") + rewards(c, done.reward, ru), style = T.Small.copy(color = C.ink))
        }
    }
}

/** Neighbour look → picture parameters. */
fun lookOf(c: AppContainer, id: String) = Look.of(c.content.person[id]?.look ?: emptyMap())

private fun facade(c: AppContainer, id: String): FacadeScene {
    val p = c.content.person.getValue(id)
    val k = p.kind
    val fac = listOf(G.ochre, G.salmon, G.mint, G.sky2, R.cream, R.lilac)[(id.hashCode() and 0x7fffffff) % 6]
    val board = listOf(G.plum, G.bottle, G.navy, R.wood)[(id.hashCode() and 0x7fffffff) % 4]
    val awn = listOf(R.red, R.enamel, R.honey, R.roofB)[(id.hashCode() / 7 and 0x7fffffff) % 4]
    val shop = when (k) { "cafe", "bakery", "pharmacy", "school", "market", "florist", "library" -> k; "station" -> "station"; else -> k }
    val n = c.now()
    return FacadeScene(shop, Icons.forKind(k), fac, board, awn, lookOf(c, id), moment(c), (n.day + id.length) % 3 != 0L)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SheetView(sheet: Sheet, go: (Sheet) -> Unit, close: () -> Unit) {
    val c = LocalContext.current.container
    val ru = ru()
    val s = c.state ?: return
    val now = c.now()
    val ver by c.version.collectAsState()
    ver.let { }
    if (sheet is Sheet.Tab) { close(); return }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().clickable(onClick = close), contentAlignment = Alignment.BottomCenter) {
            Panel(Modifier.fillMaxWidth().padding(10.dp).navigationBarsPadding().clickable(enabled = false) {}) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    when (sheet) {
                        is Sheet.Settings -> SettingsView()
                        is Sheet.Shop -> ShopView(c)
                        is Sheet.Morning -> {
                            val m = c.morning
                            PixelView(YardScene(moment(c), yardLife(c)))
                            Text(w(ru, "Утро", "Morning"), style = T.Title, modifier = Modifier.padding(top = 8.dp))
                            if (m != null) {
                                val lines = ArrayList<String>()
                                if (m.newYear) lines += w(ru, "Начался %d-й год на Зелёной улице.", "Year %d on Green Street has begun.").format(m.year)
                                m.festival?.let { lines += it.name(ru) + " — " + it.notice(ru) }
                                if (m.rained) lines += w(ru, "Шёл дождь — грядки полил он.", "It rained — the beds are watered.")
                                if (m.grew > 0) lines += w(ru, "Подросло грядок: %d.", "Beds that grew: %d.").format(m.grew)
                                if (m.ripe > 0) lines += w(ru, "Созрело: %d. Ищите искорки над грядками.", "Ripe: %d. Look for the sparkle over the beds.").format(m.ripe)
                                if (m.wild > 0) lines += w(ru, "Без присмотра одичало: %d. Урожай будет меньше, но будет.", "Gone wild without care: %d. They'll still give a smaller harvest.").format(m.wild)
                                if (m.eggs > 0) lines += w(ru, "Куры снесли яиц: %d.", "The hens laid %d eggs.").format(m.eggs)
                                if (m.mail.isNotEmpty()) lines += w(ru, "В почтовом ящике записок: %d.", "Notes in the letterbox: %d.").format(m.mail.size)
                                if (m.requests.isNotEmpty()) lines += w(ru, "Новые просьбы: ", "New requests: ") + m.requests.joinToString { c.content.person[it]?.name?.invoke(ru) ?: it }
                                c.content.almanac["%02d-%02d".format(now.date.monthValue, now.date.dayOfMonth)]?.let { lines += "«" + it(ru) + "»" }
                                lines.forEach { Text("· $it", style = T.Body, modifier = Modifier.padding(top = 4.dp)) }
                            }
                            PixelButton(w(ru, "Выйти со двора", "Out of the courtyard"), { c.morning = null; close() }, Modifier.fillMaxWidth().padding(top = 10.dp), Kind.LAMP)
                        }
                        is Sheet.BedInfo, is Sheet.Harvest -> {
                            val id = if (sheet is Sheet.BedInfo) sheet.id else (sheet as Sheet.Harvest).id
                            val b = s.beds.firstOrNull { it.id == id }
                            if (b == null) { Text(w(ru, "Грядки больше нет.", "The bed is gone."), style = T.Body) } else {
                                val v = c.content.variety.getValue(b.variety)
                                Text(v.name(ru), style = T.Title)
                                Text(v.about(ru), style = T.Small, modifier = Modifier.padding(top = 4.dp))
                                app.greenstreet.ui.BedDetails(c, b, now, ru)
                                val here = c.here.collectAsState().value
                                val close2 = here != null && Geo(b.lat, b.lon).metres(here) <= c.game.reach(s)
                                FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (c.game.ripe(b)) PixelButton(w(ru, "Собрать", "Pick"), {
                                        val h = c.game.harvest(s, now, b.id); c.changed()
                                        if (h != null) go(Sheet.Letter("harvest:${h.variety}:${h.n}:${h.seedBack}:${h.first}"))
                                    }, kind = Kind.LAMP, enabled = close2)
                                    if (!c.game.ripe(b) && c.game.stock(s, "feed") > 0)
                                        PixelButton(w(ru, "Подкормить (%d)", "Feed (%d)").format(c.game.stock(s, "feed")), { c.game.feed(s, b.id); c.changed() }, kind = Kind.PRIMARY, enabled = close2)
                                    if (b.wild && c.game.owns(s, "hoe"))
                                        PixelButton(w(ru, "Прополоть", "Weed"), { c.game.weed(s, b.id); c.changed() }, kind = Kind.PRIMARY, enabled = close2)
                                    PixelButton(w(ru, "Перекопать", "Dig up"), { c.game.dig(s, b.id); c.changed(); close() }, kind = Kind.GHOST, enabled = close2)
                                }
                                if (here != null && !close2)
                                    Text(w(ru, "Чтобы собрать или перекопать, подойдите к грядке.", "Walk up to the bed to pick or dig it."), style = T.Small, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                        is Sheet.Place -> Profile(c, sheet.id, go, close)
                        is Sheet.Visit -> Visit(c, sheet.id, go)
                        is Sheet.Fish -> Fishing(c, sheet.big)
                        is Sheet.Market -> Market(c)
                        is Sheet.Board -> Board(c)
                        is Sheet.Letter -> LetterView(c, sheet.key)
                        else -> {}
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Visit(c: AppContainer, id: String, go: (Sheet) -> Unit) {
    val ru = ru()
    c.version.collectAsState().value  // repaint after every change: the container itself never changes
    val s = c.state ?: return
    val now = c.now()
    val p = c.content.person.getValue(id)
    var said by remember { mutableStateOf<String?>(null) }
    var first by remember(id) { mutableStateOf(false) }
    val seenAtOpen = remember(id) { s.bonds[id]?.seen ?: 0 }
    LaunchedEffect(id) { if (c.game.visit(s, now, id)) first = true; c.changed() }
    val strolling = remember(id) { c.whereabouts(id) == app.greenstreet.domain.Whereabouts.OUT }
    if (p.yard) PixelView(remember(id, now.day) { YardScene(moment(c), yardLife(c, id)) })
    else if (strolling) PixelView(remember(id, now.day) { app.greenstreet.ui.art.StrollScene(lookOf(c, id), moment(c), id.hashCode()) })
    else PixelView(remember(id, now.day) { facade(c, id) })
    PersonHeader(c, p, ru)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Hearts(c.game.hearts(s, id), Modifier.weight(1f))
        PixelButton(w(ru, "Блокнот %d/%d", "Notebook %d/%d").format(p.facts.count { it.hearts <= c.game.hearts(s, id) }, p.facts.size), { go(Sheet.Place(id)) }, kind = Kind.GHOST)
    }
    if (first && said == null) Meeting(c, p, ru)
    else Says(said ?: c.game.line(s, now, id)?.text?.invoke(ru) ?: p.intro(ru).ifBlank { p.role(ru) })
    // what the player has just learned about them
    val unlocked = p.facts.count { it.hearts <= c.game.hearts(s, id) }
    val fresh = p.facts.filter { it.hearts <= c.game.hearts(s, id) }.drop(seenAtOpen)
    if (fresh.isNotEmpty()) {
        Text(w(ru, "новое в блокноте", "new in your notebook"), style = T.Eyebrow.copy(color = C.green))
        fresh.forEach { f -> Fact(f.text(ru)) }
        Spacer(Modifier.height(8.dp))
        SideEffect { s.bonds[id]?.let { b -> if (b.seen < unlocked) b.seen = unlocked } }
    }
    c.game.lostOf(s, id)?.let { k ->
        PixelButton(w(ru, "Отдать находку: ", "Give back: ") + app.greenstreet.domain.Lost.things[k](ru), {
            c.game.returnLost(s, id); c.changed()
            said = w(ru, "Ой, нашлась пропажа! Спасибо вам огромное — вот, возьмите за хлопоты.", "Oh, there it is! Thank you so much — here, for your trouble.")
        }, Modifier.fillMaxWidth().padding(bottom = 6.dp), Kind.PRIMARY)
    }
    var got by remember(id) { mutableStateOf<Pair<String, Int>?>(null) }
    c.game.seedsToday(s, now, id)?.let { (k, n) ->
        PixelButton(w(ru, "Взять семена: ", "Take seeds: ") + c.content.name(k)(ru) + " ×$n", { got = c.game.takeSeeds(s, now, id); c.changed() }, Modifier.fillMaxWidth(), Kind.LAMP)
    }
    got?.let { (k, n) ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().pixelBox(C.light).padding(8.dp)) {
            app.greenstreet.ui.components.BufImage(app.greenstreet.ui.art.ItemIcons.packet(c.content, k), Modifier.width(32.dp), 2.dp)
            Text("  " + w(ru, "В сумке: ", "In your bag: ") + c.content.name(k)(ru) + " ×$n. " + w(ru, "Посеять можно на газоне у дороги.", "Sow them on a verge by the road."), style = T.Small.copy(color = C.green))
        }
    }
    c.game.request(s, id)?.let { r ->
        Panel(Modifier.fillMaxWidth().padding(top = 10.dp), fill = C.paper) {
            Text(w(ru, "просьба", "a request"), style = T.Eyebrow)
            Text(r.ask(ru), style = T.Body, modifier = Modifier.padding(vertical = 4.dp))
            r.needs.forEach { n ->
                val have = s.bag.entries.filter { c.content.matches(n.key, it.key) }.sumOf { it.value }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    app.greenstreet.ui.components.BufImage(app.greenstreet.ui.art.ItemIcons.of(c.content, n.key), Modifier.width(28.dp), 1.75.dp)
                    Text("  ${c.content.name(n.key)(ru)}: $have / ${n.qty}", style = T.Body.copy(color = if (have >= n.qty) C.green else C.red))
                }
            }
            Text(w(ru, "Взамен: ", "In return: ") + rewards(c, r.rewards, ru), style = T.Small, modifier = Modifier.padding(top = 4.dp))
            PixelButton(w(ru, "Отдать", "Hand over"), {
                val done = c.game.fulfil(s, now, id); c.changed(); if (done != null) said = done.thanks(ru)
            }, Modifier.padding(top = 8.dp), enabled = c.game.canFulfil(s, r.needs))
        }
    }
    if (s.bonds[id]?.gifted != now.day) {
        val gifts = s.bag.keys.toList()
        if (gifts.isNotEmpty()) {
            Text(w(ru, "Подарить (раз в день) — нажмите на вещь:", "Give a present (once a day) — tap a thing:"), style = T.Body, modifier = Modifier.padding(top = 10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                gifts.forEach { k ->
                    val mark = when (s.bonds[id]?.known?.get(k)) { "love" -> "♥♥"; "like" -> "♥"; "dislike" -> "✗"; else -> null }
                    ItemSlot(app.greenstreet.ui.art.ItemIcons.of(c.content, k), s.bag[k], false, mark = mark) {
                        val r = c.game.gift(s, now, id, k); c.changed()
                        said = when (r) {
                            "love" -> w(ru, "Ох… Это же моё самое любимое. Спасибо вам.", "Oh… that's my very favourite. Thank you.")
                            "like" -> w(ru, "Вот спасибо! Угадали.", "Thank you! You guessed right.")
                            "dislike" -> w(ru, "Ну… спасибо. Поставлю куда-нибудь.", "Well… thank you. I'll find a place for it.")
                            else -> w(ru, "Спасибо, пригодится.", "Thank you, that'll come in handy.")
                        }
                    }
                }
            }
        }
    } else Text(w(ru, "Подарок сегодня уже был.", "You've already given a present today."), style = T.Small, modifier = Modifier.padding(top = 10.dp))
}

/** Mikhalych's hardware shop: things for the home (they appear in the yard) and for the garden. */
@Composable
private fun ShopView(c: AppContainer) {
    val ru = ru()
    c.version.collectAsState().value  // repaint after every change: the container itself never changes
    val s = c.state ?: return
    val now = c.now()
    var msg by remember { mutableStateOf<String?>(null) }
    Text(w(ru, "Хозтовары", "The hardware shop"), style = T.Title)
    Text(w(ru, "«Всё для дома и огорода. Не нашли — спросите, закажем».", "'Everything for the home and garden. Can't find it? Ask, we'll order it.'"), style = T.Letter, modifier = Modifier.padding(vertical = 6.dp))
    msg?.let { Text(it, style = T.Body.copy(color = C.green), modifier = Modifier.padding(bottom = 6.dp)) }
    for (home in listOf(true, false)) {
        Text(if (home) w(ru, "для дома — появится во дворе", "for the home — it shows in the yard") else w(ru, "для огорода", "for the garden"), style = T.Eyebrow, modifier = Modifier.padding(top = 8.dp))
        for (ware in app.greenstreet.domain.Wares.all.filter { it.home == home }) {
            val owned = ware.once && c.game.owns(s, ware.key)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(ware.name(ru) + if (!ware.once && c.game.stock(s, ware.key) > 0) "  ×${c.game.stock(s, ware.key)}" else "", style = T.Body)
                    Text(ware.about(ru), style = T.Small)
                }
                if (owned) Text(w(ru, "есть", "yours"), style = T.Small.copy(color = C.green), modifier = Modifier.padding(start = 8.dp))
                else PixelButton(money(ware.price, ru), {
                    val r = c.game.buyWare(s, now, ware.key)
                    if (r != null) {
                        c.changed()
                        msg = when {
                            r.isNotEmpty() -> w(ru, "В пакетике: ", "In the packet: ") + c.content.name(r)(ru) + " ×2"
                            ware.home -> w(ru, "Готово — загляните во двор.", "Done — have a look in the yard.")
                            else -> w(ru, "Куплено.", "Bought.")
                        }
                    }
                }, kind = if (s.coins >= ware.price) Kind.LAMP else Kind.GHOST, enabled = s.coins >= ware.price)
            }
        }
    }
}

/** The first meeting: their own words, what they are good for, their hours, and what presents do. */
@Composable
private fun Meeting(c: AppContainer, p: app.greenstreet.data.Person, ru: Boolean) {
    Says(p.intro(ru).ifBlank { p.about(ru) })
    Panel(Modifier.fillMaxWidth().padding(bottom = 8.dp), fill = C.paper) {
        Text(w(ru, "знакомство", "a new acquaintance"), style = T.Eyebrow)
        if (p.perk(ru).isNotBlank()) Text(p.perk(ru), style = T.Body, modifier = Modifier.padding(top = 4.dp))
        Text(w(ru, "Часы: ", "Hours: ") + app.greenstreet.domain.Schedule.hours(p)(ru), style = T.Small, modifier = Modifier.padding(top = 4.dp))
        Text(w(ru,
            "Сердечки растут, когда вы заходите, дарите подарки и помогаете. Чем их больше, тем больше сосед вам доверяет: рассказывает о себе (всё записывается в блокнот), просит о новом, пишет письма, а с четырёх сердечек даёт больше семян. Любимые вещи радуют сильнее всего, а нелюбимые могут и огорчить.",
            "Hearts grow when you drop in, give presents and help. The more there are, the more a neighbour trusts you: they tell you about themselves (it all goes into your notebook), ask for new things, write letters, and from four hearts give more seeds. Favourite things please them most; disliked ones can upset them."),
            style = T.Small, modifier = Modifier.padding(top = 6.dp))
    }
}

fun rewards(c: AppContainer, rs: List<Reward>, ru: Boolean): String = rs.joinToString {
    when (it) {
        is Reward.Coins -> money(it.n, ru)
        is Reward.Seeds -> c.content.name(it.variety)(ru) + " ×${it.n}"
        is Reward.Item -> c.content.name(it.key)(ru) + " ×${it.n}"
        is Reward.Recipe -> w(ru, "рецепт «", "recipe '") + c.content.name(it.key)(ru) + w(ru, "»", "'")
        is Reward.Tool -> when (it.key) { "can" -> w(ru, "лейка получше", "a better watering can"); "basket" -> w(ru, "корзина побольше", "a bigger basket"); "rod" -> w(ru, "удочка получше", "a better rod"); else -> w(ru, "место ещё под одну грядку", "room for one more bed") }
    }
}

@Composable
private fun Fishing(c: AppContainer, big: Boolean) {
    val ru = ru()
    c.version.collectAsState().value  // repaint after every change: the container itself never changes
    val s = c.state ?: return
    val scene = remember { FishScene(moment(c), big) }
    var state by remember { mutableStateOf("idle") }
    var msg by remember { mutableStateOf(w(ru, "Закидывайте.", "Cast away.")) }
    var biteAt by remember { mutableStateOf(0L) }
    LaunchedEffect(state) {
        scene.state = state
        if (state == "wait") { delay(biteAt - System.currentTimeMillis()); if (state == "wait") state = "bite" }
        if (state == "bite") { delay(longArrayOf(0, 650, 900, 1200)[s.tool("rod")]); if (state == "bite") { state = "done"; msg = w(ru, "Сорвалась. Бывает. Закиньте снова.", "It got away. It happens. Cast again.") } }
    }
    Text(if (big) w(ru, "Рыбалка с набережной", "Fishing from the embankment") else w(ru, "Рыбалка у пруда", "Fishing by the pond"), style = T.Title)
    PixelView(scene, Modifier.padding(vertical = 8.dp))
    Text(msg, style = T.Body)
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PixelButton(if (state == "wait" || state == "bite") w(ru, "Подсечь", "Strike") else if (state == "done") w(ru, "Закинуть снова", "Cast again") else w(ru, "Закинуть", "Cast"), {
            when (state) {
                "idle", "done" -> { biteAt = System.currentTimeMillis() + 1200 + (Math.random() * 3000).toLong(); state = "wait"; msg = w(ru, "Ждёте…", "Waiting…") }
                "wait" -> { state = "done"; msg = w(ru, "Рано. Рыба ушла. Закиньте снова.", "Too soon. The fish is gone. Cast again.") }
                "bite" -> {
                    state = "done"
                    val k = catch(c, big)
                    s.add(s.bag, k, 1); val first = s.album.add(k); s.count("fish"); s.count("fish:$k"); c.changed()
                    msg = w(ru, "Есть! ", "Got it! ") + c.content.name(k)(ru) + if (first) w(ru, ". Новая рыба в альбоме.", ". A new fish for the album.") else "."
                }
            }
        }, kind = Kind.PRIMARY)
    }
}

private fun catch(c: AppContainer, big: Boolean): String {
    val n = c.now()
    val ok = c.content.goods.filter { it.kind == app.greenstreet.data.GoodKind.FISH && (it.months.isEmpty() || n.month in it.months) &&
        (it.where in setOf("", "any") || (big && it.where in setOf("river", "canal", "lake")) || (!big && it.where in setOf("pond", "lake", "canal"))) }
    if (ok.isEmpty()) return "roach"
    val weights = ok.map { g -> (4 - g.rare).coerceAtLeast(1) * (if (g.weather.isNotBlank() && g.weather in n.sky.tags) 2 else 1) * (if (g.time.isNotBlank() && g.time == n.part) 2 else 1) }
    var r = Math.random() * weights.sum()
    ok.forEachIndexed { i, g -> r -= weights[i]; if (r < 0) return g.key }
    return ok.last().key
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Market(c: AppContainer) {
    val ru = ru()
    c.version.collectAsState().value  // repaint after every change: the container itself never changes
    val s = c.state ?: return
    val now = c.now()
    if (now.saturday) {
        Text(w(ru, "Ярмарка", "The fair"), style = T.Title)
        Text(w(ru, "Сегодня выручено: ", "Taken today: ") + money(if (s.fairDay == now.day) s.fair else 0, ru), style = T.Small)
        c.game.demand(s, now)?.let { d -> Text(w(ru, "★ Спрос недели: ", "★ This week's demand: ") + c.content.name(d)(ru).lowercase() + w(ru, " — вдвое дороже", " — twice the price"), style = T.Body.copy(color = C.green)) }
        s.bag.keys.toList().forEach { k ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text((if (c.game.demand(s, now) == c.content.speciesOf(k)) "★ " else "") + c.content.name(k)(ru) + " ×${s.bag[k]}", style = T.Body, modifier = Modifier.weight(1f))
                PixelButton(money(c.game.salePrice(s, now, k), ru), { c.game.sell(s, now, k, 1); c.changed() }, kind = Kind.GHOST)
            }
        }
        if (s.bag.isEmpty()) Text(w(ru, "Выставить нечего.", "Nothing to sell."), style = T.Body)
    } else {
        Text(w(ru, "Семенной ряд", "The seed stall"), style = T.Title)
        Text(w(ru, "По субботам здесь ярмарка: урожай и заготовки продаются за гроши.", "On Saturdays there's a fair here: harvest and preserves sell for groats."), style = T.Small)
        c.game.demand(s, now)?.let { d -> Text(w(ru, "★ На этой неделе спрос: ", "★ In demand this week: ") + c.content.name(d)(ru).lowercase() + w(ru, " — в субботу вдвое дороже.", " — twice the price on Saturday."), style = T.Body.copy(color = C.green), modifier = Modifier.padding(top = 4.dp)) }
        // every tap is one purchase: say so, and show what is already at home, so nobody buys 17 bottles of milk by accident
        var bought by remember { mutableStateOf<Pair<String, Int>?>(null) }
        Text(w(ru, "У вас: ", "You have: ") + money(s.coins, ru) + w(ru, ". Одно нажатие — одна покупка.", ". One tap buys one."), style = T.Small, modifier = Modifier.padding(top = 4.dp))
        c.game.market(now).forEach { (k, price) ->
            val have = if (c.content.variety[k] != null) s.seeds[k] ?: 0 else s.bag[k] ?: 0
            val just = bought?.first == k
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(c.content.name(k)(ru), style = T.Body)
                    if (just) Text(w(ru, "Куплено: %d. Теперь у вас %d.", "Bought %d. You now have %d.").format(bought!!.second, have), style = T.Small.copy(color = C.green))
                    else if (have > 0) Text(w(ru, "Уже есть: %d", "Already have: %d").format(have), style = T.Small)
                }
                PixelButton(money(price, ru), {
                    if (c.game.buy(s, now, k)) { bought = k to (if (just) bought!!.second else 0) + 1; c.changed() }
                }, kind = Kind.GHOST, enabled = s.coins >= price)
            }
        }
    }
}

@Composable
private fun Board(c: AppContainer) {
    val ru = ru()
    c.version.collectAsState().value  // repaint after every change: the container itself never changes
    val s = c.state ?: return
    Text(w(ru, "Доска объявлений у парадной", "The notice board by the door"), style = T.Title)
    if (s.board.isEmpty()) Text(w(ru, "Пока пусто. Новые объявления появляются раз в несколько дней.", "Empty for now. New notices go up every few days."), style = T.Body)
    s.board.toList().forEach { pin ->
        val n = c.content.board.firstOrNull { it.key == pin.key } ?: return@forEach
        Panel(Modifier.fillMaxWidth().padding(top = 8.dp), fill = C.paper) {
            Text(n.text(ru), style = T.Hand)
            Text(w(ru, "Нужно: ", "Needs: ") + n.needs.joinToString { "${c.content.name(it.key)(ru).lowercase()} ×${it.qty}" }, style = T.Small)
            Text(w(ru, "Взамен: ", "In return: ") + rewards(c, n.rewards, ru), style = T.Small)
            PixelButton(w(ru, "Отнести", "Take it over"), { c.game.answerNotice(s, pin.key); c.changed() }, Modifier.padding(top = 6.dp), enabled = c.game.canFulfil(s, n.needs))
        }
    }
}

@Composable
private fun LetterView(c: AppContainer, key: String) {
    val ru = ru()
    val s = c.state ?: return
    if (key.startsWith("harvest:")) {
        val b = key.split(':')
        Text(w(ru, "Урожай", "Harvest"), style = T.Title)
        Text(c.content.name(b[1])(ru) + ": +" + b[2], style = T.Body, modifier = Modifier.padding(top = 6.dp))
        if (b[3] == "true") Text(w(ru, "И горсть семян на развод.", "And a handful of seed to sow again."), style = T.Small)
        if (b[4] == "true") Text(w(ru, "Первый урожай этого сорта — в альбом!", "The first harvest of this variety — into the album!"), style = T.Small.copy(color = C.green))
        return
    }
    val n = c.game.note(key) ?: return
    LaunchedEffect(key) { s.inbox.firstOrNull { it.key == key }?.read = true; c.changed() }
    val p = c.content.person[n.who]
    Row(verticalAlignment = Alignment.CenterVertically) {
        app.greenstreet.ui.components.BufImage(portrait(c, n.who), Modifier.width(48.dp), 2.4.dp)
        Column(Modifier.padding(start = 10.dp)) {
            Text(p?.name?.invoke(ru) ?: "", style = T.Heading)
            Text(p?.role?.invoke(ru) ?: "", style = T.Small)
        }
    }
    Panel(Modifier.fillMaxWidth().padding(top = 10.dp), fill = C.light) { Text(n.text(ru), style = T.Hand) }
    if (n.rewards.isNotEmpty()) Text(w(ru, "Вложено: ", "Enclosed: ") + rewards(c, n.rewards, ru), style = T.Small, modifier = Modifier.padding(top = 8.dp))
}

/** Text of a pair for the current language. */
@Composable
fun Tx.text(): String = this(ru())
