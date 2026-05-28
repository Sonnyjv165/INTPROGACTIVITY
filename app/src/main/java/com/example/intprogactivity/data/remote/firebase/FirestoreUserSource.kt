package com.example.intprogactivity.data.remote.firebase

import com.example.intprogactivity.domain.model.MembershipTier
import com.example.intprogactivity.domain.model.TripCoinTransaction
import com.example.intprogactivity.domain.model.TransactionType
import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.util.Constants
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    suspend fun createUser(uid: String, data: Map<String, Any?>) {
        usersCol().document(uid).set(data).await()
    }

    suspend fun getUser(uid: String): Map<String, Any>? {
        val snapshot = usersCol().document(uid).get().await()
        return if (snapshot.exists()) snapshot.data else null
    }

    suspend fun updateUser(uid: String, updates: Map<String, Any?>) {
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
                "totalSpend"    to currentSpend
            ))
        }.await()
    }

    /** Called when a booking transitions to CONFIRMED status — increments the confirmed counter. */
    suspend fun incrementConfirmedBookings(uid: String) {
        firestore.runTransaction { tx ->
            val ref = usersCol().document(uid)
            val snap = tx.get(ref)
            val current = (snap.getLong("confirmedBookings") ?: 0L) + 1
            tx.update(ref, "confirmedBookings", current)
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
        // Field is "loyaltyPoints" — matches web Firestore schema
        batch.update(userRef, "loyaltyPoints", FieldValue.increment(amount.toLong()))
        batch.commit().await()
    }

    suspend fun getCoinHistory(uid: String): List<Map<String, Any>> {
        val snapshot = usersCol().document(uid)
            .collection(Constants.FIRESTORE_COIN_HISTORY)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get().await()
        return snapshot.documents.mapNotNull { it.data?.plus("transactionId" to it.id) }
    }
}

/**
 * Maps a Firestore user document to the domain [User] model.
 * Field names match the web project's Firestore schema.
 */
fun Map<String, Any>.toUser(uid: String): User {
    val firstName = this["firstName"] as? String ?: ""
    val lastName  = this["lastName"]  as? String ?: ""
    return User(
        uid           = uid,
        email         = this["email"]         as? String ?: "",
        firstName     = firstName,
        lastName      = lastName,
        middleInitial = this["middleInitial"] as? String ?: "",
        suffix        = this["suffix"]        as? String ?: "",
        // displayName stored separately but fall back to "firstName lastName"
        displayName   = (this["displayName"] as? String)
                            ?.ifBlank { "$firstName $lastName".trim() }
                            ?: "$firstName $lastName".trim(),
        phone         = this["phone"] as? String,
        photoUrl      = this["photoUrl"] as? String,
        nationality   = this["nationality"] as? String,
        dob           = this["dob"] as? String,         // web field name: "dob"
        role          = this["role"]   as? String ?: "user",
        status        = this["status"] as? String ?: "ACTIVE",
        providerId    = this["providerId"] as? String,
        membershipTier = MembershipTier.fromString(this["membershipTier"] as? String ?: "SILVER"),
        loyaltyPoints = (this["loyaltyPoints"] as? Long)?.toInt()    // web field name: "loyaltyPoints"
                            ?: (this["tripCoins"] as? Long)?.toInt()  // backwards-compat with old docs
                            ?: 0,
        totalBookings     = (this["totalBookings"]     as? Long)?.toInt() ?: 0,
        confirmedBookings = (this["confirmedBookings"] as? Long)?.toInt() ?: 0,
        totalSpend    = (this["totalSpend"] as? Double)
                            ?: (this["totalSpend"] as? Long)?.toDouble() ?: 0.0,
        tierExpiryDate = this["tierExpiryDate"] as? Long,
        createdAt     = this["createdAt"] as? Long ?: 0L
    )
}
