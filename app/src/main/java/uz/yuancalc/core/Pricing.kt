package uz.yuancalc.core

enum class WeightUnit { GRAMS, KILOGRAMS }

enum class MoneyCurrency { CNY, USD, UZS }

/**
 * All internal computation is in USD as the base unit; a cost entered in ¥ is
 * converted at the boundary where the field is read, not here.
 */
data class PricingInput(
    val costUsd: Double,
    val weightGrams: Double,
    val cargoRateUsdPerKg: Double,
    val otherCostsUsd: Double,
)

data class LandedCost(
    val productUsd: Double,
    val cargoUsd: Double,
    val otherUsd: Double,
) {
    val totalUsd: Double get() = productUsd + cargoUsd + otherUsd
}

fun landedCost(input: PricingInput, rates: Rates): LandedCost = LandedCost(
    productUsd = input.costUsd,
    cargoUsd = (input.weightGrams / 1_000.0) * input.cargoRateUsdPerKg,
    otherUsd = input.otherCostsUsd,
)

data class SourcingInput(
    val targetPriceUsd: Double,
    val weightGrams: Double,
    val cargoRateUsdPerKg: Double,
    val otherCostsUsd: Double,
)

/**
 * Sourcing mode's core question: the most that can be paid for the product
 * itself, so that the target price still clears [multiple].
 *
 * Null when the multiple is not positive, when there is no target, or when
 * cargo and other costs already eat the whole budget — a negative maximum is
 * not advice, it is a reason, and the caller explains it instead.
 */
fun maxProductUsd(input: SourcingInput, multiple: Double): Double? {
    if (multiple <= 0.0 || input.targetPriceUsd <= 0.0) return null
    val cargoUsd = (input.weightGrams / 1_000.0) * input.cargoRateUsdPerKg
    val budget = input.targetPriceUsd / multiple - cargoUsd - input.otherCostsUsd
    return if (budget > 0.0) budget else null
}

fun maxCostCny(maxProductUsd: Double?, rates: Rates): Double? =
    maxProductUsd?.let { if (rates.cnyToUsd > 0.0) it / rates.cnyToUsd else null }

/** Null when nothing has been spent — there is no meaningful multiple of zero. */
fun markupForPrice(landedUsd: Double, priceUsd: Double): Double? =
    if (landedUsd <= 0.0) null else priceUsd / landedUsd

data class TierQuote(
    val multiple: Double,
    val priceUsd: Double,
    val priceUzs: Double,
    val profitUsd: Double,
    val profitUzs: Double,
)

/**
 * A suggested price. The exact USD price is converted to so'm, rounded there,
 * and the USD figure is converted back from the rounded so'm — so the two
 * currencies on screen always describe the same price, and the profit beside
 * them follows the price the user would actually charge.
 */
fun tierQuote(
    landedUsd: Double,
    multiple: Double,
    rates: Rates,
    step: Int,
    mode: PriceRounding,
): TierQuote {
    val exactUzs = landedUsd * multiple * rates.usdToUzs
    val priceUzs = roundPrice(exactUzs, step, mode)
    val priceUsd = if (rates.usdToUzs > 0.0) priceUzs / rates.usdToUzs else 0.0
    val landedUzs = landedUsd * rates.usdToUzs
    return TierQuote(
        multiple = multiple,
        priceUsd = priceUsd,
        priceUzs = priceUzs,
        profitUsd = priceUsd - landedUsd,
        profitUzs = priceUzs - landedUzs,
    )
}

enum class MarkupBand { LOW, OK, GOOD, UNKNOWN }

enum class PriceVerdict { UNPROFITABLE, SOFT, PROFITABLE, EXCELLENT, NOBODY }

/**
 * The blunt read on a candidate price, in the seller's own terms: below cost
 * it's a loss, below the soft tier it's thin, between the tiers it's the
 * target zone, above them it's a great price — until it drifts a full turn
 * past the profitable tier, where it stops selling at all.
 */
fun priceVerdict(multiple: Double, soft: Double, profitable: Double): PriceVerdict {
    val low = minOf(soft, profitable)
    val high = maxOf(soft, profitable)
    return when {
        multiple < 1.0 -> PriceVerdict.UNPROFITABLE
        multiple < low -> PriceVerdict.SOFT
        multiple <= high -> PriceVerdict.PROFITABLE
        multiple <= high + 1.0 -> PriceVerdict.EXCELLENT
        else -> PriceVerdict.NOBODY
    }
}

/**
 * Thresholds come from the user's own tiers rather than fixed constants, so the
 * colour always means "against the target I set".
 */
fun markupBand(markup: Double?, soft: Double, profitable: Double): MarkupBand {
    if (markup == null) return MarkupBand.UNKNOWN
    val low = minOf(soft, profitable)
    val high = maxOf(soft, profitable)
    return when {
        markup < low -> MarkupBand.LOW
        markup < high -> MarkupBand.OK
        else -> MarkupBand.GOOD
    }
}

data class SensitivityRow(val weightGrams: Double, val markup: Double?)

/**
 * What the markup becomes if the weight guess is off. Weight moves the answer
 * more than people expect, so this is on the main screen rather than hidden.
 */
fun sensitivity(
    input: PricingInput,
    rates: Rates,
    priceUsd: Double,
    stepGrams: Double = 100.0,
): List<SensitivityRow> =
    listOf(
        input.weightGrams - stepGrams,
        input.weightGrams,
        input.weightGrams + stepGrams,
    )
        .filter { it > 0.0 }
        .map { grams ->
            val landed = landedCost(input.copy(weightGrams = grams), rates).totalUsd
            SensitivityRow(grams, markupForPrice(landed, priceUsd))
        }
