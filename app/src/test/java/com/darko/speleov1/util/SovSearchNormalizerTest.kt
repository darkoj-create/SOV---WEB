package com.darko.speleov1.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SovSearchNormalizerTest {
    @Test
    fun removesCroatianDiacritics() {
        assertEquals("zidana pec", normalizeSovSearchText("Židana peć"))
        assertEquals("curina spilja", normalizeSovSearchText("Ćurina špilja"))
        assertEquals("durina jama", normalizeSovSearchText("Đurina jama"))
    }

    @Test
    fun lowercasesAndCollapsesWhitespace() {
        assertEquals("velebit jama 12", normalizeSovSearchText("  VELEBIT\n\tJama   12  "))
    }

    @Test
    fun punctuationBecomesSearchSeparator() {
        assertEquals("jama velebit 2026", normalizeSovSearchText("jama/velebit-2026"))
        assertEquals("a b c", normalizeSovSearchText("a___b...c"))
    }

    @Test
    fun nullAndBlankBecomeEmpty() {
        assertEquals("", normalizeSovSearchText(null))
        assertEquals("", normalizeSovSearchText("   \n\t  "))
    }

    @Test
    fun compactTokenRemovesSeparatorsButKeepsLettersAndDigits() {
        assertEquals("zidanapec12", compactSovSearchToken(normalizeSovSearchText("Židana peć 12")))
        assertEquals("abc123", compactSovSearchToken("a b-c_123"))
    }

    @Test
    fun commonLocationQueryNormalizesForFuzzySearch() {
        assertEquals("zumberak sv gera", normalizeSovSearchText("Žumberak / Sv. Gera"))
    }
}
