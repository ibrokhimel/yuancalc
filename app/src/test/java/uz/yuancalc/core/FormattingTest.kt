package uz.yuancalc.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

    @Test
    fun `groups thousands with a space`() {
        assertEquals("299 000", formatGrouped(299_000.0))
        assertEquals("88 164", formatGrouped(88_164.0))
        assertEquals("152 154", formatGrouped(152_154.0))
    }

    @Test
    fun `groups numbers below one thousand without a separator`() {
        assertEquals("0", formatGrouped(0.0))
        assertEquals("999", formatGrouped(999.0))
        assertEquals("1 000", formatGrouped(1_000.0))
    }

    @Test
    fun `rounds to a whole number when grouping`() {
        assertEquals("273 877", formatGrouped(273_877.2))
    }

    @Test
    fun `formats usd to two decimals`() {
        assertEquals("$12.84", formatUsd(12.8437))
        assertEquals("$7.44", formatUsd(7.44))
        assertEquals("$23.12", formatUsd(23.122362))
        assertEquals("$0.00", formatUsd(0.0))
    }

    @Test
    fun `formats usd with a trailing zero rather than truncating`() {
        assertEquals("$16.70", formatUsd(16.695865))
    }

    @Test
    fun `formats som with a suffix`() {
        assertEquals("299 000 so'm", formatUzs(299_000.0))
    }

    @Test
    fun `formats markup to two decimals with a multiplication sign`() {
        assertEquals("1.97×", formatMarkup(1.9651))
        assertEquals("2.11×", formatMarkup(2.1132))
        assertEquals("1.84×", formatMarkup(1.8364))
    }

    @Test
    fun `formats an undefined markup as an em dash`() {
        assertEquals("—", formatMarkup(null))
    }
}
