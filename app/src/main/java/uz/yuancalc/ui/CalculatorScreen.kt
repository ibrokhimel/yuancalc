package uz.yuancalc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.yuancalc.R
import uz.yuancalc.core.MoneyCurrency
import uz.yuancalc.core.WeightUnit
import uz.yuancalc.core.formatMarkup
import uz.yuancalc.core.formatUsd
import uz.yuancalc.core.formatUzs
import uz.yuancalc.ui.components.AmountField
import uz.yuancalc.ui.components.OptionToggle
import uz.yuancalc.ui.components.RateStatusLine
import uz.yuancalc.ui.components.SectionCard
import uz.yuancalc.ui.components.TierRow
import uz.yuancalc.ui.components.bandColor
import uz.yuancalc.ui.theme.Palette

@Composable
fun CalculatorScreen(vm: CalculatorViewModel, onOpenSettings: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val inputs by vm.inputsFlow.collectAsStateWithLifecycle()
    var showOther by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        AmountField(
            label = stringResource(R.string.label_cost),
            value = inputs.cost,
            onValueChange = vm::onCostChange,
            suffix = stringResource(R.string.currency_cny),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            formatUsd(state.landed.productUsd) + "   ≈   " +
                formatUzs(state.landed.productUsd * state.rates.usdToUzs),
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextMid,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 10.dp),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
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

        TextButton(onClick = { showOther = !showOther }) {
            Text(
                (if (showOther) "−  " else "+  ") +
                    stringResource(R.string.label_other_costs)
            )
        }
        if (showOther) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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

        SectionCard(stringResource(R.string.label_landed_cost)) {
            Text(
                formatUsd(state.landed.totalUsd) + "   ≈   " + formatUzs(state.landedUzs),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                formatUsd(state.landed.productUsd) + "  +  " +
                    stringResource(R.string.breakdown_cargo) + " " +
                    formatUsd(state.landed.cargoUsd) +
                    (if (state.landed.otherUsd > 0.0) "  +  " + formatUsd(state.landed.otherUsd) else ""),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextMid,
            )
        }

        SectionCard(stringResource(R.string.label_suggested_prices)) {
            TierRow(
                name = stringResource(R.string.tier_soft),
                quote = state.softQuote,
                onMultipleChange = vm::onSoftMultipleChange,
                onUse = { vm.onMyPriceChange(state.softQuote.priceUzs.toLong().toString()) },
            )
            TierRow(
                name = stringResource(R.string.tier_profitable),
                quote = state.profitableQuote,
                onMultipleChange = vm::onProfitableMultipleChange,
                onUse = { vm.onMyPriceChange(state.profitableQuote.priceUzs.toLong().toString()) },
            )
        }

        SectionCard(stringResource(R.string.label_my_price)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                AmountField(
                    label = stringResource(R.string.label_my_price),
                    value = inputs.myPrice,
                    onValueChange = vm::onMyPriceChange,
                    modifier = Modifier.weight(1f),
                )
                OptionToggle(
                    options = listOf(
                        MoneyCurrency.UZS to stringResource(R.string.currency_uzs),
                        MoneyCurrency.USD to stringResource(R.string.currency_usd),
                    ),
                    selected = settings.myPriceCurrency,
                    onSelect = { c -> vm.updateSettings { it.copy(myPriceCurrency = c) } },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            state.myPriceCheck?.let { check ->
                Text(
                    formatMarkup(check.markup),
                    style = MaterialTheme.typography.headlineMedium,
                    color = bandColor(state.band),
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    stringResource(R.string.label_profit) + "  " +
                        formatUsd(check.profitUsd) + "  ≈  " + formatUzs(check.profitUzs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextMid,
                )
            }
        }

        SectionCard(stringResource(R.string.sensitivity_title)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                state.sensitivity.forEach { row ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            row.weightGrams.toLong().toString() + " " +
                                stringResource(R.string.unit_grams),
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.TextLo,
                        )
                        Text(
                            formatMarkup(row.markup),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }

        RateStatusLine(rates = state.rates, onClick = onOpenSettings)
    }
}
