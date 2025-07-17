// MainViewModel.kt
package com.rich.familymoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rich.familymoney.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainViewModel(
    private val groupId: String,
    private val repository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    init {
        loadGroupData()
    }

    private fun loadGroupData() {
        viewModelScope.launch {
            // Используем combine, чтобы объединить два потока данных (траты и участники)
            combine(
                repository.getPayments(groupId),
                repository.getMembers(groupId)
            ) { payments, members ->
                // Когда приходят новые данные из любого потока,
                // обновляем общее состояние экрана
                MainScreenState(
                    payments = payments,
                    members = members,
                    isLoading = false // Загрузка завершена
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun deletePayments(paymentIds: List<String>) {
        viewModelScope.launch {
            try {
                repository.deletePayments(groupId, paymentIds)
            } catch (e: Exception) {
                // TODO: Обработать ошибку
            }
        }
    }

    fun deletePayment(paymentId: String) {
        viewModelScope.launch {
            try {
                repository.deletePayment(groupId, paymentId)
            } catch (e: Exception) {
                // TODO: Обработать ошибку удаления (например, показать Snackbar)
            }
        }
    }
}