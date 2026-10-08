package com.budzetdomowy.core.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun isDueOnMatchesDayOfMonth() {
        val rule = RecurringRuleEntity(
            amountCents = 1000,
            type = TransactionType.EXPENSE,
            categoryId = 1,
            dayOfMonth = 7,
            startEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
            nextEpochDay = LocalDate.of(2026, 11, 7).toEpochDay(),
            active = true
        )
        assertTrue(rule.isDueOn(LocalDate.of(2026, 10, 7)))
        assertFalse(rule.isDueOn(LocalDate.of(2026, 10, 8)))
    }

    @Test
    fun isDueOnClampsEndOfMonth() {
        val rule = RecurringRuleEntity(
            amountCents = 1000,
            type = TransactionType.EXPENSE,
            categoryId = 1,
            dayOfMonth = 31,
            startEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
            nextEpochDay = LocalDate.of(2026, 3, 31).toEpochDay(),
            active = true
        )
        assertTrue(rule.isDueOn(LocalDate.of(2026, 2, 28)))
        assertFalse(rule.isDueOn(LocalDate.of(2026, 2, 27)))
    }
}
