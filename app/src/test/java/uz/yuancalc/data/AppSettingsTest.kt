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

    private val threeProfiles = AppSettings.DEFAULT.copy(
        cargoProfiles = listOf(
            CargoProfile("a", "Air", 15.0),
            CargoProfile("b", "Truck", 9.0),
            CargoProfile("c", "Rail", 6.5),
        ),
        selectedCargoProfileId = "b",
    )

    @Test
    fun `selected cargo profile resolves, with a fallback for dangling ids`() {
        assertEquals("Truck", threeProfiles.selectedCargoProfile().name)
        assertEquals(
            "Air",
            threeProfiles.copy(selectedCargoProfileId = "gone").selectedCargoProfile().name,
        )
    }

    @Test
    fun `deleting the selected profile reselects the first remaining`() {
        val after = threeProfiles.withCargoProfileDeleted("b")
        assertEquals(listOf("a", "c"), after.cargoProfiles.map { it.id })
        assertEquals("a", after.selectedCargoProfileId)
    }

    @Test
    fun `deleting an unselected profile keeps the selection`() {
        val after = threeProfiles.withCargoProfileDeleted("c")
        assertEquals("b", after.selectedCargoProfileId)
    }

    @Test
    fun `deleting the last profile is refused`() {
        val one = AppSettings.DEFAULT
        assertEquals(1, one.cargoProfiles.size)
        assertEquals(one, one.withCargoProfileDeleted(one.cargoProfiles.first().id))
    }

    @Test
    fun `rename keeps the id so the selection survives`() {
        val renamed = threeProfiles.withCargoProfileUpdated(CargoProfile("b", "Fast truck", 9.0))
        assertEquals("Fast truck", renamed.selectedCargoProfile().name)
        assertEquals("b", renamed.selectedCargoProfileId)
    }
}
