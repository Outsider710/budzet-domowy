package com.budzetdomowy.core.ui.util

import com.budzetdomowy.core.data.TransactionEntity
import com.budzetdomowy.core.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class SummarizeTest {
    @Test
    fun balanceIsIncomeMinusExpense() {
        val list = listOf(
            tx(TransactionType.INCOME, 10_000),
            tx(TransactionType.EXPENSE, 2_500),
            tx(TransactionType.EXPENSE, 1_500)
        )
        val summary = list.summarize()
        assertEquals(10_000L, summary.incomeCents)
        assertEquals(4_000L, summary.expenseCents)
        assertEquals(6_000L, summary.balanceCents)
    }

    @Test
    fun savingsReduceAvailableBalance() {
        val list = listOf(tx(TransactionType.INCOME, 10_000), tx(TransactionType.EXPENSE, 2_000))
        val summary = list.summarize(savedCents = 3_000)
        assertEquals(2_000L, summary.expenseCents)
        assertEquals(5_000L, summary.balanceCents)
    }

    @Test
    fun emptyIsZero() {
        val summary = emptyList<TransactionEntity>().summarize()
        assertEquals(0L, summary.balanceCents)
    }

    private fun tx(type: TransactionType, cents: Long) = TransactionEntity(
        amountCents = cents,
        type = type,
        categoryId = 1,
        epochDay = 0
    )
}
