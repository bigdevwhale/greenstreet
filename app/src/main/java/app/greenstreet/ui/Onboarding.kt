package app.greenstreet.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.greenstreet.container
import app.greenstreet.domain.Sky
import app.greenstreet.ui.art.FacadeScene
import app.greenstreet.ui.art.G
import app.greenstreet.ui.art.Icons
import app.greenstreet.ui.art.Look
import app.greenstreet.ui.art.Moment
import app.greenstreet.ui.art.PixelView
import app.greenstreet.ui.art.R
import app.greenstreet.ui.art.Scene
import app.greenstreet.ui.art.Buf
import app.greenstreet.ui.art.YardLife
import app.greenstreet.ui.art.YardScene
import app.greenstreet.ui.art.bed
import app.greenstreet.ui.art.fbm
import app.greenstreet.ui.art.grassOf
import app.greenstreet.ui.art.hash
import app.greenstreet.ui.art.rp
import app.greenstreet.ui.art.walker
import app.greenstreet.ui.components.Kind
import app.greenstreet.ui.components.PixelButton
import app.greenstreet.ui.theme.C
import app.greenstreet.ui.theme.T
import app.greenstreet.work.Tracker
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** A strip of street for the welcome pages: a pavement, a lawn with three beds at different stages, the walker passing. */
private class VergeScene(val month: Int, val rain: Boolean) : Scene(128, 64) {
    override val key get() = listOf(month, rain)
    override fun base(b: Buf) {
        val grass = grassOf(month)
        for (y in 0 until h) for (x in 0 until w) {
            val c = when {
                y < 18 -> rp(G.granite, .7 + (fbm(x * .3, y * .3) - .5) * .1 - if (x % 6 == 0 || y % 4 == 0) .1 else 0.0, x, y)
                y < 20 -> rp(G.granite, .85, x, y)
                else -> rp(grass, .46 + (fbm(x * .07, y * .07) - .5) * .3 + if (hash(x, y) < .035) .2 else 0.0, x, y)
            }
            b.p(x, y, c)
        }
    }

    override fun anim(b: Buf, t: Int) {
        val c = LocalContextHolder.content ?: return
        val kinds = listOf("radish_breakfast", "calendula_orange", "sunflower_giant")
        kinds.forEachIndexed { i, k ->
            val v = c.variety[k] ?: return@forEachIndexed
            b.bed(24 + i * 38, 40, v, v.days * listOf(.3, .7, 1.0)[i], i == 2, (t / 20) % 2 == 0 || rain, false, t)
        }
        val x = (t * 2) % (w + 20) - 10
        b.walker(x, 16, (t / 2) % 2, false)
        if (rain) for (i in 0 until 50) { val rx = ((hash(i, 1) * w + t * 2) % w).toInt(); val ry = ((hash(i, 2) * h + t * 6) % h).toInt(); b.p(rx, ry, R.glass[5], .45f) }
    }
}

/** The content for the little scenes above (they have no context of their own). */
object LocalContextHolder { var content: app.greenstreet.data.Content? = null }

@Composable
fun Onboarding() {
    val ctx = LocalContext.current
    val c = ctx.container
    LocalContextHolder.content = c.content
    val ru = ru()
    var page by remember { mutableIntStateOf(0) }
    var prologue by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
    if (prologue) { Prologue { prologue = false }; return }
    val here by c.here.collectAsState()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var why by remember { mutableStateOf("") }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        granted = r[Manifest.permission.ACCESS_FINE_LOCATION] == true || r[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) Tracker.of(ctx).start()
    }
    LaunchedEffect(granted) { if (granted) Tracker.of(ctx).start() }
    val now = LocalDateTime.now()
    val m = Moment(now.hour * 60 + now.minute, Sky(sun = true), now.monthValue, now.toLocalDate().toEpochDay())
    val pages = 6

    Column(Modifier.fillMaxSize().background(C.paper).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            when (page) {
                0 -> {
                    Text(w(ru, "Зелёная улица", "Green Street"), style = T.Display.copy(color = C.green))
                    Spacer(Modifier.height(10.dp))
                    PixelView(remember { VergeScene(now.monthValue, false) })
                    Spacer(Modifier.height(12.dp))
                    Text(w(ru,
                        "Ваш район — ваш огород. Улицы, дома, парки и вода нарисованы по настоящей карте вокруг вашего дома.\n\nЗдесь не нужно никуда специально ходить. Вы идёте в магазин, к остановке, через парк — а по дороге растут ваши грядки.",
                        "Your neighbourhood is your garden. The streets, houses, parks and water are drawn from the real map around your home.\n\nYou don't need to go anywhere special. You walk to the shop, to the stop, through the park — and your beds grow along the way."), style = T.Body)
                }
                1 -> {
                    Text(w(ru, "Грядки у дорог", "Beds along the way"), style = T.Title)
                    Spacer(Modifier.height(10.dp))
                    PixelView(remember { VergeScene(now.monthValue, true) })
                    Spacer(Modifier.height(12.dp))
                    Text(w(ru,
                        "· Сеять можно на газонах у тротуаров и парковых дорожек — встаньте рядом и выберите семена.\n· Прошли мимо грядки — значит, полили. Дождь польёт сам.\n· У каждого сорта свой срок и свои месяцы посева.\n· Три дня без присмотра — грядка дичает. Урожай будет меньше, но не пропадёт.\n· Обещают заморозок — проведайте грядки накануне, и они укрыты.",
                        "· Sow on lawns by pavements and park paths — stand next to one and pick the seeds.\n· Walk past a bed and it's watered. Rain does it too.\n· Every variety has its own time to ripen and its months to sow.\n· Three days without care and a bed goes wild. The harvest is smaller but not lost.\n· Frost forecast? Look in on your beds the day before and they're covered."), style = T.Body)
                }
                2 -> {
                    Text(w(ru, "Соседи", "Neighbours"), style = T.Title)
                    Spacer(Modifier.height(10.dp))
                    PixelView(remember {
                        FacadeScene("bakery", Icons.bakery, G.ochre, G.plum, R.red,
                            Look.of(mapOf("skin" to "2", "hair" to "wood", "cloth" to "cream", "hat" to "toque", "beard" to "1", "apron" to "1")), m.copy(minute = 11 * 60), true)
                    })
                    Spacer(Modifier.height(12.dp))
                    Text(w(ru,
                        "У каждого места есть хозяин: пекарь в булочной, травница в аптеке, смотритель на станции, библиотекарь, торговка на рынке — восемнадцать человек. Они живут в настоящих местах вашего района, вывески на карте подписаны.\n\nЗайдите — дадут семена. Потом попросят о помощи, станут писать записки, и за три года вы узнаете их истории.",
                        "Every place has its keeper: the baker, the herbalist at the pharmacy, the station keeper, the librarian, the market trader — eighteen people. They live in real places of your neighbourhood, and the signs on the map are labelled.\n\nDrop in and they'll give you seeds. Later they'll ask for help and write you notes, and over three years you'll learn their stories."), style = T.Body)
                }
                3 -> {
                    Text(w(ru, "Ваш двор", "Your yard"), style = T.Title)
                    Spacer(Modifier.height(10.dp))
                    PixelView(remember { YardScene(m, YardLife(seed = 3, weekday = 6, kind = "well")) })
                    Spacer(Modifier.height(12.dp))
                    Text(w(ru,
                        "Дома — куры, кухня, погреб и подоконник для зимних посадок. Двор рисуется по вашему дому: старый городской, панельный или свой участок в деревне.\n\nНа второй год появятся ульи и балкон-теплица, на третий — общий сад с соседями.",
                        "At home: hens, a kitchen, a cellar and a windowsill for winter sowing. The yard is drawn after your home: an old city house, a block of flats or a plot of your own in a village.\n\nIn the second year come beehives and a balcony greenhouse, in the third a garden shared with the neighbours."), style = T.Body)
                }
                4 -> {
                    Text(w(ru, "Что нужно игре", "What the game needs"), style = T.Title)
                    Spacer(Modifier.height(12.dp))
                    Text(w(ru,
                        "Геолокация — чтобы знать, мимо каких грядок вы прошли. Пока приложение открыто, оно слушает GPS само. Если нажать «Выйти на прогулку», будет слушать и в кармане, с тихим уведомлением, пока вы не закончите.\n\nУведомления — для прогулки и чтобы сказать, что созрел урожай.\n\nКоординаты не уходят никуда, кроме запросов карты (OpenStreetMap) и погоды (MET Norway) для вашего района.",
                        "Location — to know which beds you walk past. While the app is open it listens to the GPS itself. Tap 'Go for a walk' and it keeps listening with the phone in your pocket, with a quiet notification, until you're done.\n\nNotifications — for the walk and to say a harvest is ripe.\n\nYour location goes nowhere except the map (OpenStreetMap) and weather (MET Norway) requests for your area."), style = T.Body)
                    Spacer(Modifier.height(14.dp))
                    if (granted) Text(w(ru, "Геолокация разрешена.", "Location is allowed."), style = T.Body.copy(color = C.green))
                    else PixelButton(w(ru, "Разрешить", "Allow"), {
                        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
                        ask.launch(perms.toTypedArray())
                    }, Modifier.fillMaxWidth(), Kind.LAMP)
                }
                else -> {
                    Text(w(ru, "Где ваш дом?", "Where is home?"), style = T.Title)
                    Spacer(Modifier.height(12.dp))
                    Text(w(ru,
                        "Встаньте у своей парадной или калитки и нажмите кнопку. Вокруг нарисуется район — примерно полтора на полтора километра. Это займёт меньше минуты.",
                        "Stand by your front door or gate and tap the button. The neighbourhood will be drawn around it — about a mile across. It takes less than a minute."), style = T.Body)
                    Spacer(Modifier.height(16.dp))
                    when {
                        !granted -> Text(w(ru, "Сначала разрешите геолокацию — на прошлой странице.", "Allow location first — on the previous page."), style = T.Body.copy(color = C.red))
                        busy -> Loading()
                        here == null -> Text(w(ru, "Жду, пока телефон найдёт, где вы… На улице это быстрее.", "Waiting for the phone to find where you are… It's quicker outdoors."), style = T.Body)
                        else -> PixelButton(w(ru, "Здесь мой дом", "My home is here"), {
                            busy = true; error = false
                            scope.launch { runCatching { c.setHome(here!!) }.onFailure { error = true; why = it.message ?: it.javaClass.simpleName }; busy = false; c.wake() }
                        }, Modifier.fillMaxWidth(), Kind.LAMP)
                    }
                    if (error) {
                        Text(w(ru, "Сервер карты не ответил вовремя — так бывает, он бесплатный и иногда занят. Нажмите ещё раз: попробуем другой.", "The map server didn't answer in time — it's free and sometimes busy. Tap again: we'll try another one."), style = T.Small.copy(color = C.red), modifier = Modifier.padding(top = 8.dp))
                        if (why.isNotBlank()) Text(why, style = T.Small, modifier = Modifier.padding(top = 2.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(w(ru, "Карта © участники OpenStreetMap. Погода — MET Norway.", "Map © OpenStreetMap contributors. Weather by MET Norway."), style = T.Small)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (page > 0) PixelButton(w(ru, "Назад", "Back"), { page-- }, kind = Kind.GHOST) else Spacer(Modifier.size(1.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                for (i in 0 until pages) Box(Modifier.padding(3.dp).size(8.dp).background(if (i == page) C.green else C.paper2))
            }
            if (page < pages - 1) PixelButton(w(ru, "Дальше", "Next"), { page++ }) else Spacer(Modifier.size(1.dp))
        }
    }
}
