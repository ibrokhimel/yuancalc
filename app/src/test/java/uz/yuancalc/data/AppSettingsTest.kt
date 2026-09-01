package uz.yuancalc.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.yuancalc.core.MoneyCurrency
import uz.yuancalc.core.PriceRounding
import uz.yuancalc.core.RateSource
import uz.yuancalc.core.WeightUnit

class AppSettingsTest {

    @Test
    fun `defaults match the spec`() {
        val d = AppSettings.DEFAULT
        assertEquals(9.0, d.cargoRateUsdPerKg, 1e-9)
        assertEquals(1.8, d.softMultiple, 1e-9)
        assertEquals(2.3, d.profitableMultiple, 1e-9)
        assertEquals(1_000, d.priceRoundingStep)
        assertEquals(PriceRounding.UP, d.priceRoundingMode)
        assertEquals(WeightUnit.GRAMS, d.weightUnit)
        assertEquals(MoneyCurrency.UZS, d.myPriceCurrency)
        assertEquals(MoneyCurrency.UZS, d.otherCostsCurrency)
        assertEquals(AppLanguage.SYSTEM, d.language)
        assertNull(d.pinnedCnyToUsd)
        assertNull(d.pinnedUsdToUzs)
        assertNull(d.cachedCnyToUsd)
        assertNull(d.cachedUsdToUzs)
    }

    @Test
    fun `unpinned unfetched settings resolve to the bundled rates`() {
        val rates = AppSettings.DEFAULT.resolveRates()
        assertEquals(0.1485, rates.cnyToUsd, 1e-9)
        assertEquals(11_817.0, rates.usdToUzs, 1e-9)
        assertEquals(RateSource.BUNDLED, rates.source)
    }

    @Test
    fun `cached rates are used when present`() {
        val rates = AppSettings.DEFAULT
            .copy(cachedCnyToUsd = 0.1488, cachedUsdToUzs = 11_850.0, cachedAtEpochSeconds = 1_000L)
            .resolveRates()
        assertEquals(0.1488, rates.cnyToUsd, 1e-9)
        assertEquals(11_850.0, rates.usdToUzs, 1e-9)
        assertEquals(RateSource.CACHED, rates.source)
        assertEquals(1_000L, rates.fetchedAtEpochSeconds)
    }

    @Test
    fun `a pinned rate overrides the cached one`() {
        val rates = AppSettings.DEFAULT
            .copy(
                cachedCnyToUsd = 0.1488,
                cachedUsdToUzs = 11_850.0,
                pinnedUsdToUzs = 12_000.0,
            )
            .resolveRates()
        assertEquals(0.1488, rates.cnyToUsd, 1e-9)
        assertEquals(12_000.0, rates.usdToUzs, 1e-9)
        assertEquals(RateSource.PINNED, rates.source)
    }

    @Test
    fun `either rate can be pinned independently`() {
        val rates = AppSettings.DEFAULT.copy(pinnedCnyToUsd = 0.15).resolveRates()
        assertEquals(0.15, rates.cnyToUsd, 1e-9)
        assertEquals(11_817.0, rates.usdToUzs, 1e-9)
        assertEquals(RateSource.PINNED, rates.source)
    }
}
