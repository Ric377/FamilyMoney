// MainScreenState.kt
package com.rich.familymoney.viewmodel

import com.rich.familymoney.data.Payment
import com.rich.familymoney.repository.GroupMember

data class MainScreenState(
    val payments: List<Payment> = emptyList(),
    val members: List<GroupMember> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
    // Сюда можно будет добавлять и другие данные, нужные для UI
)