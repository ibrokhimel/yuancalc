package uz.yuancalc.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeApi(private val result: FetchedRates?, private val throws: Boolean = false) : RatesApi {
    var calls = 0
    override suspend fun fetch(): FetchedRates? {
        calls++
        if (throws) throw java.io.IOException("network down")
        return result
    }
}

private class FakeCache {
    var cnyToUsd: Double? = null
    var usdToUzs: Double? = null
    var at: Long? = null
}

class RatesRepositoryTest {

    private fun repo(vararg apis: RatesApi): Pair<RatesRepository, FakeCache> {
        val cache = FakeCache()
        val r = RatesRepository(apis.toList()) { cny, uzs, at ->
            cache.cnyToUsd = cny
            cache.usdToUzs = uzs
            cache.at = at
        }
        return r to cache
    }

    @Test
    fun `a successful primary fetch is cached and the fallback is never called`() = runTest {
        val primary = FakeApi(FetchedRates(0.1488, 11_850.0, 1_000L))
        val fallback = FakeApi(FetchedRates(0.99, 99_999.0, 2_000L))
        val (r, cache) = repo(primary, fallback)

        assertTrue(r.refresh())
        assertEquals(0.1488, cache.cnyToUsd!!, 1e-9)
        assertEquals(11_850.0, cache.usdToUzs!!, 1e-9)
        assertEquals(1_000L, cache.at)
        assertEquals(1, primary.calls)
        assertEquals(0, fallback.calls)
    }

    @Test
    fun `the fallback is used when the primary returns nothing`() = runTest {
        val primary = FakeApi(null)
        val fallback = FakeApi(FetchedRates(0.1485, 11_817.0, 3_000L))
        val (r, cache) = repo(primary, fallback)

        assertTrue(r.refresh())
        assertEquals(0.1485, cache.cnyToUsd!!, 1e-9)
        assertEquals(1, primary.calls)
        assertEquals(1, fallback.calls)
    }

    @Test
    fun `the fallback is used when the primary throws`() = runTest {
        val primary = FakeApi(null, throws = true)
        val fallback = FakeApi(FetchedRates(0.1485, 11_817.0, 3_000L))
        val (r, cache) = repo(primary, fallback)

        assertTrue(r.refresh())
        assertEquals(0.1485, cache.cnyToUsd!!, 1e-9)
    }

    @Test
    fun `nothing is cached when every source fails`() = runTest {
        val (r, cache) = repo(FakeApi(null, throws = true), FakeApi(null))

        assertFalse(r.refresh())
        assertNull(cache.cnyToUsd)
        assertNull(cache.at)
    }

    @Test
    fun `an implausible som rate is rejected and the fallback is used`() = runTest {
        val primary = FakeApi(FetchedRates(0.1488, 12.0, 1_000L))
        val fallback = FakeApi(FetchedRates(0.1485, 11_817.0, 3_000L))
        val (r, cache) = repo(primary, fallback)

        assertTrue(r.refresh())
        assertEquals(11_817.0, cache.usdToUzs!!, 1e-9)
    }

    @Test
    fun `an implausible yuan rate is rejected too`() = runTest {
        // cnyToUsd 0.0001 implies usdToCny 10000, far outside the plausible range.
        val primary = FakeApi(FetchedRates(0.0001, 11_850.0, 1_000L))
        val fallback = FakeApi(FetchedRates(0.1485, 11_817.0, 3_000L))
        val (r, cache) = repo(primary, fallback)

        assertTrue(r.refresh())
        assertEquals(0.1485, cache.cnyToUsd!!, 1e-9)
    }
}
