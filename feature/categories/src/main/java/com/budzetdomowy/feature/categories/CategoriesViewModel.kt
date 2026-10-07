package com.budzetdomowy.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budzetdomowy.core.data.BudgetRepository
import com.budzetdomowy.core.data.CategoryEntity
import com.budzetdomowy.core.data.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryRow(
    val category: CategoryEntity,
    val limitCents: Long?
)

data class CategoriesUiState(
    val expenses: List<CategoryRow> = emptyList(),
    val incomes: List<CategoryRow> = emptyList()
)

class CategoriesViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = combine(
        repository.observeCategories(),
        repository.observeBudgets()
    ) { categories, budgets ->
        val limits = budgets.associate { it.categoryId to it.limitCents }
        val rows = categories.map { CategoryRow(it, limits[it.id]) }
        CategoriesUiState(
            expenses = rows.filter { it.category.type == TransactionType.EXPENSE },
            incomes = rows.filter { it.category.type == TransactionType.INCOME }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun addCategory(name: String, type: TransactionType) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.addCategory(trimmed, type) }
    }

    fun rename(category: CategoryEntity, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.renameCategory(category, trimmed) }
    }

    fun setArchived(category: CategoryEntity, archived: Boolean) {
        viewModelScope.launch { repository.setCategoryArchived(category, archived) }
    }

    fun setLimit(categoryId: Long, amountText: String) {
        viewModelScope.launch {
            val cents = com.budzetdomowy.core.ui.util.MoneyFormat.parseToCents(amountText)
            repository.setBudget(categoryId, cents)
        }
    }
}
