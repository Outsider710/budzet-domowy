package com.budzetdomowy.feature.widget

import com.budzetdomowy.core.data.BudgetRepository
import com.budzetdomowy.core.data.TransactionType
import com.budzetdomowy.core.data.isDueOn
import com.budzetdomowy.core.ui.util.MonthSummary
import com.budzetdomowy.core.ui.util.MoneyFormat
import com.budzetdomowy.core.ui.util.endEpochDay
import com.budzetdomowy.core.ui.util.formatPl
import com.budzetdomowy.core.ui.util.startEpochDay
import com.budzetdomowy.core.ui.util.summarize
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first

data class MonthSnapshot(
    val monthLabel: String,
    val balanceLabel: String,
    val incomeLabel: String,
    val expenseLabel: String,
    val hasOverBudget: Boolean,
    val hasRecurringDueToday: Boolean
)

class MonthSnapshotLoader(
    private val repository: BudgetRepository
) {
    suspend fun load(month: YearMonth = YearMonth.now()): MonthSnapshot {
        val start = month.startEpochDay()
        val end = month.endEpochDay()
        val today = LocalDate.now()
        val txs = repository.observeDisplayTransactions(start, end).first()
        val budgets = repository.observeBudgets().first()
        val saved = repository.observeContributionsSum(start, end).first()
        val categories = repository.observeCategories().first()
        val recurring = repository.observeRecurringRules().first()
        val byId = categories.associateBy { it.id }

        val summary: MonthSummary = txs.map { it.entity }.summarize(saved)
        val spentByCategory = txs
            .filter { it.entity.type == TransactionType.EXPENSE }
            .groupBy { it.entity.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.entity.amountCents } }

        val hasOverBudget = budgets.any { budget ->
            val cat = byId[budget.categoryId] ?: return@any false
            if (cat.type != TransactionType.EXPENSE) return@any false
            (spentByCategory[budget.categoryId] ?: 0L) > budget.limitCents
        }

        val hasRecurringDueToday = recurring.any { it.isDueOn(today) }

        return MonthSnapshot(
            monthLabel = month.formatPl(),
            balanceLabel = MoneyFormat.fromCents(summary.balanceCents),
            incomeLabel = MoneyFormat.fromCents(summary.incomeCents),
            expenseLabel = MoneyFormat.fromCents(summary.expenseCents),
            hasOverBudget = hasOverBudget,
            hasRecurringDueToday = hasRecurringDueToday
        )
    }
}
