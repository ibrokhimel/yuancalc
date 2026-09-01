package uz.yuancalc.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
 */
@Composable
fun TierRow(
    name: String,
    quote: TierQuote,
    onMultipleChange: (Double) -> Unit,
    onUse: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(quote.multiple) { mutableStateOf(trimNumber(quote.multiple)) }

    Column(Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
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
                TextButton(onClick = { editing = true }) {
                    Text(formatMarkup(quote.multiple))
                }
            }
        }
        TextButton(onClick = onUse, contentPadding = PaddingValues(0.dp)) {
            Text(
                formatUzs(quote.priceUzs) + "    " + formatUsd(quote.priceUsd),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(
            stringResource(R.string.label_profit) + "  " +
                formatUsd(quote.profitUsd) + "  ≈  " + formatUzs(quote.profitUzs),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/**
 * Always states which rates produced the numbers above it. A stale rate
 * corrupts every figure on screen silently, so this is never hidden.
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
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodySmall)
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

/** 9.0 -> "9", 1.8 -> "1.8" — avoids showing "9.0" in an editable field. */
fun trimNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
