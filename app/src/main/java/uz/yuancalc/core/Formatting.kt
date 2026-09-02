package uz.yuancalc.core

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Display formatting. Deliberately not locale-derived: the grouping and
 * decimal separators are identical in English and Uzbek so a number never
 * changes shape when the interface language does.
 *
 * BigDecimal.valueOf is used rather than the BigDecimal(Double) constructor so
 * rounding follows the number as written rather than its binary expansion —
 * 12.845 must format as "12.85", not "12.84".
 */

private const val GROUP_SEPARATOR = " "

/** 299000.0 -> "299 000". Rounds half-up to a whole number first. */
fun formatGrouped(value: Double): String {
    val whole = BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toBigInteger()
    val digits = whole.abs().toString()
    val grouped = digits
        .reversed()
        .chunked(3)
        .joinToString(GROUP_SEPARATOR)
        .reversed()
    return if (whole.signum() < 0) "-" + grouped else grouped
}

/** 12.8437 -> "$12.84" */
fun formatUsd(value: Double): String {
    val amount = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
    return "$" + amount.toPlainString()
}

/** 299000.0 -> "299 000 so'm" */
fun formatUzs(value: Double): String = formatGrouped(value) + " so'm"

/** 1.9651 -> "1.97x" with a true multiplication sign; null -> em dash. */
fun formatMarkup(value: Double?): String {
    if (value == null) return "—"
    val amount = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
    return amount.toPlainString() + "×"
}

/**
 * Maximums round DOWN, unlike every other figure in the app. A max cost of
 * ¥37.4359 shown as ¥37.44 tells the user to pay a price that misses the
 * target markup they asked for; ¥37.43 clears it.
 */
fun formatUsdFloor(value: Double): String =
    "$" + BigDecimal.valueOf(value).setScale(2, RoundingMode.FLOOR).toPlainString()

/** ¥ maximum, floored for the same reason as [formatUsdFloor]. */
fun formatCnyFloor(value: Double): String =
    "¥" + BigDecimal.valueOf(value).setScale(2, RoundingMode.FLOOR).toPlainString()

/** ¥50.014 -> "¥50.01", half-up — for costs already paid, not maximums. */
fun formatCny(value: Double): String =
    "¥" + BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString()
