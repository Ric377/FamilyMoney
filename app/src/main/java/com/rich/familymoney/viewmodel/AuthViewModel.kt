// AuthViewModel.kt
package com.rich.familymoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.rich.familymoney.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow(false)
    val authState: StateFlow<Boolean> = _authState

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loginOrRegister(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val loginResult = authRepository.login(email, password)

            if (loginResult.isSuccess) {
                _authState.value = true
            } else {
                // Если ошибка - "пользователь не найден", то пробуем зарегистрировать
                if (loginResult.exceptionOrNull() is FirebaseAuthInvalidUserException) {
                    val registerResult = authRepository.register(email, password)
                    if (registerResult.isSuccess) {
                        _authState.value = true
                    } else {
                        // Ошибка при регистрации
                        _errorMessage.value = when (registerResult.exceptionOrNull()) {
                            is FirebaseAuthInvalidCredentialsException -> "Неверный формат email или пароля."
                            else -> "Ошибка регистрации: ${registerResult.exceptionOrNull()?.message}"
                        }
                    }
                } else {
                    // Другая ошибка входа (например, неверный пароль)
                    _errorMessage.value = when (loginResult.exceptionOrNull()) {
                        is FirebaseAuthInvalidCredentialsException -> "Неверный пароль."
                        else -> "Ошибка входа: ${loginResult.exceptionOrNull()?.message}"
                    }
                }
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}