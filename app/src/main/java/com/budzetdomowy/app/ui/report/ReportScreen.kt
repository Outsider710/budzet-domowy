package com.budzetdomowy.app.ui.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budzetdomowy.app.R
import com.budzetdomowy.app.ui.ScrollColumn
import com.budzetdomowy.app.util.MoneyFormat
import com.budzetdomowy.app.util.categoryLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val incomeColor = MaterialTheme.colorScheme.tertiary
    val expenseColor = MaterialTheme.colorScheme.error

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        ScrollColumn(contentPadding = padding) {
            Text(stringResource(R.string.period), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = state.period == period,
                        onClick = { viewModel.setPeriod(period) },
                        label = { Text(stringResource(period.labelRes)) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.rangeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.income_vs_expenses),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.buckets.isEmpty() ||
                        state.buckets.all { it.incomeCents == 0L && it.expenseCents == 0L }
                    ) {
                        Text(
                            stringResource(R.string.no_data_in_period),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    } else {
                        IncomeExpenseBarChart(
                            buckets = state.buckets,
                            incomeColor = incomeColor,
                            expenseColor = expenseColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            state.buckets.forEach { bucket ->
                                Text(
                                    text = bucket.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendDot(color = incomeColor, label = stringResource(R.string.income))
                            LegendDot(color = expenseColor, label = stringResource(R.string.expenses))
                        }
                    }
                }
            }

            if (state.categorySlices.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.expenses_by_category),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))
                        val sliceColors = sliceColors()
                        CategoryDonut(
                            slices = state.categorySlices,
                            colors = sliceColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        state.categorySlices.forEachIndexed { index, slice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LegendDot(
                                    color = sliceColors[index % sliceColors.size],
                                    label = "${categoryLabel(slice.category)} (${slice.sharePercent}%)"
                                )
                                Text(MoneyFormat.fromCents(slice.expenseCents), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.period_summary), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    SummaryLine(stringResource(R.string.income), MoneyFormat.fromCents(state.summary.incomeCents))
                    SummaryLine(stringResource(R.string.expenses), MoneyFormat.fromCents(state.summary.expenseCents))
                    if (state.summary.savedCents > 0L) {
                        SummaryLine(stringResource(R.string.saved), MoneyFormat.fromCents(state.summary.savedCents))
                    }
                    SummaryLine(
                        stringResource(R.string.balance),
                        MoneyFormat.fromCents(state.summary.balanceCents),
                        bold = true
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun sliceColors(): List<Color> {
    val scheme = MaterialTheme.colorScheme
    return listOf(
        scheme.primary,
        scheme.tertiary,
        scheme.error,
        scheme.secondary,
        scheme.primaryContainer,
        scheme.tertiaryContainer
    )
}

@Composable
private fun CategoryDonut(
    slices: List<CategorySlice>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.expenseCents }.coerceAtLeast(1L).toFloat()
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.18f)
        val diameter = size.minDimension * 0.72f
        val left = (size.width - diameter) / 2f
        val top = (size.height - diameter) / 2f
        var start = -90f
        slices.forEachIndexed { index, slice ->
            val sweep = (slice.expenseCents / total) * 360f
            drawArc(
                color = colors[index % colors.size],
                startAngle = start,
                sweepAngle = sweep.coerceAtLeast(0.5f),
                useCenter = false,
                topLeft = Offset(left, top),
                size = Size(diameter, diameter),
                style = stroke
            )
            start += sweep
        }
    }
}

@Composable
fun IncomeExpenseBarChart(
    buckets: List<ChartBucket>,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier
) {
    val maxVal = buckets.maxOf { maxOf(it.incomeCents, it.expenseCents) }.coerceAtLeast(1L).toFloat()
    Canvas(modifier = modifier) {
        val n = buckets.size.coerceAtLeast(1)
        val groupWidth = size.width / n
        val barWidth = groupWidth * 0.28f
        val gap = groupWidth * 0.08f
        val chartBottom = size.height - 4f
        val chartTop = 8f
        val chartHeight = chartBottom - chartTop

        buckets.forEachIndexed { index, bucket ->
            val groupLeft = index * groupWidth
            val incomeH = (bucket.incomeCents / maxVal) * chartHeight
            val expenseH = (bucket.expenseCents / maxVal) * chartHeight
            val incomeLeft = groupLeft + groupWidth / 2f - barWidth - gap / 2f
            val expenseLeft = groupLeft + groupWidth / 2f + gap / 2f

            drawRoundRect(
                color = incomeColor,
                topLeft = Offset(incomeLeft, chartBottom - incomeH),
                size = Size(barWidth, incomeH.coerceAtLeast(0f)),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = expenseColor,
                topLeft = Offset(expenseLeft, chartBottom - expenseH),
                size = Size(barWidth, expenseH.coerceAtLeast(0f)),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }
}
