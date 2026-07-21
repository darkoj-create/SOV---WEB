package com.darko.speleov1.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionComparatorTest {
    @Test
    fun comparesSemverLikeNumbersNumerically() {
        assertTrue(compareAppVersionNames("1.4.33", "1.4.9") > 0)
        assertTrue(compareAppVersionNames("1.10.0", "1.9.99") > 0)
        assertTrue(compareAppVersionNames("2.0", "1.99.99") > 0)
    }

    @Test
    fun equalWhenOnlyPrefixOrSuffixTextDiffers() {
        assertEquals(0, compareAppVersionNames("v1.4.33a-armory", "1.4.33"))
        assertEquals(0, compareAppVersionNames("V1.4.33", "1.4.33"))
    }

    @Test
    fun comparesBuildSuffixNumbersWhenPresent() {
        assertTrue(compareAppVersionNames("1.4.33a-2", "1.4.33a-1") > 0)
        assertTrue(compareAppVersionNames("1.4.33_10", "1.4.33_2") > 0)
    }

    @Test
    fun missingPartsAreZero() {
        assertEquals(0, compareAppVersionNames("1.4", "1.4.0"))
        assertTrue(compareAppVersionNames("1.4.0.1", "1.4") > 0)
    }

    @Test
    fun blankOrNonNumericFallsBackToZero() {
        assertEquals(0, compareAppVersionNames("", "nonsense"))
        assertTrue(compareAppVersionNames("1.0", "nonsense") > 0)
    }

    @Test
    fun extractorKeepsNumericMeaning() {
        assertEquals(listOf(1, 4, 33, 2), extractAppVersionNumbers("v1.4.33a-2"))
        assertEquals(listOf(0), extractAppVersionNumbers("beta"))
    }
}
