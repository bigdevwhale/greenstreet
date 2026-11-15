package app.greenstreet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.greenstreet.container
import app.greenstreet.domain.Town
import app.greenstreet.domain.Weather
import app.greenstreet.ui.components.Kind
import app.greenstreet.ui.components.Panel
import app.greenstreet.ui.components.PixelButton
import app.greenstreet.ui.theme.C
import app.greenstreet.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Where the game's weather comes from: home, a town found by name (for one who lives in one place and walks
 * in another, or would rather not have home's), or the game's own, by the season. Settings and the welcome.
 */
@Composable
fun WeatherPlace() {
    val ru = ru()
    val c = LocalContext.current.container
    c.version.collectAsState().value
    val s = c.state
    val (town, own) = s?.let { it.town to it.ownSky } ?: c.pendingSky ?: (null to false)
    var searching by remember { mutableStateOf(false) }
    var redraw by remember { mutableStateOf(0) }
    redraw
    fun pick(t: Town?, o: Boolean) { c.setSky(t, o); searching = false; redraw++ }

    Text(w(ru, "Дождь в игре поливает грядки, мороз и снег меняют район. Какую погоду брать:",
        "In the game rain waters the beds, frost and snow change the district. Which weather to use:"), style = T.Small)
    Choice(ru, !own && town == null && !searching,
        w(ru, "Настоящая, у моего дома", "The real one, at my home"),
        w(ru, "Как за окном там, где вы поставили дом на карте.", "As it is outside where you put your home on the map.")) { pick(null, false) }
    Choice(ru, !own && town != null || searching,
        w(ru, "Настоящая, в другом городе", "The real one, in another town") + (town?.let { ": " + it.name } ?: ""),
        w(ru, "Если дом в игре в одном месте, а живёте вы в другом, или хочется погоды родного города.",
            "If the game's home is in one place and you live in another, or you'd like the weather of your home town.")) { searching = true }
    if (searching) TownSearch(ru, town?.name.orEmpty()) { pick(it, false) }
    Choice(ru, own && !searching,
        w(ru, "Придуманная игрой", "Made up by the game"),
        w(ru, "Обычная для этого времени года, без интернета. С настоящей не совпадает.",
            "Usual for the time of year, with no internet. It won't match the real one.")) { pick(null, true) }
    Text(w(ru,
        "Настоящую погоду дают MET Norway и Open-Meteo: туда уходят только округлённые координаты места.",
        "The real weather comes from MET Norway and Open-Meteo: only the place's rounded coordinates are sent."), style = T.Small)
}

/** One way to have the weather: a card with a round mark, a name and a line on what it means. */
@Composable
private fun Choice(ru: Boolean, on: Boolean, title: String, text: String, onClick: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), fill = if (on) C.accentSoft else C.light, onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 3.dp).size(18.dp).border(2.dp, if (on) C.accent else C.faint, CircleShape), contentAlignment = Alignment.Center) {
                if (on) Box(Modifier.size(8.dp).background(C.accent, CircleShape))
            }
            Column(Modifier.padding(start = 10.dp)) {
                Text(title, style = T.Body)
                Text(text, style = T.Small)
            }
        }
    }
}

/** A search for a town by name. The field starts with the town already chosen, so the choice doesn't look forgotten. */
@Composable
private fun TownSearch(ru: Boolean, current: String, onPicked: (Town) -> Unit) {
    val scope = rememberCoroutineScope()
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    var q by remember(current) { mutableStateOf(current) }
    var results by remember { mutableStateOf<List<Town>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    fun search() {
        if (q.isBlank() || busy) return
        focus.clearFocus()
        busy = true
        scope.launch {
            val r = runCatching { withContext(Dispatchers.IO) { Weather.search(q, if (ru) "ru" else "en") } }
            failed = r.isFailure; results = r.getOrNull(); busy = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // the field is the whole box, so a tap anywhere on it starts typing
            BasicTextField(q, { q = it }, Modifier.weight(1f).background(C.light).border(2.dp, C.edge), textStyle = T.Body, singleLine = true,
                cursorBrush = SolidColor(C.ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { search() }),
                decorationBox = { inner ->
                    Box(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                        if (q.isEmpty()) Text(w(ru, "Город", "Town"), style = T.Body.copy(color = C.faint))
                        inner()
                    }
                })
            Spacer(Modifier.width(8.dp))
            PixelButton(if (busy) "…" else w(ru, "Найти", "Find"), { search() }, enabled = !busy)
        }
        if (failed) Text(w(ru, "Не удалось связаться с сервисом погоды.", "Couldn't reach the weather service."), style = T.Small.copy(color = C.red))
        if (results?.isEmpty() == true) Text(w(ru, "Ничего не нашлось. Попробуйте написать иначе.", "Nothing found. Try another spelling."), style = T.Small)
        for (t in results.orEmpty()) Panel(Modifier.fillMaxWidth(), fill = C.paper2, onClick = { focus.clearFocus(); results = null; q = t.name; onPicked(t) }) {
            Text(t.name, style = T.Body)
            if (t.region.isNotBlank()) Text(t.region, style = T.Small)
        }
    }
}
