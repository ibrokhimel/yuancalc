package uz.yuancalc.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import uz.yuancalc.R
import uz.yuancalc.core.MarkupBand
import uz.yuancalc.core.RateSource
import uz.yuancalc.core.Rates
import uz.yuancalc.core.TierQuote
import uz.yuancalc.core.formatMarkup
import uz.yuancalc.core.formatUsd
import uz.yuancalc.core.formatUzs
import uz.yuancalc.ui.theme.BandGood
import uz.yuancalc.ui.theme.BandLow
import uz.yuancalc.ui.theme.BandOk
import uz.yuancalc.ui.theme.Ds
import uz.yuancalc.ui.theme.Palette

@Composable
fun bandColor(band: MarkupBand): Color = when (band) {
    MarkupBand.LOW -> BandLow
    MarkupBand.OK -> BandOk
    MarkupBand.GOOD -> BandGood
    MarkupBand.UNKNOWN -> MaterialTheme.colorScheme.onSurface
}

/**
 * A money or markup value that changes while typing: the new value fades in
 * with a small upward slide. Timed under a keystroke repeat so the number
 * never lags behind the input that produced it.
 */
@Composable
fun AnimatedAmountText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (fadeIn(tween(150)) + slideInVertically { it / 6 })
                .togetherWith(fadeOut(tween(90)))
                .using(SizeTransform(clip = false))
        },
        label = "amount",
        modifier = modifier,
    ) { value ->
        Text(value, style = style, color = color)
    }
}

/**
 * Always states which rates produced the numbers above it. A stale rate
 * corrupts every figure on screen silently, so this is never hidden.
 *
 * While a fetch is in flight the icon spins; when it lands, the row swaps to
 * a check that pops in with a small overshoot. The icon tint keeps the
 * at-a-glance meaning: green check for fresh, amber for offline-cached,
 * gray for pinned or built-in rates.
 */
@Composable
fun RateStatusLine(
    rates: Rates,
    refreshing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            color = Color.Transparent,
            contentColor = Palette.TextMid,
        ) {
            AnimatedContent(
                targetState = refreshing to rates.source,
                transitionSpec = {
                    (fadeIn(tween(180)) + scaleIn(
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        initialScale = 0.6f,
                    ))
                        .togetherWith(fadeOut(tween(90)))
                        .using(SizeTransform(clip = false))
                },
                label = "rateState",
            ) { (busy, source) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    if (busy) {
                        val spin = rememberInfiniteTransition(label = "spin")
                        val angle by spin.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
                            label = "angle",
                        )
                        Icon(
                            Icons.Filled.Autorenew,
                            contentDescription = null,
                            tint = Palette.TextMid,
                            modifier = Modifier
                                .size(14.dp)
                                .graphicsLayer { rotationZ = angle },
                        )
                    } else {
                        val (icon, tint) = when (source) {
                            RateSource.LIVE -> Icons.Filled.Check to BandGood
                            RateSource.CACHED -> Icons.Filled.CloudOff to BandOk
                            RateSource.PINNED -> Icons.Filled.PushPin to Palette.TextLo
                            RateSource.BUNDLED -> Icons.Filled.CloudOff to Palette.TextLo
                        }
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    val text = if (busy) {
                        stringResource(R.string.rate_status_updating)
                    } else {
                        when (source) {
                            RateSource.LIVE -> stringResource(
                                R.string.rate_status_updated,
                                relativeTime(rates.fetchedAtEpochSeconds),
                            )
                            RateSource.CACHED -> stringResource(
                                R.string.rate_status_cached,
                                relativeTime(rates.fetchedAtEpochSeconds),
                            )
                            RateSource.PINNED -> stringResource(R.string.rate_status_pinned)
                            RateSource.BUNDLED -> stringResource(R.string.rate_status_bundled)
                        }
                    }
                    Text(text, style = Ds.Status, color = Palette.TextMid)
                }
            }
        }
    }
}

@Composable
private fun relativeTime(epochSeconds: Long?): String {
    if (epochSeconds == null) return stringResource(R.string.just_now)
    val elapsed = (System.currentTimeMillis() / 1000) - epochSeconds
    return when {
        elapsed < 60 -> stringResource(R.string.just_now)
        elapsed < 3_600 -> stringResource(R.string.minutes_ago, (elapsed / 60).toInt())
        elapsed < 86_400 -> stringResource(R.string.hours_ago, (elapsed / 3_600).toInt())
        else -> stringResource(R.string.days_ago, (elapsed / 86_400).toInt())
    }
}
