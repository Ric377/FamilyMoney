// MainViewModelFactory.kt
package com.rich.familymoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rich.familymoney.repository.GroupRepository

class MainViewModelFactory(
    private val groupId: String,
    private val repository: GroupRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(groupId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}