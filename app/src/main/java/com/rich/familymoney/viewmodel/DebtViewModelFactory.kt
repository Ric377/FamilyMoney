// DebtViewModelFactory.kt
package com.rich.familymoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rich.familymoney.repository.GroupRepository

class DebtViewModelFactory(
    private val groupId: String,
    private val repository: GroupRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebtViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DebtViewModel(groupId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}