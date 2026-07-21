package com.darko.speleov1.util

import java.text.Normalizer
import java.util.Locale

private val SOV_DIACRITICS_REGEX = Regex("\\p{Mn}+")
private val SOV_WHITESPACE_REGEX = Regex("\\s+")
private val SOV_SEARCH_SEPARATOR_REGEX = Regex("[^a-z0-9]+")

/**
 * Zajednička normalizacija upita za testabilnu search logiku.
 * Čuva postojeće ponašanje iz MainViewModel: dijakritike se skidaju,
 * đ/Đ se mapiraju na d/D, separatori postaju jedan razmak.
 */
internal fun normalizeSovSearchText(value: String?): String {
    if (value.isNullOrBlank()) return ""
    val replaced = value.trim().replace('đ', 'd').replace('Đ', 'D')
    return Normalizer.normalize(replaced, Normalizer.Form.NFD)
        .replace(SOV_DIACRITICS_REGEX, "")
        .lowercase(Locale.ROOT)
        .replace(SOV_SEARCH_SEPARATOR_REGEX, " ")
        .replace(SOV_WHITESPACE_REGEX, " ")
        .trim()
}

internal fun compactSovSearchToken(value: String): String =
    value.replace(SOV_SEARCH_SEPARATOR_REGEX, "").trim()
