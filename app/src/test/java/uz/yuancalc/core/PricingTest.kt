package uz.yuancalc.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The reference fixture from the spec. Every expectation below is exact. */
private val FIXTURE = Rates(
    cnyToUsd = 0.1488,
    usdToUzs = 11_850.0,
    source = RateSource.BUNDLED,
    fetchedAtEpochSeconds = null,
)

/**
 * Test inputs still speak yuan; the ¥→$ conversion the app now does at the
 * field boundary happens here instead, so every landed expectation is
 * unchanged from the original spec.
 */
private fun input(
    costCny: Double = 50.0,
    weightGrams: Double = 600.0,
    cargoRateUsdPerKg: Double = 9.0,
    otherCostsUsd: Double = 0.0,
) = PricingInput(costCny * FIXTURE.cnyToUsd, weightGrams, cargoRateUsdPerKg, otherCostsUsd)

private fun sourcingInput(
    targetUzs: Double = 299_000.0,
    weightGrams: Double = 600.0,
    cargoRateUsdPerKg: Double = 9.0,
    otherCostsUsd: Double = 0.0,
) = SourcingInput(targetUzs / FIXTURE.usdToUzs, weightGrams, cargoRateUsdPerKg, otherCostsUsd)

private fun roundTo2(value: Double): Double =
    java.math.BigDecimal.valueOf(value)
        .setScale(2, java.math.RoundingMode.HALF_UP)
        .toDouble()

class PricingTest {

    @Test
    fun `landed cost splits into product, cargo and other`() {
        val landed = landedCost(input(), FIXTURE)
        assertEquals(7.44, landed.productUsd, 1e-9)
        assertEquals(5.40, landed.cargoUsd, 1e-9)
        assertEquals(0.0, landed.otherUsd, 1e-9)
        assertEquals(12.84, landed.totalUsd, 1e-9)
    }

    @Test
    fun `landed cost converts to som`() {
        val landed = landedCost(input(), FIXTURE).totalUsd
        assertEquals(152_154.0, landed * FIXTURE.usdToUzs, 1e-6)
    }

    @Test
    fun `a hundred yuan pair at six hundred grams`() {
        assertEquals(20.28, landedCost(input(costCny = 100.0), FIXTURE).totalUsd, 1e-9)
    }

    @Test
    fun `other costs are added to the landed total`() {
        val landed = landedCost(input(otherCostsUsd = 2.0), FIXTURE)
        assertEquals(2.0, landed.otherUsd, 1e-9)
        assertEquals(14.84, landed.totalUsd, 1e-9)
    }

    @Test
    fun `zero weight means zero cargo`() {
        assertEquals(0.0, landedCost(input(weightGrams = 0.0), FIXTURE).cargoUsd, 1e-9)
    }

    @Test
    fun `markup of the real 299k sale`() {
        val landed = landedCost(input(), FIXTURE).totalUsd
        val priceUsd = 299_000.0 / FIXTURE.usdToUzs
        val markup = markupForPrice(landed, priceUsd)!!
        assertEquals(1.97, roundTo2(markup), 1e-9)
        assertEquals(12.39, roundTo2(priceUsd - landed), 1e-9)
    }

    @Test
    fun `markup falls as the pair gets heavier`() {
        val priceUsd = 299_000.0 / FIXTURE.usdToUzs
        fun markupAt(grams: Double): Double {
            val landed = landedCost(input(weightGrams = grams), FIXTURE).totalUsd
            return roundTo2(markupForPrice(landed, priceUsd)!!)
        }
        assertEquals(2.11, markupAt(500.0), 1e-9)
        assertEquals(1.97, markupAt(600.0), 1e-9)
        assertEquals(1.84, markupAt(700.0), 1e-9)
        assertEquals(1.72, markupAt(800.0), 1e-9)
    }

    @Test
    fun `markup is undefined when nothing has been spent`() {
        assertNull(markupForPrice(0.0, 25.0))
        assertNull(markupForPrice(-1.0, 25.0))
    }

    @Test
    fun `soft tier quote is consistent between currencies`() {
        val landed = landedCost(input(), FIXTURE).totalUsd
        val quote = tierQuote(landed, 1.8, FIXTURE, 1_000, PriceRounding.UP)
        assertEquals(274_000.0, quote.priceUzs, 1e-6)
        assertEquals(23.12, roundTo2(quote.priceUsd), 1e-9)
        assertEquals(10.28, roundTo2(quote.profitUsd), 1e-9)
        assertEquals(121_846.0, quote.profitUzs, 1e-3)
    }

    @Test
    fun `profitable tier quote is consistent between currencies`() {
        val landed = landedCost(input(), FIXTURE).totalUsd
        val quote = tierQuote(landed, 2.3, FIXTURE, 1_000, PriceRounding.UP)
        assertEquals(350_000.0, quote.priceUzs, 1e-6)
        assertEquals(29.54, roundTo2(quote.priceUsd), 1e-9)
        assertEquals(16.70, roundTo2(quote.profitUsd), 1e-9)
        assertEquals(197_846.0, quote.profitUzs, 1e-3)
    }

    @Test
    fun `tier quote profit is derived from the rounded price`() {
        val landed = landedCost(input(), FIXTURE).totalUsd
        val quote = tierQuote(landed, 1.8, FIXTURE, 10_000, PriceRounding.UP)
        assertEquals(280_000.0, quote.priceUzs, 1e-6)
        assertEquals(280_000.0 - 152_154.0, quote.profitUzs, 1e-3)
    }

    @Test
    fun `band is tied to the configured tiers`() {
        assertEquals(MarkupBand.LOW, markupBand(1.5, soft = 1.8, profitable = 2.3))
        assertEquals(MarkupBand.OK, markupBand(1.97, soft = 1.8, profitable = 2.3))
        assertEquals(MarkupBand.OK, markupBand(1.8, soft = 1.8, profitable = 2.3))
        assertEquals(MarkupBand.GOOD, markupBand(2.3, soft = 1.8, profitable = 2.3))
        assertEquals(MarkupBand.GOOD, markupBand(3.0, soft = 1.8, profitable = 2.3))
        assertEquals(MarkupBand.UNKNOWN, markupBand(null, soft = 1.8, profitable = 2.3))
    }

    @Test
    fun `band still behaves when the tiers are entered the wrong way round`() {
        assertEquals(MarkupBand.OK, markupBand(2.0, soft = 2.3, profitable = 1.8))
    }

    @Test
    fun `sensitivity brackets the entered weight`() {
        val priceUsd = 299_000.0 / FIXTURE.usdToUzs
        val rows = sensitivity(input(), FIXTURE, priceUsd, stepGrams = 100.0)
        assertEquals(listOf(500.0, 600.0, 700.0), rows.map { it.weightGrams })
        assertEquals(2.11, roundTo2(rows[0].markup!!), 1e-9)
        assertEquals(1.97, roundTo2(rows[1].markup!!), 1e-9)
        assertEquals(1.84, roundTo2(rows[2].markup!!), 1e-9)
    }

    @Test
    fun `sensitivity omits rows at or below zero grams`() {
        val priceUsd = 299_000.0 / FIXTURE.usdToUzs
        val rows = sensitivity(input(weightGrams = 100.0), FIXTURE, priceUsd, stepGrams = 100.0)
        assertEquals(listOf(100.0, 200.0), rows.map { it.weightGrams })
    }

    @Test
    fun `rate bounds reject nonsense`() {
        assertTrue(RateBounds.isPlausible(usdToUzs = 11_850.0, usdToCny = 6.74))
        assertFalse(RateBounds.isPlausible(usdToUzs = 0.0, usdToCny = 6.74))
        assertFalse(RateBounds.isPlausible(usdToUzs = 11_850.0, usdToCny = 0.0))
        assertFalse(RateBounds.isPlausible(usdToUzs = 500_000.0, usdToCny = 6.74))
        assertFalse(RateBounds.isPlausible(usdToUzs = 11_850.0, usdToCny = 900.0))
    }

    @Test
    fun `price verdict zones follow the user's tiers`() {
        assertEquals(PriceVerdict.UNPROFITABLE, priceVerdict(0.99, soft = 1.8, profitable = 2.3))
        assertEquals(PriceVerdict.SOFT, priceVerdict(1.0, soft = 1.8, profitable = 2.3))
        assertEquals(PriceVerdict.SOFT, priceVerdict(1.79, soft = 1.8, profitable = 2.3))
        assertEquals(PriceVerdict.PROFITABLE, priceVerdict(1.8, soft = 1.8, profitable = 2.3))
        assertEquals(PriceVerdict.PROFITABLE, priceVerdict(2.3, soft = 1.8, profitable = 2.3))
        assertEquals(PriceVerdict.EXCELLENT, priceVerdict(2.35, soft = 1.8, profitable = 2.3))
        assertEquals(PriceVerdict.EXCELLENT, priceVerdict(3.3, soft = 1.8, profitable = 2.3))
        assertEquals(PriceVerdict.NOBODY, priceVerdict(3.35, soft = 1.8, profitable = 2.3))
    }

    @Test
    fun `price verdict tolerates swapped tiers`() {
        assertEquals(PriceVerdict.SOFT, priceVerdict(1.5, soft = 2.3, profitable = 1.8))
        assertEquals(PriceVerdict.PROFITABLE, priceVerdict(2.0, soft = 2.3, profitable = 1.8))
    }

    @Test
    fun `sourcing budget at the profitable tier matches the spec exactly`() {
        val max = maxProductUsd(sourcingInput(), 2.3)!!
        assertEquals(5.570464, max, 1e-6)
        assertEquals("$5.57", formatUsdFloor(max))
        assertEquals("¥37.43", formatCnyFloor(maxCostCny(max, FIXTURE)!!))
    }

    @Test
    fun `sourcing budget at the soft tier`() {
        val max = maxProductUsd(sourcingInput(), 1.8)!!
        assertEquals(8.617815, max, 1e-6)
        assertEquals("$8.61", formatUsdFloor(max))
        assertEquals("¥57.91", formatCnyFloor(maxCostCny(max, FIXTURE)!!))
    }

    @Test
    fun `sourcing budget shifts with weight`() {
        val at500 = maxCostCny(maxProductUsd(sourcingInput(weightGrams = 500.0), 2.3), FIXTURE)!!
        val at700 = maxCostCny(maxProductUsd(sourcingInput(weightGrams = 700.0), 2.3), FIXTURE)!!
        assertEquals("¥43.48", formatCnyFloor(at500))
        assertEquals("¥31.38", formatCnyFloor(at700))
    }

    @Test
    fun `floor rounding is the rule - half-up advice would miss the target`() {
        // The exact maximum is ¥37.4359. Feed the half-up display value ¥37.44
        // back through forward mode: the markup lands BELOW 2.3×, which is
        // exactly the bad advice floor rounding exists to prevent.
        val landedAtHalfUp = landedCost(input(costCny = 37.44), FIXTURE).totalUsd
        val markup = markupForPrice(landedAtHalfUp, 299_000.0 / FIXTURE.usdToUzs)!!
        assertTrue(markup < 2.3)

        // The floored ¥37.43 clears it.
        val landedAtFloor = landedCost(input(costCny = 37.43), FIXTURE).totalUsd
        assertTrue(markupForPrice(landedAtFloor, 299_000.0 / FIXTURE.usdToUzs)!! >= 2.3)
    }

    @Test
    fun `a forward-rounded price survives the reverse trip with headroom`() {
        // Forward: ¥50 at 600 g at 2.3× suggests 350 000 so'm (rounded up).
        // Reverse on that price returns slightly MORE than ¥50 — the rounding
        // headroom — and never less.
        val cny = maxCostCny(maxProductUsd(sourcingInput(targetUzs = 350_000.0), 2.3), FIXTURE)!!
        assertEquals(50.01, roundTo2(cny), 1e-9)
        assertTrue(cny > 50.0)
    }

    @Test
    fun `sourcing returns null when the price cannot work`() {
        // 60 000 so'm at 2.3×: cargo $5.40 exceeds the $2.20 landed budget.
        assertNull(maxProductUsd(sourcingInput(targetUzs = 60_000.0), 2.3))
        assertNull(maxProductUsd(sourcingInput(), 0.0))
        assertNull(maxProductUsd(sourcingInput(targetUzs = 0.0), 2.3))
    }

    @Test
    fun `zero weight leaves the whole budget for the product`() {
        val max = maxProductUsd(sourcingInput(weightGrams = 0.0), 2.3)!!
        assertEquals(299_000.0 / FIXTURE.usdToUzs / 2.3, max, 1e-9)
    }

    @Test
    fun `round trip - reverse then forward always clears the multiple`() {
        for (targetUzs in listOf(150_000.0, 299_000.0, 500_000.0, 1_000_000.0)) {
            for (grams in listOf(100.0, 600.0, 1_500.0)) {
                for (multiple in listOf(1.5, 1.8, 2.3, 3.0)) {
                    val max = maxProductUsd(
                        sourcingInput(targetUzs = targetUzs, weightGrams = grams),
                        multiple,
                    ) ?: continue
                    val landed = landedCost(
                        PricingInput(max, grams, 9.0, 0.0),
                        FIXTURE,
                    ).totalUsd
                    val markup = markupForPrice(landed, targetUzs / FIXTURE.usdToUzs)!!
                    assertTrue(
                        "markup $markup below $multiple at $targetUzs/$grams",
                        markup >= multiple - 1e-9,
                    )
                }
            }
        }
    }
}
