package com.budzetdomowy.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budzetdomowy.core.data.BudgetRepository
import com.budzetdomowy.core.data.GoalContributionEntity
import com.budzetdomowy.core.data.GoalWithSaved
import com.budzetdomowy.core.data.SavingsGoalEntity
import com.budzetdomowy.core.ui.util.MoneyFormat
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
}
