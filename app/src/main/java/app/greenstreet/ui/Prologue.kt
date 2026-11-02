package app.greenstreet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.greenstreet.ui.art.Buf
import app.greenstreet.ui.art.G
import app.greenstreet.ui.art.R
import app.greenstreet.ui.art.Scene
import app.greenstreet.ui.art.PixelView
import app.greenstreet.ui.art.ball
import app.greenstreet.ui.art.fbm
import app.greenstreet.ui.art.hash
import app.greenstreet.ui.art.line
import app.greenstreet.ui.art.mix
import app.greenstreet.ui.art.rgb
import app.greenstreet.ui.art.rp
import app.greenstreet.ui.components.Kind
import app.greenstreet.ui.components.Panel
import app.greenstreet.ui.components.PixelButton
import app.greenstreet.ui.theme.C
import app.greenstreet.ui.theme.T

private fun Buf.rr(x: Int, y: Int, w: Int, h: Int, r: IntArray, v: (Int, Int) -> Double) { for (j in 0 until h) for (i in 0 until w) p(x + i, y + j, rp(r, v(i, j), x + i, y + j)) }

/** Late night at a desk: a laptop's cold glow, a dead pot plant, rain on the window over a city that never goes dark. */
private class DeskScene : Scene(128, 80) {
    override val key get() = "desk"
    override fun base(b: Buf) {
        // the window: a night city of lit squares
        b.rr(0, 0, w, 56, R.night) { _, y -> .12 + y / 56.0 * .25 }
        for (k in 0 until 9) {
            val bx = k * 15 - 4; val bh = 18 + (hash(k, 4) * 26).toInt()
            b.rr(bx, 56 - bh, 14, bh, R.night) { _, _ -> .32 }
            for (j in 0 until bh / 4) for (i in 0 until 3) if (hash(k * 31 + i, j) < .35) b.p(bx + 2 + i * 4, 58 - bh + j * 4, G.WARM)
        }
        b.r(0, 0, w, 2, R.wood[2]); b.r(62, 0, 3, 56, R.wood[2]); b.r(0, 54, w, 3, R.wood[3])
        // the desk
        b.rr(0, 57, w, 23, R.wood) { x, y -> .42 + (fbm(x * .1, y * .4) - .5) * .12 - y * .006 }
        // the laptop
        b.rr(38, 36, 44, 24, R.iron) { _, _ -> .35 }
        b.rr(40, 38, 40, 20, R.glass) { i, j -> .55 + (if (j % 4 == 0 && i < 30 - j) .25 else 0.0) }
        for (j in 0 until 5) b.r(43, 41 + j * 3, 18 + (hash(j, 2) * 14).toInt(), 1, R.glass[2])
        b.rr(34, 60, 52, 4, R.iron) { i, _ -> .55 - i * .002 }
        // a mug gone cold, and a pot plant long dead
        b.rr(94, 54, 8, 9, R.cream) { i, _ -> .8 - i * .04 }; b.r(102, 56, 2, 4, R.cream[3]); b.r(95, 54, 6, 1, R.wood[1])
        b.rr(12, 54, 10, 9, R.brick) { i, _ -> .6 - i * .04 }
        b.line(17, 54, 13, 44, G.oak[2]); b.line(17, 54, 21, 46, G.oak[2]); b.p(13, 44, G.oak[3]); b.p(21, 46, G.oak[4]); b.p(15, 48, G.oak[3])
        // the cold light of the screen on the desk
        for (j in 56 until 72) for (i in 26 until 96) { val d = Math.hypot((i - 60) / 36.0, (j - 56) / 14.0); if (d < 1) b.p(i, j, mix(b.get(i, j), R.glass[4], ((1 - d) * .35).toFloat())) }
    }
    override fun anim(b: Buf, t: Int) {
        for (i in 0 until 40) { val x = ((hash(i, 1) * 62 + t) % 62).toInt(); val y = ((hash(i, 2) * 54 + t * 4) % 54).toInt(); b.p(x, y, R.glass[5], .35f); b.p(x + 66, (y + 17) % 54, R.glass[5], .3f) }
        if ((t / 5) % 2 == 0) b.r(43 + 18, 53, 2, 2, R.glass[5])   // the cursor blinks
    }
}

/** The envelope, open on the kitchen table: a tin of seeds, a key on a string, a pressed calendula. */
private class LetterScene : Scene(128, 80) {
    override val key get() = "letter"
    override fun base(b: Buf) {
        b.rr(0, 0, w, h, R.wood) { x, y -> .5 + (fbm(x * .08, y * .5) - .5) * .18 + if (y % 13 == 0) -.12 else 0.0 }
        // the oilcloth corner
        for (y in 0 until 30) for (x in 0 until 40 - y) b.p(x, y, if (((x / 4) + (y / 4)) % 2 == 0) R.red[3] else R.cream[4])
        // the envelope with a stamp and a postmark
        b.rr(46, 22, 58, 36, R.paper) { i, j -> .8 - j * .004 + if (i < 2) -.15 else 0.0 }
        for (i in 0 until 29) { b.p(46 + i, 22 + i / 2, R.paper[2]); b.p(103 - i, 22 + i / 2, R.paper[2]) }
        b.rr(92, 25, 9, 11, R.roofB) { _, _ -> .6 }; b.rr(93, 26, 7, 9, R.green) { _, j -> .55 + j * .02 }; b.ball(96, 30, 1, R.honey, null)
        for (k in 0 until 3) b.line(84, 27 + k * 3, 92, 26 + k * 3, R.iron[3], .5f)
        for (k in 0 until 4) b.line(52, 46 + k * 3, 80 - k * 4, 46 + k * 3, G.navy[3], .7f)
        // the tin of seeds with paper bags inside
        b.rr(8, 44, 32, 24, R.iron) { i, j -> .45 + (if (j < 2) .2 else 0.0) - i * .004 }
        for (k in 0 until 4) { val px = 11 + k * 7; b.rr(px, 40 + (k % 2) * 2, 6, 10, R.paper) { _, _ -> .75 }; b.r(px + 1, 42 + (k % 2) * 2, 4, 1, G.navy[3]); b.ball(px + 3, 46 + (k % 2) * 2, 1, listOf(R.autumn, R.green, R.jam, R.honey)[k], null) }
        // the key on a string with a cardboard tag
        b.ball(112, 64, 2, R.brass, R.INK); b.r(114, 64, 8, 1, R.brass[3]); b.r(120, 64, 1, 3, R.brass[3])
        b.line(110, 62, 100, 56, R.cream[3]); b.rr(92, 60, 9, 6, R.paper) { _, _ -> .8 }; b.r(93, 62, 6, 1, G.navy[3])
        // a pressed calendula
        b.line(58, 70, 70, 62, R.green[3]); for ((dx, dy) in listOf(0 to -2, -2 to 0, 2 to 0, 0 to 2, -1 to -1, 1 to 1, 1 to -1, -1 to 1)) b.p(71 + dx, 61 + dy, R.autumn[4]); b.p(71, 61, R.autumn[1])
    }
    override fun anim(b: Buf, t: Int) { if ((t / 7) % 3 == 0) b.p(31, 66, R.cream[5]) }
}

@Composable
fun Prologue(done: () -> Unit) {
    val ru = ru()
    var page by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(Color(0xFF1B2433)).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            when (page) {
                0 -> {
                    PixelView(remember { DeskScene() })
                    Spacer(Modifier.height(16.dp))
                    Text(w(ru,
                        "Город, работа, экран до ночи. Дорога от дома до метро — с телефоном перед глазами.\n\nВы уже не помните, когда в последний раз видели, как что-то растёт.",
                        "The city, work, a screen until late. The way from home to the station — eyes on the phone.\n\nYou can't remember the last time you watched something grow."), style = T.Letter.copy(color = C.paper))
                }
                1 -> {
                    PixelView(remember { LetterScene() })
                    Spacer(Modifier.height(16.dp))
                    Text(w(ru,
                        "Однажды в почтовом ящике — толстый конверт. Почерк знакомый с детства: двоюродная бабушка, тётя Клава.\n\nВнутри письмо, ключ на верёвочке и старая жестянка из-под монпансье, полная бумажных пакетиков.",
                        "One day there's a fat envelope in the letterbox. Handwriting you've known since childhood: your great-aunt, Aunt Klava.\n\nInside: a letter, a key on a string and an old sweet tin full of paper packets."), style = T.Letter.copy(color = C.paper))
                }
                else -> Panel(Modifier.fillMaxWidth(), fill = C.light) {
                    Text(w(ru,
                        "Здравствуй, моя радость!\n\nПишу уже из деревни — перебралась к сестре, вдвоём веселее. А дом, кур и двор оставляю тебе. Ключ от курятника в конверте, остальное покажет Семёныч.\n\nСорок лет я сажала вдоль нашей улицы что-нибудь живое: календулу у остановки, укроп у аптеки, подсолнухи у школьного забора. Соседи думали, что всё растёт само. Пусть думают.\n\nЯ знаю, ты много работаешь и мало гуляешь. Сделай мне одолжение: посади хоть одну грядку у дороги, по которой ходишь каждый день, и проходи мимо почаще. Остальное улица сделает сама — увидишь, она умеет.\n\nВ жестянке мои семена, подписаны как умела.\n\nОбнимаю,\nтвоя тётя Клава\n\nP. S. Пряника колбасой не корми, что бы он тебе ни говорил.",
                        "Hello, my dear!\n\nI'm writing from the village already — I've moved in with my sister, it's merrier for two. The home, the hens and the yard are yours now. The hen-house key is in the envelope; Semyonych will show you the rest.\n\nFor forty years I planted something living along our street: calendula by the tram stop, dill by the pharmacy, sunflowers along the school fence. The neighbours thought it all grew by itself. Let them.\n\nI know you work a lot and walk little. Do me a favour: plant just one bed by a road you take every day, and pass by it often. The street will do the rest — you'll see, it knows how.\n\nMy seeds are in the tin, labelled as best I could.\n\nA big hug,\nyour Aunt Klava\n\nP.S. Don't feed Pryanik sausage, whatever he tells you."), style = T.Hand)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            PixelButton(w(ru, "Пропустить", "Skip"), done, kind = Kind.GHOST)
            Spacer(Modifier.weight(1f))
            PixelButton(if (page < 2) w(ru, "Дальше", "Next") else w(ru, "Открыть дверь", "Open the door"), { if (page < 2) page++ else done() }, kind = if (page < 2) Kind.PRIMARY else Kind.LAMP)
        }
    }
}
