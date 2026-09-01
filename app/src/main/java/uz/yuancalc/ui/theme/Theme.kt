package uz.yuancalc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BandLow = Color(0xFFD32F2F)
val BandOk = Color(0xFFE08600)
val BandGood = Color(0xFF2E7D32)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E20),
    secondary = Color(0xFF4E6A50),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BC48F),
    secondary = Color(0xFFB2CCB4),
)

@Composable
fun YuanCalcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
