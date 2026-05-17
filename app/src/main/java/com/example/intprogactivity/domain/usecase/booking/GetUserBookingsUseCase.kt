package com.example.intprogactivity.domain.usecase.booking

import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.domain.repository.BookingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserBookingsUseCase @Inject constructor(
    private val bookingRepository: BookingRepository
) {
    operator fun invoke(userId: String): Flow<List<Booking>> =
        bookingRepository.getUserBookingsFlow(userId)
}
