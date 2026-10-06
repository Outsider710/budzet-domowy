package com.budzetdomowy.app.ui.report

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budzetdomowy.app.R
import com.budzetdomowy.app.data.BudgetRepository
import com.budzetdomowy.app.data.CategoryEntity
import com.budzetdomowy.app.data.TransactionEntity
import com.budzetdomowy.app.data.TransactionType
import com.budzetdomowy.app.util.MonthSummary
import com.budzetdomowy.app.util.summarize
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class ReportPeriod(@StringRes val labelRes: Int) {
    WEEK(R.string.period_week),
    MONTH(R.string.period_month),
    QUARTER(R.string.period_quarter),
    YEAR(R.string.period_year)
}

data class ChartBucket(
    val label: String,
    val incomeCents: Long,
    val expenseCents: Long
)

data class CategorySlice(
    val category: CategoryEntity?,
    val expenseCents: Long,
    val sharePercent: Int
)

data class ReportUiState(
    val period: ReportPeriod = ReportPeriod.MONTH,
    val rangeLabel: String = "",
    val buckets: List<ChartBucket> = emptyList(),
    val categorySlices: List<CategorySlice> = emptyList(),
    val summary: MonthSummary = MonthSummary(0, 0)
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(
    application: Application,
    private val repository: BudgetRepository
) : AndroidViewModel(application) {

    private val period = MutableStateFlow(ReportPeriod.MONTH)
    private val locale: Locale
        get() = getApplication<Application>().resources.configuration.locales[0]

    val uiState: StateFlow<ReportUiState> = period
        .flatMapLatest { selected ->
            val (start, end) = rangeFor(selected)
            combine(
                repository.observeTransactionsBetween(start.toEpochDay(), end.toEpochDay()),
                repository.observeCategories(),
                repository.observeContributionsSum(start.toEpochDay(), end.toEpochDay())
            ) { list, categories, saved ->
                ReportUiState(
                    period = selected,
                    rangeLabel = formatRange(start, end),
                    buckets = bucketize(list, selected, start, end),
                    categorySlices = slices(list, categories),
                    summary = list.summarize(saved)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    fun setPeriod(value: ReportPeriod) {
        period.value = value
    }

    private fun formatRange(start: LocalDate, end: LocalDate): String {
        val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
        return getApplication<Application>().getString(
            R.string.date_range,
            start.format(fmt),
            end.format(fmt)
        )
    }

    private fun slices(
        list: List<TransactionEntity>,
        categories: List<CategoryEntity>
    ): List<CategorySlice> {
        val byId = categories.associateBy { it.id }
        val expenses = list.filter { it.type == TransactionType.EXPENSE }
        val total = expenses.sumOf { it.amountCents }
        if (total <= 0L) return emptyList()
        return expenses
            .groupBy { it.categoryId }
            .map { (id, txs) ->
                val cents = txs.sumOf { it.amountCents }
                CategorySlice(
                    category = byId[id],
                    expenseCents = cents,
                    sharePercent = ((cents * 100) / total).toInt()
                )
            }
            .sortedByDescending { it.expenseCents }
    }

    private fun bucketize(
        list: List<TransactionEntity>,
        period: ReportPeriod,
        start: LocalDate,
        end: LocalDate
    ): List<ChartBucket> {
        val app = getApplication<Application>()
        return when (period) {
            ReportPeriod.WEEK -> {
                val days = generateSequence(start) { d -> d.plusDays(1).takeIf { !it.isAfter(end) } }.toList()
                days.map { day ->
                    val dayList = list.filter { it.epochDay == day.toEpochDay() }
                    val s = dayList.summarize()
                    ChartBucket(
                        label = day.format(DateTimeFormatter.ofPattern("E", locale))
                            .replaceFirstChar { it.titlecase(locale) },
                        incomeCents = s.incomeCents,
                        expenseCents = s.expenseCents
                    )
                }
            }
            ReportPeriod.MONTH -> {
                var cursor = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val buckets = mutableListOf<ChartBucket>()
                var idx = 1
                while (!cursor.isAfter(end)) {
                    val weekEnd = cursor.plusDays(6).let { if (it.isAfter(end)) end else it }
                    val weekStart = if (cursor.isBefore(start)) start else cursor
                    val weekList = list.filter {
                        it.epochDay in weekStart.toEpochDay()..weekEnd.toEpochDay()
                    }
                    val s = weekList.summarize()
                    buckets += ChartBucket(
                        label = app.getString(R.string.week_bucket, idx),
                        incomeCents = s.incomeCents,
                        expenseCents = s.expenseCents
                    )
                    cursor = cursor.plusWeeks(1)
                    idx++
                }
                buckets
            }
            ReportPeriod.QUARTER, ReportPeriod.YEAR -> {
                var ym = YearMonth.from(start)
                val endYm = YearMonth.from(end)
                val buckets = mutableListOf<ChartBucket>()
                while (!ym.isAfter(endYm)) {
                    val mStart = ym.atDay(1).toEpochDay()
                    val mEnd = ym.atEndOfMonth().toEpochDay()
                    val monthList = list.filter { it.epochDay in mStart..mEnd }
                    val s = monthList.summarize()
                    buckets += ChartBucket(
                        label = ym.format(DateTimeFormatter.ofPattern("LLL", locale))
                            .replaceFirstChar { it.titlecase(locale) },
                        incomeCents = s.incomeCents,
                        expenseCents = s.expenseCents
                    )
                    ym = ym.plusMonths(1)
                }
                buckets
            }
        }
    }

    companion object {
        fun rangeFor(period: ReportPeriod, today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> {
            val end = today
            val start = when (period) {
                ReportPeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ReportPeriod.MONTH -> today.withDayOfMonth(1)
                ReportPeriod.QUARTER -> {
                    val m = ((today.monthValue - 1) / 3) * 3 + 1
                    LocalDate.of(today.year, m, 1)
                }
                ReportPeriod.YEAR -> LocalDate.of(today.year, 1, 1)
            }
            return start to end
        }
    }

    class Factory(
        private val application: Application,
        private val repository: BudgetRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReportViewModel(application, repository) as T
    }
}
