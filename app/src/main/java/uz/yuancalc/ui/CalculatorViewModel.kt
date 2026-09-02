package uz.yuancalc.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.yuancalc.BuildConfig
import uz.yuancalc.core.isNewerVersion
import uz.yuancalc.data.AppSettings
import uz.yuancalc.data.RatesRepository
import uz.yuancalc.data.SettingsRepository
import uz.yuancalc.data.UpdatesApi

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object UpToDate : UpdateStatus
    data class Available(val version: String, val url: String, val apkUrl: String?) : UpdateStatus
    data class Downloading(val percent: Int) : UpdateStatus
    data class ReadyToInstall(val file: File, val version: String) : UpdateStatus
    data object Failed : UpdateStatus
}

class CalculatorViewModel(
    private val settingsRepository: SettingsRepository,
    private val ratesRepository: RatesRepository,
    private val updatesApi: UpdatesApi,
    private val updatesDir: File,
) : ViewModel() {

    private val inputs = MutableStateFlow(CalculatorInputs())

    /** Bumped after a refresh so the combined state recomputes with the new rates. */
    private val refreshTick = MutableStateFlow(0)

    private val refreshingFlow = MutableStateFlow(false)

    /** True while a rate fetch is in flight; drives the status line spinner. */
    val refreshing: StateFlow<Boolean> = refreshingFlow

    private val updateFlow = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = updateFlow

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings.DEFAULT)

    val inputsFlow: StateFlow<CalculatorInputs> = inputs

    val state: StateFlow<CalculatorState> =
        combine(inputs, settingsRepository.settings, refreshTick) { i, s, _ ->
            computeState(i, s, ratesRepository.lastLiveFetchAt)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            computeState(CalculatorInputs(), AppSettings.DEFAULT),
        )

    private var restored = false

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                if (!restored) {
                    restored = true
                    // myPrice is not restored: its card is gone from the
                    // UI, and a stale value would invisibly steer the
                    // weight-sensitivity strip.
                    inputs.value = CalculatorInputs(
                        s.lastCost,
                        s.lastWeight,
                        s.lastOtherCosts,
                        "",
                        s.lastTargetPrice,
                    )
                }
            }
        }
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            inputs.debounce(400).collect { i ->
                if (restored) {
                    settingsRepository.update {
                        it.copy(
                            lastCost = i.cost,
                            lastWeight = i.weight,
                            lastOtherCosts = i.otherCosts,
                            lastMyPrice = i.myPrice,
                            lastTargetPrice = i.targetPrice,
                        )
                    }
                }
            }
        }
        refreshRates()
    }

    fun onCostChange(value: String) = edit { it.copy(cost = value) }
    fun onWeightChange(value: String) = edit { it.copy(weight = value) }
    fun onOtherCostsChange(value: String) = edit { it.copy(otherCosts = value) }
    fun onMyPriceChange(value: String) = edit { it.copy(myPrice = value) }
    fun onTargetPriceChange(value: String) = edit { it.copy(targetPrice = value) }

    /**
     * Checks GitHub for a newer release. User-triggered only — an automatic
     * check on every launch would burn the unauthenticated rate limit and nag.
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            updateFlow.value = UpdateStatus.Checking
            val latest = updatesApi.fetchLatest()
            updateFlow.value = when {
                latest == null -> UpdateStatus.Failed
                isNewerVersion(latest.versionName, BuildConfig.VERSION_NAME) ->
                    UpdateStatus.Available(latest.versionName, latest.pageUrl, latest.apkUrl)
                else -> UpdateStatus.UpToDate
            }
        }
    }

    /**
     * Pulls the release APK into the app cache and hands it to the system
     * installer — the whole update happens without leaving the app.
     */
    fun downloadUpdate() {
        val available = updateFlow.value as? UpdateStatus.Available ?: return
        val apkUrl = available.apkUrl ?: return
        viewModelScope.launch {
            updateFlow.value = UpdateStatus.Downloading(0)
            val dest = File(File(updatesDir, "updates"), "yuancalc-" + available.version + ".apk")
            val ok = updatesApi.download(apkUrl, dest) { percent ->
                updateFlow.value = UpdateStatus.Downloading(percent)
            }
            updateFlow.value =
                if (ok) UpdateStatus.ReadyToInstall(dest, available.version)
                else UpdateStatus.Failed
        }
    }

    fun refreshRates() {
        viewModelScope.launch {
            refreshingFlow.value = true
            val started = System.currentTimeMillis()
            try {
                ratesRepository.refresh()
            } finally {
                // A spinner that flashes for 50ms reads as a glitch; hold it
                // long enough to be seen before the tick takes over.
                val elapsed = System.currentTimeMillis() - started
                if (elapsed < 600) delay(600 - elapsed)
                refreshingFlow.value = false
                refreshTick.value += 1
            }
        }
    }

    /**
     * Typing only touches the in-memory state; persistence trails behind on a
     * debounce. A disk write per keystroke was measurable jank on-device, and
     * the last-inputs restore only matters across app restarts anyway.
     */
    private fun edit(transform: (CalculatorInputs) -> CalculatorInputs) {
        inputs.value = transform(inputs.value)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val ratesRepository: RatesRepository,
        private val updatesApi: UpdatesApi,
        private val updatesDir: File,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CalculatorViewModel(settingsRepository, ratesRepository, updatesApi, updatesDir) as T
    }
}
