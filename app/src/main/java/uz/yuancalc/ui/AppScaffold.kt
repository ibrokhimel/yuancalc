package uz.yuancalc.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.yuancalc.R
import uz.yuancalc.ui.theme.Palette

private enum class Tab(val labelRes: Int, val icon: ImageVector) {
    CALCULATOR(R.string.tab_calculator, Icons.Filled.Calculate),
    CONVERT(R.string.tab_convert, Icons.Filled.SwapHoriz),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings),
}

@Composable
fun AppScaffold(vm: CalculatorViewModel) {
    var tab by remember { mutableStateOf(Tab.CALCULATOR) }
    val settings by vm.settings.collectAsStateWithLifecycle()

    ApplyLanguage(settings.language)

    Scaffold(
        containerColor = Palette.Ink,
        topBar = { Header(tab) },
        bottomBar = {
            Column {
                HorizontalDivider(color = Palette.Hairline, thickness = 1.dp)
                NavigationBar(containerColor = Palette.Ink, tonalElevation = 0.dp) {
                    val colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Palette.TextHi,
                        selectedTextColor = Palette.TextHi,
                        indicatorColor = Palette.SurfaceHigh,
                        unselectedIconColor = Palette.TextLo,
                        unselectedTextColor = Palette.TextLo,
                    )
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(stringResource(t.labelRes)) },
                            colors = colors,
                        )
                    }
                }
            }
        }
    ) { padding ->
        val openSettings = { tab = Tab.SETTINGS }
        Box(Modifier.padding(padding)) {
            Crossfade(targetState = tab, label = "tab") { t ->
                when (t) {
                    Tab.CALCULATOR -> CalculatorScreen(vm, openSettings)
                    Tab.CONVERT -> ConvertScreen(vm, openSettings)
                    Tab.SETTINGS -> SettingsScreen(vm)
                }
            }
        }
    }
}

/** Brand line in gold, then the screen title large — the reference layout. */
@Composable
private fun Header(tab: Tab) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    ) {
        Text(
            stringResource(R.string.app_name).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Palette.Gold,
        )
        Text(
            stringResource(tab.labelRes),
            style = MaterialTheme.typography.headlineMedium,
            color = Palette.TextHi,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
