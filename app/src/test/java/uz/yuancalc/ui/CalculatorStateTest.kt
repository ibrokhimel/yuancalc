package uz.yuancalc.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.yuancalc.core.MarkupBand
import uz.yuancalc.core.MoneyCurrency
import uz.yuancalc.core.RateSource
import uz.yuancalc.core.WeightUnit
import uz.yuancalc.data.AppSettings

/** Settings pinned to the reference fixture so expectations are exact. */
private val FIXTURE_SETTINGS = AppSettings.DEFAULT.copy(
    pinnedCnyToUsd = 0.1488,
    pinnedUsdToUzs = 11_850.0,
)

private fun round2(v: Double): Double =
    java.math.BigDecimal.valueOf(v).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()

class CalculatorStateTest {

    @Test
    fun `computes the landed cost from grams`() {
        val s = computeState(
            CalculatorInputs(cost = "50", weight = "600", otherCosts = "", myPrice = ""),
            FIXTURE_SETTINGS,
        )
        assertEquals(12.84, s.landed.totalUsd, 1e-9)
    }

    @Test
    fun `reads weight in kilograms when that unit is selected`() {
        val s = computeState(
            CalculatorInputs(cost = "50", weight = "0,6", otherCosts = "", myPrice = ""),
            FIXTURE_SETTINGS.copy(weightUnit = WeightUnit.KILOGRAMS),
        )
        assertEquals(12.84, s.landed.totalUsd, 1e-9)
    }

    @Test
    fun `other costs entered in som are converted to usd`() {
        val s = computeState(
            CalculatorInputs(cost = "50", weight = "600", otherCosts = "11 850", myPrice = ""),
            FIXTURE_SETTINGS.copy(otherCostsCurrency = MoneyCurrency.UZS),
        )
        assertEquals(13.84, s.landed.totalUsd, 1e-9)
    }

    @Test
    fun `other costs entered in usd are used directly`() {
        val s = computeState(
            CalculatorInputs(cost = "50", weight = "600", otherCosts = "2", myPrice = ""),
            FIXTURE_SETTINGS.copy(otherCostsCurrency = MoneyCurrency.USD),
        )
        assertEquals(14.84, s.landed.totalUsd, 1e-9)
    }

    @Test
    fun `produces both tier quotes`() {
        val s = computeState(
            CalculatorInputs(cost = "50", weight = "600", otherCosts = "", myPrice = ""),
            FIXTURE_SETTINGS,
        )
        assertEquals(274_000.0, s.softQuote.priceUzs, 1e-6)
        assertEquals(350_000.0, s.profitableQuote.priceUzs, 1e-6)
    }

    @Test
    fun `checks a price entered in som`() {
        val s = computeState(
            CalculatorInputs(cost = "50", weight = "600", otherCosts = "", myPrice = "299 000"),
            FIXTURE_SETTINGS,
        )
        val check = s.myPriceCheck!!
        assertEquals(299_000.0, check.priceUzs, 1e-6)
        assertEquals(1.97, round2(check.markup!!), 1e-9)
        assertEquals(MarkupBand.OK, s.band)
    }

    @Test
    fun `checks a price entered in usd`() {
        val s = computeState(
            CalculatorInputs(cost = "50", weight = "600", otherCosts = "", myPrice = "25.2321"),
            FIXTURE_SETTINGS.copy(myPriceCurrency = MoneyCurrency.USD),
        )
        assertEquals(1.97, round2(s.myPriceCheck!!.markup!!), 1e-9)
    }

    @Test
    fun `my price is absent when the field is empty`() {
        val s = computeState(
            CalculatorInputs(cost = "50", weight = "600", otherCosts = "", myPrice = ""),
            FIXTURE_SETTINGS,
        )
        assertNull(s.myPriceCheck)
        assertEquals(MarkupBand.UNKNOWN, s.band)
    }

    @Test
    fun `sensitivity follows my price when one is set`() {
        val s = computeState(
            CalculatorInputs(cost = "50", weight = "600", otherCosts = "", myPrice = "299 000"),
            FIXTURE_SETTINGS,
        )
        assertEquals(listOf(500.0, 600.0, 700.0), s.sensitivity.map { it.weightGrams })
        assertEquals(2.11, round2(s.sensitivity[0].markup!!), 1e-9)
        assertEquals(1.84, round2(s.sensitivity[2].markup!!), 1e-9)
    }

    @Test
    fun `sensitivity falls back to the profitable tier when my price is empty`() {
        val s = computeState(
            CalculatorInputs(cost = "50", weight = "600", otherCosts = "", myPrice = ""),
            FIXTURE_SETTINGS,
        )
        assertEquals(2.30, round2(s.sensitivity[1].markup!!), 1e-2)
    }

    @Test
    fun `an empty cost leaves the markup undefined rather than crashing`() {
        val s = computeState(
            CalculatorInputs(cost = "", weight = "", otherCosts = "", myPrice = "299 000"),
            FIXTURE_SETTINGS,
        )
        assertEquals(0.0, s.landed.totalUsd, 1e-9)
        assertNull(s.myPriceCheck!!.markup)
        assertEquals(MarkupBand.UNKNOWN, s.band)
    }

    @Test
    fun `changed tier multiples flow through to the quotes`() {
        val s = computeState(
            CalculatorInputs(cost = "50", weight = "600", otherCosts = "", myPrice = ""),
            FIXTURE_SETTINGS.copy(softMultiple = 2.0, profitableMultiple = 3.0),
        )
        assertEquals(305_000.0, s.softQuote.priceUzs, 1e-6)
        assertEquals(457_000.0, s.profitableQuote.priceUzs, 1e-6)
    }

    @Test
    fun `rates fetched this session report as live rather than offline`() {
        val cached = AppSettings.DEFAULT.copy(
            cachedCnyToUsd = 0.1488,
            cachedUsdToUzs = 11_850.0,
            cachedAtEpochSeconds = 5_000L,
        )
        assertEquals(RateSource.CACHED, computeState(CalculatorInputs(), cached).rates.source)
        assertEquals(
            RateSource.LIVE,
            computeState(CalculatorInputs(), cached, liveFetchAt = 5_000L).rates.source,
        )
    }

    @Test
    fun `a stale cache is not upgraded to live by an unrelated fetch timestamp`() {
        val cached = AppSettings.DEFAULT.copy(
            cachedCnyToUsd = 0.1488,
            cachedUsdToUzs = 11_850.0,
            cachedAtEpochSeconds = 5_000L,
        )
        assertEquals(
            RateSource.CACHED,
            computeState(CalculatorInputs(), cached, liveFetchAt = 9_999L).rates.source,
        )
    }

    @Test
    fun `pinned rates are never relabelled as live`() {
        val s = computeState(CalculatorInputs(), FIXTURE_SETTINGS, liveFetchAt = 5_000L)
        assertEquals(RateSource.PINNED, s.rates.source)
    }
}
