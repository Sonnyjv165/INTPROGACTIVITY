package com.example.intprogactivity.data.remote.firebase

import com.example.intprogactivity.domain.model.PriceAlert
import com.example.intprogactivity.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreAlertSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun alertsCol() = firestore.collection(Constants.FIRESTORE_ALERTS)

    suspend fun saveAlert(data: Map<String, Any>): String {
        val ref = alertsCol().add(data).await()
        return ref.id
    }

    suspend fun getUserAlerts(userId: String): List<Map<String, Any>> {
        val snapshot = alertsCol()
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.data?.plus("alertId" to doc.id)
        }
    }

    suspend fun deleteAlert(alertId: String) {
        alertsCol().document(alertId).delete().await()
    }

    suspend fun updateAlert(alertId: String, updates: Map<String, Any>) {
        alertsCol().document(alertId).set(updates, SetOptions.merge()).await()
    }

    fun getUserAlertsFlow(userId: String): Flow<List<PriceAlert>> = callbackFlow {
        val listener = alertsCol()
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val alerts = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.plus("alertId" to doc.id)?.toPriceAlert()
                } ?: emptyList()
                trySend(alerts)
            }
        awaitClose { listener.remove() }
    }
}

fun Map<String, Any>.toPriceAlert(): PriceAlert? = try {
    PriceAlert(
        alertId = this["alertId"] as? String ?: "",
        userId = this["userId"] as? String ?: "",
        origin = this["origin"] as? String ?: "",
        originCity = this["originCity"] as? String ?: "",
        destination = this["destination"] as? String ?: "",
        destinationCity = this["destinationCity"] as? String ?: "",
        targetPrice = this["targetPrice"] as? Double ?: 0.0,
        currentPrice = this["currentPrice"] as? Double,
        isActive = this["isActive"] as? Boolean ?: true,
        createdAt = this["createdAt"] as? Long ?: 0L
    )
} catch (_: Exception) {
    null
}
