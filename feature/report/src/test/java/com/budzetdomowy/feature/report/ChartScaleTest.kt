package com.budzetdomowy.feature.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartScaleTest {

    @Test
    fun niceAxisMax_roundsUpToNiceSteps() {
        assertEquals(100_00L, niceAxisMax(0L))
        assertEquals(100_00L, niceAxisMax(87_50L))
        assertEquals(200_00L, niceAxisMax(150_00L))
        assertEquals(500_00L, niceAxisMax(401_00L))
        assertEquals(1_000_00L, niceAxisMax(750_00L))
    }

    @Test
    fun axisTicks_includeZeroMidAndMax() {
        assertEquals(listOf(0L, 50_00L, 100_00L), axisTicks(100_00L))
        assertEquals(listOf(0L), axisTicks(0L))
    }

    @Test
    fun formatAxisLabel_compactsLargeValues() {
        assertEquals("0", formatAxisLabel(0L))
        assertEquals("50", formatAxisLabel(50_00L))
        assertTrue(formatAxisLabel(12_500_00L).endsWith("k"))
    }
}
