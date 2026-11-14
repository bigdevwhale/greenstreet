package app.greenstreet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.greenstreet.ui.art.Buf
import app.greenstreet.ui.art.toBitmap
import app.greenstreet.ui.theme.C
import app.greenstreet.ui.theme.T
import kotlin.math.roundToInt

/** A unit of spacing small enough to nudge things: a pressed button sinks by one. */
val PX: Dp = 2.dp

/** How round things are: cards, buttons and slots. */
val CardShape = RoundedCornerShape(18.dp)
val ButtonShape = RoundedCornerShape(14.dp)
val ChipShape = RoundedCornerShape(10.dp)

/**
 * A small flat plaque: a rounded fill, nothing else. (Once a pixel box with outline, bevel and lip; the parameters
 * stay so old call sites read the same, and are ignored.)
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.pixelBox(fill: Color, outline: Color = Color.Transparent, bevel: Color? = null, lip: Color? = null, pressed: Boolean = false): Modifier =
    this.background(fill, ChipShape).border(1.dp, C.shadow.copy(alpha = .25f), ChipShape)

/** A card: cream on a solid warm shadow a few dp below, a thin edge round it. */
fun Modifier.card(fill: Color = C.light, lifted: Boolean = true): Modifier = this
    .drawBehind {
        if (lifted) {
            val r = CardShape.createOutline(size, layoutDirection, this)
            translate(top = 3.dp.toPx()) { drawOutline(r, C.under) }
        }
    }
    .background(fill, CardShape)
    .border(1.5.dp, C.edge, CardShape)

/** A place for an item, a face or a packet, set a little into the card; [gold] marks the picked one with the accent. */
fun Modifier.slot(fill: Color = Color(0xFFF7E9CA), gold: Boolean = false): Modifier =
    this.background(if (gold) C.accentSoft else fill, ChipShape)
        .border(if (gold) 2.dp else 1.5.dp, if (gold) C.accent else C.edge, ChipShape)

/** The page every screen is laid on: parchment with a faint dotted weave. */
fun Modifier.parchment(): Modifier = this.drawBehind {
    drawRect(C.paper)
    val step = 14.dp.toPx(); val dot = 1.5.dp.toPx()
    var y = step / 2; var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) step / 2 else step
        while (x < size.width) { drawRect(C.under.copy(alpha = .55f), Offset(x, y), Size(dot, dot)); x += step * 2 }
        y += step; row++
    }
}

/** One of the menu's own painted icons (assets/art/ui), or nothing. */
@Composable
fun UiIcon(name: String, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    val b = remember(name) { app.greenstreet.ui.art.Pictures.buf("art/ui/$name.png") } ?: return
    BufImage(b, modifier, size / b.w)
}

/** A face in a circle with a cream ring; dimmed when out of reach, a small mark («z», «!») on the rim. */
@Composable
fun Avatar(face: Buf, size: Dp = 60.dp, dim: Boolean = false, mark: String? = null, onClick: (() -> Unit)? = null) {
    Box(Modifier.size(size)) {
        Box(Modifier.matchParentSize().clip(CircleShape).background(C.paper2).border(2.dp, C.edge, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), contentAlignment = Alignment.Center) {
            BufImage(face, Modifier.alpha(if (dim) .5f else 1f), size / face.w)
        }
        if (mark != null) Box(Modifier.align(Alignment.TopEnd).size(20.dp).background(if (mark == "!") C.accent else C.light, CircleShape).border(1.5.dp, C.edge, CircleShape), contentAlignment = Alignment.Center) {
            Text(mark, style = T.Small.copy(color = if (mark == "!") C.light else C.muted, fontSize = 16.sp, lineHeight = 16.sp))
        }
    }
}

/** One of several, picked: pills in a row, the picked one filled with the accent. */
@Composable
fun PlaqueTabs(names: List<String>, picked: Int, modifier: Modifier = Modifier, pick: (Int) -> Unit) {
    Row(modifier.fillMaxWidth().card().padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        names.forEachIndexed { i, n ->
            val on = i == picked
            Box(
                Modifier.weight(1f)
                    .then(if (on) Modifier.background(C.inset, ChipShape).border(1.5.dp, C.edge, ChipShape) else Modifier)
                    .clip(ChipShape).clickable { pick(i) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) { Text(n, style = T.Button.copy(fontSize = 19.sp, color = if (on) C.accent else C.muted), maxLines = 1) }
        }
    }
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    fill: Color = C.light,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier
            .card(if (pressed && onClick != null) C.paper2 else fill)
            .then(if (onClick != null) Modifier.clip(CardShape).clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

enum class Kind { PRIMARY, LAMP, GHOST, RED }

/** A small secondary action: a cream plaque with a picture in front of a short word. */
@Composable
fun ActionChip(label: String, onClick: () -> Unit, icon: String? = null, leading: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.card().clip(CardShape).clickable(onClick = onClick).padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) leading() else if (icon != null) UiIcon(icon, 26.dp)
        Text(label, style = T.Body, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
fun PixelButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: Kind = Kind.PRIMARY, enabled: Boolean = true) {
    // a face over a darker lip: pressed, the face sinks onto the lip
    val (fill, lip, ink) = when (kind) {
        Kind.PRIMARY -> Triple(C.accent, C.accentDeep, C.light)
        Kind.LAMP -> Triple(C.lamp, C.lampDeep, C.ink)
        Kind.GHOST -> Triple(C.light, C.edge, C.ink)
        Kind.RED -> Triple(C.red, C.redDeep, C.light)
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val a = if (enabled) 1f else .45f
    val depth = 4.dp
    val sink = pressed && enabled
    Box(
        modifier
            .drawBehind {
                val d = depth.toPx()
                val r = ButtonShape.createOutline(Size(size.width, size.height - d), layoutDirection, this)
                translate(top = d) { drawOutline(r, lip.copy(alpha = a)) }
                translate(top = if (sink) d else 0f) {
                    drawOutline(r, fill.copy(alpha = a))
                    drawOutline(r, C.shadow.copy(alpha = .22f * a), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                }
            }
            .clip(ButtonShape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(top = if (sink) depth else 0.dp, bottom = if (sink) 0.dp else depth)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = T.Button.copy(color = ink.copy(alpha = a)), textAlign = TextAlign.Center)
    }
}

/** A progress bar in rounded segments. */
@Composable
fun Segments(fraction: Float, modifier: Modifier = Modifier, n: Int = 10) {
    Row(modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        val on = (fraction.coerceIn(0f, 1f) * n).roundToInt()
        repeat(n) { i -> Box(Modifier.weight(1f).height(8.dp).background(if (i < on) C.accent else C.paper2, RoundedCornerShape(4.dp))) }
    }
}


@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = C.muted) =
    Text(text.uppercase(), style = T.Eyebrow.copy(color = color), modifier = modifier)

/** A still pixel picture (an item, a stamp, an icon), scaled with hard pixels. */
@Composable
fun BufImage(buf: Buf, modifier: Modifier = Modifier, scale: Dp = 2.dp) {
    val image = remember(buf) { buf.toBitmap().asImageBitmap() }
    Canvas(modifier.size(scale * buf.w, scale * buf.h)) {
        drawImage(image, srcOffset = IntOffset.Zero, srcSize = IntSize(buf.w, buf.h), dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.None)
    }
}

/** A title bar with a back arrow drawn in pixels. */
@Composable
fun TopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, dark: Boolean = false) {
    Row(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(20.dp)) {
                val p = size.width / 10
                val col = if (dark) C.light else C.ink
                // "<": the tip on the left, two arms going right
                for (i in 0 until 5) {
                    drawRect(col, Offset((1 + i) * p, (5 - i) * p - p / 2), Size(p * 2, p))
                    drawRect(col, Offset((1 + i) * p, (4 + i) * p + p / 2), Size(p * 2, p))
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(title, style = T.Title.copy(color = if (dark) C.light else C.ink))
    }
}

@Composable
fun Spacer8() = Spacer(Modifier.height(8.dp))

@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.card().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(value, style = T.Number)
        Text(label, style = T.Small)
    }
}

/** A small tag, like «срочно». */
@Composable
fun Tag(text: String, color: Color = C.red) {
    Text(text, style = T.Small.copy(color = C.light), modifier = Modifier.background(color, ChipShape).padding(horizontal = 6.dp, vertical = 2.dp))
}
