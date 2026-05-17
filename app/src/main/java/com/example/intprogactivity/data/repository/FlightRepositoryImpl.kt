package com.example.intprogactivity.data.repository

import com.example.intprogactivity.data.local.LocalAirportData
import com.example.intprogactivity.data.local.LocalFlightData
import com.example.intprogactivity.domain.model.Airport
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.model.FlightSearchParams
import com.example.intprogactivity.domain.repository.FlightRepository
import com.example.intprogactivity.util.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightRepositoryImpl @Inject constructor() : FlightRepository {

    override suspend fun searchFlights(params: FlightSearchParams): Result<List<FlightOffer>> {
        return try {
            val offers = LocalFlightData.searchFlights(params)
            Result.Success(offers)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to search flights")
        }
    }

    override suspend fun getFlightPrice(flightOffer: FlightOffer): Result<FlightOffer> {
        // Local data — price is already final, return as-is
        return Result.Success(flightOffer)
    }

    override suspend fun searchAirports(keyword: String): Result<List<Airport>> {
        return try {
            Result.Success(LocalAirportData.search(keyword))
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Airport search failed")
        }
    }
}
