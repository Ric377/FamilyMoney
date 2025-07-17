// DebtViewModel.kt
package com.rich.familymoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rich.familymoney.repository.GroupRepository
import com.rich.familymoney.util.calculateDebts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DebtViewModel(
    private val groupId: String,
    private val repository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebtScreenState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAndCalculateDebts()
    }

    private fun loadAndCalculateDebts() {
        viewModelScope.launch {
            _uiState.value = DebtScreenState(isLoading = true)
            try {
                // Загружаем всю историю один раз
                val allPayments = repository.getAllPayments(groupId)
                // Слушаем изменения в списке участников
                repository.getMembers(groupId).collect { members ->
                    if (members.isNotEmpty()) {
                        // --- Расчёт итогового долга ---
                        val overallDebts = calculateDebts(allPayments, members)
                        val overallSummary = OverallDebtSummary(
                            totalSpent = allPayments.sumOf { it.sum },
                            debts = overallDebts
                        )

                        // --- Расчёт долгов по месяцам ---
                        val sdf = SimpleDateFormat("LLLL yyyy", Locale("ru"))
                        val monthlySummaries = allPayments
                            .groupBy { sdf.format(Date(it.date)) } // Группируем по "июль 2025"
                            .map { (monthYear, payments) ->
                                MonthlyDebtSummary(
                                    monthYear = monthYear.replaceFirstChar { it.uppercase() },
                                    totalSpent = payments.sumOf { it.sum },
                                    debts = calculateDebts(payments, members)
                                )
                            }
                            .sortedByDescending { summary -> // Сортируем по дате
                                sdf.parse(summary.monthYear.lowercase())?.time
                            }


                        _uiState.value = DebtScreenState(
                            isLoading = false,
                            overallSummary = overallSummary,
                            monthlySummaries = monthlySummaries
                        )
                    } else {
                        _uiState.value = DebtScreenState(isLoading = false, error = "В группе нет участников")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = DebtScreenState(isLoading = false, error = "Ошибка загрузки данных: ${e.message}")
            }
        }
    }
}