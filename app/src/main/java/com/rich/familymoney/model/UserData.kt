package com.rich.familymoney.data

data class UserData(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val groupId: String? = null,
    val photoUrl: String = ""
)
