package uz.yuancalc.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.yuancalc.R
import uz.yuancalc.core.formatGrouped
import uz.yuancalc.core.formatUsd
import uz.yuancalc.core.formatUzs
import uz.yuancalc.core.parseAmountOrZero
import uz.yuancalc.ui.components.AmountField
import uz.yuancalc.ui.components.OptionToggle
import uz.yuancalc.ui.components.RateStatusLine
import uz.yuancalc.ui.components.SectionCard

private enum class ConvertFrom { CNY, USD, UZS }

/**
 * A plain converter, for amounts with no product attached — a supplier invoice,
 * say. Same rates and same status line as the calculator.
 */
@Composable
fun ConvertScreen(vm: CalculatorViewModel, onOpenSettings: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        AmountField(
            label = stringResource(R.string.convert_amount),
            value = amount,
            onValueChange = { amount = it },
            modifier = Modifier.fillMaxWidth(),
        )
        OptionToggle(
            options = listOf(
                ConvertFrom.CNY to stringResource(R.string.currency_cny),
                ConvertFrom.USD to stringResource(R.string.currency_usd),
                ConvertFrom.UZS to stringResource(R.string.currency_uzs),
            ),
            selected = from,
            onSelect = { from = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )

        SectionCard(stringResource(R.string.convert_result)) {
            Text(
                formatGrouped(cny) + " " + stringResource(R.string.currency_cny),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                formatUsd(usd),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                formatUzs(uzs),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        RateStatusLine(rates = rates, onClick = onOpenSettings)
    }
}
