package com.budzetdomowy.feature.report

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.budzetdomowy.core.ui.BudzetTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budzetdomowy.core.ui.R
import com.budzetdomowy.core.ui.ScrollColumn
import com.budzetdomowy.core.ui.util.MoneyFormat
import com.budzetdomowy.core.ui.util.categoryLabel
import com.budzetdomowy.core.ui.util.formatPl
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onBack: () -> Unit,
    showBack: Boolean = true
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val incomeColor = MaterialTheme.colorScheme.tertiary
    val expenseColor = MaterialTheme.colorScheme.error

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            BudzetTopBar(
                title = stringResource(R.string.report),
                onBack = onBack.takeIf { showBack }
            )
        }
    ) { padding ->
        ScrollColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 120.dp
            )
        ) {
            ReportMonthSelector(
                label = state.month.formatPl(),
                isCurrentMonth = state.isCurrentMonth,
                onPrev = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onGoCurrent = viewModel::goToCurrentMonth
            )
            Spacer(Modifier.height(16.dp))
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
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.chart_tap_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            periodKey = state.period,
                            incomeColor = incomeColor,
                            expenseColor = expenseColor,
                            gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )
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
                            periodKey = state.period,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
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
private fun ReportMonthSelector(
    label: String,
    isCurrentMonth: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onGoCurrent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrev,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_previous_month),
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
            if (!isCurrentMonth) {
                Surface(
                    onClick = onGoCurrent,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(end = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Today,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.go_current_month),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_next_month),
                    modifier = Modifier.size(22.dp)
                )
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

private enum class BarSeries { Income, Expense, Both }

private data class BarSelection(val index: Int, val series: BarSeries)

@Composable
fun IncomeExpenseBarChart(
    buckets: List<ChartBucket>,
    periodKey: Any,
    incomeColor: Color,
    expenseColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    var selection by remember { mutableStateOf<BarSelection?>(null) }
    val entryProgress = remember { Animatable(0f) }
    val dimAlpha by animateFloatAsState(
        targetValue = if (selection != null) 0.35f else 1f,
        animationSpec = tween(150),
        label = "barDim"
    )

    LaunchedEffect(periodKey, buckets) {
        selection = null
        entryProgress.snapTo(0f)
        entryProgress.animateTo(
            1f,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
        )
    }

    val rawMax = buckets.maxOf { maxOf(it.incomeCents, it.expenseCents) }.coerceAtLeast(1L)
    val axisMax = remember(rawMax) { niceAxisMax(rawMax) }
    val ticks = remember(axisMax) { axisTicks(axisMax) }
    val selectedBucket = selection?.let { buckets.getOrNull(it.index) }
    val incomeLabel = stringResource(R.string.income)
    val expenseLabel = stringResource(R.string.expenses)
    val balanceLabel = stringResource(R.string.chart_balance)
    val progress = entryProgress.value

    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = selectedBucket != null && selection != null,
            enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { -it / 2 },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 2 }
        ) {
            val bucket = selectedBucket ?: return@AnimatedVisibility
            val series = selection?.series ?: return@AnimatedVisibility
            val detail = when (series) {
                BarSeries.Income -> "$incomeLabel ${MoneyFormat.fromCents(bucket.incomeCents)}"
                BarSeries.Expense -> "$expenseLabel ${MoneyFormat.fromCents(bucket.expenseCents)}"
                BarSeries.Both -> {
                    val net = bucket.incomeCents - bucket.expenseCents
                    "$incomeLabel ${MoneyFormat.fromCents(bucket.incomeCents)} · " +
                        "$expenseLabel ${MoneyFormat.fromCents(bucket.expenseCents)} · " +
                        "$balanceLabel ${MoneyFormat.fromCents(net)}"
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(bucket.label, fontWeight = FontWeight.SemiBold)
                    Text(detail, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .padding(bottom = 22.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                ticks.asReversed().forEach { tick ->
                    Text(
                        text = formatAxisLabel(tick),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(buckets, axisMax, progress) {
                            detectTapGestures { offset ->
                                val hit = hitTestBar(
                                    tap = offset,
                                    buckets = buckets,
                                    axisMax = axisMax,
                                    progress = progress,
                                    canvasWidth = size.width.toFloat(),
                                    canvasHeight = size.height.toFloat()
                                )
                                selection = when {
                                    hit == null -> null
                                    selection == hit -> null
                                    else -> hit
                                }
                            }
                        }
                ) {
                    val n = buckets.size.coerceAtLeast(1)
                    val groupWidth = size.width / n
                    val barWidth = groupWidth * 0.28f
                    val gap = groupWidth * 0.08f
                    val chartBottom = size.height - 2f
                    val chartTop = 4f
                    val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)

                    ticks.forEach { tick ->
                        val y = chartBottom - (tick / axisMax.toFloat()) * chartHeight
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }

                    buckets.forEachIndexed { index, bucket ->
                        val groupLeft = index * groupWidth
                        val incomeH = (bucket.incomeCents / axisMax.toFloat()) * chartHeight * progress
                        val expenseH = (bucket.expenseCents / axisMax.toFloat()) * chartHeight * progress
                        val incomeLeft = groupLeft + groupWidth / 2f - barWidth - gap / 2f
                        val expenseLeft = groupLeft + groupWidth / 2f + gap / 2f
                        val sel = selection
                        val incomeAlpha = when {
                            sel == null -> 1f
                            sel.index == index &&
                                (sel.series == BarSeries.Income || sel.series == BarSeries.Both) -> 1f
                            else -> dimAlpha
                        }
                        val expenseAlpha = when {
                            sel == null -> 1f
                            sel.index == index &&
                                (sel.series == BarSeries.Expense || sel.series == BarSeries.Both) -> 1f
                            else -> dimAlpha
                        }

                        drawRoundRect(
                            color = incomeColor.copy(alpha = incomeAlpha),
                            topLeft = Offset(incomeLeft, chartBottom - incomeH),
                            size = Size(barWidth, incomeH.coerceAtLeast(0f)),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = expenseColor.copy(alpha = expenseAlpha),
                            topLeft = Offset(expenseLeft, chartBottom - expenseH),
                            size = Size(barWidth, expenseH.coerceAtLeast(0f)),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    buckets.forEachIndexed { index, bucket ->
                        val labelSelected = selection?.index == index
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            fontWeight = if (labelSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (labelSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val next = BarSelection(index, BarSeries.Both)
                                    selection = if (selection == next) null else next
                                },
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** Hit-test a single income/expense bar (not the whole period group). */
private fun hitTestBar(
    tap: Offset,
    buckets: List<ChartBucket>,
    axisMax: Long,
    progress: Float,
    canvasWidth: Float,
    canvasHeight: Float
): BarSelection? {
    val n = buckets.size.coerceAtLeast(1)
    val groupWidth = canvasWidth / n
    val barWidth = groupWidth * 0.28f
    val gap = groupWidth * 0.08f
    val chartBottom = canvasHeight - 2f
    val chartTop = 4f
    val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)
    // Finger-friendly padding around thin bars
    val padX = (barWidth * 0.35f).coerceAtLeast(8f)
    val minHitH = 28f

    var best: Pair<BarSelection, Float>? = null
    buckets.forEachIndexed { index, bucket ->
        val groupLeft = index * groupWidth
        val incomeH = (bucket.incomeCents / axisMax.toFloat()) * chartHeight * progress
        val expenseH = (bucket.expenseCents / axisMax.toFloat()) * chartHeight * progress
        val incomeLeft = groupLeft + groupWidth / 2f - barWidth - gap / 2f
        val expenseLeft = groupLeft + groupWidth / 2f + gap / 2f

        fun consider(series: BarSeries, left: Float, height: Float, cents: Long) {
            if (cents <= 0L && height < 1f) return
            val h = height.coerceAtLeast(minHitH)
            val top = chartBottom - h
            val l = left - padX
            val r = left + barWidth + padX
            if (tap.x in l..r && tap.y in top..chartBottom) {
                val cx = left + barWidth / 2f
                val dist = kotlin.math.abs(tap.x - cx)
                val candidate = BarSelection(index, series)
                val prev = best
                if (prev == null || dist < prev.second) best = candidate to dist
            }
        }

        consider(BarSeries.Income, incomeLeft, incomeH, bucket.incomeCents)
        consider(BarSeries.Expense, expenseLeft, expenseH, bucket.expenseCents)
    }
    return best?.first
}

@Composable
private fun CategoryDonut(
    slices: List<CategorySlice>,
    colors: List<Color>,
    periodKey: Any,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val entryProgress = remember { Animatable(0f) }
    val dimAlpha by animateFloatAsState(
        targetValue = if (selectedIndex != null) 0.3f else 1f,
        animationSpec = tween(150),
        label = "donutDim"
    )
    val total = slices.sumOf { it.expenseCents }.coerceAtLeast(1L)

    LaunchedEffect(periodKey, slices) {
        selectedIndex = null
        entryProgress.snapTo(0f)
        entryProgress.animateTo(
            1f,
            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
        )
    }

    val ranges = remember(slices, total) {
        var start = -90f
        slices.map { slice ->
            val sweep = (slice.expenseCents / total.toFloat()) * 360f
            val range = start to (start + sweep)
            start += sweep
            range
        }
    }
    val progress = entryProgress.value

    Column {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slices, ranges) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val dist = sqrt(dx * dx + dy * dy)
                            val minSide = min(size.width, size.height).toFloat()
                            val diameter = minSide * 0.72f
                            val radius = diameter / 2f
                            val stroke = minSide * 0.18f
                            val inner = radius - stroke / 2f
                            val outer = radius + stroke / 2f
                            if (dist < inner * 0.55f || dist > outer) {
                                selectedIndex = null
                                return@detectTapGestures
                            }
                            // Compose drawArc: 0° at 3 o'clock, positive clockwise.
                            // atan2(dy, dx) with Y-down matches that convention.
                            val tap = normalizeDegrees(
                                Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            )
                            val hit = ranges.indexOfFirst { (from, to) ->
                                angleInSweep(tap, from, to)
                            }
                            selectedIndex = when {
                                hit < 0 -> null
                                selectedIndex == hit -> null
                                else -> hit
                            }
                        }
                    }
            ) {
                val strokeWidth = size.minDimension * 0.18f
                val diameter = size.minDimension * 0.72f
                val left = (size.width - diameter) / 2f
                val top = (size.height - diameter) / 2f
                var start = -90f
                slices.forEachIndexed { index, slice ->
                    val fullSweep = (slice.expenseCents / total.toFloat()) * 360f
                    val sweep = (fullSweep * progress).coerceAtLeast(if (progress > 0f) 0.5f else 0f)
                    val selectedHere = selectedIndex == null || selectedIndex == index
                    val alpha = if (selectedHere) 1f else dimAlpha
                    val width = if (selectedIndex == index) strokeWidth * 1.2f else strokeWidth
                    drawArc(
                        color = colors[index % colors.size].copy(alpha = alpha),
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(left, top),
                        size = Size(diameter, diameter),
                        style = Stroke(width = width, cap = StrokeCap.Butt)
                    )
                    start += fullSweep
                }
            }

            AnimatedContent(
                targetState = selectedIndex,
                transitionSpec = {
                    fadeIn(tween(150)) togetherWith fadeOut(tween(120))
                },
                label = "donutCenter"
            ) { index ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 48.dp)
                ) {
                    if (index == null) {
                        Text(
                            stringResource(R.string.chart_total_expenses),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            MoneyFormat.fromCents(total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val slice = slices[index]
                        Text(
                            categoryLabel(slice.category),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            MoneyFormat.fromCents(slice.expenseCents),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "${slice.sharePercent}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        slices.forEachIndexed { index, slice ->
            val selectedHere = selectedIndex == null || selectedIndex == index
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedIndex = if (selectedIndex == index) null else index
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(
                    color = colors[index % colors.size].copy(alpha = if (selectedHere) 1f else 0.4f),
                    label = "${categoryLabel(slice.category)} (${slice.sharePercent}%)"
                )
                Text(
                    MoneyFormat.fromCents(slice.expenseCents),
                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.SemiBold
                )
            }
        }
    }
}

private fun normalizeDegrees(degrees: Float): Float =
    ((degrees % 360f) + 360f) % 360f

/** Whether [angle] lies on the drawArc sweep from [start] with end = [end] (degrees). */
private fun angleInSweep(angle: Float, start: Float, end: Float): Boolean {
    val a = normalizeDegrees(angle)
    val s = normalizeDegrees(start)
    val e = normalizeDegrees(end)
    return if (s <= e) a in s..e else a >= s || a <= e
}
