package uz.yuancalc.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.yuancalc.data.AppSettings
import uz.yuancalc.data.RatesRepository
import uz.yuancalc.data.SettingsRepository

class CalculatorViewModel(
    private val settingsRepository: SettingsRepository,
    private val ratesRepository: RatesRepository,
) : ViewModel() {

    private val inputs = MutableStateFlow(CalculatorInputs())

    /** Bumped after a refresh so the combined state recomputes with the new rates. */
    private val refreshTick = MutableStateFlow(0)

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
                    inputs.value = CalculatorInputs(
                        s.lastCost,
                        s.lastWeight,
                        s.lastOtherCosts,
                        s.lastMyPrice,
                    )
                }
            }
        }
        refreshRates()
    }

    fun onCostChange(value: String) = edit { it.copy(cost = value) }
    fun onWeightChange(value: String) = edit { it.copy(weight = value) }
    fun onOtherCostsChange(value: String) = edit { it.copy(otherCosts = value) }
    fun onMyPriceChange(value: String) = edit { it.copy(myPrice = value) }

    fun onSoftMultipleChange(value: Double) = updateSettings { it.copy(softMultiple = value) }
    fun onProfitableMultipleChange(value: Double) = updateSettings { it.copy(profitableMultiple = value) }

    fun refreshRates() {
        viewModelScope.launch {
            ratesRepository.refresh()
            refreshTick.value += 1
        }
    }

    private fun edit(transform: (CalculatorInputs) -> CalculatorInputs) {
        val next = transform(inputs.value)
        inputs.value = next
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(
                    lastCost = next.cost,
                    lastWeight = next.weight,
                    lastOtherCosts = next.otherCosts,
                    lastMyPrice = next.myPrice,
                )
            }
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val ratesRepository: RatesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CalculatorViewModel(settingsRepository, ratesRepository) as T
    }
}
