package com.example.intprogactivity.domain.usecase.booking

import com.example.intprogactivity.domain.model.BookingStatus
import com.example.intprogactivity.domain.repository.BookingRepository
import com.example.intprogactivity.util.Constants
import com.example.intprogactivity.util.Result
import java.time.temporal.ChronoUnit
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class CancelBookingUseCase @Inject constructor(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(bookingId: String, travelDateMs: Long): Result<Unit> {
        val travelDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(travelDateMs), ZoneOffset.UTC)
        val hoursUntilDeparture = ChronoUnit.HOURS.between(LocalDateTime.now(ZoneOffset.UTC), travelDate)
        if (hoursUntilDeparture < Constants.MIN_CANCEL_HOURS_BEFORE)
            return Result.Error(Exception("Bookings cannot be cancelled within 24 hours of departure."))
        return bookingRepository.cancelBooking(bookingId)
    }
}
