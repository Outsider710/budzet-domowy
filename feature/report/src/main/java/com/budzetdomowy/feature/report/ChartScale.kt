package com.budzetdomowy.feature.report

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Round [maxCents] up to a “nice” axis maximum (for Y scale). */
internal fun niceAxisMax(maxCents: Long): Long {
    if (maxCents <= 0L) return 100_00L
    val max = maxCents.toDouble()
    val exp = floor(log10(max)).toInt().coerceAtLeast(0)
    val base = 10.0.pow(exp.toDouble())
    val fraction = max / base
    val niceFraction = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }
    return ceil(niceFraction * base).toLong().coerceAtLeast(1L)
}

/** Tick values from 0 to [maxNice], inclusive (typically 3 ticks). */
internal fun axisTicks(maxNice: Long): List<Long> {
    if (maxNice <= 0L) return listOf(0L)
    return listOf(0L, maxNice / 2, maxNice)
}

/** Compact label for Y-axis (values in cents → whole złoty / thousands). */
internal fun formatAxisLabel(cents: Long): String {
    val zl = cents / 100.0
    return when {
        zl >= 10_000 -> String.format("%.0fk", zl / 1000.0)
        zl >= 1_000 -> String.format("%.1fk", zl / 1000.0)
        zl == floor(zl) -> zl.toLong().toString()
        else -> String.format("%.0f", zl)
    }
}
