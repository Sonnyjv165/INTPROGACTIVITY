package com.example.intprogactivity.domain.model

import java.util.UUID

data class PriceAlert(
    val alertId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val origin: String = "",
    val originCity: String = "",
    val destination: String = "",
    val destinationCity: String = "",
    val targetPrice: Double = 0.0,
    val currentPrice: Double? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun routeLabel(): String = "$originCity ($origin) → $destinationCity ($destination)"
}
