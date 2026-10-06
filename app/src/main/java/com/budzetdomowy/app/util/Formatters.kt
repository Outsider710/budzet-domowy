package com.budzetdomowy.app.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.budzetdomowy.app.R
import com.budzetdomowy.app.data.BuiltInCategory
import com.budzetdomowy.app.data.CategoryEntity
import com.budzetdomowy.app.data.TransactionEntity
import com.budzetdomowy.app.data.TransactionType
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object MoneyFormat {
    private val locale: Locale
        get() = Locale.getDefault()

    fun fromCents(cents: Long): String {
        val value = cents / 100.0
        return NumberFormat.getCurrencyInstance(locale).format(value)
    }

    fun parseToCents(input: String): Long? {
        val normalized = input.trim()
            .replace(" ", "")
            .replace('\u00A0', ' ')
            .replace(Regex("[^0-9,.-]"), "")
            .replace(',', '.')
        if (normalized.isEmpty()) return null
        val value = normalized.toDoubleOrNull() ?: return null
        if (value < 0) return null
        return Math.round(value * 100.0)
    }
}

fun YearMonth.startEpochDay(): Long = atDay(1).toEpochDay()
fun YearMonth.endEpochDay(): Long = atEndOfMonth().toEpochDay()

fun LocalDate.formatPl(): String =
    format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

fun YearMonth.formatPl(): String =
    format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
        .replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

data class MonthSummary(
    val incomeCents: Long,
    val expenseCents: Long,
    val savedCents: Long = 0
) {
    val balanceCents: Long get() = incomeCents - expenseCents - savedCents
}

@Composable
fun categoryLabel(category: CategoryEntity?): String {
    if (category == null) return stringResource(R.string.category_other)
    val builtIn = BuiltInCategory.fromKey(category.builtInKey)
    return if (builtIn != null) stringResource(builtIn.labelRes) else category.name
}

fun List<TransactionEntity>.summarize(savedCents: Long = 0): MonthSummary {
    var income = 0L
    var expense = 0L
    forEach { tx ->
        when (tx.type) {
            TransactionType.INCOME -> income += tx.amountCents
            TransactionType.EXPENSE -> expense += tx.amountCents
        }
    }
    return MonthSummary(income, expense, savedCents)
}
