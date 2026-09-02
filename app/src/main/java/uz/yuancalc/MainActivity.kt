package uz.yuancalc

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.delay
import uz.yuancalc.data.CurrencyApiRatesApi
import uz.yuancalc.data.GitHubUpdatesApi
import uz.yuancalc.data.OpenErRatesApi
import uz.yuancalc.data.RatesRepository
import uz.yuancalc.data.SettingsRepository
import uz.yuancalc.data.defaultHttpClient
import uz.yuancalc.ui.AppScaffold
import uz.yuancalc.ui.CalculatorViewModel
import uz.yuancalc.ui.SplashScreen
import uz.yuancalc.ui.theme.YuanCalcTheme

class MainActivity : AppCompatActivity() {

    companion object {
        /**
         * Process-scoped: the splash belongs to a cold start only. Reopening
         * the app while the process is still warm in the background skips it;
         * only a fresh process (or a swipe-away kill) earns it again.
         */
        private var splashShownThisProcess = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val coldStart = !splashShownThisProcess && savedInstanceState == null
        splashShownThisProcess = true
        val createdAt = SystemClock.uptimeMillis()

        val settings = SettingsRepository(applicationContext)
        val client = defaultHttpClient()
        val rates = RatesRepository(
            apis = listOf(OpenErRatesApi(client), CurrencyApiRatesApi(client)),
            cacheRates = settings::cacheRates,
        )
        val updates = GitHubUpdatesApi(client)
        val vm = ViewModelProvider(
            this,
            CalculatorViewModel.Factory(settings, rates, updates, applicationContext.cacheDir),
        )[CalculatorViewModel::class.java]

        setContent {
            YuanCalcTheme {
                var splash by rememberSaveable { mutableStateOf(coldStart) }
                // The app composes underneath from the first frame, so the
                // splash hides warm-up rather than adding to it.
                Box {
                    AppScaffold(vm)
                    AnimatedVisibility(
                        visible = splash,
                        exit = fadeOut(tween(400)),
                    ) {
                        SplashScreen()
                    }
                }
                if (splash) {
                    LaunchedEffect(Unit) {
                        // ~3s from launch including warm-up and the fade, not
                        // 3s on top of however long the first frame took.
                        val elapsed = SystemClock.uptimeMillis() - createdAt
                        delay((2600 - elapsed).coerceAtLeast(600))
                        splash = false
                    }
                }
            }
        }
    }
}
