package app.greenstreet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.greenstreet.AppContainer
import app.greenstreet.container
import app.greenstreet.data.GoodKind
import app.greenstreet.data.Place
import app.greenstreet.ui.art.ItemIcons
import app.greenstreet.ui.art.blit
import app.greenstreet.ui.art.up2
import app.greenstreet.ui.art.ball
import app.greenstreet.ui.components.BufImage
import app.greenstreet.ui.components.Panel
import app.greenstreet.ui.components.pixelBox
import app.greenstreet.ui.components.slot
import app.greenstreet.ui.theme.C
import app.greenstreet.ui.theme.T

private val SLOT = 62.dp

/** One wooden inventory slot: the icon, a count in the corner, a highlight when picked. */
@Composable
fun ItemSlot(icon: app.greenstreet.ui.art.Buf?, count: Int?, picked: Boolean, dim: Boolean = false, mark: String? = null, onClick: () -> Unit) {
    Box(Modifier.size(SLOT).slot(gold = picked).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        if (icon != null) BufImage(icon, Modifier.size(44.dp).then(if (dim) Modifier.background(Color.Transparent) else Modifier), 44.dp / icon.w)
        else Text("?", style = T.Title.copy(color = C.faint))
        if (dim && icon != null) Box(Modifier.size(SLOT).background(Color(0x99E6CFA0)))
        // the count on a little plaque in the corner, so the picture's frame doesn't run through the digits
        if (count != null && count > 1) Text("$count", style = T.Small.copy(color = C.ink),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 2.dp)
                .background(C.paper, app.greenstreet.ui.components.ChipShape).border(1.dp, C.faint, app.greenstreet.ui.components.ChipShape)
                .padding(horizontal = 4.dp))
        if (mark != null) Text(mark, style = T.Small.copy(color = C.red), modifier = Modifier.align(Alignment.TopEnd).padding(end = 5.dp, top = 2.dp))
    }
}

/** Twelve little cells for the months a variety may be sown, the current one marked. */
@Composable
private fun Months(sow: Set<Int>, now: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (m in 1..12) Box(Modifier.size(width = 18.dp, height = 12.dp).background(if (m in sow) C.green else C.paper2), contentAlignment = Alignment.Center) {
            if (m == now) Box(Modifier.size(4.dp).background(C.lamp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BagScreen() {
    val c = LocalContext.current.container
    val ver by c.version.collectAsState()
    ver.let { }
    val ru = ru()
    val s = c.state ?: return
    val now = c.now()
    var tab by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        // the purse, the road, the beds: three figures at a glance
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("${s.coins}", w(ru, "грошей", "groats"), Modifier.weight(1f))
            Stat("%.1f".format(s.km).replace('.', if (ru) ',' else '.'), w(ru, "км пройдено", "km walked"), Modifier.weight(1f))
            Stat("${s.beds.count { c.content.variety[it.variety]?.place == Place.VERGE }}/${c.game.maxBeds(s, now)}", w(ru, "грядок", "beds"), Modifier.weight(1f))
        }
        val tabs = listOf(w(ru, "Семена", "Seeds"), w(ru, "Добро", "Goods"), w(ru, "Альбом", "Album"), w(ru, "Ленты", "Ribbons"))
        app.greenstreet.ui.components.PlaqueTabs(tabs, tab, Modifier.padding(horizontal = 12.dp)) { tab = it; picked = null }
        // what the picked slot is
        picked?.let { k -> Detail(c, k, tab, now.month) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp)) {
            val keys: List<Pair<String, Int?>> = when (tab) {
                0 -> s.seeds.entries.sortedByDescending { c.content.variety[it.key]?.sow?.contains(now.month) == true }.map { it.key to it.value }
                1 -> s.bag.entries.sortedBy { kindOrder(c, it.key) }.map { it.key to it.value }
                2 -> albumKeys(c).map { it to null }
                else -> c.content.ribbons.map { it.key to null }
            }
            // neighbours' lost things picked up on the walk: not goods, so listed apart, with whom to give them to
            val lost = if (tab == 1) c.game.lostAll(s) else emptyList()
            if (lost.isNotEmpty()) {
                Panel(Modifier.fillMaxWidth().padding(bottom = 10.dp), fill = C.paper) {
                    Text(w(ru, "чужие вещи", "other people's things"), style = T.Eyebrow)
                    for ((id, k) in lost) {
                        val who = c.content.person[id]?.name?.invoke(ru) ?: id
                        Text(app.greenstreet.domain.Lost.things[k](ru).replaceFirstChar { it.uppercase() } + " — " + w(ru, "отдать: ", "give to ") + who, style = T.Body, modifier = Modifier.padding(top = 4.dp))
                    }
                    Text(w(ru, "Загляните к соседу на страницу: там будет кнопка «Отдать находку».", "Open the neighbour's page: there'll be a \"Give back\" button."), style = T.Small, modifier = Modifier.padding(top = 6.dp))
                }
            }
            if (keys.isEmpty() && lost.isEmpty()) Text(when (tab) {
                0 -> w(ru, "Семян нет. Их дают соседи и семенной ряд на рынке.", "No seeds. Neighbours and the market seed stall give them.")
                else -> w(ru, "Пусто. Пока.", "Empty. For now.")
            }, style = T.Body.copy(color = C.muted))
            if (tab == 2) Text(w(ru, "Записано %d из %d", "%d of %d recorded").format(s.album.size, keys.size), style = T.Small, modifier = Modifier.padding(bottom = 8.dp))
            if (tab == 3) Text(w(ru, "Получено %d из %d", "%d of %d earned").format(s.ribbons.size, keys.size), style = T.Small, modifier = Modifier.padding(bottom = 8.dp))
            // seeds in groups with a heading each (#39); the other tabs as one grid
            val groups = if (tab == 0) keys.groupBy { (k, _) -> app.greenstreet.data.SeedGroup.of(c.content.variety[k]?.crop ?: "") }.toSortedMap().toList()
                else listOf(-1 to keys)
            for ((g, part) in groups) {
            if (g >= 0) Text(app.greenstreet.data.SeedGroup.name(g)(ru).uppercase(), style = T.Eyebrow, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((k, n) in part) {
                    val known = when (tab) { 2 -> k in s.album; 3 -> k in s.ribbons; else -> true }
                    val icon = when (tab) { 0 -> ItemIcons.packet(c.content, k); 3 -> ribbonIcon(c, k); else -> if (known) ItemIcons.of(c.content, k) else null }
                    val dim = tab == 3 && !known
                    ItemSlot(icon, n, picked == k, dim) { picked = if (picked == k) null else k }
                }
            }
            }
            Spacer(Modifier.height(16.dp))
            c.content.almanac["%02d-%02d".format(now.date.monthValue, now.date.dayOfMonth)]?.let {
                Panel(Modifier.fillMaxWidth(), fill = C.paper) { Text(w(ru, "народный календарь", "folk calendar"), style = T.Eyebrow); Text(it(ru), style = T.Hand) }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Panel(modifier, padding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
        Text(value, style = T.Number, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(label, style = T.Small, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun Detail(c: AppContainer, k: String, tab: Int, month: Int) {
    val ru = ru()
    c.version.collectAsState().value  // repaint after every change: the container itself never changes
    val s = c.state ?: return
    Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (tab != 3) BufImage(if (tab == 0) ItemIcons.packet(c.content, k) else ItemIcons.of(c.content, k), Modifier.width(40.dp), 1.25.dp)
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                when (tab) {
                    3 -> {
                        val r = c.content.ribbons.firstOrNull { it.key == k } ?: return@Column
                        Text(r.name(ru), style = T.Heading); Text(r.about(ru), style = T.Small)
                    }
                    2 -> if (k in s.album) { Text(c.content.name(k)(ru), style = T.Heading); Text(about(c, k, ru), style = T.Small) } else Text(w(ru, "Ещё не встречалось.", "Not found yet."), style = T.Body)
                    else -> {
                        Text(c.content.name(k)(ru), style = T.Heading)
                        Text(about(c, k, ru), style = T.Small)
                        val v = c.content.variety[k]
                        if (v != null && tab == 0) {
                            Text(w(ru, "Лучше всего сеять (можно в любой месяц):", "Best sown in (any month will do):"), style = T.Small.copy(color = C.ink), modifier = Modifier.padding(top = 4.dp))
                            Months(v.sow, month)
                            Text(w(ru, "Созреет за %d дн. · где: %s", "Ripens in %d days · where: %s").format(v.days, place(v.place, ru)), style = T.Small, modifier = Modifier.padding(top = 4.dp))
                        } else Text(w(ru, "На ярмарке: ", "At the fair: ") + money(c.game.salePrice(c.state!!, c.now(), k), ru) + (if (c.game.demand(c.state!!, c.now()) == c.content.speciesOf(k)) w(ru, " ★ спрос недели", " ★ in demand") else ""), style = T.Small.copy(color = C.green), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

private fun place(p: Place, ru: Boolean) = when (p) {
    Place.VERGE -> w(ru, "газон у дороги", "a verge"); Place.SILL -> w(ru, "подоконник", "the windowsill")
    Place.BALCONY -> w(ru, "балкон", "the balcony"); Place.GARDEN -> w(ru, "общий сад", "the shared garden")
}

private fun about(c: AppContainer, k: String, ru: Boolean): String =
    (c.content.variety[k]?.about ?: c.content.good[k]?.about ?: c.content.recipesFor[k]?.firstOrNull()?.about)?.invoke(ru).orEmpty()

private fun kindOrder(c: AppContainer, k: String): Int = when {
    c.content.variety[k] != null -> 0
    c.content.good[k]?.kind == GoodKind.FISH -> 2
    c.content.good[k]?.kind == GoodKind.FIND -> 3
    c.content.good[k]?.kind == GoodKind.YARD -> 1
    c.content.good[k]?.kind == GoodKind.STAPLE -> 5
    else -> 4
}

/** Everything that can be recorded: varieties, fish, finds, home produce, what's cooked. */
private fun albumKeys(c: AppContainer): List<String> =
    c.content.varieties.map { it.key } + c.content.goods.filter { it.kind != GoodKind.STAPLE }.map { it.key } + c.content.products.toList()

/** A small rosette for a ribbon. */
private val ribbonCache = HashMap<String, app.greenstreet.ui.art.Buf>()

/**
 * A ribbon: a painted rosette for its kind; for one earned by a particular crop, fish, find or dish, a plain rosette
 * with that thing's own icon in the middle, and for a neighbour's friendship, their face.
 */
private fun ribbonIcon(c: app.greenstreet.AppContainer, k: String): app.greenstreet.ui.art.Buf = ribbonCache.getOrPut(k) {
    val r = c.content.ribbons.firstOrNull { it.key == k }
    val kind = r?.kind ?: k.substringBefore('_')
    val arg = r?.arg?.substringBefore(':') ?: ""
    fun pic(n: String) = app.greenstreet.ui.art.Pictures.buf("art/ribbons/$n.png")
    val centre: app.greenstreet.ui.art.Buf? = when {
        kind == "hearts" && c.content.person.containsKey(arg) -> app.greenstreet.ui.portraitFace(c, arg, 14)
        kind in setOf("harvest", "fish", "find", "cook") && arg.isNotEmpty() && arg != "any" ->
            app.greenstreet.ui.art.Pictures.buf("art/items/$arg.t.png") ?: c.content.crops[arg]?.firstOrNull()?.let { app.greenstreet.ui.art.Pictures.buf("art/items/${it.key}.t.png") }
        else -> null
    }
    val base = if (centre != null) pic("blank") else pic(if (kind == "season") "season_$arg" else kind) ?: pic(if (kind == "season") k else kind)
    if (base == null) return@getOrPut ribbonDrawn(k)
    if (centre == null) return@getOrPut base
    val o = app.greenstreet.ui.art.Buf(base.w, base.h)
    o.blit(base, 0, 0)
    // the rosette's disc is about a third of the way down
    o.blit(centre, (o.w - centre.w) / 2, (o.h * 38 / 100) - centre.h / 2)
    o
}

private fun ribbonDrawn(k: String): app.greenstreet.ui.art.Buf {
    val b = app.greenstreet.ui.art.Buf(16, 16)
    val colours = listOf(app.greenstreet.ui.art.R.red, app.greenstreet.ui.art.R.roofB, app.greenstreet.ui.art.R.green, app.greenstreet.ui.art.R.honey, app.greenstreet.ui.art.R.lilac)
    val r = colours[(k.hashCode() and 0x7fffffff) % colours.size]
    b.ball(8, 6, 5, r, app.greenstreet.ui.art.R.INK)
    b.ball(8, 6, 2, app.greenstreet.ui.art.R.gold, null)
    for (j in 0 until 5) { b.p(5 - j / 2, 11 + j, r[2]); b.p(6 - j / 2, 11 + j, r[3]); b.p(10 + j / 2, 11 + j, r[2]); b.p(11 + j / 2, 11 + j, r[3]) }
    return b.up2()
}
