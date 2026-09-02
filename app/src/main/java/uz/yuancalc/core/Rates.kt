package uz.yuancalc.core

enum class RateSource { LIVE, CACHED, PINNED, BUNDLED }

data class Rates(
    val cnyToUsd: Double,
    val usdToUzs: Double,
    val source: RateSource,
    val fetchedAtEpochSeconds: Long?,
) {
    fun cnyToUzs(): Double = cnyToUsd * usdToUzs
}

/**
 * A malformed or hijacked API response would silently corrupt every figure in
 * the app, so a fetched pair is checked against generous but finite bounds and
 * treated as a failed fetch if it falls outside them.
 */
object RateBounds {
    const val MIN_USD_TO_UZS = 1_000.0
    const val MAX_USD_TO_UZS = 100_000.0
    const val MIN_USD_TO_CNY = 1.0
    const val MAX_USD_TO_CNY = 50.0

    fun isPlausible(usdToUzs: Double, usdToCny: Double): Boolean =
        usdToUzs.isFinite() && usdToCny.isFinite() &&
            usdToUzs in MIN_USD_TO_UZS..MAX_USD_TO_UZS &&
            usdToCny in MIN_USD_TO_CNY..MAX_USD_TO_CNY
}

/** Verified against both rate APIs on 2026-09-02. Used only before a first successful fetch. */
val BUNDLED_RATES = Rates(
    cnyToUsd = 0.1485,
    usdToUzs = 11_817.0,
    source = RateSource.BUNDLED,
    fetchedAtEpochSeconds = null,
)
