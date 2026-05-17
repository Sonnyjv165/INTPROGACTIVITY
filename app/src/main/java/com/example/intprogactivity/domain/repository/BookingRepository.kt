package com.example.intprogactivity.domain.repository

import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.util.Result
import kotlinx.coroutines.flow.Flow

interface BookingRepository {
    suspend fun createBooking(booking: Booking): Result<Booking>
    suspend fun getUserBookings(userId: String): Result<List<Booking>>
    suspend fun getBookingById(bookingId: String): Result<Booking>
    suspend fun cancelBooking(bookingId: String): Result<Unit>
    fun getUserBookingsFlow(userId: String): Flow<List<Booking>>
}
