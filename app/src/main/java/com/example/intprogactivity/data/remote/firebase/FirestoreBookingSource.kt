package com.example.intprogactivity.data.remote.firebase

import com.example.intprogactivity.domain.model.AddOns
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.domain.model.BookingStatus
import com.example.intprogactivity.domain.model.Passenger
import com.example.intprogactivity.domain.model.PassengerType
import com.example.intprogactivity.domain.model.SeatSelection
import com.example.intprogactivity.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
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

    suspend fun saveBooking(bookingData: Map<String, Any?>): String {
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
        }.sortedByDescending { map ->
            when (val raw = map["createdAt"]) {
                is com.google.firebase.Timestamp -> raw.toDate().time
                is Number -> raw.toLong()
                else -> 0L
            }
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus) {
        bookingsCol().document(bookingId).update(
            mapOf(
                "status"     to status.name,
                "updatedAt"  to com.google.firebase.Timestamp.now()
            )
        ).await()
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
    // ── passengerDetails (web schema) or passengers (old Android schema) ──
    val passengerDetails = (this["passengerDetails"] as? List<*>)?.mapNotNull { item ->
        (item as? Map<*, *>)?.let { m ->
            Passenger(
                firstName    = m["firstName"] as? String ?: "",
                lastName     = m["lastName"]  as? String ?: "",
                dateOfBirth  = m["dob"]       as? String ?: m["dateOfBirth"] as? String ?: "",
                gender       = m["gender"]    as? String ?: "",
                nationality  = m["nationality"] as? String ?: ""
            )
        }
    }
    val passengersLegacy = (this["passengers"] as? List<*>)?.mapNotNull { item ->
        (item as? Map<*, *>)?.let { m ->
            Passenger(
                id           = m["id"]           as? String ?: "",
                firstName    = m["firstName"]    as? String ?: "",
                lastName     = m["lastName"]     as? String ?: "",
                dateOfBirth  = m["dateOfBirth"]  as? String ?: "",
                gender       = m["gender"]       as? String ?: "",
                email        = m["email"]        as? String ?: "",
                phone        = m["phone"]        as? String ?: "",
                nationality  = m["nationality"]  as? String ?: "",
                passportNumber = m["passportNumber"] as? String ?: "",
                type = try { PassengerType.valueOf(m["type"] as? String ?: "ADULT") }
                       catch (_: Exception) { PassengerType.ADULT }
            )
        }
    }
    val passengers = passengerDetails ?: passengersLegacy ?: emptyList()

    // ── seat selections ────────────────────────────────────────────────────
    val seatSelections = (this["seatSelections"] as? List<*>)?.mapNotNull { item ->
        (item as? Map<*, *>)?.let { m ->
            SeatSelection(
                passengerId = m["passengerId"] as? String ?: "",
                segmentId   = m["segmentId"]   as? String ?: "SEG1",
                seatNumber  = m["seatNumber"]  as? String ?: "",
                price       = 0.0
            )
        }
    } ?: emptyList()

    // ── origin / destination: prefer root depart/arrival (web), fallback to originIata ──
    val originIata      = this["depart"]        as? String
        ?: this["originIata"]       as? String ?: ""
    val destinationIata = this["arrival"]       as? String
        ?: this["destinationIata"]  as? String ?: ""

    // ── PNR / confirm code ─────────────────────────────────────────────────
    val pnr = this["confirmCode"] as? String ?: this["pnr"] as? String ?: ""

    // ── total price ────────────────────────────────────────────────────────
    val totalPrice = (this["total"] as? Number)?.toDouble()
        ?: (this["totalPrice"] as? Number)?.toDouble() ?: 0.0

    // ── createdAt: Firestore Timestamp or Long millis ──────────────────────
    val createdAt = when (val raw = this["createdAt"]) {
        is com.google.firebase.Timestamp -> raw.toDate().time
        is Number                        -> raw.toLong()
        else                             -> 0L
    }
    val travelDate = when (val raw = this["departDate"] ?: this["travelDate"]) {
        is com.google.firebase.Timestamp -> raw.toDate().time
        is Number                        -> raw.toLong()
        else                             -> 0L
    }

    // ── legs array (web schema) ────────────────────────────────────────────
    val legs = this["legs"] as? List<*>
    val leg0 = legs?.getOrNull(0) as? Map<*, *>
    val leg1 = legs?.getOrNull(1) as? Map<*, *>

    // ── outboundFlightJson: rebuild from legs[0] if not present ───────────
    val storedOutboundJson = this["outboundFlightJson"] as? String
    val outboundFlightJson: String =
        (if (!storedOutboundJson.isNullOrBlank()) storedOutboundJson
         else legToFlightOfferJson(leg0)) ?: ""

    // ── returnFlightJson: rebuild from legs[1] if not present ────────────
    val storedReturnJson = this["returnFlightJson"] as? String
    val returnFlightJson: String? =
        if (!storedReturnJson.isNullOrBlank()) storedReturnJson
        else legToFlightOfferJson(leg1)

    Booking(
        bookingId    = this["bookingId"]  as? String ?: "",
        userId       = this["userId"]     as? String ?: "",
        userEmail    = this["userEmail"]  as? String ?: "",
        userName     = this["userName"]   as? String ?: "",
        pnr          = pnr,
        status       = try { BookingStatus.valueOf(this["status"] as? String ?: "PENDING") }
                       catch (_: Exception) { BookingStatus.PENDING },
        outboundFlightJson = outboundFlightJson,
        returnFlightJson   = returnFlightJson,
        passengers   = passengers,
        totalPrice   = totalPrice,
        currency     = this["currency"]  as? String ?: "PHP",
        addOns       = AddOns(
            travelInsurance = this["addOnsInsurance"] as? Boolean ?: false,
            seatSelections  = seatSelections
        ),
        tripCoinsEarned = (this["tripCoinsEarned"] as? Number)?.toInt() ?: 0,
        createdAt    = createdAt,
        travelDate   = travelDate,
        originIata   = originIata,
        destinationIata = destinationIata,
        airlineName  = this["airlineName"] as? String
            ?: this["airlineCode"]  as? String ?: "",
        cabinClass   = this["class"]        as? String ?: "ECONOMY",
        paymentMethod = this["paymentMethod"] as? String ?: "",
        paymentStatus = this["paymentStatus"] as? String ?: "",
        promoCode    = this["promoCode"]    as? String
    )
} catch (_: Exception) {
    null
}

/**
 * Converts a Firestore leg map (web booking schema) into a minimal FlightOffer JSON string
 * that the boarding-pass display composables can parse.
 *
 * Leg fields used: depart, arrival, departDate, arriveDate, airlineCode, flightNumber, fare
 */
private fun legToFlightOfferJson(leg: Map<*, *>?): String? {
    if (leg == null) return null
    val depart      = leg["depart"]      as? String ?: return null
    val arrival     = leg["arrival"]     as? String ?: return null
    val airlineCode = leg["airlineCode"] as? String ?: ""
    val fullFlight  = leg["flightNumber"] as? String ?: ""
    // strip airline prefix to get just the number (e.g. "5J102" → "102")
    val flightNum   = if (fullFlight.startsWith(airlineCode) && airlineCode.isNotEmpty())
        fullFlight.removePrefix(airlineCode) else fullFlight
    val fare        = (leg["fare"] as? Number)?.toDouble() ?: 0.0

    val departIso = timestampOrMillisToIso(leg["departDate"])
    val arriveIso = timestampOrMillisToIso(leg["arriveDate"])

    // Escape strings for JSON safety
    fun String.esc() = replace("\"", "\\\"")

    return """{"id":"${fullFlight.esc()}","itineraries":[{"duration":"","segments":[{"departure":{"iataCode":"${depart.esc()}","at":"${departIso.esc()}"},"arrival":{"iataCode":"${arrival.esc()}","at":"${arriveIso.esc()}"},"carrierCode":"${airlineCode.esc()}","number":"${flightNum.esc()}","aircraft":{"code":""},"operating":{"carrierCode":"${airlineCode.esc()}"},"duration":"","id":"1","numberOfStops":0}]}],"price":{"currency":"PHP","total":"$fare","grandTotal":"$fare"},"travelerPricings":[]}"""
}

private fun timestampOrMillisToIso(raw: Any?): String = when (raw) {
    is com.google.firebase.Timestamp -> {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        sdf.format(raw.toDate())
    }
    is Number -> {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        sdf.format(java.util.Date(raw.toLong()))
    }
    else -> ""
}
