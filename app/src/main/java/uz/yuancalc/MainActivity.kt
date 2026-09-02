package uz.yuancalc

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import uz.yuancalc.data.CurrencyApiRatesApi
import uz.yuancalc.data.OpenErRatesApi
import uz.yuancalc.data.RatesRepository
import uz.yuancalc.data.SettingsRepository
import uz.yuancalc.data.defaultHttpClient
import uz.yuancalc.ui.AppScaffold
import uz.yuancalc.ui.CalculatorViewModel
import uz.yuancalc.ui.theme.YuanCalcTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = SettingsRepository(applicationContext)
        val client = defaultHttpClient()
        val rates = RatesRepository(
            apis = listOf(OpenErRatesApi(client), CurrencyApiRatesApi(client)),
            cacheRates = settings::cacheRates,
        )
        val vm = ViewModelProvider(
            this,
            CalculatorViewModel.Factory(settings, rates),
        )[CalculatorViewModel::class.java]

        setContent {
            YuanCalcTheme {
                AppScaffold(vm)
            }
        }
    }
}
