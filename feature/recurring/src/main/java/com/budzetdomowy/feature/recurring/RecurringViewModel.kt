package com.budzetdomowy.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budzetdomowy.core.data.BudgetRepository
import com.budzetdomowy.core.data.CategoryEntity
import com.budzetdomowy.core.data.RecurringRuleEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecurringItem(
    val rule: RecurringRuleEntity,
    val category: CategoryEntity?
)

data class RecurringUiState(
    val items: List<RecurringItem> = emptyList()
)

class RecurringViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    val uiState: StateFlow<RecurringUiState> = combine(
        repository.observeRecurringRules(),
        repository.observeCategories()
    ) { rules, categories ->
        val byId = categories.associateBy { it.id }
        RecurringUiState(
            rules.filter { it.active }.map { rule ->
                RecurringItem(rule, byId[rule.categoryId])
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringUiState())

    fun setActive(item: RecurringItem, active: Boolean) {
        viewModelScope.launch { repository.setRecurringActive(item.rule, active) }
    }

    fun setEndDate(item: RecurringItem, end: LocalDate?) {
        viewModelScope.launch { repository.setRecurringEnd(item.rule, end?.toEpochDay()) }
    }

    fun delete(item: RecurringItem) {
        viewModelScope.launch { repository.deleteRecurringRule(item.rule) }
    }
}
