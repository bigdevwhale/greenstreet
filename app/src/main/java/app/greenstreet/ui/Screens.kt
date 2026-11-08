package app.greenstreet.ui

import androidx.compose.foundation.layout.Arrangement
import app.greenstreet.ui.components.pixelBox
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.greenstreet.container
import app.greenstreet.data.Place
import app.greenstreet.domain.Geo
import app.greenstreet.domain.metres
import app.greenstreet.ui.art.Buf
import app.greenstreet.ui.art.PixelView
import app.greenstreet.ui.art.YardScene
import app.greenstreet.ui.art.person
import app.greenstreet.ui.art.R
import app.greenstreet.ui.art.rp
import app.greenstreet.ui.components.BufImage
import app.greenstreet.ui.components.Kind
import app.greenstreet.ui.components.Panel
import app.greenstreet.ui.components.PixelButton
import app.greenstreet.ui.theme.C
import app.greenstreet.ui.theme.T

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(title, style = T.Heading)
        content()
    }
}

/** The courtyard: hens, windowsill and balcony pots, the kitchen, the cellar, the board. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YardScreen(open: (Sheet) -> Unit) {
    val c = LocalContext.current.container
    val ver by c.version.collectAsState()
    val ru = ru()
    val s = c.state ?: return
    val now = c.now()
    val home = c.atHome()
    LaunchedEffect(ver) { if (home && c.game.tendHome(s, now) > 0) c.changed() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        YardWithLabels(c, ver, ru)
        if (!home) Text(w(ru, "Вы не дома. Готовить, собирать яйца и сеять в горшки можно только во дворе.", "You're not home. Cooking, collecting eggs and potting up happen only in the courtyard."),
            style = T.Small, modifier = Modifier.padding(12.dp))
        Section(w(ru, "Соседи по двору", "The yard's people")) {
            Text(if (home) w(ru, "Нажмите на соседа, чтобы поговорить.", "Tap a neighbour to talk.") else w(ru, "Поговорить можно, когда вы дома.", "You can talk to them when you're home."), style = T.Small)
            FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                c.content.people.filter { it.yard }.forEach { p ->
                    val asks = c.game.request(s, p.id) != null
                    val awake = c.whereabouts(p.id) != app.greenstreet.domain.Whereabouts.ASLEEP
                    ItemSlot(portrait(c, p.id), null, false, dim = !home || !awake, mark = if (!awake) "z" else if (asks) "!" else null) {
                        if (home && awake) open(Sheet.Visit(p.id)) else open(Sheet.Place(p.id))
                    }
                }
            }
        }
        Section(w(ru, "Курятник", "The hen house")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(w(ru, "Яиц в гнёздах: %d", "Eggs in the nests: %d").format(s.eggs), style = T.Body, modifier = Modifier.weight(1f))
                PixelButton(w(ru, "Собрать", "Collect"), { c.game.collectEggs(s); c.changed() }, enabled = home && s.eggs > 0)
            }
        }
        for (place in listOf(Place.SILL, Place.BALCONY, Place.GARDEN)) {
            if (!c.game.placeOpen(s, now, place)) continue
            val title = when (place) { Place.SILL -> w(ru, "Подоконник", "The windowsill"); Place.BALCONY -> w(ru, "Балкон-теплица", "The balcony greenhouse"); else -> w(ru, "Общий сад", "The shared garden") }
            Section(title) {
                val pots = s.beds.filter { c.content.variety[it.variety]?.place == place }
                pots.forEach { b ->
                    val v = c.content.variety.getValue(b.variety)
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${v.name(ru)} · ${bedState(c, b, now, ru)}", style = T.Small.copy(color = C.ink), modifier = Modifier.weight(1f))
                        if (c.game.ripe(b)) PixelButton(w(ru, "Собрать", "Pick"), { c.game.harvest(s, now, b.id); c.changed() }, kind = Kind.LAMP, enabled = home)
                    }
                }
                val sowable = s.seeds.keys.mapNotNull { c.content.variety[it] }.filter { it.place == place && c.game.canSow(s, now, it) }
                if (sowable.isNotEmpty() && home) {
                    Text(w(ru, "Посеять — нажмите на пакетик:", "To sow, tap a packet:"), style = T.Small, modifier = Modifier.padding(top = 6.dp))
                    FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sowable.forEach { v -> ItemSlot(app.greenstreet.ui.art.ItemIcons.packet(c.content, v.key), s.seeds[v.key], false) { c.game.sow(s, now, v.key, s.home ?: Geo(0.0, 0.0)); c.changed() } }
                    }
                }
                if (sowable.isNotEmpty() && !home) Text(w(ru, "Семена есть — посеять можно, когда вы дома.", "You have seeds: sow them when you're home."), style = T.Small, modifier = Modifier.padding(top = 6.dp))
                if (pots.isEmpty() && sowable.isEmpty()) Text(
                    if (place == Place.SILL) w(ru, "Пусто. Семена для подоконника — ростки, зелень, луковицы — круглый год продаёт рынок. Растут в любой месяц.",
                        "Empty. The market sells windowsill seeds — shoots, greens, bulbs — all year. They grow in any month.")
                    else w(ru, "Пусто. Семена для этого места дают соседи.", "Empty. Neighbours give seeds for this spot."), style = T.Small)
            }
        }
        Section(w(ru, "Кухня", "The kitchen")) { Kitchen(c, home) }
        Section(w(ru, "Погреб и кладовка", "Cellar and pantry")) {
            val products = s.bag.keys.filter { it in c.content.products }
            if (products.isEmpty()) Text(w(ru, "Полки пока пустые. Всё, что приготовите, встанет сюда.", "The shelves are empty. Whatever you cook goes here."), style = T.Small)
            else FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                products.forEach { k -> ItemSlot(app.greenstreet.ui.art.ItemIcons.of(c.content, k), s.bag[k], false) {} }
            }
        }
        Section(w(ru, "Доска объявлений", "The notice board")) {
            Text(w(ru, "Объявлений: %d", "Notices: %d").format(s.board.size), style = T.Body)
            PixelButton(w(ru, "Посмотреть", "Read them"), { open(Sheet.Board) }, Modifier.padding(top = 6.dp), Kind.GHOST)
        }
        Section(w(ru, "Хозяйство", "Household")) {
            Text(w(ru, "На что копить гроши. Каждое улучшение меняет одно правило — выбирайте, что вам нужнее.", "What the groats are for. Each upgrade changes one rule — pick what you need most."), style = T.Small)
            for (u in app.greenstreet.domain.Upgrades.all) {
                val lvl = c.game.up(s, u.key)
                val price = c.game.nextPrice(s, u.key)
                Panel(Modifier.fillMaxWidth().padding(top = 6.dp), fill = if (lvl > 0) C.paper else C.light) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(u.name(ru) + if (u.max > 1) "  ${"●".repeat(lvl)}${"○".repeat(u.max - lvl)}" else if (lvl > 0) "  ✓" else "", style = T.Body)
                            Text(u.effect(if (price == null) lvl else lvl + 1)(ru).replaceFirstChar { it.uppercase() }, style = T.Small)
                        }
                        if (price != null) PixelButton(money(price, ru), { if (c.game.buyUpgrade(s, u.key)) c.changed() }, kind = if (s.coins >= price) Kind.LAMP else Kind.GHOST, enabled = s.coins >= price)
                    }
                }
            }
        }
        Section(w(ru, "Инструменты", "Tools")) {
            Text(w(ru, "Лейка: ", "Watering can: ") + listOf("", w(ru, "жестяная, 10 м", "tin, 10 m"), w(ru, "медная, 20 м", "copper, 20 m"), w(ru, "лужёная, 30 м", "tinned, 30 m"))[s.tool("can")], style = T.Body)
            Text(w(ru, "Корзина: ", "Basket: ") + listOf("", w(ru, "плетёная", "wicker"), w(ru, "большая, +1", "big, +1"), w(ru, "короб, +2", "a pack basket, +2"))[s.tool("basket")], style = T.Body)
            Text(w(ru, "Удочка: ", "Rod: ") + listOf("", w(ru, "ореховая", "hazel"), w(ru, "бамбуковая", "bamboo"), w(ru, "с катушкой", "with a reel"))[s.tool("rod")], style = T.Body)
            Text(w(ru, "Мест под грядки: %d", "Bed places: %d").format(c.game.maxBeds(s, now)), style = T.Body)
            Text(w(ru, "Инструменты улучшает Михалыч из хозтоваров — за помощь.", "Mikhalych at the hardware shop improves tools — for a helping hand."), style = T.Small)
        }
    }
}

/** A neighbour's face, 20×20 art pixels. */
fun portrait(c: app.greenstreet.AppContainer, id: String): Buf {
    val b = Buf(20, 20)
    for (y in 0 until 20) for (x in 0 until 20) b.p(x, y, rp(R.paper, .55 + y / 20.0 * .2, x, y))
    if (c.content.person[id]?.look?.containsKey("cat") == true) catFace(b) else b.person(10, 34, lookOf(c, id))
    return b
}

/** Pryanik: a ginger tom, round-faced and well fed, looking at you as if you owe him sausage. */
private fun catFace(b: Buf) {
    val g = R.ginger
    // the body and a white chest
    for (y in 14 until 20) for (x in 2 until 18) { val dx = (x - 9.5) / 8.0; val dy = (y - 20.0) / 6.5; if (dx * dx + dy * dy < 1) b.p(x, y, rp(g, .55 - dx * .2, x, y)) }
    for (y in 15 until 20) for (x in 8 until 12) b.p(x, y, g[6])
    // ears with pink insides
    for (k in 0 until 4) for (i in 0..k) { b.p(4 + i, 3 + k, g[3]); b.p(15 - i, 3 + k, g[3]) }
    b.p(5, 5, 0xFFE89A9A.toInt()); b.p(14, 5, 0xFFE89A9A.toInt()); b.p(5, 6, 0xFFE89A9A.toInt()); b.p(14, 6, 0xFFE89A9A.toInt())
    // the round head, lit from the left
    for (y in 4 until 17) for (x in 3 until 17) { val dx = (x - 9.5) / 6.6; val dy = (y - 10.5) / 6.0; if (dx * dx + dy * dy < 1) b.p(x, y, rp(g, .62 - dx * .18 - dy * .1, x, y)) }
    // tabby stripes on the forehead and cheeks
    b.p(9, 5, g[2]); b.p(10, 5, g[2]); b.p(9, 6, g[2]); b.p(10, 6, g[2]); b.p(7, 6, g[2]); b.p(12, 6, g[2])
    b.p(4, 11, g[2]); b.p(5, 12, g[2]); b.p(15, 11, g[2]); b.p(14, 12, g[2])
    // a pale muzzle, green eyes, a pink nose
    for (y in 12 until 15) for (x in 7 until 13) b.p(x, y, g[5])
    for ((ex, pupil) in listOf(6 to 7, 12 to 12)) { b.p(ex, 9, 0xFF7FD05A.toInt()); b.p(ex + 1, 9, 0xFF7FD05A.toInt()); b.p(pupil, 9, R.INK); b.p(ex, 8, g[2]); b.p(ex + 1, 8, g[2]) }
    b.p(9, 12, 0xFFE07A80.toInt()); b.p(10, 12, 0xFFE07A80.toInt()); b.p(9, 13, R.INK); b.p(10, 13, R.INK)
    // whiskers
    for (k in 0 until 2) { b.p(4 - k, 13 + k, g[6]); b.p(15 + k, 13 + k, g[6]); b.p(5 - k, 13, g[6]); b.p(14 + k, 13, g[6]) }
}

/** Neighbours met so far, their requests and the letterbox. */
@Composable
fun PeopleScreen(open: (Sheet) -> Unit) {
    val c = LocalContext.current.container
    val ver by c.version.collectAsState()
    ver.let { }
    val ru = ru()
    val s = c.state ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        // every open request in one place: what's still missing and where to get it, the ones ready to take first
        val now = c.now()
        val asks = s.bonds.keys.mapNotNull { id -> c.game.request(s, id)?.let { id to it } }
            .sortedWith(compareByDescending<Pair<String, app.greenstreet.data.Request>> { c.game.canFulfil(s, it.second.needs) }.thenBy { s.bonds[it.first]?.reqShown ?: 0 })
        if (asks.isNotEmpty()) Section(w(ru, "Что нужно соседям", "What the neighbours need")) {
            for ((id, r) in asks) {
                val ready = c.game.canFulfil(s, r.needs)
                Panel(Modifier.fillMaxWidth().padding(top = 6.dp), fill = if (ready) C.light else C.paper, onClick = { open(Sheet.Place(id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.content.person[id]?.name?.invoke(ru) ?: id, style = T.Heading, modifier = Modifier.weight(1f))
                        if (ready) Text(w(ru, "можно отнести", "ready to take"), style = T.Small.copy(color = C.light),
                            modifier = Modifier.pixelBox(C.green, bevel = null, lip = null).padding(horizontal = 6.dp, vertical = 1.dp))
                    }
                    for (n in r.needs) {
                        val have = app.greenstreet.domain.Sources.have(c.content, s, n.key).coerceAtMost(n.qty)
                        Text("${c.content.name(n.key)(ru).replaceFirstChar { it.uppercase() }} $have/${n.qty}" +
                            if (have < n.qty) " — " + app.greenstreet.domain.Sources.hint(c.game, s, now, n)(ru) else " ✓",
                            style = T.Small.copy(color = if (have >= n.qty) C.green else C.ink), modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
        val mail = s.inbox.sortedByDescending { it.day }
        Section(w(ru, "Почтовый ящик", "The letterbox")) {
            if (mail.isEmpty()) Text(w(ru, "Пока пусто. Соседи пишут, когда узнают вас получше.", "Empty so far. Neighbours write once they know you better."), style = T.Small)
            mail.take(30).forEach { m ->
                val n = c.game.note(m.key) ?: return@forEach
                Row(Modifier.fillMaxWidth().padding(top = 6.dp).clickable { open(Sheet.Letter(m.key)) }, verticalAlignment = Alignment.CenterVertically) {
                    BufImage(envelope(!m.read), Modifier.width(40.dp), 2.5.dp)
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text(c.content.person[n.who]?.name?.invoke(ru) ?: "", style = T.Body.copy(color = if (m.read) C.muted else C.ink))
                        Text(n.text(ru).take(46) + "…", style = T.Small, maxLines = 1)
                    }
                }
            }
        }
        val people = c.content.people.sortedByDescending { s.bonds[it.id]?.points ?: -1 }
        for (p in people) {
            val b = s.bonds[p.id]
            val opened = b?.isOpen == true
            Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), onClick = { open(Sheet.Place(p.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BufImage(remember(p.id) { portrait(c, p.id) }, Modifier.width(56.dp), 2.8.dp)
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text(if (opened) p.name(ru) else "???", style = T.Heading)
                        Text(p.role(ru), style = T.Small)
                        val st = c.whereabouts(p.id)
                        Text(app.greenstreet.domain.Schedule.now(p, java.time.LocalDateTime.now())(ru).replaceFirstChar { it.uppercase() },
                            style = T.Small.copy(color = if (st == app.greenstreet.domain.Whereabouts.ASLEEP) C.muted else C.green))
                        if (opened) {
                            val h = c.game.hearts(s, p.id)
                            Hearts(h, Modifier.padding(vertical = 3.dp))
                            c.game.request(s, p.id)?.let { r -> Text(w(ru, "Просит: ", "Asks for: ") + r.needs.joinToString { "${c.content.name(it.key)(ru).lowercase()} ×${it.qty}" }, style = T.Small.copy(color = C.ink)) }
                            val loves = b!!.known.filterValues { it == "love" || it == "like" }.keys
                            if (loves.isNotEmpty()) Text(w(ru, "Радуется: ", "Glad of: ") + loves.joinToString { c.content.name(it)(ru).lowercase() }, style = T.Small)
                            val notes = p.facts.count { it.hearts <= h }
                            Text(w(ru, "В блокноте записей: %d из %d", "Notebook: %d of %d lines").format(notes, p.facts.size), style = T.Small)
                        } else Text(if (p.yard) w(ru, "Живёт в вашем доме.", "Lives in your house.") else w(ru, "Ещё не знакомы. Загляните — место отмечено на карте.", "Not met yet. Look in — the place is marked on the map."), style = T.Small)
                        if (!p.yard) (c.spotNow(p.id)?.let { sp -> c.geoOf(sp) } ?: c.placeGeo(p.id))?.let { g ->
                            val here = c.here.value ?: s.home
                            if (here != null) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Text(where(here, g, ru), style = T.Small.copy(color = C.green), modifier = Modifier.weight(1f))
                                PixelButton(w(ru, "На карте", "On the map"), { c.focus.value = g; open(Sheet.Tab(0)) }, kind = Kind.GHOST)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "320 m north-east" — how far and which way a place is from here. */
fun where(from: Geo, to: Geo, ru: Boolean): String {
    val m = from.metres(to)
    val dx = (to.lon - from.lon) * Math.cos(Math.toRadians(from.lat)); val dy = to.lat - from.lat
    val a = (Math.toDegrees(Math.atan2(dx, dy)) + 360 + 22.5) % 360
    val dirs = if (ru) listOf("к северу", "к северо-востоку", "к востоку", "к юго-востоку", "к югу", "к юго-западу", "к западу", "к северо-западу")
    else listOf("north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west")
    val dist = if (m < 1000) "${(m / 10).toInt() * 10} " + (if (ru) "м" else "m") else "%.1f ".format(m / 1000).replace('.', if (ru) ',' else '.') + (if (ru) "км" else "km")
    return if (m < 40) (if (ru) "совсем рядом" else "right here") else "$dist ${dirs[(a / 45).toInt() % 8]}"
}

/** The kitchen as a shelf of dishes: what can be cooked now comes first and bright; a tap shows what it takes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Kitchen(c: app.greenstreet.AppContainer, home: Boolean) {
    val ru = ru()
    val s = c.state ?: return
    val now = c.now()
    var picked by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val known = c.game.known(s).sortedBy { !c.game.canFulfil(s, it.needs) }
    val ready = known.count { c.game.canFulfil(s, it.needs) }
    Text(if (ready > 0) w(ru, "Можно приготовить: %d. Нажмите на блюдо.", "Ready to cook: %d. Tap a dish.").format(ready) else w(ru, "Пока ни на что не хватает продуктов. Нажмите на блюдо — покажу, чего не хватает.", "Nothing has all its ingredients yet. Tap a dish to see what's missing."), style = T.Small)
    picked?.let { key ->
        val r = c.content.recipes.firstOrNull { it.key == key } ?: return@let
        Panel(Modifier.fillMaxWidth().padding(top = 6.dp), fill = C.paper) {
            Text(r.name(ru), style = T.Heading)
            Text(r.about(ru), style = T.Small)
            r.needs.forEach { n ->
                val have = c.game.run { s.bag.entries.filter { c.content.matches(n.key, it.key) }.sumOf { it.value } }
                Text("${c.content.name(n.key)(ru)} — $have / ${n.qty}", style = T.Body.copy(color = if (have >= n.qty) C.green else C.red))
            }
            val station = c.game.stationOpen(s, now, r.station)
            if (!station) Text(w(ru, "Для этого нужен погреб — он появится на второй год.", "This needs the cellar — it comes in the second year."), style = T.Small.copy(color = C.red))
            if (!home) Text(w(ru, "Готовить можно только дома.", "You can only cook at home."), style = T.Small.copy(color = C.red))
            PixelButton(w(ru, "Готовить", "Cook"), { if (c.game.cook(s, now, r.key)) { c.changed() } }, Modifier.padding(top = 6.dp), enabled = home && station && c.game.canFulfil(s, r.needs))
        }
    }
    FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        known.forEach { r -> ItemSlot(app.greenstreet.ui.art.ItemIcons.of(c.content, r.makes), null, picked == r.key, dim = !c.game.canFulfil(s, r.needs)) { picked = if (picked == r.key) null else r.key } }
    }
    val unknown = c.content.recipes.size - known.size
    if (unknown > 0) Text(w(ru, "Ещё %d рецептов знают соседи.", "Neighbours know %d more recipes.").format(unknown), style = T.Small, modifier = Modifier.padding(top = 6.dp))
}

/** A small envelope, sealed with red wax while unread. */
fun envelope(unread: Boolean): Buf {
    val b = Buf(16, 12)
    for (y in 1 until 11) for (x in 1 until 15) b.p(x, y, rp(R.paper, .8 - y * .01, x, y))
    for (i in 0 until 7) { b.p(1 + i, 1 + i * 6 / 7, R.paper[2]); b.p(14 - i, 1 + i * 6 / 7, R.paper[2]) }
    for (x in 0 until 16) { b.p(x, 0, R.INK); b.p(x, 11, R.INK) }; for (y in 0 until 12) { b.p(0, y, R.INK); b.p(15, y, R.INK) }
    if (unread) { b.p(7, 6, R.red[3]); b.p(8, 6, R.red[4]); b.p(7, 7, R.red[2]); b.p(8, 7, R.red[3]) }
    return b
}


/** The yard picture with small signs on what is what: the hen house, your window, the boxes, the bench. Tap the picture to hide them. */
@Composable
private fun YardWithLabels(c: app.greenstreet.AppContainer, ver: Int, ru: Boolean) {
    val now = c.now()
    val life = remember(ver, now.day) { yardLife(c) }
    var labels by remember { mutableStateOf(true) }
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.padding(horizontal = 12.dp).clickable { labels = !labels }) {
        val k = maxWidth.value / 128f
        PixelView(remember(ver, now.day) { YardScene(moment(c), life) })
        if (labels) {
            val window = when (life.kind) { "block" -> 79f to 40f; "house" -> 77f to 58f; else -> 63f to 45f }
            val spots = buildList {
                add(Triple(15f, 66f, w(ru, "Курятник · яиц: %d", "Hen house · eggs: %d").format(c.state?.eggs ?: 0)))
                add(Triple(window.first, window.second, if (life.balcony) w(ru, "Ваш балкон", "Your balcony") else w(ru, "Ваше окно", "Your window")))
                add(Triple(74f, 78f, if (life.garden) w(ru, "Общий сад", "The shared garden") else w(ru, "Ящики с зеленью", "Herb boxes")))
                add(Triple(107f, 101f, w(ru, "Скамейка", "The bench")))
            }
            for ((x, y, t) in spots) {
                val tw = t.length * 7.2f + 8f
                val lx = (x * k - tw / 2).coerceIn(0f, maxWidth.value - tw)
                Text(t, style = T.Small.copy(color = C.ink, fontSize = 13.sp), maxLines = 1,
                    modifier = Modifier.offset(x = lx.dp, y = (y * k - 18).dp).pixelBox(C.light.copy(alpha = .9f), bevel = null, lip = null).padding(horizontal = 4.dp))
            }
        }
    }
    Text(if (labels) w(ru, "Нажмите на двор, чтобы скрыть подписи.", "Tap the yard to hide the labels.") else w(ru, "Нажмите на двор, чтобы показать подписи.", "Tap the yard to show the labels."),
        style = T.Small, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
}
