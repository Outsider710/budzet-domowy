package com.budzetdomowy.core.data

import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared selected calendar month for Home and Report. */
class SelectedMonthStore {
    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    fun setMonth(value: YearMonth) {
        _month.value = value
    }

    fun previous() {
        _month.value = _month.value.minusMonths(1)
    }

    fun next() {
        _month.value = _month.value.plusMonths(1)
    }

    fun goToCurrent() {
        _month.value = YearMonth.now()
    }
}
