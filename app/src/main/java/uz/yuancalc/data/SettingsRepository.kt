package uz.yuancalc.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.yuancalc.core.MoneyCurrency
import uz.yuancalc.core.PriceRounding
import uz.yuancalc.core.WeightUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yuancalc_settings")

private object Keys {
    val cargoRate = doublePreferencesKey("cargo_rate_usd_per_kg")
    val soft = doublePreferencesKey("soft_multiple")
    val profitable = doublePreferencesKey("profitable_multiple")
    val roundingStep = intPreferencesKey("price_rounding_step")
    val roundingMode = stringPreferencesKey("price_rounding_mode")
    val weightUnit = stringPreferencesKey("weight_unit")
    val myPriceCurrency = stringPreferencesKey("my_price_currency")
    val otherCostsCurrency = stringPreferencesKey("other_costs_currency")
    val language = stringPreferencesKey("language")
    val pinnedCny = doublePreferencesKey("pinned_cny_to_usd")
    val pinnedUzs = doublePreferencesKey("pinned_usd_to_uzs")
    val cachedCny = doublePreferencesKey("cached_cny_to_usd")
    val cachedUzs = doublePreferencesKey("cached_usd_to_uzs")
    val cachedAt = longPreferencesKey("cached_at_epoch_seconds")
    val lastCost = stringPreferencesKey("last_cost")
    val lastWeight = stringPreferencesKey("last_weight")
    val lastOther = stringPreferencesKey("last_other_costs")
    val lastMyPrice = stringPreferencesKey("last_my_price")
}

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toAppSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val updated = transform(prefs.toAppSettings())
            prefs.write(updated)
        }
    }

    suspend fun cacheRates(cnyToUsd: Double, usdToUzs: Double, atEpochSeconds: Long) {
        update {
            it.copy(
                cachedCnyToUsd = cnyToUsd,
                cachedUsdToUzs = usdToUzs,
                cachedAtEpochSeconds = atEpochSeconds,
            )
        }
    }
}

private inline fun <reified T : Enum<T>> String?.toEnumOr(fallback: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

private fun Preferences.toAppSettings(): AppSettings {
    val d = AppSettings.DEFAULT
    return AppSettings(
        cargoRateUsdPerKg = this[Keys.cargoRate] ?: d.cargoRateUsdPerKg,
        softMultiple = this[Keys.soft] ?: d.softMultiple,
        profitableMultiple = this[Keys.profitable] ?: d.profitableMultiple,
        priceRoundingStep = this[Keys.roundingStep] ?: d.priceRoundingStep,
        priceRoundingMode = this[Keys.roundingMode].toEnumOr(d.priceRoundingMode),
        weightUnit = this[Keys.weightUnit].toEnumOr(d.weightUnit),
        myPriceCurrency = this[Keys.myPriceCurrency].toEnumOr(d.myPriceCurrency),
        otherCostsCurrency = this[Keys.otherCostsCurrency].toEnumOr(d.otherCostsCurrency),
        language = this[Keys.language].toEnumOr(d.language),
        pinnedCnyToUsd = this[Keys.pinnedCny],
        pinnedUsdToUzs = this[Keys.pinnedUzs],
        cachedCnyToUsd = this[Keys.cachedCny],
        cachedUsdToUzs = this[Keys.cachedUzs],
        cachedAtEpochSeconds = this[Keys.cachedAt],
        lastCost = this[Keys.lastCost] ?: d.lastCost,
        lastWeight = this[Keys.lastWeight] ?: d.lastWeight,
        lastOtherCosts = this[Keys.lastOther] ?: d.lastOtherCosts,
        lastMyPrice = this[Keys.lastMyPrice] ?: d.lastMyPrice,
    )
}

private fun MutablePreferences.write(s: AppSettings) {
    this[Keys.cargoRate] = s.cargoRateUsdPerKg
    this[Keys.soft] = s.softMultiple
    this[Keys.profitable] = s.profitableMultiple
    this[Keys.roundingStep] = s.priceRoundingStep
    this[Keys.roundingMode] = s.priceRoundingMode.name
    this[Keys.weightUnit] = s.weightUnit.name
    this[Keys.myPriceCurrency] = s.myPriceCurrency.name
    this[Keys.otherCostsCurrency] = s.otherCostsCurrency.name
    this[Keys.language] = s.language.name
    this[Keys.lastCost] = s.lastCost
    this[Keys.lastWeight] = s.lastWeight
    this[Keys.lastOther] = s.lastOtherCosts
    this[Keys.lastMyPrice] = s.lastMyPrice

    if (s.pinnedCnyToUsd != null) this[Keys.pinnedCny] = s.pinnedCnyToUsd else remove(Keys.pinnedCny)
    if (s.pinnedUsdToUzs != null) this[Keys.pinnedUzs] = s.pinnedUsdToUzs else remove(Keys.pinnedUzs)
    if (s.cachedCnyToUsd != null) this[Keys.cachedCny] = s.cachedCnyToUsd else remove(Keys.cachedCny)
    if (s.cachedUsdToUzs != null) this[Keys.cachedUzs] = s.cachedUsdToUzs else remove(Keys.cachedUzs)
    if (s.cachedAtEpochSeconds != null) this[Keys.cachedAt] = s.cachedAtEpochSeconds else remove(Keys.cachedAt)
}
