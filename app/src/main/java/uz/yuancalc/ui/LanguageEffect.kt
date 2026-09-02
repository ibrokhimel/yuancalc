package uz.yuancalc.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.os.LocaleListCompat
import uz.yuancalc.data.AppLanguage

/**
 * Applies the stored language choice. AppCompat persists it across launches on
 * its own via the AppLocalesMetadataHolderService entry in the manifest, so
 * this only has to react to a change.
 */
@Composable
fun ApplyLanguage(language: AppLanguage) {
    LaunchedEffect(language) {
        val tags = when (language) {
            AppLanguage.SYSTEM -> ""
            AppLanguage.ENGLISH -> "en"
            AppLanguage.UZBEK -> "uz"
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
    }
}
