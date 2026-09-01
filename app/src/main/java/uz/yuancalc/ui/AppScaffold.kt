package uz.yuancalc.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.yuancalc.R

private enum class Tab { CALCULATOR, CONVERT, SETTINGS }

@Composable
fun AppScaffold(vm: CalculatorViewModel) {
    var tab by remember { mutableStateOf(Tab.CALCULATOR) }
    val settings by vm.settings.collectAsStateWithLifecycle()

    ApplyLanguage(settings.language)

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.CALCULATOR,
                    onClick = { tab = Tab.CALCULATOR },
                    icon = { Icon(Icons.Filled.Calculate, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_calculator)) },
                )
                NavigationBarItem(
                    selected = tab == Tab.CONVERT,
                    onClick = { tab = Tab.CONVERT },
                    icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_convert)) },
                )
                NavigationBarItem(
                    selected = tab == Tab.SETTINGS,
                    onClick = { tab = Tab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_settings)) },
                )
            }
        }
    ) { padding ->
        val openSettings = { tab = Tab.SETTINGS }
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.CALCULATOR -> CalculatorScreen(vm, openSettings)
                Tab.CONVERT -> ConvertScreen(vm, openSettings)
                Tab.SETTINGS -> SettingsScreen(vm)
            }
        }
    }
}
