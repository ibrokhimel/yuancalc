package uz.yuancalc.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The only file allowed to contain color literals. Every screen and component
 * reads from [Palette] or [MaterialTheme.colorScheme], so retheming the app is
 * an edit to this file alone.
 */
object Palette {
    /** App background — near-black ink, never pure #000. */
    val Ink = Color(0xFF0A0A0C)
    /** Card surface, one step above the background. */
    val Surface = Color(0xFF141418)
    /** Inputs, pills and pressed states, one step above cards. */
    val SurfaceHigh = Color(0xFF1D1D23)
    /** 1dp borders separating surfaces instead of shadows. */
    val Hairline = Color(0xFF26262C)
    val TextHi = Color(0xFFF4F4F6)
    val TextMid = Color(0xFF9A9AA3)
    val TextLo = Color(0xFF63636B)
    /** Single warm accent; used sparingly so it stays premium. */
    val Gold = Color(0xFFE2C287)
    val OnGold = Color(0xFF17130A)
}

/** Markup verdict colors, tuned for contrast on dark surfaces. */
val BandLow = Color(0xFFE5604C)
val BandOk = Color(0xFFE0A83C)
val BandGood = Color(0xFF57C98F)

private val DarkColors = darkColorScheme(
    primary = Palette.Gold,
    onPrimary = Palette.OnGold,
    secondary = Palette.TextMid,
    onSecondary = Palette.Ink,
    background = Palette.Ink,
    onBackground = Palette.TextHi,
    surface = Palette.Ink,
    onSurface = Palette.TextHi,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.TextMid,
    surfaceContainer = Palette.Surface,
    surfaceContainerLow = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Palette.SurfaceHigh,
    outline = Palette.Hairline,
    outlineVariant = Palette.Hairline,
    error = BandLow,
)

/** "tnum" keeps digits monospaced so money doesn't jitter while typing. */
private val Type = Typography(
    headlineMedium = TextStyle(
        fontSize = 30.sp, lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp, lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp,
        fontFeatureSettings = "tnum",
    ),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(
        fontSize = 17.sp, lineHeight = 23.sp,
        fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum",
    ),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = "tnum"),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp, fontFeatureSettings = "tnum"),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(
        fontSize = 11.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp,
    ),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
)

private val Shape = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * Always dark. The app deals in money on a market floor; one deliberate dark
 * scheme beats two half-tuned ones, and the palette is built for it.
 */
@Composable
fun YuanCalcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Type,
        shapes = Shape,
        content = content,
    )
}
