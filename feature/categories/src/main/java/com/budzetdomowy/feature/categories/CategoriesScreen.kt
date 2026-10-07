package com.budzetdomowy.feature.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budzetdomowy.core.ui.BudzetTopBar
import com.budzetdomowy.core.ui.R
import com.budzetdomowy.core.data.CategoryEntity
import com.budzetdomowy.core.data.TransactionType
import com.budzetdomowy.core.ui.ScrollColumn
import com.budzetdomowy.core.ui.util.categoryLabel

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BudzetTopBar(
                title = stringResource(R.string.categories_title),
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_category))
            }
        }
    ) { padding ->
        ScrollColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.expenses), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            state.expenses.forEach { row ->
                CategoryCard(
                    row = row,
                    showLimit = true,
                    onRename = { renameTarget = row.category },
                    onArchive = { viewModel.setArchived(row.category, !row.category.archived) },
                    onLimitSave = { viewModel.setLimit(row.category.id, it) }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.income), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            state.incomes.forEach { row ->
                CategoryCard(
                    row = row,
                    showLimit = false,
                    onRename = { renameTarget = row.category },
                    onArchive = { viewModel.setArchived(row.category, !row.category.archived) },
                    onLimitSave = {}
                )
            }
            Spacer(Modifier.height(88.dp))
        }
    }

    if (showAdd) {
        AddCategoryDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, type ->
                viewModel.addCategory(name, type)
                showAdd = false
            }
        )
    }
    renameTarget?.let { category ->
        RenameCategoryDialog(
            initial = if (category.builtInKey == null) category.name else "",
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                viewModel.rename(category, name)
                renameTarget = null
            }
        )
    }
}

@Composable
private fun CategoryCard(
    row: CategoryRow,
    showLimit: Boolean,
    onRename: () -> Unit,
    onArchive: () -> Unit,
    onLimitSave: (String) -> Unit
) {
    var limitText by remember(row.category.id, row.limitCents) {
        mutableStateOf(
            row.limitCents?.let { formatAmountInput(it) } ?: ""
        )
    }
    val archived = row.category.archived
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = categoryLabel(row.category),
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    color = if (archived) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface
                )
                if (row.category.builtInKey == null && !archived) {
                    IconButton(onClick = onRename) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename_category))
                    }
                }
                IconButton(onClick = onArchive) {
                    Icon(
                        imageVector = if (archived) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (archived) {
                            stringResource(R.string.unarchive_category)
                        } else {
                            stringResource(R.string.archive_category)
                        }
                    )
                }
            }
            if (showLimit && !archived) {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text(stringResource(R.string.monthly_limit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(stringResource(R.string.monthly_limit_hint)) }
                )
                TextButton(onClick = { onLimitSave(limitText) }) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, TransactionType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_category)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.category_name)) },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text(stringResource(R.string.expense)) }
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text(stringResource(R.string.income_type)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, type) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun RenameCategoryDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_category)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.category_name)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private fun formatAmountInput(cents: Long): String {
    val major = cents / 100
    val minor = cents % 100
    return if (minor == 0L) major.toString() else String.format("%d,%02d", major, minor)
}
