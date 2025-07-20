// MainViewModel.kt
package com.rich.familymoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rich.familymoney.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val groupId: String,
    private val repository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    init {
        // Загружаем название группы один раз
        loadGroupName()
        // Отдельно запускаем прослушивание обновлений трат и участников
        listenToGroupUpdates()
    }

    private fun loadGroupName() {
        viewModelScope.launch {
            val name = repository.getGroupName(groupId)
            _uiState.update { it.copy(groupName = name) }
        }
    }

    private fun listenToGroupUpdates() {
        // combine теперь только для двух потоков, которые постоянно обновляются
        combine(
            repository.getPayments(groupId),
            repository.getMembers(groupId)
        ) { payments, members ->
            // Обновляем состояние, не трогая уже загруженное название
            _uiState.update { currentState ->
                currentState.copy(
                    payments = payments,
                    members = members,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope) // <--- ИСПРАВЛЕНИЕ ЗДЕСЬ. Запускаем и "слушаем" поток.
    }

    fun deletePayment(paymentId: String) {
        viewModelScope.launch {
            try {
                repository.deletePayment(groupId, paymentId)
            } catch (e: Exception) {
                // TODO: Обработать ошибку удаления
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
}