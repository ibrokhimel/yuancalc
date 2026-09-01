package uz.yuancalc.data

import uz.yuancalc.core.RateBounds

/**
 * Tries each source in order and caches the first plausible result.
 *
 * The cache write is injected rather than taking a SettingsRepository directly,
 * so the fallback chain can be tested without Android or DataStore.
 */
class RatesRepository(
    private val apis: List<RatesApi>,
    private val cacheRates: suspend (cnyToUsd: Double, usdToUzs: Double, atEpochSeconds: Long) -> Unit,
) {
    /**
     * Timestamp of the pair fetched successfully during this session, or null if
     * none has been. The status line uses it to tell a rate fetched moments ago
     * from the same rate reloaded off disk, which is otherwise indistinguishable.
     */
    @Volatile
    var lastLiveFetchAt: Long? = null
        private set

    /** Returns true when a source produced a usable rate pair. */
    suspend fun refresh(): Boolean {
        for (api in apis) {
            val fetched = try {
                api.fetch()
            } catch (e: Exception) {
                null
            } ?: continue

            val usdToCny = if (fetched.cnyToUsd > 0.0) 1.0 / fetched.cnyToUsd else 0.0
            if (!RateBounds.isPlausible(fetched.usdToUzs, usdToCny)) continue

            // Set before the cache write so the settings emission it triggers
            // already sees this session's fetch and reports LIVE, not CACHED.
            lastLiveFetchAt = fetched.fetchedAtEpochSeconds
            cacheRates(fetched.cnyToUsd, fetched.usdToUzs, fetched.fetchedAtEpochSeconds)
            return true
        }
        return false
    }
}
