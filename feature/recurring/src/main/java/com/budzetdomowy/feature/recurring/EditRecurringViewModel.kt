package com.budzetdomowy.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budzetdomowy.core.data.BudgetRepository
import com.budzetdomowy.core.data.CategoryEntity
import com.budzetdomowy.core.data.RecurringRuleEntity
import com.budzetdomowy.core.data.TransactionType
import com.budzetdomowy.core.ui.util.MoneyFormat
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditRecurringForm(
    val id: Long = 0,
    val amountText: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: Long = 0,
    val note: String = "",
    val dayOfMonthText: String = "1",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val active: Boolean = true,
    val notifyEnabled: Boolean = false,
    val originalDayOfMonth: Int = 1,
    val originalStartEpochDay: Long = 0,
    val originalNextEpochDay: Long = 0,
    val amountError: Boolean = false,
    val dayError: Boolean = false,
    val loaded: Boolean = false,
    val saved: Boolean = false,
    val missing: Boolean = false
)

data class EditRecurringUiState(
    val form: EditRecurringForm = EditRecurringForm(),
    val categories: List<CategoryEntity> = emptyList()
) {
    val id get() = form.id
    val amountText get() = form.amountText
    val type get() = form.type
    val categoryId get() = form.categoryId
    val note get() = form.note
    val dayOfMonthText get() = form.dayOfMonthText
    val startDate get() = form.startDate
    val endDate get() = form.endDate
    val active get() = form.active
    val notifyEnabled get() = form.notifyEnabled
    val amountError get() = form.amountError
    val dayError get() = form.dayError
    val loaded get() = form.loaded
    val saved get() = form.saved
    val missing get() = form.missing
    val visibleCategories get() = categories.filter { it.type == form.type }
}

class EditRecurringViewModel(
    private val repository: BudgetRepository,
    private val ruleId: Long
) : ViewModel() {

    private val form = MutableStateFlow(EditRecurringForm())

    val uiState: StateFlow<EditRecurringUiState> = combine(
        form,
        repository.observeActiveCategories()
    ) { formState, categories ->
        val forType = categories.filter { it.type == formState.type }
        val categoryId = when {
            forType.any { it.id == formState.categoryId } -> formState.categoryId
            else -> forType.firstOrNull()?.id ?: 0L
        }
        EditRecurringUiState(
            form = formState.copy(categoryId = categoryId),
            categories = categories
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditRecurringUiState())

    init {
        viewModelScope.launch {
            val rule = repository.getRecurringRule(ruleId)
            if (rule == null) {
                form.value = EditRecurringForm(loaded = true, missing = true)
                return@launch
            }
            form.value = EditRecurringForm(
                id = rule.id,
                amountText = formatAmountInput(rule.amountCents),
                type = rule.type,
                categoryId = rule.categoryId,
                note = rule.note,
                dayOfMonthText = rule.dayOfMonth.toString(),
                startDate = LocalDate.ofEpochDay(rule.startEpochDay),
                endDate = rule.endEpochDay?.let { LocalDate.ofEpochDay(it) },
                active = rule.active,
                notifyEnabled = rule.notifyEnabled,
                originalDayOfMonth = rule.dayOfMonth,
                originalStartEpochDay = rule.startEpochDay,
                originalNextEpochDay = rule.nextEpochDay,
                loaded = true
            )
        }
    }

    fun onAmountChange(value: String) {
        form.update { it.copy(amountText = value, amountError = false) }
    }

    fun onTypeChange(type: TransactionType) {
        form.update { it.copy(type = type) }
    }

    fun onCategoryChange(categoryId: Long) {
        form.update { it.copy(categoryId = categoryId) }
    }

    fun onNoteChange(note: String) {
        form.update { it.copy(note = note) }
    }

    fun onDayOfMonthChange(value: String) {
        form.update { it.copy(dayOfMonthText = value.filter { ch -> ch.isDigit() }.take(2), dayError = false) }
    }

    fun onStartDateChange(date: LocalDate) {
        form.update { it.copy(startDate = date) }
    }

    fun onEndDateChange(date: LocalDate?) {
        form.update { it.copy(endDate = date) }
    }

    fun onActiveChange(active: Boolean) {
        form.update { it.copy(active = active) }
    }

    fun onNotifyChange(notifyEnabled: Boolean) {
        form.update { it.copy(notifyEnabled = notifyEnabled) }
    }

    fun save() {
        val cents = MoneyFormat.parseToCents(form.value.amountText)
        if (cents == null || cents == 0L) {
            form.update { it.copy(amountError = true) }
            return
        }
        val day = form.value.dayOfMonthText.toIntOrNull()
        if (day == null || day !in 1..31) {
            form.update { it.copy(dayError = true) }
            return
        }
        val state = uiState.value
        if (state.categoryId == 0L || state.id == 0L) return
        viewModelScope.launch {
            val startEpochDay = state.startDate.toEpochDay()
            val scheduleChanged =
                day != state.form.originalDayOfMonth ||
                    startEpochDay != state.form.originalStartEpochDay
            val nextEpochDay = if (scheduleChanged) {
                BudgetRepository.occurrenceOnOrAfter(state.startDate, day).toEpochDay()
            } else {
                state.form.originalNextEpochDay
            }
            repository.updateRecurringRule(
                RecurringRuleEntity(
                    id = state.id,
                    amountCents = cents,
                    type = state.type,
                    categoryId = state.categoryId,
                    note = state.note.trim(),
                    dayOfMonth = day,
                    startEpochDay = startEpochDay,
                    nextEpochDay = nextEpochDay,
                    active = state.active,
                    endEpochDay = state.endDate?.toEpochDay(),
                    notifyEnabled = state.notifyEnabled
                )
            )
            form.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val id = form.value.id
        if (id == 0L) return
        viewModelScope.launch {
            repository.getRecurringRule(id)?.let { repository.deleteRecurringRule(it) }
            form.update { it.copy(saved = true) }
        }
    }

    private fun formatAmountInput(cents: Long): String {
        val major = cents / 100
        val minor = cents % 100
        return if (minor == 0L) major.toString() else String.format("%d,%02d", major, minor)
    }
}
