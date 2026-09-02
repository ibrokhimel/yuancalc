package uz.yuancalc.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.yuancalc.R
import uz.yuancalc.core.formatCny
import uz.yuancalc.core.formatGrouped
import uz.yuancalc.core.formatUsd
import uz.yuancalc.core.formatUzs
import uz.yuancalc.core.parseAmountOrZero
import uz.yuancalc.ui.components.AnimatedAmountText
import uz.yuancalc.ui.components.RateStatusLine
import uz.yuancalc.ui.components.SectionCard
import uz.yuancalc.ui.components.trimNumber
import uz.yuancalc.ui.theme.Ds
import uz.yuancalc.ui.theme.Palette

private enum class ConvertFrom { CNY, USD, UZS }

/**
 * A plain converter, for amounts with no product attached — a supplier invoice,
 * say. Same rates and same status line as the calculator.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ConvertScreen(vm: CalculatorViewModel, onOpenSettings: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    var amount by remember { mutableStateOf("") }
    var from by remember { mutableStateOf(ConvertFrom.CNY) }

    val rates = state.rates
    val value = parseAmountOrZero(amount)

    val usd = when (from) {
        ConvertFrom.CNY -> value * rates.cnyToUsd
        ConvertFrom.USD -> value
        ConvertFrom.UZS -> if (rates.usdToUzs > 0.0) value / rates.usdToUzs else 0.0
    }
    val cny = if (rates.cnyToUsd > 0.0) usd / rates.cnyToUsd else 0.0
    val uzs = usd * rates.usdToUzs

    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = vm::refreshRates,
        state = pullState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = refreshing,
                containerColor = Palette.SurfaceHigh,
                color = Palette.Accent,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AmountWithCurrencyField(
            label = stringResource(R.string.convert_amount),
            value = amount,
            onValueChange = { amount = it },
            selected = from,
            onSelect = { from = it },
            modifier = Modifier.fillMaxWidth(),
        )

        SectionCard(stringResource(R.string.convert_result)) {
            // The echoed input first in muted ink, then the conversions,
            // growing to the 32sp so'm line — the design's Result card.
            val symbol = when (from) {
                ConvertFrom.CNY -> stringResource(R.string.currency_cny)
                ConvertFrom.USD -> stringResource(R.string.currency_usd)
                ConvertFrom.UZS -> stringResource(R.string.currency_uzs)
            }
            AnimatedAmountText(
                formatGrouped(value) + " " + symbol,
                style = Ds.ResultEcho,
                color = Palette.TextMid,
                modifier = Modifier.padding(top = 4.dp),
            )
            val others = listOf(
                ConvertFrom.CNY to { formatCny(cny) },
                ConvertFrom.USD to { formatUsd(usd) },
                ConvertFrom.UZS to { formatUzs(uzs) },
            ).filter { it.first != from }
            AnimatedAmountText(
                others[0].second(),
                style = Ds.ResultUsd,
                modifier = Modifier.padding(top = 4.dp),
            )
            AnimatedAmountText(
                others[1].second(),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        RateStatusLine(rates = rates, refreshing = refreshing, onClick = onOpenSettings)
    }
    }
}

/**
 * The amount input and the source currency fused into one control: a
 * borderless field, a fine +/- stepper, and an attached currency dropdown
 * behind a hairline divider. One container means one focus ring, and the
 * currency always reads as a property of the amount rather than a separate
 * setting further down the screen.
 */
@Composable
private fun AmountWithCurrencyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    selected: ConvertFrom,
    onSelect: (ConvertFrom) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun stepped(delta: Double): String {
        val next = (parseAmountOrZero(value) + delta).coerceAtLeast(0.0)
        return trimNumber(next)
    }

    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(
        targetValue = if (focused) Palette.AccentFocus else Palette.Hairline,
        animationSpec = tween(180),
        label = "convertBorder",
    )
    val shape = RoundedCornerShape(14.dp)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = Ds.AmountLabel,
            color = Palette.TextMid,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(56.dp)
                .clip(shape)
                .background(Palette.SurfaceHigh)
                .border(1.dp, border, shape),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Palette.TextHi),
                cursorBrush = SolidColor(Palette.Accent),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                "0",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Palette.TextLo,
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
                    .onFocusChanged { focused = it.isFocused },
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(end = 8.dp),
            ) {
                StepChevron(Icons.Filled.KeyboardArrowUp) { onValueChange(stepped(+1.0)) }
                StepChevron(Icons.Filled.KeyboardArrowDown) { onValueChange(stepped(-1.0)) }
            }
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Palette.Hairline),
            )
            CurrencySelect(selected = selected, onSelect = onSelect)
        }
    }
}

@Composable
private fun StepChevron(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Icon(
        icon,
        contentDescription = null,
        tint = Palette.TextMid,
        modifier = Modifier
            .size(14.dp)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun CurrencySelect(selected: ConvertFrom, onSelect: (ConvertFrom) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val options = listOf(
        ConvertFrom.CNY to stringResource(R.string.currency_cny),
        ConvertFrom.USD to stringResource(R.string.currency_usd),
        ConvertFrom.UZS to stringResource(R.string.currency_uzs),
    )
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxHeight()
                .clickable { open = true }
                .padding(horizontal = 14.dp),
        ) {
            Text(
                options.first { it.first == selected }.second,
                style = Ds.SelectValue,
                color = Palette.TextHi,
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Palette.TextLo,
                modifier = Modifier.size(14.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (option, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(option)
                        open = false
                    },
                )
            }
        }
    }
}
