package com.example.intprogactivity.domain.repository

import com.example.intprogactivity.domain.model.PriceAlert
import com.example.intprogactivity.util.Result
import kotlinx.coroutines.flow.Flow

interface PriceAlertRepository {
    suspend fun createAlert(alert: PriceAlert): Result<PriceAlert>
    suspend fun getUserAlerts(userId: String): Result<List<PriceAlert>>
    suspend fun deleteAlert(alertId: String): Result<Unit>
    suspend fun toggleAlert(alertId: String, isActive: Boolean): Result<Unit>
    fun getUserAlertsFlow(userId: String): Flow<List<PriceAlert>>
}
