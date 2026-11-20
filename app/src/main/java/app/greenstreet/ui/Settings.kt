package app.greenstreet.ui

import app.greenstreet.domain.metres
import app.greenstreet.container
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
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
    /** Where to chip in: the game is free and has no ads. */
    const val SUPPORT = "https://bigdevwhale.github.io/stillcraft/support"
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

        val c = ctx.container
        val ver by c.version.collectAsState()
        ver.let { }
        val s = c.state
        if (s != null) {
            // Home where the player stands, if the start put it down the street (#45, #46): only on a sure fix.
            Text(w(ru, "дом", "home"), style = T.Eyebrow, modifier = Modifier.padding(top = 6.dp))
            val here by c.here.collectAsState()
            val acc by c.hereAcc.collectAsState()
            val scope = rememberCoroutineScope()
            var moving by remember { mutableStateOf(false) }
            val off = here?.let { h -> s.home?.metres(h)?.toInt() }
            Text(when {
                here == null -> w(ru, "Ищу, где вы: включите геолокацию.", "Finding where you are: turn location on.")
                acc > app.greenstreet.WEAK_GPS -> w(ru, "Слабый сигнал GPS (±%d м): дом встанет не туда. Подойдите к окну и подождите немного.", "Weak GPS signal (±%d m): home would land in the wrong place. Go to a window and wait a little.").format(acc.toInt())
                off != null && off <= 40 -> w(ru, "Вы дома.", "You're home.")
                off != null -> w(ru, "Дом в %d м от вас. Если вы сейчас дома, а игра думает иначе, перенесите его сюда: соседи, грядки и прогресс останутся.", "Home is %d m away. If you're home now and the game thinks otherwise, move it here: neighbours, beds and progress stay.").format(off)
                else -> ""
            }, style = T.Small)
            if (here != null && acc <= app.greenstreet.WEAK_GPS && (off ?: 0) > 40) PixelButton(if (moving) w(ru, "Переношу…", "Moving…") else w(ru, "Я дома – перенести дом сюда", "I'm home – move home here"), {
                if (!moving) { moving = true; scope.launch { runCatching { c.moveHome(here!!) }; moving = false } }
            }, Modifier.fillMaxWidth())

            // The look of the courtyard (#43, #53): as the map shows, or one picked.
            Text(w(ru, "двор", "courtyard"), style = T.Eyebrow, modifier = Modifier.padding(top = 6.dp))
            val yard = s.flags.firstOrNull { it.startsWith("yard.") }?.removePrefix("yard.")
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((k, label) in listOf(null to w(ru, "Как на карте", "As on the map"), "house" to w(ru, "Деревенский дом", "Country house"), "block" to w(ru, "Панельный двор", "Block of flats"), "well" to w(ru, "Двор-колодец", "Courtyard well"))) {
                    PixelButton(label, {
                        s.flags.removeAll { it.startsWith("yard.") }; if (k != null) s.flags += "yard.$k"; c.changed()
                    }, kind = if (yard == k) Kind.PRIMARY else Kind.GHOST)
                }
            }

            // A pause: the street waits while you're away (#32).
            Text(w(ru, "пауза", "pause"), style = T.Eyebrow, modifier = Modifier.padding(top = 6.dp))
            val paused = "pause" in s.flags
            Text(if (paused) w(ru, "Игра на паузе: грядки не сохнут, соседи ждут. Снимите паузу, когда вернётесь.", "The game is paused: beds don't dry out, the neighbours wait. Lift the pause when you're back.")
                else w(ru, "Уезжаете или не выйдете гулять несколько дней? Поставьте игру на паузу: пока она стоит, дни на улице не идут.", "Going away or no walks for a few days? Pause the game: while it's paused, no days pass on the street."), style = T.Small)
            PixelButton(if (paused) w(ru, "Снять паузу", "Lift the pause") else w(ru, "Поставить на паузу", "Pause the game"), {
                if (paused) s.flags -= "pause" else { c.wake(); s.flags += "pause" }
                c.changed()
            }, Modifier.fillMaxWidth(), kind = if (paused) Kind.PRIMARY else Kind.GHOST)
        }

        Text(w(ru, "погода", "weather"), style = T.Eyebrow, modifier = Modifier.padding(top = 6.dp))
        WeatherPlace()

        Text(w(ru, "перенос прогресса", "moving to another phone"), style = T.Eyebrow, modifier = Modifier.padding(top = 6.dp))
        Text(w(ru, "Сохраните игру в файл и загрузите его на новом телефоне. Вместе с прогрессом переедет и карта района: дом, грядки и соседи окажутся на своих местах.",
            "Save the game to a file and load it on the new phone. The district map goes with it: home, the beds and the neighbours stay where they were."), style = T.Small)
        Transfer(canSave = true)

        Text(w(ru, "поддержать", "support"), style = T.Eyebrow, modifier = Modifier.padding(top = 6.dp))
        Text(w(ru, "Игра бесплатная, без рекламы и аккаунтов. Если она вам по душе — можно поддержать её разработку.",
            "The game is free, with no ads and no accounts. If you like it, you can support its making."), style = T.Small)
        PixelButton(w(ru, "♥ Поддержать игру", "♥ Support the game"), { open(Community.SUPPORT) }, Modifier.fillMaxWidth())

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
            "Карта района — © участники OpenStreetMap, тайлы OpenFreeMap. Погода — MET Norway (yr.no) и Open-Meteo.com (CC BY 4.0).\nРисунки, тексты и соседи сделаны для «Зелёной улицы».",
            "District map © OpenStreetMap contributors, tiles by OpenFreeMap. Weather by MET Norway (yr.no) and Open-Meteo.com (CC BY 4.0).\nPictures, texts and neighbours were made for Green Street."), style = T.Small)
    }
}
