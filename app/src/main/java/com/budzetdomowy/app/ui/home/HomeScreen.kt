package com.budzetdomowy.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budzetdomowy.app.data.TransactionEntity
import com.budzetdomowy.app.data.TransactionType
import com.budzetdomowy.app.util.MoneyFormat
import com.budzetdomowy.app.util.formatPl
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budżet Domowy") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ustawienia")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj transakcję")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            MonthSelector(
                label = state.month.formatPl(),
                onPrev = viewModel::previousMonth,
                onNext = viewModel::nextMonth
            )
            Spacer(Modifier.height(12.dp))
            SummaryCard(
                balance = state.summary.balanceCents,
                income = state.summary.incomeCents,
                expense = state.summary.expenseCents
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Transakcje",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            if (state.transactions.isEmpty()) {
                Text(
                    text = "Brak transakcji w tym miesiącu.\nDodaj pierwszy przychód lub wydatek.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.transactions, key = { it.id }) { tx ->
                        TransactionRow(tx = tx, onClick = { onEdit(tx.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Poprzedni miesiąc")
        }
        Text(text = label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Następny miesiąc")
        }
    }
}

@Composable
private fun SummaryCard(balance: Long, income: Long, expense: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Saldo miesiąca", style = MaterialTheme.typography.labelLarge)
            Text(
                text = MoneyFormat.fromCents(balance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (balance >= 0) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Przychody", style = MaterialTheme.typography.labelMedium)
                    Text(MoneyFormat.fromCents(income), fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Wydatki", style = MaterialTheme.typography.labelMedium)
                    Text(MoneyFormat.fromCents(expense), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: TransactionEntity, onClick: () -> Unit) {
    val isIncome = tx.type == TransactionType.INCOME
    val amountLabel = if (isIncome) {
        "+ ${MoneyFormat.fromCents(tx.amountCents)}"
    } else {
        "− ${MoneyFormat.fromCents(tx.amountCents)}"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.category.labelPl, fontWeight = FontWeight.SemiBold)
                Text(
                    text = LocalDate.ofEpochDay(tx.epochDay).formatPl() +
                        if (tx.note.isNotBlank()) " · ${tx.note}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Text(
                text = amountLabel,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.error
            )
        }
    }
}
