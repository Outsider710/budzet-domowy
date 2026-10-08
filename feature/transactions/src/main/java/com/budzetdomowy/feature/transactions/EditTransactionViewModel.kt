package com.budzetdomowy.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budzetdomowy.core.data.BudgetRepository
import com.budzetdomowy.core.data.CategoryEntity
import com.budzetdomowy.core.data.RecurringRuleEntity
import com.budzetdomowy.core.data.TransactionEntity
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

data class EditFormState(
    val id: Long = 0,
    val amountText: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: Long = 0,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val repeatMonthly: Boolean = false,
    val notifyEnabled: Boolean = false,
    val endDate: LocalDate? = null,
    val amountError: Boolean = false,
    val loaded: Boolean = false,
    val saved: Boolean = false
)

data class EditUiState(
    val form: EditFormState = EditFormState(),
    val categories: List<CategoryEntity> = emptyList()
) {
    val id get() = form.id
    val amountText get() = form.amountText
    val type get() = form.type
    val categoryId get() = form.categoryId
    val note get() = form.note
    val date get() = form.date
    val repeatMonthly get() = form.repeatMonthly
    val notifyEnabled get() = form.notifyEnabled
    val endDate get() = form.endDate
    val amountError get() = form.amountError
    val loaded get() = form.loaded
    val saved get() = form.saved
    val visibleCategories get() = categories.filter { it.type == form.type }
}

class EditTransactionViewModel(
    private val repository: BudgetRepository,
    private val transactionId: Long,
    defaultRepeatMonthly: Boolean = false
) : ViewModel() {

    private val form = MutableStateFlow(
        EditFormState(repeatMonthly = transactionId == 0L && defaultRepeatMonthly)
    )

    val uiState: StateFlow<EditUiState> = combine(
        form,
        repository.observeActiveCategories()
    ) { formState, categories ->
        val forType = categories.filter { it.type == formState.type }
        val categoryId = when {
            forType.any { it.id == formState.categoryId } -> formState.categoryId
            else -> forType.firstOrNull()?.id ?: 0L
        }
        EditUiState(
            form = formState.copy(categoryId = categoryId, loaded = formState.loaded),
            categories = categories
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditUiState())

    init {
        if (transactionId != 0L) {
            viewModelScope.launch {
                val existing = repository.getTransaction(transactionId) ?: return@launch
                form.value = EditFormState(
                    id = existing.id,
                    amountText = formatAmountInput(existing.amountCents),
                    type = existing.type,
                    categoryId = existing.categoryId,
                    note = existing.note,
                    date = LocalDate.ofEpochDay(existing.epochDay),
                    loaded = true
                )
            }
        } else {
            form.update { it.copy(loaded = true) }
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

    fun onDateChange(date: LocalDate) {
        form.update { it.copy(date = date) }
    }

    fun onRepeatChange(repeat: Boolean) {
        form.update {
            it.copy(
                repeatMonthly = repeat,
                endDate = if (repeat) it.endDate else null,
                notifyEnabled = if (repeat) it.notifyEnabled else false
            )
        }
    }

    fun onNotifyChange(notifyEnabled: Boolean) {
        form.update { it.copy(notifyEnabled = notifyEnabled) }
    }

    fun onEndDateChange(date: LocalDate?) {
        form.update { it.copy(endDate = date) }
    }

    fun save() {
        val cents = MoneyFormat.parseToCents(form.value.amountText)
        if (cents == null || cents == 0L) {
            form.update { it.copy(amountError = true) }
            return
        }
        val state = uiState.value
        if (state.categoryId == 0L) return
        viewModelScope.launch {
            repository.saveTransaction(
                TransactionEntity(
                    id = state.id,
                    amountCents = cents,
                    type = state.type,
                    categoryId = state.categoryId,
                    note = state.note.trim(),
                    epochDay = state.date.toEpochDay()
                )
            )
            if (state.id == 0L && state.repeatMonthly) {
                val next = BudgetRepository.nextOccurrence(state.date, state.date.dayOfMonth)
                repository.addRecurringRule(
                    RecurringRuleEntity(
                        amountCents = cents,
                        type = state.type,
                        categoryId = state.categoryId,
                        note = state.note.trim(),
                        dayOfMonth = state.date.dayOfMonth,
                        startEpochDay = state.date.toEpochDay(),
                        nextEpochDay = next.toEpochDay(),
                        active = true,
                        endEpochDay = state.endDate?.toEpochDay(),
                        notifyEnabled = state.notifyEnabled
                    )
                )
            }
            form.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val id = form.value.id
        if (id == 0L) return
        viewModelScope.launch {
            repository.getTransaction(id)?.let { repository.deleteTransaction(it) }
            form.update { it.copy(saved = true) }
        }
    }

    private fun formatAmountInput(cents: Long): String {
        val major = cents / 100
        val minor = cents % 100
        return if (minor == 0L) major.toString() else String.format("%d,%02d", major, minor)
    }
}
