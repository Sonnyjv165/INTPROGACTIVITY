package com.example.intprogactivity.domain.repository

import com.example.intprogactivity.domain.model.Airport
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.model.FlightSearchParams
import com.example.intprogactivity.util.Result

interface FlightRepository {
    suspend fun searchFlights(params: FlightSearchParams): Result<List<FlightOffer>>
    suspend fun getFlightPrice(flightOffer: FlightOffer): Result<FlightOffer>
    suspend fun searchAirports(keyword: String): Result<List<Airport>>
}
