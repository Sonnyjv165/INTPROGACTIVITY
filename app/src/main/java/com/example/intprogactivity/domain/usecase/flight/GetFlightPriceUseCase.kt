package com.example.intprogactivity.domain.usecase.flight

import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.repository.FlightRepository
import com.example.intprogactivity.util.Result
import com.example.intprogactivity.util.isDepartingWithin2Hours
import javax.inject.Inject

class GetFlightPriceUseCase @Inject constructor(
    private val flightRepository: FlightRepository
) {
    suspend operator fun invoke(flightOffer: FlightOffer): Result<FlightOffer> {
        if (flightOffer.isDepartingWithin2Hours())
            return Result.Error(Exception("This flight departs within 2 hours and cannot be booked."))
        return flightRepository.getFlightPrice(flightOffer)
    }
}
