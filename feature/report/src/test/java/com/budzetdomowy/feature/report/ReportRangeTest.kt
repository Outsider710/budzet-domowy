package com.budzetdomowy.feature.report

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportRangeTest {
    private val wednesday = LocalDate.of(2026, 10, 7)
    private val october = YearMonth.of(2026, 10)

    @Test
    fun weekStartsMonday() {
        val (start, end) = ReportViewModel.rangeFor(ReportPeriod.WEEK, october, wednesday)
        assertEquals(LocalDate.of(2026, 10, 5), start)
        assertEquals(wednesday, end)
    }

    @Test
    fun monthStartsFirst() {
        val (start, end) = ReportViewModel.rangeFor(ReportPeriod.MONTH, october, wednesday)
        assertEquals(LocalDate.of(2026, 10, 1), start)
        assertEquals(wednesday, end)
    }

    @Test
    fun pastMonthUsesFullMonth() {
        val september = YearMonth.of(2026, 9)
        val (start, end) = ReportViewModel.rangeFor(ReportPeriod.MONTH, september, wednesday)
        assertEquals(LocalDate.of(2026, 9, 1), start)
        assertEquals(LocalDate.of(2026, 9, 30), end)
    }

    @Test
    fun quarterStartsOctober() {
        val (start, _) = ReportViewModel.rangeFor(ReportPeriod.QUARTER, october, wednesday)
        assertEquals(LocalDate.of(2026, 10, 1), start)
    }

    @Test
    fun yearStartsJanuary() {
        val (start, _) = ReportViewModel.rangeFor(ReportPeriod.YEAR, october, wednesday)
        assertEquals(LocalDate.of(2026, 1, 1), start)
    }
}
