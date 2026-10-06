package com.budzetdomowy.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringDateTest {
    @Test
    fun nextMonthSameDay() {
        val next = BudgetRepository.nextOccurrence(LocalDate.of(2026, 1, 15), 15)
        assertEquals(LocalDate.of(2026, 2, 15), next)
    }

    @Test
    fun clampsToEndOfShorterMonth() {
        val next = BudgetRepository.nextOccurrence(LocalDate.of(2026, 1, 31), 31)
        assertEquals(LocalDate.of(2026, 2, 28), next)
    }

    @Test
    fun leapFebruary() {
        val next = BudgetRepository.nextOccurrence(LocalDate.of(2028, 1, 31), 31)
        assertEquals(LocalDate.of(2028, 2, 29), next)
    }

    @Test
    fun previewStopsAtEndDate() {
        val dates = BudgetRepository.previewOccurrences(
            start = LocalDate.of(2026, 8, 10),
            dayOfMonth = 10,
            end = LocalDate.of(2026, 10, 10)
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 10, 10)
            ),
            dates
        )
    }
}
