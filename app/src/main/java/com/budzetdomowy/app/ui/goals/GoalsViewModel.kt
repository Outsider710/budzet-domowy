package com.budzetdomowy.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budzetdomowy.app.data.BudgetRepository
import com.budzetdomowy.app.data.GoalContributionEntity
import com.budzetdomowy.app.data.GoalWithSaved
import com.budzetdomowy.app.data.SavingsGoalEntity
import com.budzetdomowy.app.util.MoneyFormat
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalsViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    val uiState: StateFlow<List<GoalWithSaved>> = repository.observeActiveGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addGoal(name: String, amountText: String) {
        val trimmed = name.trim()
        val cents = MoneyFormat.parseToCents(amountText) ?: return
        if (trimmed.isEmpty() || cents <= 0L) return
        viewModelScope.launch {
            repository.saveGoal(SavingsGoalEntity(name = trimmed, targetCents = cents))
        }
    }

    class Factory(private val repository: BudgetRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GoalsViewModel(repository) as T
    }
}

data class GoalDetailUiState(
    val goal: GoalWithSaved? = null,
    val contributions: List<GoalContributionEntity> = emptyList()
)

class GoalDetailViewModel(
    private val repository: BudgetRepository,
    private val goalId: Long
) : ViewModel() {

    val uiState: StateFlow<GoalDetailUiState> =
        kotlinx.coroutines.flow.combine(
            repository.observeGoal(goalId),
            repository.observeContributions(goalId)
        ) { goal, contributions ->
            GoalDetailUiState(goal, contributions)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalDetailUiState())

    fun addContribution(amountText: String, date: LocalDate, note: String) {
        val cents = MoneyFormat.parseToCents(amountText) ?: return
        if (cents <= 0L) return
        viewModelScope.launch {
            repository.addContribution(
                GoalContributionEntity(
                    goalId = goalId,
                    amountCents = cents,
                    epochDay = date.toEpochDay(),
                    note = note.trim()
                )
            )
        }
    }

    fun deleteContribution(entity: GoalContributionEntity) {
        viewModelScope.launch { repository.deleteContribution(entity) }
    }

    fun archiveGoal() {
        viewModelScope.launch { repository.archiveGoal(goalId) }
    }

    class Factory(
        private val repository: BudgetRepository,
        private val goalId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GoalDetailViewModel(repository, goalId) as T
    }
}
