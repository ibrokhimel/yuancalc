package uz.yuancalc.ui

import uz.yuancalc.core.LandedCost
import uz.yuancalc.core.MarkupBand
import uz.yuancalc.core.MoneyCurrency
import uz.yuancalc.core.PricingInput
import uz.yuancalc.core.RateSource
import uz.yuancalc.core.Rates
import uz.yuancalc.core.SensitivityRow
import uz.yuancalc.core.TierQuote
import uz.yuancalc.core.WeightUnit
import uz.yuancalc.core.landedCost
import uz.yuancalc.core.markupBand
import uz.yuancalc.core.markupForPrice
import uz.yuancalc.core.parseAmount
import uz.yuancalc.core.parseAmountOrZero
import uz.yuancalc.core.sensitivity
import uz.yuancalc.core.tierQuote
import uz.yuancalc.data.AppSettings

data class CalculatorInputs(
    val cost: String = "",
    val weight: String = "",
    val otherCosts: String = "",
    val myPrice: String = "",
)

data class PriceCheck(
    val priceUsd: Double,
    val priceUzs: Double,
    val markup: Double?,
    val profitUsd: Double,
    val profitUzs: Double,
)

data class CalculatorState(
    val landed: LandedCost,
    val landedUzs: Double,
    val softQuote: TierQuote,
    val profitableQuote: TierQuote,
    val myPriceCheck: PriceCheck?,
    val band: MarkupBand,
    val sensitivity: List<SensitivityRow>,
    val rates: Rates,
)

/**
 * Turns raw text plus settings into everything the screen displays. Pure, so
 * the whole calculator can be tested without Android.
 *
 * [liveFetchAt] is the timestamp of a rate pair fetched during this session. It
 * exists only so the status line can say "Live" rather than "Offline" for rates
 * that were just downloaded — the stored settings cannot tell those apart.
 */
fun computeState(
    inputs: CalculatorInputs,
    settings: AppSettings,
    liveFetchAt: Long? = null,
): CalculatorState {
    val stored = settings.resolveRates()
    val rates = if (
        stored.source == RateSource.CACHED &&
        liveFetchAt != null &&
        stored.fetchedAtEpochSeconds == liveFetchAt
    ) {
        stored.copy(source = RateSource.LIVE)
    } else {
        stored
    }

    val weightValue = parseAmountOrZero(inputs.weight)
    val weightGrams = when (settings.weightUnit) {
        WeightUnit.GRAMS -> weightValue
        WeightUnit.KILOGRAMS -> weightValue * 1_000.0
    }

    val otherRaw = parseAmountOrZero(inputs.otherCosts)
    val otherUsd = when (settings.otherCostsCurrency) {
        MoneyCurrency.USD -> otherRaw
        MoneyCurrency.UZS -> if (rates.usdToUzs > 0.0) otherRaw / rates.usdToUzs else 0.0
    }

    val pricingInput = PricingInput(
        costCny = parseAmountOrZero(inputs.cost),
        weightGrams = weightGrams,
        cargoRateUsdPerKg = settings.cargoRateUsdPerKg,
        otherCostsUsd = otherUsd,
    )

    val landed = landedCost(pricingInput, rates)
    val landedTotal = landed.totalUsd

    val soft = tierQuote(
        landedTotal, settings.softMultiple, rates,
        settings.priceRoundingStep, settings.priceRoundingMode,
    )
    val profitable = tierQuote(
        landedTotal, settings.profitableMultiple, rates,
        settings.priceRoundingStep, settings.priceRoundingMode,
    )

    val check = parseAmount(inputs.myPrice)?.let { entered ->
        val priceUsd = when (settings.myPriceCurrency) {
            MoneyCurrency.USD -> entered
            MoneyCurrency.UZS -> if (rates.usdToUzs > 0.0) entered / rates.usdToUzs else 0.0
        }
        val priceUzs = priceUsd * rates.usdToUzs
        PriceCheck(
            priceUsd = priceUsd,
            priceUzs = priceUzs,
            markup = markupForPrice(landedTotal, priceUsd),
            profitUsd = priceUsd - landedTotal,
            profitUzs = priceUzs - landedTotal * rates.usdToUzs,
        )
    }

    // With no price of their own to check, the strip shows what the profitable
    // suggestion would be worth at each weight — still the same question.
    val sensitivityPriceUsd = check?.priceUsd ?: profitable.priceUsd

    return CalculatorState(
        landed = landed,
        landedUzs = landedTotal * rates.usdToUzs,
        softQuote = soft,
        profitableQuote = profitable,
        myPriceCheck = check,
        band = markupBand(check?.markup, settings.softMultiple, settings.profitableMultiple),
        sensitivity = sensitivity(pricingInput, rates, sensitivityPriceUsd),
        rates = rates,
    )
}
