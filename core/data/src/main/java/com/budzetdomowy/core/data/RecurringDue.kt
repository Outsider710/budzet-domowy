package com.budzetdomowy.core.data

import java.time.LocalDate

/** Whether [today] is a payment day for an active recurring rule (ignores nextEpochDay). */
fun RecurringRuleEntity.isDueOn(today: LocalDate): Boolean {
    if (!active) return false
    val start = LocalDate.ofEpochDay(startEpochDay)
    if (today.isBefore(start)) return false
    val end = endEpochDay?.let { LocalDate.ofEpochDay(it) }
    if (end != null && today.isAfter(end)) return false
    val dueDay = dayOfMonth.coerceIn(1, 31).coerceAtMost(today.lengthOfMonth())
    return today.dayOfMonth == dueDay
}
