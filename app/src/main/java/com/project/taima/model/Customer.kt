package com.project.taima.model

data class Customer(
    var id: String = "",  // ID from document in Firestore
    val route: String = "",
    val alias: String = "",
    var routeDay: List<String> = listOf(),
    val balance: Double = 0.0,
    var position: MutableMap<String, Int> = mutableMapOf(),
    var isExpanded: Boolean = false
)