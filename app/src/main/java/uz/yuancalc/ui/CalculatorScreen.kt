package uz.yuancalc.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.yuancalc.R
import uz.yuancalc.core.MoneyCurrency
import uz.yuancalc.core.PriceVerdict
import uz.yuancalc.core.WeightUnit
import uz.yuancalc.core.formatCny
import uz.yuancalc.core.formatCnyFloor
import uz.yuancalc.core.formatMarkup
import uz.yuancalc.core.formatUsd
import uz.yuancalc.core.formatUsdFloor
import uz.yuancalc.core.formatUzs
import uz.yuancalc.core.priceVerdict
import uz.yuancalc.core.tierQuote
import uz.yuancalc.data.AppSettings
import uz.yuancalc.data.CalcMode
import uz.yuancalc.ui.components.AmountField
import uz.yuancalc.ui.components.AnimatedAmountText
import uz.yuancalc.ui.components.MultipleSlider
import uz.yuancalc.ui.components.OptionToggle
import uz.yuancalc.ui.components.RateStatusLine
import uz.yuancalc.ui.components.SectionCard
import uz.yuancalc.ui.components.bandColor
import uz.yuancalc.ui.components.trimNumber
import uz.yuancalc.ui.theme.BandGood
import uz.yuancalc.ui.theme.BandLow
import uz.yuancalc.ui.theme.BandOk
import uz.yuancalc.ui.theme.Ds
import uz.yuancalc.ui.theme.Palette

private fun verdictLabel(verdict: PriceVerdict): Int = when (verdict) {
    PriceVerdict.UNPROFITABLE -> R.string.verdict_unprofitable
    PriceVerdict.SOFT -> R.string.verdict_soft
    PriceVerdict.PROFITABLE -> R.string.verdict_profitable
    PriceVerdict.EXCELLENT -> R.string.verdict_excellent
    PriceVerdict.NOBODY -> R.string.verdict_nobody
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CalculatorScreen(vm: CalculatorViewModel, onOpenSettings: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val inputs by vm.inputsFlow.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()

    // Chrome-style pull past the top re-fetches the rates.
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OptionToggle(
            options = listOf(
                CalcMode.PRICE to stringResource(R.string.mode_price),
                CalcMode.SOURCE to stringResource(R.string.mode_source),
            ),
            selected = settings.calcMode,
            onSelect = { mode -> vm.updateSettings { it.copy(calcMode = mode) } },
            fillEqually = true,
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedContent(
            targetState = settings.calcMode,
            transitionSpec = {
                fadeIn(tween(200)).togetherWith(fadeOut(tween(120)))
            },
            label = "calcMode",
        ) { mode ->
            Column {
                when (mode) {
                    CalcMode.PRICE -> PriceContent(vm, state, settings, inputs)
                    CalcMode.SOURCE -> SourceContent(vm, state, settings, inputs)
                }
            }
        }

        RateStatusLine(rates = state.rates, refreshing = refreshing, onClick = onOpenSettings)
    }
    }
}

/** The inputs both modes share: weight + unit, cargo profile, other costs. */
@Composable
private fun SharedItemInputs(
    vm: CalculatorViewModel,
    settings: AppSettings,
    inputs: CalculatorInputs,
    gap: Dp,
) {
    var showOther by remember { mutableStateOf(inputs.otherCosts.isNotBlank()) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = gap),
    ) {
        AmountField(
            label = stringResource(R.string.label_weight),
            value = inputs.weight,
            onValueChange = vm::onWeightChange,
            modifier = Modifier.weight(1f),
        )
        OptionToggle(
            options = listOf(
                WeightUnit.GRAMS to stringResource(R.string.unit_grams),
                WeightUnit.KILOGRAMS to stringResource(R.string.unit_kilograms),
            ),
            selected = settings.weightUnit,
            onSelect = { unit -> vm.updateSettings { it.copy(weightUnit = unit) } },
            modifier = Modifier.padding(start = 8.dp),
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = gap),
    ) {
        CargoChip(settings) { id -> vm.updateSettings { it.copy(selectedCargoProfileId = id) } }
        TextButton(onClick = { showOther = !showOther }) {
            Text(
                (if (showOther) "−  " else "+  ") +
                    stringResource(R.string.label_other_costs)
            )
        }
    }
    AnimatedVisibility(
        visible = showOther,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = gap),
        ) {
            AmountField(
                label = stringResource(R.string.label_other_costs),
                value = inputs.otherCosts,
                onValueChange = vm::onOtherCostsChange,
                modifier = Modifier.weight(1f),
            )
            OptionToggle(
                options = listOf(
                    MoneyCurrency.UZS to stringResource(R.string.currency_uzs),
                    MoneyCurrency.USD to stringResource(R.string.currency_usd),
                ),
                selected = settings.otherCostsCurrency,
                onSelect = { c -> vm.updateSettings { it.copy(otherCostsCurrency = c) } },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** `Truck · $9/kg` — switching agents is a per-item decision, so it lives here. */
@Composable
private fun CargoChip(settings: AppSettings, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selected = settings.selectedCargoProfile()

    fun label(name: String, rate: Double) = "$name · $${trimNumber(rate)}/kg"

    Box {
        Surface(
            onClick = { open = true },
            shape = RoundedCornerShape(20.dp),
            color = Palette.SurfaceHigh,
            contentColor = Palette.TextHi,
            border = BorderStroke(1.dp, Palette.Hairline),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(
                    Icons.Filled.LocalShipping,
                    contentDescription = null,
                    tint = Palette.TextMid,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label(selected.name, selected.ratePerKgUsd),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Palette.TextLo,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            settings.cargoProfiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(label(profile.name, profile.ratePerKgUsd)) },
                    onClick = {
                        onSelect(profile.id)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PriceContent(
    vm: CalculatorViewModel,
    state: CalculatorState,
    settings: AppSettings,
    inputs: CalculatorInputs,
) {
    val gap = 12.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = gap),
    ) {
        AmountField(
            label = stringResource(R.string.label_cost),
            value = inputs.cost,
            onValueChange = vm::onCostChange,
            modifier = Modifier.weight(1f),
        )
        OptionToggle(
            options = listOf(
                MoneyCurrency.CNY to stringResource(R.string.currency_cny),
                MoneyCurrency.USD to stringResource(R.string.currency_usd),
            ),
            selected = settings.costCurrency,
            onSelect = { c -> vm.updateSettings { it.copy(costCurrency = c) } },
            modifier = Modifier.padding(start = 8.dp),
        )
    }
    // The other two currencies, whichever one was entered.
    val productUsd = state.landed.productUsd
    val helperFirst = when (settings.costCurrency) {
        MoneyCurrency.CNY -> formatUsd(productUsd)
        else -> formatCny(if (state.rates.cnyToUsd > 0.0) productUsd / state.rates.cnyToUsd else 0.0)
    }
    Text(
        helperFirst + "  ≈  " + formatUzs(productUsd * state.rates.usdToUzs),
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextMid,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )

    SharedItemInputs(vm, settings, inputs, gap)

    Column(Modifier.padding(top = gap)) {
        SectionCard(stringResource(R.string.label_landed_cost)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                AnimatedAmountText(
                    formatUsd(state.landed.totalUsd),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "≈  " + formatUzs(state.landedUzs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextMid,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }
            Text(
                formatUsd(state.landed.productUsd) + "  +  " +
                    stringResource(R.string.breakdown_cargo) + " " +
                    formatUsd(state.landed.cargoUsd) +
                    (if (state.landed.otherUsd > 0.0) "  +  " + formatUsd(state.landed.otherUsd) else ""),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextMid,
            )
        }
    }

    Column(Modifier.padding(top = gap)) {
        SectionCard(stringResource(R.string.label_suggested_prices)) {
            var explored by remember { mutableStateOf<Double?>(null) }
            val multiple = explored ?: settings.profitableMultiple
            val quote = tierQuote(
                landedUsd = state.landed.totalUsd,
                multiple = multiple,
                rates = state.rates,
                step = settings.priceRoundingStep,
                mode = settings.priceRoundingMode,
            )
            val verdict = priceVerdict(multiple, settings.softMultiple, settings.profitableMultiple)
            val verdictColor by animateColorAsState(
                targetValue = when (verdict) {
                    PriceVerdict.UNPROFITABLE, PriceVerdict.NOBODY -> BandLow
                    PriceVerdict.SOFT -> BandOk
                    PriceVerdict.PROFITABLE -> BandGood
                    PriceVerdict.EXCELLENT -> Palette.Accent
                },
                animationSpec = tween(250),
                label = "verdictColor",
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp),
            ) {
                AnimatedAmountText(
                    stringResource(verdictLabel(verdict)),
                    style = MaterialTheme.typography.titleLarge,
                    color = verdictColor,
                    modifier = Modifier.weight(1f),
                )
                AnimatedAmountText(
                    formatMarkup(multiple),
                    style = Ds.Multiple,
                    color = Palette.TextHi,
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                AnimatedAmountText(
                    formatUzs(quote.priceUzs),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Palette.TextHi,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    formatUsd(quote.priceUsd),
                    style = Ds.AccentSmall,
                    color = Palette.Accent,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            val low = minOf(settings.softMultiple, settings.profitableMultiple)
            val high = maxOf(settings.softMultiple, settings.profitableMultiple)
            MultipleSlider(
                value = multiple,
                onChange = { explored = it },
                range = 0.5f..4f,
                notches = listOf(1.0, low, high, high + 1.0),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.label_profit) + "  " +
                    formatUsd(quote.profitUsd) + "  ≈  " + formatUzs(quote.profitUzs),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextMid,
            )
        }
    }

    Column(Modifier.padding(top = gap)) {
        SectionCard(stringResource(R.string.sensitivity_title)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                state.sensitivity.forEachIndexed { index, row ->
                    val isCurrent =
                        if (state.sensitivity.size == 3) index == 1 else index == 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            row.weightGrams.toLong().toString() + " " +
                                stringResource(R.string.unit_grams),
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.TextLo,
                        )
                        AnimatedAmountText(
                            formatMarkup(row.markup),
                            style = if (isCurrent) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reverse mode: standing in the market, "people pay 299 000 so'm for this —
 * what is the most I can pay for it?" Maximums are displayed floored, never
 * half-up, so following them cannot miss the target markup.
 */
@Composable
private fun SourceContent(
    vm: CalculatorViewModel,
    state: CalculatorState,
    settings: AppSettings,
    inputs: CalculatorInputs,
) {
    val gap = 8.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = gap),
    ) {
        AmountField(
            label = stringResource(R.string.label_target_price),
            value = inputs.targetPrice,
            onValueChange = vm::onTargetPriceChange,
            modifier = Modifier.weight(1f),
        )
        OptionToggle(
            options = listOf(
                MoneyCurrency.USD to stringResource(R.string.currency_usd),
                MoneyCurrency.UZS to stringResource(R.string.currency_uzs),
            ),
            selected = settings.targetPriceCurrency,
            onSelect = { c -> vm.updateSettings { it.copy(targetPriceCurrency = c) } },
            modifier = Modifier.padding(start = 8.dp),
        )
    }

    SharedItemInputs(vm, settings, inputs, gap)

    val src = state.sourcing
    Column(Modifier.padding(top = gap)) {
        SectionCard(stringResource(R.string.label_max_cost), rounded = true) {
            if (src.targetUsd > 0.0 && src.profitable.maxProductUsd == null) {
                Text(
                    stringResource(
                        R.string.sourcing_impossible,
                        formatUsd(src.cargoUsd),
                        formatMarkup(settings.profitableMultiple),
                        formatUsd(src.landedBudgetUsd ?: 0.0),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BandLow,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            for ((nameRes, tier) in listOf(
                R.string.tier_soft to src.soft,
                R.string.tier_profitable to src.profitable,
            )) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 6.dp),
                ) {
                    Text(
                        stringResource(nameRes) + "  " + formatMarkup(tier.multiple),
                        style = MaterialTheme.typography.titleSmall,
                        color = Palette.TextMid,
                        modifier = Modifier.weight(1f),
                    )
                    if (tier.maxCostCny != null && tier.maxProductUsd != null) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedAmountText(
                                formatCnyFloor(tier.maxCostCny),
                                style = MaterialTheme.typography.titleLarge,
                                color = Palette.TextHi,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                formatUsdFloor(tier.maxProductUsd),
                                style = Ds.AccentSmall,
                                color = Palette.Accent,
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }
                    } else {
                        Text(
                            "—",
                            style = MaterialTheme.typography.titleLarge,
                            color = Palette.TextLo,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                src.strip.forEachIndexed { index, row ->
                    val isCurrent = if (src.strip.size == 3) index == 1 else index == 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            row.weightGrams.toLong().toString() + " " +
                                stringResource(R.string.unit_grams),
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.TextLo,
                        )
                        AnimatedAmountText(
                            row.maxCostCny?.let { formatCnyFloor(it) } ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCurrent) Palette.TextHi else Palette.TextMid,
                        )
                    }
                }
            }
        }
    }
}
