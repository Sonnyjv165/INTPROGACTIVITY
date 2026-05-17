package com.example.intprogactivity.data.remote.firebase

import com.example.intprogactivity.domain.model.AddOns
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.domain.model.BookingStatus
import com.example.intprogactivity.domain.model.Passenger
import com.example.intprogactivity.domain.model.PassengerType
import com.example.intprogactivity.domain.model.SeatSelection
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
            .get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.data?.plus("bookingId" to doc.id)
        }.sortedByDescending { it["createdAt"] as? Long ?: 0L }
    }

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus) {
        bookingsCol().document(bookingId).update("status", status.name).await()
    }

    fun getUserBookingsFlow(userId: String): Flow<List<Booking>> = callbackFlow {
        val listener = bookingsCol()
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener  // keep last good data on error
                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.plus("bookingId" to doc.id)?.toBooking()
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(bookings)
            }
        awaitClose { listener.remove() }
    }
}

fun Map<String, Any>.toBooking(): Booking? = try {
    val passengers = (this["passengers"] as? List<*>)?.mapNotNull { item ->
        (item as? Map<*, *>)?.let { m ->
            Passenger(
                id = m["id"] as? String ?: "",
                firstName = m["firstName"] as? String ?: "",
                lastName = m["lastName"] as? String ?: "",
                dateOfBirth = m["dateOfBirth"] as? String ?: "",
                gender = m["gender"] as? String ?: "",
                email = m["email"] as? String ?: "",
                phone = m["phone"] as? String ?: "",
                nationality = m["nationality"] as? String ?: "",
                passportNumber = m["passportNumber"] as? String ?: "",
                type = try { PassengerType.valueOf(m["type"] as? String ?: "ADULT") } catch (_: Exception) { PassengerType.ADULT }
            )
        }
    } ?: emptyList()

    val seatSelections = (this["seatSelections"] as? List<*>)?.mapNotNull { item ->
        (item as? Map<*, *>)?.let { m ->
            SeatSelection(
                passengerId = m["passengerId"] as? String ?: "",
                segmentId = m["segmentId"] as? String ?: "SEG1",
                seatNumber = m["seatNumber"] as? String ?: "",
                price = 0.0
            )
        }
    } ?: emptyList()

    Booking(
        bookingId = this["bookingId"] as? String ?: "",
        userId = this["userId"] as? String ?: "",
        pnr = this["pnr"] as? String ?: "",
        status = BookingStatus.valueOf(this["status"] as? String ?: "CONFIRMED"),
        outboundFlightJson = this["outboundFlightJson"] as? String ?: "",
        returnFlightJson = this["returnFlightJson"] as? String,
        passengers = passengers,
        totalPrice = (this["totalPrice"] as? Number)?.toDouble() ?: 0.0,
        currency = this["currency"] as? String ?: "PHP",
        addOns = AddOns(
            travelInsurance = this["addOnsInsurance"] as? Boolean ?: false,
            seatSelections = seatSelections
        ),
        tripCoinsEarned = (this["tripCoinsEarned"] as? Number)?.toInt() ?: 0,
        createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L,
        travelDate = (this["travelDate"] as? Number)?.toLong() ?: 0L,
        originIata = this["originIata"] as? String ?: "",
        destinationIata = this["destinationIata"] as? String ?: "",
        airlineName = this["airlineName"] as? String ?: ""
    )
} catch (_: Exception) {
    null
}
