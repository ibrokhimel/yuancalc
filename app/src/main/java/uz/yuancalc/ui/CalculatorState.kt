package uz.yuancalc.ui

import uz.yuancalc.core.LandedCost
import uz.yuancalc.core.MarkupBand
import uz.yuancalc.core.MoneyCurrency
import uz.yuancalc.core.PricingInput
import uz.yuancalc.core.RateSource
import uz.yuancalc.core.Rates
import uz.yuancalc.core.SensitivityRow
import uz.yuancalc.core.SourcingInput
import uz.yuancalc.core.TierQuote
import uz.yuancalc.core.WeightUnit
import uz.yuancalc.core.landedCost
import uz.yuancalc.core.markupBand
import uz.yuancalc.core.markupForPrice
import uz.yuancalc.core.maxCostCny
import uz.yuancalc.core.maxProductUsd
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
    val targetPrice: String = "",
)

data class PriceCheck(
    val priceUsd: Double,
    val priceUzs: Double,
    val markup: Double?,
    val profitUsd: Double,
    val profitUzs: Double,
)

data class SourcingTier(
    val multiple: Double,
    val maxProductUsd: Double?,
    val maxCostCny: Double?,
)

data class SourcingRow(val weightGrams: Double, val maxCostCny: Double?)

data class SourcingState(
    val targetUsd: Double,
    val targetUzs: Double,
    val cargoUsd: Double,
    /** Landed budget at the profitable tier — quoted when the price cannot work. */
    val landedBudgetUsd: Double?,
    val soft: SourcingTier,
    val profitable: SourcingTier,
    /** Max ¥ cost at the profitable tier for weight −100 g / as-is / +100 g. */
    val strip: List<SourcingRow>,
)

data class CalculatorState(
    val landed: LandedCost,
    val landedUzs: Double,
    val softQuote: TierQuote,
    val profitableQuote: TierQuote,
    val myPriceCheck: PriceCheck?,
    val band: MarkupBand,
    val sensitivity: List<SensitivityRow>,
    val sourcing: SourcingState,
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
    val otherUsd = toUsd(otherRaw, settings.otherCostsCurrency, rates)

    val cargoRate = settings.selectedCargoProfile().ratePerKgUsd

    val pricingInput = PricingInput(
        costUsd = toUsd(parseAmountOrZero(inputs.cost), settings.costCurrency, rates),
        weightGrams = weightGrams,
        cargoRateUsdPerKg = cargoRate,
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
        val priceUsd = toUsd(entered, settings.myPriceCurrency, rates)
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
        sourcing = computeSourcing(inputs, settings, rates, weightGrams, cargoRate, otherUsd),
        rates = rates,
    )
}

/** A raw entry normalized to USD; internal computation is USD-only. */
private fun toUsd(value: Double, currency: MoneyCurrency, rates: Rates): Double = when (currency) {
    MoneyCurrency.CNY -> value * rates.cnyToUsd
    MoneyCurrency.USD -> value
    MoneyCurrency.UZS -> if (rates.usdToUzs > 0.0) value / rates.usdToUzs else 0.0
}

/**
 * Reverse mode: from a target selling price down to the most that can be paid.
 * The target is used exactly as entered — [uz.yuancalc.core.roundPrice] rounds
 * *suggested* prices, not the user's own number.
 */
private fun computeSourcing(
    inputs: CalculatorInputs,
    settings: AppSettings,
    rates: Rates,
    weightGrams: Double,
    cargoRate: Double,
    otherUsd: Double,
): SourcingState {
    val targetUsd = toUsd(parseAmountOrZero(inputs.targetPrice), settings.targetPriceCurrency, rates)
    val input = SourcingInput(
        targetPriceUsd = targetUsd,
        weightGrams = weightGrams,
        cargoRateUsdPerKg = cargoRate,
        otherCostsUsd = otherUsd,
    )

    fun tier(multiple: Double): SourcingTier {
        val max = maxProductUsd(input, multiple)
        return SourcingTier(multiple, max, maxCostCny(max, rates))
    }

    val strip = listOf(weightGrams - 100.0, weightGrams, weightGrams + 100.0)
        .filter { it >= 0.0 }
        .map { grams ->
            val max = maxProductUsd(input.copy(weightGrams = grams), settings.profitableMultiple)
            SourcingRow(grams, maxCostCny(max, rates))
        }

    return SourcingState(
        targetUsd = targetUsd,
        targetUzs = targetUsd * rates.usdToUzs,
        cargoUsd = (weightGrams / 1_000.0) * cargoRate,
        landedBudgetUsd = if (settings.profitableMultiple > 0.0) {
            targetUsd / settings.profitableMultiple
        } else {
            null
        },
        soft = tier(settings.softMultiple),
        profitable = tier(settings.profitableMultiple),
        strip = strip,
    )
}
