package app.greenstreet.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.greenstreet.container
import app.greenstreet.ui.components.Kind
import app.greenstreet.ui.components.PixelButton
import app.greenstreet.ui.theme.C
import app.greenstreet.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Carrying the game to another phone: save it to a file (Settings), and take it from one (Settings, or the first
 * screens on a new phone). The file holds the map too, so home, the beds and the neighbours stay where they were.
 */
@Composable
fun Transfer(canSave: Boolean) {
    val ru = ru()
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    /** A file picked to load, waiting for the player to agree to lose the game on this phone. */
    var pending by remember { mutableStateOf<Uri?>(null) }

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true; note = null
        scope.launch {
            val ok = runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = c.exportSave() ?: error("no game")
                    ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(bytes) }
                }
            }.isSuccess
            note = if (ok) w(ru, "Готово. Перенесите файл на другой телефон и загрузите его там.", "Done. Move the file to the other phone and load it there.") to true
            else w(ru, "Не удалось сохранить файл.", "Couldn't save the file.") to false
            busy = false
        }
    }
    fun load(uri: Uri) {
        busy = true; note = null
        scope.launch {
            val r = runCatching {
                val bytes = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() } }
                c.importSave(bytes)
            }
            note = if (r.isSuccess) w(ru, "Прогресс перенесён.", "Your progress is here.") to true
            else when (r.exceptionOrNull()) {
                is IllegalArgumentException -> w(ru, "Это не файл «Зелёной улицы» или он повреждён.", "This isn't a Green Street file, or it's damaged.") to false
                else -> w(ru, "Не удалось загрузить: карта не скачалась. Попробуйте ещё раз.", "Couldn't load it: the map didn't download. Try again.") to false
            }
            busy = false
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (canSave) pending = uri else load(uri)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            busy -> Loading()
            pending != null -> {
                Text(w(ru, "Игра на этом телефоне заменится игрой из файла, и нынешний прогресс пропадёт. Продолжить?",
                    "The game on this phone will be replaced by the one in the file, and the progress here will be lost. Go on?"), style = T.Small.copy(color = C.red))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton(w(ru, "Заменить", "Replace"), { val u = pending!!; pending = null; load(u) }, Modifier.weight(1f), Kind.RED)
                    PixelButton(w(ru, "Отмена", "Cancel"), { pending = null }, Modifier.weight(1f), Kind.GHOST)
                }
            }
            else -> {
                if (canSave) PixelButton(w(ru, "Сохранить прогресс в файл", "Save progress to a file"), {
                    save.launch("greenstreet-${java.time.LocalDate.now()}.zip")
                }, Modifier.fillMaxWidth())
                PixelButton(w(ru, "Загрузить прогресс из файла", "Load progress from a file"), {
                    open.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                }, Modifier.fillMaxWidth(), Kind.GHOST)
            }
        }
        note?.let { (t, ok) -> Text(t, style = T.Small.copy(color = if (ok) C.green else C.red)) }
    }
}
