package com.budzetdomowy.app.ui.report

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportRangeTest {
    private val wednesday = LocalDate.of(2026, 10, 7)

    @Test
    fun weekStartsMonday() {
        val (start, end) = ReportViewModel.rangeFor(ReportPeriod.WEEK, wednesday)
        assertEquals(LocalDate.of(2026, 10, 5), start)
        assertEquals(wednesday, end)
    }

    @Test
    fun monthStartsFirst() {
        val (start, end) = ReportViewModel.rangeFor(ReportPeriod.MONTH, wednesday)
        assertEquals(LocalDate.of(2026, 10, 1), start)
        assertEquals(wednesday, end)
    }

    @Test
    fun quarterStartsOctober() {
        val (start, _) = ReportViewModel.rangeFor(ReportPeriod.QUARTER, wednesday)
        assertEquals(LocalDate.of(2026, 10, 1), start)
    }

    @Test
    fun yearStartsJanuary() {
        val (start, _) = ReportViewModel.rangeFor(ReportPeriod.YEAR, wednesday)
        assertEquals(LocalDate.of(2026, 1, 1), start)
    }
}
