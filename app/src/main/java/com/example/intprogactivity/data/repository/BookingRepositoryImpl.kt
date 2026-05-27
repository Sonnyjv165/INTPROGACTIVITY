package com.example.intprogactivity.data.repository

import com.example.intprogactivity.data.remote.firebase.FirestoreBookingSource
import com.example.intprogactivity.data.remote.firebase.toBooking
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.domain.model.BookingStatus
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.repository.BookingRepository
import com.example.intprogactivity.util.Result
import com.google.firebase.Timestamp
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepositoryImpl @Inject constructor(
    private val bookingSource: FirestoreBookingSource,
    private val gson: Gson
) : BookingRepository {

    override suspend fun createBooking(booking: Booking): Result<Booking> = try {
        val data = booking.toWebMap(gson)
        val id = bookingSource.saveBooking(data)
        Result.Success(booking.copy(bookingId = id))
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override suspend fun getUserBookings(userId: String): Result<List<Booking>> = try {
        val rawList = bookingSource.getUserBookings(userId)
        Result.Success(rawList.mapNotNull { it.toBooking() })
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override suspend fun getBookingById(bookingId: String): Result<Booking> = try {
        val data = bookingSource.getBooking(bookingId)
            ?: return Result.Error(Exception("Booking not found"))
        val booking = data.toBooking()
            ?: return Result.Error(Exception("Invalid booking data"))
        Result.Success(booking)
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override suspend fun cancelBooking(bookingId: String): Result<Unit> = try {
        bookingSource.updateBookingStatus(bookingId, BookingStatus.CANCELLED)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }

    override fun getUserBookingsFlow(userId: String): Flow<List<Booking>> =
        bookingSource.getUserBookingsFlow(userId)
}

// ─── Web-compatible Firestore map builder ─────────────────────────────────────

private fun Booking.toWebMap(gson: Gson): Map<String, Any?> {
    val outbound = runCatching { gson.fromJson(outboundFlightJson, FlightOffer::class.java) }.getOrNull()
    val returnFl = returnFlightJson?.let {
        runCatching { gson.fromJson(it, FlightOffer::class.java) }.getOrNull()
    }

    val firstSeg = outbound?.itineraries?.firstOrNull()?.segments?.firstOrNull()
    val airlineCode = firstSeg?.carrierCode ?: ""
    val fullFlightNum = airlineCode + (firstSeg?.number ?: "")

    val now = Timestamp.now()

    return mapOf(
        // ── Root-level flight info (matches web) ──────────────────────────
        "airlineCode"   to airlineCode,
        "airlineName"   to airlineNameFor(airlineCode),
        "arrival"       to (firstSeg?.arrival?.iataCode ?: destinationIata),
        "class"         to cabinClass,
        "confirmCode"   to pnr,
        "createdAt"     to now,
        "depart"        to (firstSeg?.departure?.iataCode ?: originIata),
        "departDate"    to isoToTimestamp(firstSeg?.departure?.at),
        "flightNumber"  to fullFlightNum,

        // ── Legs array (one per flight leg) ───────────────────────────────
        "legs"          to buildLegs(outbound, returnFl, cabinClass),

        // ── Passenger details ─────────────────────────────────────────────
        "passengerDetails" to passengers.map { p ->
            mapOf(
                "dob"         to p.dateOfBirth,
                "firstName"   to p.firstName,
                "gender"      to p.gender,
                "lastName"    to p.lastName,
                "middleInit"  to "",          // not captured in current model
                "name"        to p.fullName(),
                "nationality" to p.nationality,
                "suffix"      to ""           // not captured in current model
            )
        },
        "passengers"    to passengers.size,

        // ── Payment & status ──────────────────────────────────────────────
        "paymentMethod" to paymentMethod,
        "paymentStatus" to paymentStatus,
        "promoCode"     to promoCode,
        "status"        to status.name,       // "PENDING", "CONFIRMED", etc.
        "total"         to totalPrice,
        "updatedAt"     to now,

        // ── User info ─────────────────────────────────────────────────────
        "userEmail"     to userEmail,
        "userId"        to userId,
        "userName"      to userName,

        // ── Android-side extras (kept for backwards compat with app) ──────
        "currency"        to currency,
        "tripCoinsEarned" to tripCoinsEarned,
        "travelDate"      to travelDate,
        "addOnsInsurance" to addOns.travelInsurance,
        "seatSelections"  to addOns.seatSelections.map { s ->
            mapOf("passengerId" to s.passengerId, "segmentId" to s.segmentId, "seatNumber" to s.seatNumber)
        }
    )
}

private fun buildLegs(
    outbound: FlightOffer?,
    returnFlight: FlightOffer?,
    cabinClass: String
): List<Map<String, Any?>> {
    val legs = mutableListOf<Map<String, Any?>>()
    outbound?.let { legs.add(flightOfferToLeg(it, cabinClass)) }
    returnFlight?.let { legs.add(flightOfferToLeg(it, cabinClass)) }
    return legs
}

private fun flightOfferToLeg(offer: FlightOffer, cabinClass: String): Map<String, Any?> {
    val seg = offer.itineraries.firstOrNull()?.segments?.firstOrNull()
    val lastSeg = offer.itineraries.firstOrNull()?.segments?.lastOrNull()
    val airlineCode = seg?.carrierCode ?: ""
    val perAdultFare = offer.travelerPricings
        .firstOrNull { it.travelerType == "ADULT" }
        ?.price?.total?.toDoubleOrNull()
        ?: offer.totalPriceDouble()

    return mapOf(
        "airlineCode"  to airlineCode,
        "airlineName"  to airlineNameFor(airlineCode),
        "arrival"      to (lastSeg?.arrival?.iataCode ?: ""),
        "arriveDate"   to isoToTimestamp(lastSeg?.arrival?.at),
        "class"        to cabinClass,
        "depart"       to (seg?.departure?.iataCode ?: ""),
        "departDate"   to isoToTimestamp(seg?.departure?.at),
        "fare"         to perAdultFare,
        "flightId"     to offer.id,       // Firestore doc ID from flights collection
        "flightNumber" to (airlineCode + (seg?.number ?: ""))
    )
}

private fun isoToTimestamp(iso: String?): Timestamp {
    if (iso.isNullOrEmpty()) return Timestamp.now()
    return try {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val ldt = LocalDateTime.parse(iso, fmt)
        val instant = ldt.atZone(ZoneId.systemDefault()).toInstant()
        Timestamp(Date.from(instant))
    } catch (_: Exception) {
        Timestamp.now()
    }
}

private fun airlineNameFor(code: String): String = when (code.uppercase()) {
    "5J" -> "Cebu Pacific"
    "PR" -> "Philippine Airlines"
    "Z2" -> "AirAsia Philippines"
    "2P" -> "PAL Express"
    "DG" -> "Cebgo"
    "SQ" -> "Singapore Airlines"
    "CX" -> "Cathay Pacific"
    "MH" -> "Malaysia Airlines"
    "EK" -> "Emirates"
    "OZ" -> "Asiana Airlines"
    "KE" -> "Korean Air"
    "NH" -> "ANA"
    "JL" -> "Japan Airlines"
    else -> code
}
