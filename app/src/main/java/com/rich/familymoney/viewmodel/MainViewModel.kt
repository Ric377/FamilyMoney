// MainViewModel.kt
package com.rich.familymoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth // ИСПРАВЛЕНО: Добавлен импорт
import com.google.firebase.ktx.Firebase    // ИСПРАВЛЕНО: Добавлен импорт
import com.rich.familymoney.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val groupId: String,
    private val repository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    init {
        loadGroupName()
        listenToGroupUpdates()
    }

    private fun loadGroupName() {
        viewModelScope.launch {
            val name = repository.getGroupName(groupId)
            _uiState.update { it.copy(groupName = name) }
        }
    }

    private fun listenToGroupUpdates() {
        combine(
            repository.getPayments(groupId),
            repository.getMembers(groupId)
        ) { payments, members ->
            _uiState.update { currentState ->
                currentState.copy(
                    payments = payments,
                    members = members,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
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

    fun leaveGroup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // ИСПРАВЛЕНО: Используем правильный вызов Firebase.auth
            val uid = Firebase.auth.currentUser?.uid
            if (uid != null) {
                try {
                    repository.leaveGroup(uid)
                    // Вызываем коллбэк при успехе, чтобы сработала навигация
                    onSuccess()
                } catch (e: Exception) {
                    // Здесь можно обработать ошибку, если не удалось покинуть группу
                }
            }
        }
    }
}