package com.budzetdomowy.app.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budzetdomowy.app.data.Category
import com.budzetdomowy.app.data.TransactionEntity
import com.budzetdomowy.app.data.TransactionRepository
import com.budzetdomowy.app.data.TransactionType
import com.budzetdomowy.app.util.MoneyFormat
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditUiState(
    val id: Long = 0,
    val amountText: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.FOOD,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val amountError: Boolean = false,
    val loaded: Boolean = false,
    val saved: Boolean = false
)

class EditTransactionViewModel(
    private val repository: TransactionRepository,
    private val transactionId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    init {
        if (transactionId != 0L) {
            viewModelScope.launch {
                val existing = repository.get(transactionId) ?: return@launch
                _uiState.value = EditUiState(
                    id = existing.id,
                    amountText = formatAmountInput(existing.amountCents),
                    type = existing.type,
                    category = existing.category,
                    note = existing.note,
                    date = LocalDate.ofEpochDay(existing.epochDay),
                    loaded = true
                )
            }
        } else {
            _uiState.update { it.copy(loaded = true) }
        }
    }

    fun onAmountChange(value: String) {
        _uiState.update { it.copy(amountText = value, amountError = false) }
    }

    fun onTypeChange(type: TransactionType) {
        val categories = Category.forType(type)
        _uiState.update {
            it.copy(
                type = type,
                category = if (it.category in categories) it.category else categories.first()
            )
        }
    }

    fun onCategoryChange(category: Category) {
        _uiState.update { it.copy(category = category) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun onDateChange(date: LocalDate) {
        _uiState.update { it.copy(date = date) }
    }

    fun save() {
        val cents = MoneyFormat.parseToCents(_uiState.value.amountText)
        if (cents == null || cents == 0L) {
            _uiState.update { it.copy(amountError = true) }
            return
        }
        val state = _uiState.value
        viewModelScope.launch {
            repository.save(
                TransactionEntity(
                    id = state.id,
                    amountCents = cents,
                    type = state.type,
                    category = state.category,
                    note = state.note.trim(),
                    epochDay = state.date.toEpochDay()
                )
            )
            _uiState.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val id = _uiState.value.id
        if (id == 0L) return
        viewModelScope.launch {
            repository.get(id)?.let { repository.delete(it) }
            _uiState.update { it.copy(saved = true) }
        }
    }

    private fun formatAmountInput(cents: Long): String {
        val major = cents / 100
        val minor = cents % 100
        return if (minor == 0L) major.toString() else String.format("%d,%02d", major, minor)
    }

    class Factory(
        private val repository: TransactionRepository,
        private val transactionId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditTransactionViewModel(repository, transactionId) as T
    }
}
