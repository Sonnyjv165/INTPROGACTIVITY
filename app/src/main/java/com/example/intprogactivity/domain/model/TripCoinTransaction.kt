package com.example.intprogactivity.domain.model

import java.util.UUID

data class TripCoinTransaction(
    val transactionId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val amount: Int = 0,
    val type: TransactionType = TransactionType.EARNED,
    val description: String = "",
    val bookingId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TransactionType { EARNED, REDEEMED, EXPIRED, BONUS }
