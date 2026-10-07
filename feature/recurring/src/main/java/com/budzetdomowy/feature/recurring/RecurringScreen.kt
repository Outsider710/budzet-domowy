package com.budzetdomowy.feature.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budzetdomowy.core.ui.BudzetTopBar
import com.budzetdomowy.core.ui.R
import com.budzetdomowy.core.data.TransactionType
import com.budzetdomowy.core.ui.util.MoneyFormat
import com.budzetdomowy.core.ui.util.categoryLabel
import com.budzetdomowy.core.ui.util.formatPl
import java.time.LocalDate

@Composable
fun RecurringScreen(
    viewModel: RecurringViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    showBack: Boolean = true
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            BudzetTopBar(
                title = stringResource(R.string.recurring_title),
                onBack = onBack.takeIf { showBack }
            )
        }
    ) { padding ->
        if (state.items.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_recurring),
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items, key = { it.rule.id }) { item ->
                    RecurringRow(
                        item = item,
                        onClick = { onEdit(item.rule.id) },
                        onActive = { viewModel.setActive(item, it) },
                        onDelete = { viewModel.delete(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringRow(
    item: RecurringItem,
    onClick: () -> Unit,
    onActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val typeLabel = if (item.rule.type == TransactionType.INCOME) {
        stringResource(R.string.income_type)
    } else {
        stringResource(R.string.expense)
    }
    val startLabel = LocalDate.ofEpochDay(item.rule.startEpochDay).formatPl()
    val endLabel = item.rule.endEpochDay?.let { LocalDate.ofEpochDay(it).formatPl() }
        ?: stringResource(R.string.recurring_no_end)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${MoneyFormat.fromCents(item.rule.amountCents)} · ${categoryLabel(item.category)}",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.recurring_subtitle, typeLabel, item.rule.dayOfMonth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (item.rule.note.isNotBlank()) {
                    Text(item.rule.note, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    stringResource(R.string.recurring_start_button, startLabel),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    stringResource(R.string.recurring_end_button, endLabel),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = item.rule.active, onCheckedChange = onActive)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.edit_recurring),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
