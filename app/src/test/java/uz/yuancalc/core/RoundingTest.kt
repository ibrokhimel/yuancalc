package uz.yuancalc.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 273_877.2 is the exact soft-tier price for the reference fixture:
 * 50 CNY at 0.1488, 600 g at $9/kg, multiplied by 1.8, in so'm at 11850.
 */
private const val SOFT_TIER_EXACT = 273_877.2

class RoundingTest {

    @Test
    fun `step 1000 up`() {
        assertEquals(274_000.0, roundPrice(SOFT_TIER_EXACT, 1_000, PriceRounding.UP), 1e-6)
    }

    @Test
    fun `step 1000 nearest`() {
        assertEquals(274_000.0, roundPrice(SOFT_TIER_EXACT, 1_000, PriceRounding.NEAREST), 1e-6)
    }

    @Test
    fun `step 5000 up`() {
        assertEquals(275_000.0, roundPrice(SOFT_TIER_EXACT, 5_000, PriceRounding.UP), 1e-6)
    }

    @Test
    fun `step 5000 nearest`() {
        assertEquals(275_000.0, roundPrice(SOFT_TIER_EXACT, 5_000, PriceRounding.NEAREST), 1e-6)
    }

    @Test
    fun `step 10000 up and nearest differ`() {
        assertEquals(280_000.0, roundPrice(SOFT_TIER_EXACT, 10_000, PriceRounding.UP), 1e-6)
        assertEquals(270_000.0, roundPrice(SOFT_TIER_EXACT, 10_000, PriceRounding.NEAREST), 1e-6)
    }

    @Test
    fun `step off rounds to the nearest whole som`() {
        assertEquals(273_877.0, roundPrice(SOFT_TIER_EXACT, 0, PriceRounding.UP), 1e-6)
        assertEquals(273_877.0, roundPrice(SOFT_TIER_EXACT, 0, PriceRounding.NEAREST), 1e-6)
    }

    @Test
    fun `an exact multiple is left alone`() {
        assertEquals(274_000.0, roundPrice(274_000.0, 1_000, PriceRounding.UP), 1e-6)
    }

    @Test
    fun `zero stays zero`() {
        assertEquals(0.0, roundPrice(0.0, 1_000, PriceRounding.UP), 1e-6)
    }
}
