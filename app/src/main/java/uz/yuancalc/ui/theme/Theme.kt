package uz.yuancalc.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.yuancalc.R

/**
 * The design-token layer, ported 1:1 from the reference .pen file. This is the
 * only file allowed to contain color literals, text styles or radii; every
 * screen reads from [Palette], [Ds] or [MaterialTheme], so the app can only
 * ever speak the reference palette.
 */
object Palette {
    /** App background — near-black ink, never pure #000. */
    val Ink = Color(0xFF0A0A0C)
    /** Card surface, one step above the background. */
    val Surface = Color(0xFF141418)
    /** Inputs, pills and pressed states, one step above cards. */
    val SurfaceHigh = Color(0xFF1D1D23)
    /** The raised thumb of segmented toggles, one step above [SurfaceHigh]. */
    val SurfaceRaised = Color(0xFF33333B)
    /** 1dp borders separating surfaces instead of shadows. */
    val Hairline = Color(0xFF26262C)
    val TextHi = Color(0xFFF4F4F6)
    val TextMid = Color(0xFF9A9AA3)
    val TextLo = Color(0xFF63636B)
    /** Single cool ice-blue accent; used sparingly so it stays premium. */
    val Accent = Color(0xFF8AB4F8)
    val OnAccent = Color(0xFF0B1626)
    /** Focused-field border — accent at the reference's 8C alpha. */
    val AccentFocus = Color(0x8C8AB4F8)
    /** Active nav item wash — accent at the reference's 26 alpha. */
    val AccentSoft = Color(0x268AB4F8)
}

/** Markup verdict colors, tuned for contrast on dark surfaces. */
val BandLow = Color(0xFFE5604C)
val BandOk = Color(0xFFE0A83C)
val BandGood = Color(0xFF57C98F)

/** The reference typeface for every style in the app. */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private val DarkColors = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Palette.OnAccent,
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

private fun inter(
    size: Double,
    line: Double,
    weight: FontWeight = FontWeight.Normal,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = InterFamily,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
    fontFeatureSettings = "tnum",
)

/**
 * Reference styles with no natural Material role. Everything here is an exact
 * size/weight/tracking from the .pen frames.
 */
object Ds {
    /** "2.10×" beside the verdict. */
    val Multiple = inter(15.0, 20.0, FontWeight.SemiBold)
    /** Convert result, echoed input line — "1 000 ¥". */
    val ResultEcho = inter(16.0, 20.0, FontWeight.SemiBold, -0.66)
    /** Convert result, middle line — "$148.50". */
    val ResultUsd = inter(22.0, 28.0, FontWeight.SemiBold, -0.66)
    /** Accent secondary money next to a big figure — "$32.24". */
    val AccentSmall = inter(14.0, 22.0, FontWeight.SemiBold)
    /** Active nav item label. */
    val NavLabel = inter(12.0, 16.0, FontWeight.Medium)
    /** Rate status line. */
    val Status = inter(12.0, 16.0)
    /** Standalone field label above the Convert amount input. */
    val AmountLabel = inter(13.0, 18.0, FontWeight.Medium)
    /** "Version 1.2 is available · Download update". */
    val UpdateNote = inter(13.0, 18.0, FontWeight.Medium)
    /** Currency shown inside the Convert select — "¥". */
    val SelectValue = inter(15.0, 20.0, FontWeight.Medium)
}

/** "tnum" keeps digits monospaced so money doesn't jitter while typing. */
private val Type = Typography(
    // The big markup figure — 1.93×.
    displaySmall = inter(40.0, 46.0, FontWeight.SemiBold, -0.8),
    // Landed cost total and the Convert so'm result.
    headlineLarge = inter(32.0, 38.0, FontWeight.SemiBold, -0.6),
    // Screen title in the header.
    headlineMedium = inter(22.0, 26.0, FontWeight.Normal, -0.66),
    // Suggested price in so'm.
    headlineSmall = inter(24.0, 30.0, FontWeight.SemiBold, -0.3),
    // Verdict, sensitivity center value, max-cost ¥.
    titleLarge = inter(20.0, 26.0, FontWeight.SemiBold),
    // Sensitivity side values.
    titleMedium = inter(17.0, 23.0, FontWeight.SemiBold, -0.48),
    titleSmall = inter(14.0, 20.0, FontWeight.Medium),
    // Field values.
    bodyLarge = inter(16.0, 20.0, FontWeight.Normal, -0.48),
    bodyMedium = inter(14.0, 20.0),
    bodySmall = inter(12.5, 18.0),
    // Segment labels, buttons, chips.
    labelLarge = inter(14.0, 20.0, FontWeight.Medium),
    // Card titles and the brand line.
    labelMedium = inter(11.0, 16.0, FontWeight.Medium, 1.2),
    // Field labels inside inputs.
    labelSmall = inter(11.0, 16.0),
)

private val Shape = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Always dark. The app deals in money on a market floor; one deliberate dark
 * scheme beats two half-tuned ones, and the palette is built for it.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun YuanCalcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Type,
        shapes = Shape,
    ) {
        // Presses answer with the app's own motion (thumb slides, color
        // washes) — no ripple layered on top of it.
        CompositionLocalProvider(
            LocalIndication provides NoIndication,
            LocalRippleConfiguration provides null,
            content = content,
        )
    }
}

private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        object : Modifier.Node() {}

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}
