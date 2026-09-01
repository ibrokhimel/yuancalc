package uz.yuancalc.data

import uz.yuancalc.core.BUNDLED_RATES
import uz.yuancalc.core.MoneyCurrency
import uz.yuancalc.core.PriceRounding
import uz.yuancalc.core.RateSource
import uz.yuancalc.core.Rates
import uz.yuancalc.core.WeightUnit

enum class AppLanguage { SYSTEM, ENGLISH, UZBEK }

data class AppSettings(
    val cargoRateUsdPerKg: Double,
    val softMultiple: Double,
    val profitableMultiple: Double,
    val priceRoundingStep: Int,
    val priceRoundingMode: PriceRounding,
    val weightUnit: WeightUnit,
    val myPriceCurrency: MoneyCurrency,
    val otherCostsCurrency: MoneyCurrency,
    val language: AppLanguage,
    val pinnedCnyToUsd: Double?,
    val pinnedUsdToUzs: Double?,
    val cachedCnyToUsd: Double?,
    val cachedUsdToUzs: Double?,
    val cachedAtEpochSeconds: Long?,
    val lastCost: String,
    val lastWeight: String,
    val lastOtherCosts: String,
    val lastMyPrice: String,
) {
    /**
     * Resolves the rate pair to use, in priority order: a pinned rate wins over
     * a cached one, which wins over the bundled fallback. Each side is resolved
     * independently, so pinning only the so'm rate leaves the yuan rate live.
     */
    fun resolveRates(): Rates {
        val cny = pinnedCnyToUsd ?: cachedCnyToUsd ?: BUNDLED_RATES.cnyToUsd
        val uzs = pinnedUsdToUzs ?: cachedUsdToUzs ?: BUNDLED_RATES.usdToUzs
        val source = when {
            pinnedCnyToUsd != null || pinnedUsdToUzs != null -> RateSource.PINNED
            cachedCnyToUsd != null || cachedUsdToUzs != null -> RateSource.CACHED
            else -> RateSource.BUNDLED
        }
        return Rates(
            cnyToUsd = cny,
            usdToUzs = uzs,
            source = source,
            fetchedAtEpochSeconds = cachedAtEpochSeconds,
        )
    }

    companion object {
        val DEFAULT = AppSettings(
            cargoRateUsdPerKg = 9.0,
            softMultiple = 1.8,
            profitableMultiple = 2.3,
            priceRoundingStep = 1_000,
            priceRoundingMode = PriceRounding.UP,
            weightUnit = WeightUnit.GRAMS,
            myPriceCurrency = MoneyCurrency.UZS,
            otherCostsCurrency = MoneyCurrency.UZS,
            language = AppLanguage.SYSTEM,
            pinnedCnyToUsd = null,
            pinnedUsdToUzs = null,
            cachedCnyToUsd = null,
            cachedUsdToUzs = null,
            cachedAtEpochSeconds = null,
            lastCost = "",
            lastWeight = "",
            lastOtherCosts = "",
            lastMyPrice = "",
        )
    }
}
