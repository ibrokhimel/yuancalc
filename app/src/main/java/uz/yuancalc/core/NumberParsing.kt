package uz.yuancalc.core

private const val NBSP = '\u00A0'
private const val NARROW_NBSP = '\u202F'

/**
 * Parses a number the way a person types it: '.' or ',' as the decimal
 * separator, spaces of any kind ignored as thousands grouping.
 *
 * Returns null for blank, malformed, or negative input. Negative is rejected
 * rather than clamped so callers can tell "nothing entered" from "zero".
 */
fun parseAmount(raw: String): Double? {
    val cleaned = buildString(raw.length) {
        for (ch in raw) {
            when (ch) {
                ' ', NBSP, NARROW_NBSP -> Unit
                ',' -> append('.')
                else -> append(ch)
            }
        }
    }

    if (cleaned.isEmpty()) return null
    if (cleaned.count { it == '.' } > 1) return null

    val value = cleaned.toDoubleOrNull() ?: return null
    if (value.isNaN() || value.isInfinite()) return null
    if (value < 0.0) return null

    return value
}

/** As [parseAmount], but unparseable input reads as zero. */
fun parseAmountOrZero(raw: String): Double = parseAmount(raw) ?: 0.0
