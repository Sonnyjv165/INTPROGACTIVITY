package com.example.intprogactivity.domain.usecase.flight

import com.example.intprogactivity.domain.model.Airport
import com.example.intprogactivity.domain.repository.FlightRepository
import com.example.intprogactivity.util.Result
import javax.inject.Inject

class SearchAirportsUseCase @Inject constructor(
    private val flightRepository: FlightRepository
) {
    suspend operator fun invoke(keyword: String): Result<List<Airport>> {
        if (keyword.length < 2) return Result.Success(emptyList())
        return flightRepository.searchAirports(keyword)
    }
}
