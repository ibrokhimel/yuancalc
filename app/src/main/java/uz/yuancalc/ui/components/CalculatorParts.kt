package uz.yuancalc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.yuancalc.R
import uz.yuancalc.core.MarkupBand
import uz.yuancalc.core.RateSource
import uz.yuancalc.core.Rates
import uz.yuancalc.core.TierQuote
import uz.yuancalc.core.formatMarkup
import uz.yuancalc.core.formatUsd
import uz.yuancalc.core.formatUzs
import uz.yuancalc.core.parseAmount
import uz.yuancalc.ui.theme.BandGood
import uz.yuancalc.ui.theme.BandLow
import uz.yuancalc.ui.theme.BandOk
import uz.yuancalc.ui.theme.Palette

@Composable
fun bandColor(band: MarkupBand): Color = when (band) {
    MarkupBand.LOW -> BandLow
    MarkupBand.OK -> BandOk
    MarkupBand.GOOD -> BandGood
    MarkupBand.UNKNOWN -> MaterialTheme.colorScheme.onSurface
}

/**
 * One suggested price. The multiple is edited here rather than in Settings, so
 * a one-off "what if I charged 2x" needs no trip to another screen; the edited
 * value persists as the new default.
 *
 * The draft is seeded when editing starts and is otherwise left alone, so a
 * keystroke that commits (typing "2." commits 2.0) does not reset the text and
 * swallow the dot.
 */
@Composable
fun TierRow(
    name: String,
    quote: TierQuote,
    onMultipleChange: (Double) -> Unit,
    onUse: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    Column(Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                color = Palette.TextMid,
                modifier = Modifier.weight(1f),
            )
            if (editing) {
                AmountField(
                    label = "×",
                    value = draft,
                    onValueChange = { text ->
                        draft = text
                        parseAmount(text)?.let { if (it > 0.0) onMultipleChange(it) }
                    },
                    modifier = Modifier.fillMaxWidth(0.4f),
                )
                TextButton(onClick = { editing = false }) {
                    Text(stringResource(R.string.action_done))
                }
            } else {
                Surface(
                    onClick = {
                        draft = trimNumber(quote.multiple)
                        editing = true
                    },
                    shape = RoundedCornerShape(50),
                    color = Palette.SurfaceHigh,
                    contentColor = Palette.TextHi,
                    border = BorderStroke(1.dp, Palette.Hairline),
                ) {
                    Text(
                        formatMarkup(quote.multiple),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }
        }
        TextButton(onClick = onUse, contentPadding = PaddingValues(0.dp)) {
            Text(
                formatUzs(quote.priceUzs),
                style = MaterialTheme.typography.titleMedium,
                color = Palette.TextHi,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                formatUsd(quote.priceUsd),
                style = MaterialTheme.typography.titleMedium,
                color = Palette.Gold,
            )
        }
        Text(
            stringResource(R.string.label_profit) + "  " +
                formatUsd(quote.profitUsd) + "  ≈  " + formatUzs(quote.profitUzs),
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextMid,
        )
    }
}

/**
 * Always states which rates produced the numbers above it. A stale rate
 * corrupts every figure on screen silently, so this is never hidden — the dot
 * color says at a glance whether rates are live (green), cached (amber) or
 * manual/bundled (gray).
 */
@Composable
fun RateStatusLine(rates: Rates, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val text = when (rates.source) {
        RateSource.LIVE ->
            stringResource(R.string.rate_status_live, relativeTime(rates.fetchedAtEpochSeconds))
        RateSource.CACHED ->
            stringResource(R.string.rate_status_cached, relativeTime(rates.fetchedAtEpochSeconds))
        RateSource.PINNED -> stringResource(R.string.rate_status_pinned)
        RateSource.BUNDLED -> stringResource(R.string.rate_status_bundled)
    }
    val dot = when (rates.source) {
        RateSource.LIVE -> BandGood
        RateSource.CACHED -> BandOk
        RateSource.PINNED, RateSource.BUNDLED -> Palette.TextLo
    }
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            color = Palette.Surface,
            border = BorderStroke(1.dp, Palette.Hairline),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(dot, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(text, style = MaterialTheme.typography.bodySmall, color = Palette.TextMid)
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
