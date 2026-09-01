package uz.yuancalc.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.yuancalc.R
import uz.yuancalc.core.PriceRounding
import uz.yuancalc.data.AppLanguage
import uz.yuancalc.ui.components.DraftNumberField
import uz.yuancalc.ui.components.OptionToggle
import uz.yuancalc.ui.components.SectionCard

@Composable
fun SettingsScreen(vm: CalculatorViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        SectionCard(stringResource(R.string.settings_cargo_rate)) {
            DraftNumberField(
                label = stringResource(R.string.settings_cargo_rate),
                value = s.cargoRateUsdPerKg,
                onCommit = { v -> v?.let { r -> vm.updateSettings { it.copy(cargoRateUsdPerKg = r) } } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }

        SectionCard(stringResource(R.string.settings_multiples)) {
            DraftNumberField(
                label = stringResource(R.string.settings_soft_multiple),
                value = s.softMultiple,
                onCommit = { v -> v?.let(vm::onSoftMultipleChange) },
                accept = { it > 0.0 },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            DraftNumberField(
                label = stringResource(R.string.settings_profitable_multiple),
                value = s.profitableMultiple,
                onCommit = { v -> v?.let(vm::onProfitableMultipleChange) },
                accept = { it > 0.0 },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            if (s.softMultiple > s.profitableMultiple) {
                Text(
                    stringResource(R.string.settings_tiers_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        SectionCard(stringResource(R.string.settings_rounding)) {
            OptionToggle(
                options = listOf(
                    0 to stringResource(R.string.settings_rounding_off),
                    500 to "500",
                    1_000 to "1 000",
                    5_000 to "5 000",
                    10_000 to "10 000",
                ),
                selected = s.priceRoundingStep,
                onSelect = { step -> vm.updateSettings { it.copy(priceRoundingStep = step) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            OptionToggle(
                options = listOf(
                    PriceRounding.UP to stringResource(R.string.settings_rounding_mode_up),
                    PriceRounding.NEAREST to stringResource(R.string.settings_rounding_mode_nearest),
                ),
                selected = s.priceRoundingMode,
                onSelect = { mode -> vm.updateSettings { it.copy(priceRoundingMode = mode) } },
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        SectionCard(stringResource(R.string.settings_rates)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                DraftNumberField(
                    label = stringResource(R.string.settings_pin_cny),
                    value = s.pinnedCnyToUsd,
                    onCommit = { v -> vm.updateSettings { it.copy(pinnedCnyToUsd = v) } },
                    allowEmpty = true,
                    accept = { it > 0.0 },
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { vm.updateSettings { it.copy(pinnedCnyToUsd = null) } }) {
                    Text(stringResource(R.string.settings_unpin))
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                DraftNumberField(
                    label = stringResource(R.string.settings_pin_uzs),
                    value = s.pinnedUsdToUzs,
                    onCommit = { v -> vm.updateSettings { it.copy(pinnedUsdToUzs = v) } },
                    allowEmpty = true,
                    accept = { it > 0.0 },
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { vm.updateSettings { it.copy(pinnedUsdToUzs = null) } }) {
                    Text(stringResource(R.string.settings_unpin))
                }
            }
            Button(
                onClick = vm::refreshRates,
                modifier = Modifier.padding(top = 10.dp),
            ) {
                Text(stringResource(R.string.settings_refresh))
            }
        }

        SectionCard(stringResource(R.string.settings_language)) {
            OptionToggle(
                options = listOf(
                    AppLanguage.SYSTEM to stringResource(R.string.settings_language_system),
                    AppLanguage.ENGLISH to stringResource(R.string.settings_language_english),
                    AppLanguage.UZBEK to stringResource(R.string.settings_language_uzbek),
                ),
                selected = s.language,
                onSelect = { lang -> vm.updateSettings { it.copy(language = lang) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}
