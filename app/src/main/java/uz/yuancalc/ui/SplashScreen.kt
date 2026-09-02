package uz.yuancalc.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.yuancalc.ui.theme.Palette

/** CSS `ease-in-out`, applied per keyframe segment like the reference. */
private val SegmentEase = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

private val Scales = floatArrayOf(1f, 0.9f, 1.1f, 0.85f, 1.05f, 1f)
private val Angles = floatArrayOf(0f, 72f, 144f, 216f, 288f, 360f)
private val RadiusPct = floatArrayOf(50f, 35f, 15f, 8f, 25f, 50f)

/** Piecewise keyframe lookup: 0..1 across evenly spaced [values], eased per segment. */
private fun keyframe(progress: Float, values: FloatArray): Float {
    val segments = values.size - 1
    val scaled = (progress * segments).coerceIn(0f, segments - 0.0001f)
    val i = scaled.toInt()
    val local = SegmentEase.transform(scaled - i)
    return values[i] + (values[i + 1] - values[i]) * local
}

/**
 * A square that morphs circle → rounded square → near-square and back while
 * rotating and breathing, on a 3s loop — a Compose port of the reference
 * morphing-spinner keyframes. All animated values are read inside the draw
 * and layer lambdas, so the loop never recomposes anything.
 */
@Composable
fun MorphingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = Palette.Accent,
) {
    val transition = rememberInfiniteTransition(label = "morph")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "morphProgress",
    )
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                val scale = keyframe(progress, Scales)
                scaleX = scale
                scaleY = scale
                rotationZ = keyframe(progress, Angles)
            }
            .drawBehind {
                val radius = this.size.minDimension * keyframe(progress, RadiusPct) / 100f
                drawRoundRect(color = color, cornerRadius = CornerRadius(radius, radius))
            },
    )
}

/**
 * Cold-start splash: nothing but the morphing spinner centered on the ink.
 * The empty [pointerInput] makes the overlay hit-testable, so touches don't
 * fall through to the app loading beneath it.
 */
@Composable
fun SplashScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Ink)
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        MorphingSpinner(size = 48.dp, color = MaterialTheme.colorScheme.primary)
    }
}
