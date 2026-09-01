package uz.yuancalc.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberParsingTest {

    @Test
    fun `accepts a dot decimal separator`() {
        assertEquals(1.5, parseAmount("1.5")!!, 1e-9)
    }

    @Test
    fun `accepts a comma decimal separator`() {
        assertEquals(1.5, parseAmount("1,5")!!, 1e-9)
    }

    @Test
    fun `ignores ordinary space thousands separators`() {
        assertEquals(299_000.0, parseAmount("299 000")!!, 1e-9)
    }

    @Test
    fun `ignores non-breaking and narrow no-break spaces`() {
        assertEquals(299_000.0, parseAmount("299\u00A0000")!!, 1e-9)
        assertEquals(299_000.0, parseAmount("299\u202F000")!!, 1e-9)
    }

    @Test
    fun `returns null for blank input`() {
        assertNull(parseAmount(""))
        assertNull(parseAmount("   "))
    }

    @Test
    fun `returns null for negative input`() {
        assertNull(parseAmount("-5"))
    }

    @Test
    fun `returns null for non-numeric input`() {
        assertNull(parseAmount("abc"))
    }

    @Test
    fun `returns null when there are two decimal separators`() {
        assertNull(parseAmount("1.2.3"))
        assertNull(parseAmount("1,2,3"))
    }

    @Test
    fun `parses zero`() {
        assertEquals(0.0, parseAmount("0")!!, 1e-9)
    }

    @Test
    fun `parseAmountOrZero substitutes zero for unparseable input`() {
        assertEquals(0.0, parseAmountOrZero(""), 1e-9)
        assertEquals(0.0, parseAmountOrZero("abc"), 1e-9)
        assertEquals(2.5, parseAmountOrZero("2,5"), 1e-9)
    }
}
