package uz.yuancalc.data

import uz.yuancalc.core.BUNDLED_RATES
import uz.yuancalc.core.MoneyCurrency
import uz.yuancalc.core.PriceRounding
import uz.yuancalc.core.RateSource
import uz.yuancalc.core.Rates
import uz.yuancalc.core.WeightUnit

enum class AppLanguage { SYSTEM, ENGLISH, UZBEK }

/** Forward "what do I charge" vs reverse "what can I pay". */
enum class CalcMode { PRICE, SOURCE }

/**
 * One cargo agent's rate. [id] is stable across renames so the selection
 * survives them; rates are $/kg (so'm-quoting agents are out of scope).
 */
@kotlinx.serialization.Serializable
data class CargoProfile(
    val id: String,
    val name: String,
    val ratePerKgUsd: Double,
)

data class AppSettings(
    val cargoRateUsdPerKg: Double,
    val softMultiple: Double,
    val profitableMultiple: Double,
    val priceRoundingStep: Int,
    val priceRoundingMode: PriceRounding,
    val weightUnit: WeightUnit,
    val myPriceCurrency: MoneyCurrency,
    val otherCostsCurrency: MoneyCurrency,
    val costCurrency: MoneyCurrency,
    val targetPriceCurrency: MoneyCurrency,
    val calcMode: CalcMode,
    val cargoProfiles: List<CargoProfile>,
    val selectedCargoProfileId: String?,
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
    val lastTargetPrice: String,
) {
    /**
     * The profile driving the cargo figure. Falls back to the first profile
     * (one always exists) so a dangling selection id cannot zero the cargo.
     */
    fun selectedCargoProfile(): CargoProfile =
        cargoProfiles.firstOrNull { it.id == selectedCargoProfileId }
            ?: cargoProfiles.first()

    /** Deleting the last profile is refused; deleting the selected one reselects. */
    fun withCargoProfileDeleted(id: String): AppSettings {
        if (cargoProfiles.size <= 1) return this
        val remaining = cargoProfiles.filterNot { it.id == id }
        val selected = if (selectedCargoProfileId == id) remaining.first().id else selectedCargoProfileId
        return copy(cargoProfiles = remaining, selectedCargoProfileId = selected)
    }

    /** Replaces the profile with the same id; renames keep the id, so selection survives. */
    fun withCargoProfileUpdated(profile: CargoProfile): AppSettings =
        copy(cargoProfiles = cargoProfiles.map { if (it.id == profile.id) profile else it })
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
        /** Deterministic id for the profile synthesized before any user edit. */
        const val DEFAULT_CARGO_PROFILE_ID = "cargo-default"

        val DEFAULT = AppSettings(
            cargoRateUsdPerKg = 9.0,
            softMultiple = 1.8,
            profitableMultiple = 2.3,
            priceRoundingStep = 1_000,
            priceRoundingMode = PriceRounding.UP,
            weightUnit = WeightUnit.GRAMS,
            myPriceCurrency = MoneyCurrency.UZS,
            otherCostsCurrency = MoneyCurrency.UZS,
            costCurrency = MoneyCurrency.CNY,
            targetPriceCurrency = MoneyCurrency.UZS,
            calcMode = CalcMode.PRICE,
            cargoProfiles = listOf(
                CargoProfile(id = DEFAULT_CARGO_PROFILE_ID, name = "Cargo", ratePerKgUsd = 9.0),
            ),
            selectedCargoProfileId = DEFAULT_CARGO_PROFILE_ID,
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
            lastTargetPrice = "",
        )
    }
}
