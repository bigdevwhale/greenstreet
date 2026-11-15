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
import androidx.compose.foundation.layout.width
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

    Text(when {
        own -> w(ru, "Сейчас: своя погода игры — по сезону, без интернета.", "Now: the game's own weather, by the season, with no internet.")
        town != null -> w(ru, "Сейчас: погода в городе ${town.name}", "Now: the weather in ${town.name}") + if (town.region.isNotBlank()) " (${town.region})." else "."
        else -> w(ru, "Сейчас: погода у вашего дома.", "Now: the weather at your home.")
    }, style = T.Body)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PixelButton(w(ru, "У дома", "At home"), { pick(null, false) }, Modifier.weight(1f), kind = if (!own && town == null && !searching) Kind.PRIMARY else Kind.GHOST)
        PixelButton(w(ru, "Город", "A town"), { searching = true }, Modifier.weight(1f), kind = if (!own && town != null || searching) Kind.PRIMARY else Kind.GHOST)
        PixelButton(w(ru, "Своя", "Own"), { pick(null, true) }, Modifier.weight(1f), kind = if (own && !searching) Kind.PRIMARY else Kind.GHOST)
    }
    if (searching) TownSearch(ru, town?.name.orEmpty()) { pick(it, false) }
    Text(w(ru,
        "Дождь поливает грядки, мороз и снег меняют район. Погоду дают MET Norway и Open-Meteo: туда уходят только округлённые координаты места, без вас.",
        "Rain waters the beds, frost and snow change the district. The weather comes from MET Norway and Open-Meteo: only the place's rounded coordinates are sent, nothing about you."), style = T.Small)
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
