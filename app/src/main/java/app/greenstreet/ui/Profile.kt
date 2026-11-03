package app.greenstreet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.greenstreet.AppContainer
import app.greenstreet.data.Person
import app.greenstreet.domain.Game
import app.greenstreet.domain.Schedule
import app.greenstreet.domain.Whereabouts
import app.greenstreet.ui.components.BufImage
import app.greenstreet.ui.components.Kind
import app.greenstreet.ui.components.Panel
import app.greenstreet.ui.components.PixelButton
import app.greenstreet.ui.components.Segments
import app.greenstreet.ui.components.pixelBox
import app.greenstreet.ui.theme.C
import app.greenstreet.ui.theme.T
import java.time.LocalDateTime

/** Portrait, name, trade and where they are right now: the top of a neighbour's card and of a visit. */
@Composable
fun PersonHeader(c: AppContainer, p: Person, ru: Boolean, known: Boolean = true) {
    val lt = LocalDateTime.now()
    val st = Schedule.status(p, lt)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Box(Modifier.pixelBox(C.paper2).padding(4.dp)) { BufImage(remember(p.id) { portrait(c, p.id) }, Modifier, 3.dp) }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(if (known) p.name(ru) else w(ru, "Пока незнакомы", "Not met yet"), style = T.Title)
            Text(p.role(ru), style = T.Small)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                val dot = when (st) { Whereabouts.WORK -> C.green; Whereabouts.OUT -> C.lamp; Whereabouts.ASLEEP -> C.faint }
                Box(Modifier.size(10.dp).background(dot))
                Text("  " + Schedule.now(p, lt)(ru), style = T.Small.copy(color = if (st == Whereabouts.ASLEEP) C.muted else C.ink))
            }
        }
    }
}

/** Hearts, the bar to the next one, and what it will open. */
@Composable
fun Friendship(c: AppContainer, p: Person, ru: Boolean) {
    val s = c.state ?: return
    val pts = s.bonds[p.id]?.points ?: 0
    val h = c.game.hearts(pts)
    Panel(Modifier.fillMaxWidth().padding(top = 10.dp), fill = C.paper) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(w(ru, "дружба", "friendship"), style = T.Eyebrow, modifier = Modifier.weight(1f))
            Hearts(h)
        }
        if (h < 10) {
            val lo = Game.HEART_POINTS[h]; val hi = Game.HEART_POINTS[h + 1]
            Segments((pts - lo).toFloat() / (hi - lo), Modifier.padding(vertical = 6.dp))
            val next = p.facts.firstOrNull { it.hearts > h }
            val opens = buildList {
                if (next != null && next.hearts == h + 1) add(w(ru, "новая запись в блокноте", "a new notebook line"))
                if (h + 1 == 4) add(w(ru, "больше семян", "more seeds"))
                if (isEmpty()) add(w(ru, "новые просьбы и письма", "new requests and letters"))
            }
            Text(w(ru, "Следующее сердечко: ", "Next heart: ") + opens.joinToString(", "), style = T.Small)
        }
    }
}

/** A neighbour's card: who they are, the notebook, their tastes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Profile(c: AppContainer, id: String, go: (Sheet) -> Unit, close: () -> Unit) {
    val ru = ru()
    c.version.collectAsState().value  // repaint after every change: the container itself never changes
    val s = c.state ?: return
    val p = c.content.person.getValue(id)
    val met = s.bonds[id]?.isOpen == true
    val lt = LocalDateTime.now()
    val st = Schedule.status(p, lt)
    PersonHeader(c, p, ru, met)
    if (!met) {
        Text(when (st) {
            Whereabouts.OUT -> w(ru, "Гуляет неподалёку. Подойдите поближе — познакомитесь.", "Out for a walk nearby. Come closer to meet them.")
            Whereabouts.ASLEEP -> w(ru, "Сейчас спит. Загляните в часы работы.", "Asleep now. Look in during opening hours.")
            Whereabouts.WORK -> w(ru, "На месте. Зайдите — познакомитесь.", "In now. Drop in to meet them.")
        }, style = T.Body, modifier = Modifier.padding(top = 10.dp))
        Text(w(ru, "Часы: ", "Hours: ") + Schedule.hours(p)(ru), style = T.Small, modifier = Modifier.padding(top = 4.dp))
        WhereRow(c, p, ru, st, close)
        return
    }
    Friendship(c, p, ru)
    var tab by remember(id) { mutableIntStateOf(0) }
    val h = c.game.hearts(s, id)
    val notes = p.facts.count { it.hearts <= h }
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(w(ru, "О соседе", "About"), w(ru, "Блокнот", "Notebook"), w(ru, "Вкусы", "Tastes")).forEachIndexed { i, t ->
            PixelButton(t, { tab = i }, Modifier.weight(1f), if (tab == i) Kind.PRIMARY else Kind.GHOST)
        }
    }
    when (tab) {
        0 -> {
            if (p.perk(ru).isNotBlank()) Text(p.perk(ru), style = T.Body, modifier = Modifier.padding(top = 10.dp))
            Text(w(ru, "Часы: ", "Hours: ") + Schedule.hours(p)(ru), style = T.Small, modifier = Modifier.padding(top = 8.dp))
            if (!p.yard) c.places[id]?.name?.takeIf { it.isNotBlank() }?.let { Text(w(ru, "Место: ", "Place: ") + it, style = T.Small) }
            c.game.request(s, id)?.let { r ->
                Panel(Modifier.fillMaxWidth().padding(top = 8.dp), fill = C.light) {
                    Text(w(ru, "просит", "asks for"), style = T.Eyebrow)
                    Text(r.needs.joinToString { "${c.content.name(it.key)(ru).lowercase()} ×${it.qty}" }, style = T.Body)
                }
            }
            WhereRow(c, p, ru, st, close)
        }
        1 -> {
            Text(w(ru, "Записей: %d из %d. Новые появляются, когда вы становитесь ближе.", "%d of %d lines. New ones come as you grow closer.").format(notes, p.facts.size), style = T.Small, modifier = Modifier.padding(top = 10.dp))
            if (p.intro(ru).isNotBlank()) FactCard(w(ru, "при знакомстве", "when you met"), p.intro(ru), quote = true)
            p.facts.forEach { f ->
                if (f.hearts <= h) Fact(f.text(ru)) else LockedCard(f.hearts, ru)
            }
        }
        else -> {
            val tastes = s.bonds[id]?.known.orEmpty()
            Text(w(ru, "Узнаёте, когда дарите: по ответу видно, что по душе.", "You learn by giving: the answer shows what they like."), style = T.Small, modifier = Modifier.padding(top = 10.dp))
            for ((kind, title) in listOf("love" to w(ru, "очень любит", "loves"), "like" to w(ru, "радуется", "glad of"), "dislike" to w(ru, "не любит", "doesn't like"))) {
                val keys = tastes.filterValues { it == kind }.keys
                if (keys.isEmpty()) continue
                Text(title, style = T.Eyebrow, modifier = Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    keys.forEach { k -> ItemSlot(app.greenstreet.ui.art.ItemIcons.of(c.content, k), null, false, mark = if (kind == "love") "♥♥" else if (kind == "like") "♥" else "✗") {} }
                }
            }
            if (tastes.none { it.value != "plain" }) Text(w(ru, "Пока ничего не известно.", "Nothing known yet."), style = T.Body, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun WhereRow(c: AppContainer, p: Person, ru: Boolean, st: Whereabouts, close: () -> Unit) {
    if (p.yard || st == Whereabouts.ASLEEP) return
    val g = c.spotNow(p.id)?.let { c.geoOf(it) } ?: return
    val here = c.here.collectAsState().value ?: c.state?.home ?: return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(where(here, g, ru), style = T.Small.copy(color = C.green), modifier = Modifier.weight(1f))
        PixelButton(if (st == Whereabouts.OUT) w(ru, "Где сейчас", "Where now") else w(ru, "На карте", "On the map"), { c.focus.value = g; close() }, kind = Kind.GHOST)
    }
}

/** A notebook line as a card: where it came from on top, what was learned below. */
@Composable
fun Fact(t: String) {
    val i = t.indexOf(':')
    if (i in 1..42) FactCard(t.substring(0, i).lowercase(), t.substring(i + 1).trim().replaceFirstChar { it.uppercase() })
    else FactCard(null, t)
}

/** A speech bubble: what a neighbour says, without doubled quotes. */
@Composable
fun Says(text: String) {
    Panel(Modifier.fillMaxWidth().padding(vertical = 8.dp), fill = C.light) {
        Text(if (text.startsWith("«") || text.startsWith("—")) text else "«$text»", style = T.Letter)
    }
}

@Composable
private fun FactCard(source: String?, text: String, quote: Boolean = false) {
    Panel(Modifier.fillMaxWidth().padding(top = 8.dp), fill = C.light) {
        if (source != null) Text(source, style = T.Eyebrow)
        Text(if (quote) "«$text»" else text, style = T.Body.copy(color = C.blue), modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun LockedCard(hearts: Int, ru: Boolean) {
    Panel(Modifier.fillMaxWidth().padding(top = 8.dp), fill = C.paper2) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(w(ru, "? ? ?   откроется на %d ", "? ? ?   opens at %d ").format(hearts), style = T.Small.copy(color = C.muted))
            BufImage(FULL, Modifier, 2.dp)
        }
    }
}

/** Ten small hearts, as many filled as the friendship has (text, for plain lines). */
fun heartRow(h: Int): String = "♥".repeat(h.coerceIn(0, 10)) + "♡".repeat(10 - h.coerceIn(0, 10))

private val HEART = listOf(".##.##.", "#######", "#######", ".#####.", "..###..", "...#...")

/** One pixel heart, filled or an outline. */
private fun heartBuf(full: Boolean): app.greenstreet.ui.art.Buf {
    val b = app.greenstreet.ui.art.Buf(9, 8)
    val ink = 0xFF2B1A14.toInt(); val red = 0xFFD8402E.toInt(); val lit = 0xFFF58A6A.toInt(); val empty = 0xFFE6CFA0.toInt()
    for (y in HEART.indices) for (x in 0 until 7) if (HEART[y][x] == '#') b.p(x + 1, y + 1, if (!full) empty else if (x < 3 && y < 3) lit else red)
    for (y in 0 until 8) for (x in 0 until 9) if (b.get(x, y) == 0) {
        val n = listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1).any { (i, j) -> i in 0..8 && j in 0..7 && b.get(i, j) != 0 && b.get(i, j) != ink }
        if (n) b.p(x, y, ink)
    }
    return b
}
private val FULL by lazy { heartBuf(true) }
private val EMPTY by lazy { heartBuf(false) }

/** The friendship as ten pixel hearts. */
@Composable
fun Hearts(h: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        for (i in 0 until 10) BufImage(if (i < h) FULL else EMPTY, Modifier, 2.dp)
    }
}
