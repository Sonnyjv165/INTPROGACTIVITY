package com.example.intprogactivity.domain.usecase.flight

import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.model.FlightSearchParams
import com.example.intprogactivity.domain.repository.FlightRepository
import com.example.intprogactivity.util.Constants
import com.example.intprogactivity.util.Result
import com.example.intprogactivity.util.containsRyanair
import javax.inject.Inject

class SearchFlightsUseCase @Inject constructor(
    private val flightRepository: FlightRepository
) {
    suspend operator fun invoke(params: FlightSearchParams): Result<List<FlightOffer>> {
        if (params.origin.isBlank() || params.destination.isBlank())
            return Result.Error(Exception("Origin and destination are required."))
        if (params.origin == params.destination)
            return Result.Error(Exception("Origin and destination cannot be the same."))
        return when (val result = flightRepository.searchFlights(params)) {
            is Result.Success -> Result.Success(
                result.data.filter { !it.containsRyanair() }
            )
            else -> result
        }
    }
}
