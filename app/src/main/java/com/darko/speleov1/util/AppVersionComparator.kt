package com.darko.speleov1.util

/**
 * Čista logika usporedbe verzija za self-update i unit testove.
 *
 * Primjeri koje mora podržati:
 * - 1.4.33 > 1.4.9
 * - v1.4.33a-armory ≈ 1.4.33
 * - 1.4.33a-2 > 1.4.33a-1
 */
internal fun compareAppVersionNames(left: String, right: String): Int {
    val leftParts = extractAppVersionNumbers(left)
    val rightParts = extractAppVersionNumbers(right)
    val maxSize = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until maxSize) {
        val l = leftParts.getOrElse(index) { 0 }
        val r = rightParts.getOrElse(index) { 0 }
        if (l != r) return l.compareTo(r)
    }
    return 0
}

internal fun extractAppVersionNumbers(raw: String): List<Int> {
    val normalized = raw.trim().removePrefix("v").removePrefix("V")
    return normalized
        .split('.', '-', '_')
        .mapNotNull { token -> token.filter(Char::isDigit).takeIf { it.isNotBlank() }?.toIntOrNull() }
        .ifEmpty { listOf(0) }
}
