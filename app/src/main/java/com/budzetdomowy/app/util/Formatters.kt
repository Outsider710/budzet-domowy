package com.budzetdomowy.app.util

import com.budzetdomowy.app.data.TransactionEntity
import com.budzetdomowy.app.data.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object MoneyFormat {
    private val locale = Locale.forLanguageTag("pl-PL")

    fun fromCents(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = kotlin.math.abs(cents)
        val major = abs / 100
        val minor = abs % 100
        return String.format(locale, "%s%d,%02d zł", sign, major, minor)
    }

    fun parseToCents(input: String): Long? {
        val normalized = input.trim()
            .replace(" ", "")
            .replace("zł", "", ignoreCase = true)
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
    format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("pl-PL")))

fun YearMonth.formatPl(): String =
    format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("pl-PL")))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("pl-PL")) else it.toString() }

data class MonthSummary(
    val incomeCents: Long,
    val expenseCents: Long
) {
    val balanceCents: Long get() = incomeCents - expenseCents
}

fun List<TransactionEntity>.summarize(): MonthSummary {
    var income = 0L
    var expense = 0L
    forEach { tx ->
        when (tx.type) {
            TransactionType.INCOME -> income += tx.amountCents
            TransactionType.EXPENSE -> expense += tx.amountCents
        }
    }
    return MonthSummary(income, expense)
}
