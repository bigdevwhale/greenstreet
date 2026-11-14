package app.greenstreet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.greenstreet.R

/** A tidy pixel game: a parchment page, cream cards on a soft two-tone shadow, warm brown ink, one terracotta accent. */
@Immutable
object C {
    val bg = Color(0xFF2B2420)
    val paper = Color(0xFFF6EAD2)
    val paper2 = Color(0xFFEEDDBA)
    val light = Color(0xFFFCF4E2)
    val ink = Color(0xFF3B2A1E)
    val muted = Color(0xFF8A6E52)
    val faint = Color(0xFFB79E7E)
    val green = Color(0xFF557A45)
    val greenDeep = Color(0xFF2F4430)
    val greenLight = Color(0xFF86A26F)
    val red = Color(0xFFC0473A)
    val redDeep = Color(0xFF8E2F25)
    val ochre = Color(0xFFD99A4E)
    val ochreDeep = Color(0xFFA9712F)
    val lamp = Color(0xFFF5DC9C)
    val lampDeep = Color(0xFFC9A04E)
    val blue = Color(0xFF34497A)
    val telegram = Color(0xFFF6DC84)
    val night = Color(0xFF151D34)

    /** The one accent: the main button, the picked tab, the badge. */
    val accent = Color(0xFFC4553C)
    val accentDeep = Color(0xFF8E3524)
    val accentSoft = Color(0xFFF4D9C9)
    /** Card edges, the solid shadow under cards, the sunk-in place of a picked tab. */
    val edge = Color(0xFFE0C79C)
    val under = Color(0xFFE2CDA4)
    val inset = Color(0xFFEBD8B2)
    val shadow = Color(0xFF6B4A2E)
}

/**
 * Every text in the app: m6x11 by Daniel Linssen with Cyrillic by Natto Games (free with attribution).
 * One pixel family; sizes and colour set the hierarchy.
 */
val Pixel = FontFamily(Font(R.font.m6x11, FontWeight.Normal))

object T {
    val Display = TextStyle(fontFamily = Pixel, fontSize = 32.sp, lineHeight = 32.sp, color = C.ink)
    val Title = TextStyle(fontFamily = Pixel, fontSize = 28.sp, lineHeight = 28.sp, color = C.ink)
    val Heading = TextStyle(fontFamily = Pixel, fontSize = 24.sp, lineHeight = 25.sp, color = C.ink)
    val Eyebrow = TextStyle(fontFamily = Pixel, fontSize = 16.sp, lineHeight = 17.sp, letterSpacing = 0.04.em, color = C.muted)
    val Body = TextStyle(fontFamily = Pixel, fontSize = 20.sp, lineHeight = 22.sp, color = C.ink)
    val Letter = TextStyle(fontFamily = Pixel, fontSize = 21.sp, lineHeight = 24.sp, color = C.ink)
    val Sub = TextStyle(fontFamily = Pixel, fontSize = 18.sp, lineHeight = 19.sp, color = C.muted)
    val Small = TextStyle(fontFamily = Pixel, fontSize = 17.sp, lineHeight = 18.sp, color = C.muted)
    val Number = TextStyle(fontFamily = Pixel, fontSize = 28.sp, lineHeight = 26.sp, color = C.ink)
    val Button = TextStyle(fontFamily = Pixel, fontSize = 22.sp, lineHeight = 22.sp)
    val Hand = TextStyle(fontFamily = Pixel, fontSize = 21.sp, lineHeight = 22.sp, color = C.blue)
}

@Composable
fun GreenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.green, onPrimary = C.light, background = C.paper, surface = C.paper, onSurface = C.ink,
            onBackground = C.ink, surfaceContainer = C.paper, surfaceContainerHigh = C.paper, surfaceContainerHighest = C.paper2,
            outline = C.ink, error = C.red,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
