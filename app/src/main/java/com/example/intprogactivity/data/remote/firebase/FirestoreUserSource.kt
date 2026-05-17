package com.example.intprogactivity.data.remote.firebase

import com.example.intprogactivity.domain.model.MembershipTier
import com.example.intprogactivity.domain.model.TripCoinTransaction
import com.example.intprogactivity.domain.model.TransactionType
import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUserSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun usersCol() = firestore.collection(Constants.FIRESTORE_USERS)

    suspend fun createUser(uid: String, data: Map<String, Any>) {
        usersCol().document(uid).set(data).await()
    }

    suspend fun getUser(uid: String): Map<String, Any>? {
        val snapshot = usersCol().document(uid).get().await()
        return if (snapshot.exists()) snapshot.data else null
    }

    suspend fun updateUser(uid: String, updates: Map<String, Any>) {
        usersCol().document(uid).set(updates, SetOptions.merge()).await()
    }

    fun getUserFlow(uid: String): Flow<User?> = callbackFlow {
        val listener = usersCol().document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) { trySend(null); return@addSnapshotListener }
            trySend(snapshot?.data?.toUser(uid))
        }
        awaitClose { listener.remove() }
    }

    suspend fun incrementBookingStats(uid: String, spend: Double) {
        firestore.runTransaction { tx ->
            val ref = usersCol().document(uid)
            val snap = tx.get(ref)
            val currentBookings = (snap.getLong("totalBookings") ?: 0L) + 1
            val currentSpend = (snap.getDouble("totalSpend") ?: 0.0) + spend
            tx.update(ref, mapOf(
                "totalBookings" to currentBookings,
                "totalSpend" to currentSpend
            ))
        }.await()
    }

    suspend fun awardCoins(uid: String, amount: Int, description: String, bookingId: String?) {
        val batch = firestore.batch()
        val userRef = usersCol().document(uid)
        val txRef = usersCol().document(uid)
            .collection(Constants.FIRESTORE_COIN_HISTORY)
            .document(UUID.randomUUID().toString())

        val txData = mutableMapOf<String, Any>(
            "amount" to amount,
            "type" to TransactionType.EARNED.name,
            "description" to description,
            "createdAt" to System.currentTimeMillis()
        )
        bookingId?.let { txData["bookingId"] = it }

        batch.set(txRef, txData)
        batch.update(userRef, "tripCoins", com.google.firebase.firestore.FieldValue.increment(amount.toLong()))
        batch.commit().await()
    }

    suspend fun getCoinHistory(uid: String): List<Map<String, Any>> {
        val snapshot = usersCol().document(uid)
            .collection(Constants.FIRESTORE_COIN_HISTORY)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .get().await()
        return snapshot.documents.mapNotNull { it.data?.plus("transactionId" to it.id) }
    }
}

fun Map<String, Any>.toUser(uid: String): User = User(
    uid = uid,
    email = this["email"] as? String ?: "",
    displayName = this["displayName"] as? String ?: "",
    phone = this["phone"] as? String,
    photoUrl = this["photoUrl"] as? String,
    membershipTier = MembershipTier.fromString(this["membershipTier"] as? String ?: "SILVER"),
    tripCoins = (this["tripCoins"] as? Long)?.toInt() ?: 0,
    totalBookings = (this["totalBookings"] as? Long)?.toInt() ?: 0,
    totalSpend = this["totalSpend"] as? Double ?: 0.0,
    tierExpiryDate = this["tierExpiryDate"] as? Long,
    createdAt = this["createdAt"] as? Long ?: 0L
)
