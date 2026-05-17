package com.example.intprogactivity.data.remote.firebase

import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.domain.model.BookingStatus
import com.example.intprogactivity.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreBookingSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun bookingsCol() = firestore.collection(Constants.FIRESTORE_BOOKINGS)

    suspend fun saveBooking(bookingData: Map<String, Any>): String {
        val docRef = bookingsCol().add(bookingData).await()
        return docRef.id
    }

    suspend fun getBooking(bookingId: String): Map<String, Any>? {
        val snapshot = bookingsCol().document(bookingId).get().await()
        return if (snapshot.exists()) snapshot.data?.plus("bookingId" to snapshot.id) else null
    }

    suspend fun getUserBookings(userId: String): List<Map<String, Any>> {
        val snapshot = bookingsCol()
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.data?.plus("bookingId" to doc.id)
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus) {
        bookingsCol().document(bookingId).update("status", status.name).await()
    }

    fun getUserBookingsFlow(userId: String): Flow<List<Booking>> = callbackFlow {
        val listener = bookingsCol()
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.plus("bookingId" to doc.id)?.toBooking()
                } ?: emptyList()
                trySend(bookings)
            }
        awaitClose { listener.remove() }
    }
}

fun Map<String, Any>.toBooking(): Booking? = try {
    Booking(
        bookingId = this["bookingId"] as? String ?: "",
        userId = this["userId"] as? String ?: "",
        pnr = this["pnr"] as? String ?: "",
        status = BookingStatus.valueOf(this["status"] as? String ?: "CONFIRMED"),
        outboundFlightJson = this["outboundFlightJson"] as? String ?: "",
        returnFlightJson = this["returnFlightJson"] as? String,
        totalPrice = this["totalPrice"] as? Double ?: 0.0,
        currency = this["currency"] as? String ?: "PHP",
        tripCoinsEarned = (this["tripCoinsEarned"] as? Long)?.toInt() ?: 0,
        createdAt = this["createdAt"] as? Long ?: 0L,
        travelDate = this["travelDate"] as? Long ?: 0L,
        originIata = this["originIata"] as? String ?: "",
        destinationIata = this["destinationIata"] as? String ?: "",
        airlineName = this["airlineName"] as? String ?: ""
    )
} catch (_: Exception) {
    null
}
