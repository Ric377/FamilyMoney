// DebtScreenState.kt
package com.rich.familymoney.viewmodel

import com.rich.familymoney.util.Debt

// Состояние для итогового блока "за всё время"
data class OverallDebtSummary(
    val totalSpent: Double = 0.0,
    val debts: List<Debt> = emptyList()
)

// Состояние для каждого месяца в списке
data class MonthlyDebtSummary(
    val monthYear: String, // например, "Июль 2025"
    val totalSpent: Double,
    val debts: List<Debt>
)

// Общее состояние экрана
data class DebtScreenState(
    val isLoading: Boolean = true,
    val overallSummary: OverallDebtSummary = OverallDebtSummary(),
    val monthlySummaries: List<MonthlyDebtSummary> = emptyList(),
    val error: String? = null
)