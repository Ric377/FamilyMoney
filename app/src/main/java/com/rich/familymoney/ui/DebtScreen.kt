// DebtScreen.kt
package com.rich.familymoney.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.rich.familymoney.repository.GroupRepository
import com.rich.familymoney.util.Debt
import com.rich.familymoney.viewmodel.DebtViewModel
import com.rich.familymoney.viewmodel.DebtViewModelFactory
import com.rich.familymoney.viewmodel.MonthlyDebtSummary
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtScreen(
    groupId: String,
    navController: NavController
) {
    val viewModel: DebtViewModel = viewModel(
        factory = DebtViewModelFactory(groupId, GroupRepository())
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Расчёт долгов") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Блок с итоговым расчётом за всё время
                    item {
                        OverallSummaryCard(
                            totalSpent = state.overallSummary.totalSpent,
                            debts = state.overallSummary.debts
                        )
                    }

                    // Блоки для каждого месяца
                    items(state.monthlySummaries) { monthlySummary ->
                        MonthlySummaryCard(summary = monthlySummary)
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallSummaryCard(totalSpent: Double, debts: List<Debt>) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Итог за всё время",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text("Всего потрачено: ${formatCurrency(totalSpent)} ₽")
            Spacer(Modifier.height(16.dp))
            DebtList(debts = debts)
        }
    }
}

@Composable
private fun MonthlySummaryCard(summary: MonthlyDebtSummary) {
    var isExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.clickable { isExpanded = !isExpanded }
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                summary.monthYear,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Потрачено: ${formatCurrency(summary.totalSpent)} ₽",
                style = MaterialTheme.typography.bodySmall
            )
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    DebtList(debts = summary.debts)
                }
            }
        }
    }
}

@Composable
private fun DebtList(debts: List<Debt>) {
    if (debts.isEmpty()) {
        Text("✅ Долгов нет, всё ровно!", color = MaterialTheme.colorScheme.primary)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            debts.forEach { debt ->
                Text("• ${debt.from} → ${debt.to}: ${formatCurrency(debt.amount)} ₽")
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    return DecimalFormat("#,##0.00").format(amount)
}