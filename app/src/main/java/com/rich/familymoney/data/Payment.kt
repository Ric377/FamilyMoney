package com.rich.familymoney.data

data class Payment(
    val id: String = "",
    val sum: Double,
    val comment: String,
    val date: Long,
    val name: String,
    val photoUrl: String = ""
)
