package uz.yuancalc.core

/**
 * True when [latest] is a strictly newer dotted version than [current].
 * Tolerates a leading "v" and unequal segment counts ("1.2" vs "1.2.1").
 * An unparseable version is never "newer" — a malformed release tag must not
 * nag the user on every visit to Settings.
 */
fun isNewerVersion(latest: String, current: String): Boolean {
    fun parse(v: String): List<Int>? =
        v.trim().removePrefix("v").removePrefix("V").split(".")
            .map { it.toIntOrNull() ?: return null }

    val l = parse(latest) ?: return false
    val c = parse(current) ?: return false
    for (i in 0 until maxOf(l.size, c.size)) {
        val a = l.getOrElse(i) { 0 }
        val b = c.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}
