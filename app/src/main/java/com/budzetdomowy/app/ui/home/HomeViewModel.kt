package com.budzetdomowy.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budzetdomowy.app.data.TransactionEntity
import com.budzetdomowy.app.data.TransactionRepository
import com.budzetdomowy.app.util.MonthSummary
import com.budzetdomowy.app.util.endEpochDay
import com.budzetdomowy.app.util.startEpochDay
import com.budzetdomowy.app.util.summarize
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val month: YearMonth = YearMonth.now(),
    val transactions: List<TransactionEntity> = emptyList(),
    val summary: MonthSummary = MonthSummary(0, 0)
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<HomeUiState> = month
        .flatMapLatest { current ->
            repository.observeMonth(current.startEpochDay(), current.endEpochDay())
                .map { list ->
                    HomeUiState(
                        month = current,
                        transactions = list,
                        summary = list.summarize()
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

    fun delete(entity: TransactionEntity) {
        viewModelScope.launch { repository.delete(entity) }
    }

    class Factory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository) as T
    }
}
