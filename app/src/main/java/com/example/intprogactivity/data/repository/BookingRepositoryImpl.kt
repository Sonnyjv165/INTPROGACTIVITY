package com.example.intprogactivity.data.repository

import com.example.intprogactivity.data.remote.firebase.FirestoreBookingSource
import com.example.intprogactivity.data.remote.firebase.toBooking
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.domain.model.BookingStatus
import com.example.intprogactivity.domain.repository.BookingRepository
import com.example.intprogactivity.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepositoryImpl @Inject constructor(
    private val bookingSource: FirestoreBookingSource
) : BookingRepository {

    override suspend fun createBooking(booking: Booking): Result<Booking> = try {
        val data = booking.toMap()
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

    private fun Booking.toMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("pnr", pnr)
        put("status", status.name)
        put("outboundFlightJson", outboundFlightJson)
        returnFlightJson?.let { put("returnFlightJson", it) }
        put("totalPrice", totalPrice)
        put("currency", currency)
        put("tripCoinsEarned", tripCoinsEarned)
        put("createdAt", createdAt)
        put("travelDate", travelDate)
        put("originIata", originIata)
        put("destinationIata", destinationIata)
        put("airlineName", airlineName)
        put("addOnsInsurance", addOns.travelInsurance)
    }
}
