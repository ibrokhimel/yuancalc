package uz.yuancalc.core

import kotlin.math.ceil
import kotlin.math.round

enum class PriceRounding { UP, NEAREST }

/**
 * Rounds a so'm price to a whole multiple of [step].
 *
 * UP is the default because a suggested price rounded down quietly gives away
 * margin, which is the opposite of what this screen is for. A [step] of 0 or
 * less means no step rounding — the value is returned to the nearest so'm.
 */
fun roundPrice(valueUzs: Double, step: Int, mode: PriceRounding): Double {
    if (step <= 0) return round(valueUzs)
    val steps = valueUzs / step
    val whole = when (mode) {
        PriceRounding.UP -> ceil(steps)
        PriceRounding.NEAREST -> round(steps)
    }
    return whole * step
}
