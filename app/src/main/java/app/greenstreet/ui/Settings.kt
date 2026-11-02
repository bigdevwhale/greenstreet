package app.greenstreet.ui

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.greenstreet.ui.art.Buf
import app.greenstreet.ui.components.Kind
import app.greenstreet.ui.components.Panel
import app.greenstreet.ui.components.PixelButton
import app.greenstreet.ui.theme.C
import app.greenstreet.ui.theme.T

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "greenstreet"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
}

/** A small brass gear for the top line. */
val gearIcon: Buf by lazy {
    val b = Buf(11, 11)
    val ink = 0xFF2B1A14.toInt(); val brass = 0xFFF0C24A.toInt(); val lit = 0xFFFCE9A0.toInt()
    for (y in 0 until 11) for (x in 0 until 11) {
        val dx = x - 5.0; val dy = y - 5.0; val r = Math.hypot(dx, dy)
        val a = Math.atan2(dy, dx)
        val tooth = Math.cos(a * 8) > .25
        if (r <= 2.6 && r > 1.2 || r <= 3.6 && r > 1.2 || (tooth && r <= 5.2 && r > 1.2)) b.p(x, y, if (dx + dy < -1) lit else brass)
    }
    for (y in 0 until 11) for (x in 0 until 11) if (b.get(x, y) == 0) {
        val n = listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1).any { (i, j) -> i in 0..10 && j in 0..10 && b.get(i, j).let { it == brass || it == lit } }
        if (n) b.p(x, y, ink)
    }
    b
}

@Composable
fun SettingsView() {
    val ru = ru()
    val ctx = LocalContext.current
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(w(ru, "Настройки", "Settings"), style = T.Title)

        Text(w(ru, "язык", "language"), style = T.Eyebrow)
        val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((tag, label) in listOf("" to w(ru, "Как в системе", "System"), "ru" to "Русский", "en" to "English")) {
                PixelButton(label, {
                    AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
                }, Modifier.weight(1f), kind = if (cur == tag) Kind.PRIMARY else Kind.GHOST)
            }
        }

        Text(w(ru, "сообщество", "community"), style = T.Eyebrow, modifier = Modifier.padding(top = 6.dp))
        Text(w(ru, "Игра делается вместе с теми, кто в неё играет. Всё идёт через бота StillCraft в Telegram.",
            "The game is made together with the people who play it. It all goes through the StillCraft bot on Telegram."), style = T.Small)
        for ((icon, label, url) in listOf(
            Triple("🐞", w(ru, "Сообщить об ошибке", "Report a bug"), Community.BUG),
            Triple("💡", w(ru, "Предложить идею", "Suggest an idea"), Community.IDEA),
            Triple("🔔", w(ru, "Подписаться на обновления", "Follow updates"), Community.FOLLOW),
            Triple("💬", w(ru, "Чат StillCraft", "StillCraft chat"), Community.CHAT),
        )) {
            Panel(Modifier.fillMaxWidth(), fill = C.paper, onClick = { open(url) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, style = T.Heading, modifier = Modifier.width(34.dp))
                    Text(label, style = T.Body, modifier = Modifier.weight(1f))
                    Text("›", style = T.Heading.copy(color = C.green))
                }
            }
        }

        Text(w(ru, "об игре", "about"), style = T.Eyebrow, modifier = Modifier.padding(top = 6.dp))
        Text(w(ru,
            "Карта района — © участники OpenStreetMap, тайлы OpenFreeMap. Погода — MET Norway (yr.no).\nРисунки, тексты и соседи сделаны для «Зелёной улицы».",
            "District map © OpenStreetMap contributors, tiles by OpenFreeMap. Weather by MET Norway (yr.no).\nPictures, texts and neighbours were made for Green Street."), style = T.Small)
    }
}
