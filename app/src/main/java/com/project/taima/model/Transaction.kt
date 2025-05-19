package com.project.taima.model

import com.google.firebase.Timestamp

data class Transaction(
    val id: String = "", // Document id
    val type: String = "",
    val date: Timestamp? = null,
    val amount: Double = 0.0,
    val isIncome: Boolean = false,
    val customerId: String = ""
)