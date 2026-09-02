package uz.yuancalc.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionsTest {

    @Test
    fun `newer dotted versions are detected`() {
        assertTrue(isNewerVersion("1.2", "1.1"))
        assertTrue(isNewerVersion("2.0", "1.9"))
        assertTrue(isNewerVersion("1.1.1", "1.1"))
        assertTrue(isNewerVersion("v1.2", "1.1"))
    }

    @Test
    fun `same or older versions are not newer`() {
        assertFalse(isNewerVersion("1.1", "1.1"))
        assertFalse(isNewerVersion("1.0", "1.1"))
        assertFalse(isNewerVersion("1.1", "1.1.0"))
        assertFalse(isNewerVersion("1.9", "2.0"))
    }

    @Test
    fun `garbage tags never nag`() {
        assertFalse(isNewerVersion("latest", "1.1"))
        assertFalse(isNewerVersion("", "1.1"))
        assertFalse(isNewerVersion("1.2", "not-a-version"))
    }
}
