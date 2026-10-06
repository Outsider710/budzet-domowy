package com.budzetdomowy.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budzetdomowy.app.data.BudgetRepository
import com.budzetdomowy.app.data.CategoryEntity
import com.budzetdomowy.app.data.DisplayTransaction
import com.budzetdomowy.app.data.GoalWithSaved
import com.budzetdomowy.app.data.TransactionType
import com.budzetdomowy.app.util.MonthSummary
import com.budzetdomowy.app.util.endEpochDay
import com.budzetdomowy.app.util.startEpochDay
import com.budzetdomowy.app.util.summarize
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val repository: BudgetRepository
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    init {
        viewModelScope.launch { repository.materializeDue(LocalDate.now()) }
    }

    val uiState: StateFlow<HomeUiState> = month
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

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun goToCurrentMonth() {
        month.value = YearMonth.now()
    }

    class Factory(private val repository: BudgetRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository) as T
    }
}
