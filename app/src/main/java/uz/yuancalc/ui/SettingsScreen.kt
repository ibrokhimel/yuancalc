package uz.yuancalc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.util.UUID
import uz.yuancalc.BuildConfig
import uz.yuancalc.R
import uz.yuancalc.core.PriceRounding
import uz.yuancalc.data.AppLanguage
import uz.yuancalc.data.CargoProfile
import uz.yuancalc.ui.components.DraftNumberField
import uz.yuancalc.ui.components.DraftTextField
import uz.yuancalc.ui.components.OptionToggle
import uz.yuancalc.ui.components.SectionCard
import uz.yuancalc.ui.theme.BandGood
import uz.yuancalc.ui.theme.BandLow
import uz.yuancalc.ui.theme.Ds
import uz.yuancalc.ui.theme.Palette

@Composable
fun SettingsScreen(vm: CalculatorViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(stringResource(R.string.settings_cargo_profiles), rounded = true) {
            s.cargoProfiles.forEach { profile ->
                key(profile.id) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        DraftTextField(
                            label = stringResource(R.string.cargo_profile_name),
                            value = profile.name,
                            onCommit = { name ->
                                vm.updateSettings { it.withCargoProfileUpdated(profile.copy(name = name)) }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        DraftNumberField(
                            label = stringResource(R.string.cargo_profile_rate),
                            value = profile.ratePerKgUsd,
                            onCommit = { v ->
                                v?.let { rate ->
                                    vm.updateSettings {
                                        it.withCargoProfileUpdated(profile.copy(ratePerKgUsd = rate))
                                    }
                                }
                            },
                            accept = { it > 0.0 },
                            modifier = Modifier.width(100.dp),
                        )
                        if (s.cargoProfiles.size > 1) {
                            TextButton(
                                onClick = { vm.updateSettings { it.withCargoProfileDeleted(profile.id) } },
                            ) {
                                Text(stringResource(R.string.cargo_profile_delete), color = BandLow)
                            }
                        }
                    }
                }
            }
            TextButton(
                onClick = {
                    vm.updateSettings {
                        it.copy(
                            cargoProfiles = it.cargoProfiles + CargoProfile(
                                id = UUID.randomUUID().toString(),
                                name = "Cargo " + (it.cargoProfiles.size + 1),
                                ratePerKgUsd = 9.0,
                            ),
                        )
                    }
                },
            ) {
                Text("+ " + stringResource(R.string.cargo_profile_add))
            }
        }

        SectionCard(stringResource(R.string.settings_rounding)) {
            OptionToggle(
                options = listOf(
                    0 to stringResource(R.string.settings_rounding_off),
                    500 to "500",
                    1_000 to "1k",
                    5_000 to "5k",
                    10_000 to "10k",
                ),
                selected = s.priceRoundingStep,
                onSelect = { step -> vm.updateSettings { it.copy(priceRoundingStep = step) } },
                fillEqually = true,
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
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                DraftNumberField(
                    label = stringResource(R.string.settings_pin_cny),
                    value = s.pinnedCnyToUsd,
                    onCommit = { v -> vm.updateSettings { it.copy(pinnedCnyToUsd = v) } },
                    allowEmpty = true,
                    accept = { it > 0.0 },
                    placeholder = stringResource(R.string.settings_not_pinned),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { vm.updateSettings { it.copy(pinnedCnyToUsd = null) } }) {
                    Text(stringResource(R.string.settings_unpin))
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                DraftNumberField(
                    label = stringResource(R.string.settings_pin_uzs),
                    value = s.pinnedUsdToUzs,
                    onCommit = { v -> vm.updateSettings { it.copy(pinnedUsdToUzs = v) } },
                    allowEmpty = true,
                    accept = { it > 0.0 },
                    placeholder = stringResource(R.string.settings_not_pinned),
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
                fillEqually = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }

        SectionCard(stringResource(R.string.settings_app), rounded = true) {
            val update by vm.updateStatus.collectAsStateWithLifecycle()
            val uriHandler = LocalUriHandler.current

            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextMid,
                modifier = Modifier.padding(top = 10.dp),
            )
            Button(
                onClick = vm::checkForUpdates,
                enabled = update != UpdateStatus.Checking,
                modifier = Modifier.padding(top = 10.dp),
            ) {
                Text(
                    stringResource(
                        if (update == UpdateStatus.Checking) R.string.update_checking
                        else R.string.update_check
                    )
                )
            }
            when (val u = update) {
                UpdateStatus.UpToDate -> Text(
                    stringResource(R.string.update_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = BandGood,
                    modifier = Modifier.padding(top = 10.dp),
                )
                UpdateStatus.Failed -> Text(
                    stringResource(R.string.update_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = BandLow,
                    modifier = Modifier.padding(top = 10.dp),
                )
                is UpdateStatus.Available -> UpdateNoteRow(
                    text = stringResource(R.string.update_available, u.version) +
                        " · " + stringResource(R.string.update_get),
                    onClick = {
                        if (u.apkUrl != null) vm.downloadUpdate()
                        else uriHandler.openUri(u.url)
                    },
                )
                is UpdateStatus.Downloading -> UpdateNoteRow(
                    text = stringResource(R.string.update_downloading, u.percent),
                    onClick = {},
                )
                is UpdateStatus.ReadyToInstall -> {
                    val context = LocalContext.current
                    LaunchedEffect(u.file) { installApk(context, u.file) }
                    UpdateNoteRow(
                        text = stringResource(R.string.update_install, u.version),
                        onClick = { installApk(context, u.file) },
                    )
                }
                else -> Unit
            }
        }
    }
}

/** The accent one-liner under the update button: icon + short call to action. */
@Composable
private fun UpdateNoteRow(text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(top = 10.dp)
            .clickable(onClick = onClick),
    ) {
        Icon(
            Icons.Filled.ArrowCircleDown,
            contentDescription = null,
            tint = Palette.Accent,
            modifier = Modifier.size(16.dp),
        )
        Text(text, style = Ds.UpdateNote, color = Palette.Accent)
    }
}

/**
 * Hands the downloaded APK to the system installer. Android always shows its
 * own confirmation sheet here; no app can skip that step, so this is as
 * automatic as an update gets outside a store.
 */
private fun installApk(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
