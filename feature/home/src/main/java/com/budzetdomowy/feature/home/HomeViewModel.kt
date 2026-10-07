package com.budzetdomowy.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budzetdomowy.core.data.BudgetRepository
import com.budzetdomowy.core.data.CategoryEntity
import com.budzetdomowy.core.data.DisplayTransaction
import com.budzetdomowy.core.data.GoalWithSaved
import com.budzetdomowy.core.data.SelectedMonthStore
import com.budzetdomowy.core.data.TransactionType
import com.budzetdomowy.core.ui.util.MonthSummary
import com.budzetdomowy.core.ui.util.endEpochDay
import com.budzetdomowy.core.ui.util.startEpochDay
import com.budzetdomowy.core.ui.util.summarize
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetProgress(
    val category: CategoryEntity,
    val spentCents: Long,
    val limitCents: Long
)

data class HomeUiState(
    val month: YearMonth = YearMonth.now(),
    val transactions: List<DisplayTransaction> = emptyList(),
    val summary: MonthSummary = MonthSummary(0, 0),
    val budgets: List<BudgetProgress> = emptyList(),
    val goals: List<GoalWithSaved> = emptyList(),
    val savedThisMonthCents: Long = 0L,
    val isCurrentMonth: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: BudgetRepository,
    private val selectedMonth: SelectedMonthStore
) : ViewModel() {

    init {
        viewModelScope.launch { repository.materializeDue(LocalDate.now()) }
    }

    val uiState: StateFlow<HomeUiState> = selectedMonth.month
        .flatMapLatest { current ->
            val start = current.startEpochDay()
            val end = current.endEpochDay()
            combine(
                repository.observeDisplayTransactions(start, end),
                repository.observeBudgets(),
                repository.observeActiveGoals(),
                repository.observeContributionsSum(start, end),
                repository.observeCategories()
            ) { txs, budgets, goals, savedThisMonth, categories ->
                val spentByCategory = txs
                    .filter { it.entity.type == TransactionType.EXPENSE }
                    .groupBy { it.entity.categoryId }
                    .mapValues { (_, list) -> list.sumOf { it.entity.amountCents } }
                val byId = categories.associateBy { it.id }
                HomeUiState(
                    month = current,
                    transactions = txs,
                    summary = txs.map { it.entity }.summarize(savedThisMonth),
                    budgets = budgets.mapNotNull { budget ->
                        val cat = byId[budget.categoryId] ?: return@mapNotNull null
                        if (cat.type != TransactionType.EXPENSE) return@mapNotNull null
                        BudgetProgress(
                            category = cat,
                            spentCents = spentByCategory[budget.categoryId] ?: 0L,
                            limitCents = budget.limitCents
                        )
                    },
                    goals = goals.take(2),
                    savedThisMonthCents = savedThisMonth,
                    isCurrentMonth = current == YearMonth.now()
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun previousMonth() = selectedMonth.previous()

    fun nextMonth() = selectedMonth.next()

    fun goToCurrentMonth() = selectedMonth.goToCurrent()
}
