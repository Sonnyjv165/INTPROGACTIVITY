package com.example.intprogactivity.data.repository

import com.example.intprogactivity.data.remote.firebase.FirestoreAlertSource
import com.example.intprogactivity.data.remote.firebase.toPriceAlert
import com.example.intprogactivity.domain.model.PriceAlert
import com.example.intprogactivity.domain.repository.PriceAlertRepository
import com.example.intprogactivity.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceAlertRepositoryImpl @Inject constructor(
    private val alertSource: FirestoreAlertSource
) : PriceAlertRepository {

    override suspend fun createAlert(alert: PriceAlert): Result<PriceAlert> = try {
        val data = mapOf(
            "userId" to alert.userId,
            "origin" to alert.origin,
            "originCity" to alert.originCity,
            "destination" to alert.destination,
            "destinationCity" to alert.destinationCity,
            "targetPrice" to alert.targetPrice,
            "isActive" to alert.isActive,
            "createdAt" to alert.createdAt
        )
        val id = alertSource.saveAlert(data)
        Result.Success(alert.copy(alertId = id))
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override suspend fun getUserAlerts(userId: String): Result<List<PriceAlert>> = try {
        val rawList = alertSource.getUserAlerts(userId)
        Result.Success(rawList.mapNotNull { it.toPriceAlert() })
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override suspend fun deleteAlert(alertId: String): Result<Unit> = try {
        alertSource.deleteAlert(alertId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override suspend fun toggleAlert(alertId: String, isActive: Boolean): Result<Unit> = try {
        alertSource.updateAlert(alertId, mapOf("isActive" to isActive))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override fun getUserAlertsFlow(userId: String): Flow<List<PriceAlert>> =
        alertSource.getUserAlertsFlow(userId)
}
