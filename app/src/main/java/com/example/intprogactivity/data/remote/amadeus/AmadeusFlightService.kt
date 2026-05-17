package com.example.intprogactivity.data.remote.amadeus

import com.example.intprogactivity.data.remote.amadeus.dto.FlightOffersResponse
import com.example.intprogactivity.data.remote.amadeus.dto.LocationResponse
import com.example.intprogactivity.data.remote.amadeus.dto.PricingRequest
import com.example.intprogactivity.data.remote.amadeus.dto.PricingResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AmadeusFlightService {

    @GET("v2/shopping/flight-offers")
    suspend fun searchFlights(
        @Query("originLocationCode") origin: String,
        @Query("destinationLocationCode") destination: String,
        @Query("departureDate") departureDate: String,
        @Query("returnDate") returnDate: String? = null,
        @Query("adults") adults: Int,
        @Query("children") children: Int? = null,
        @Query("infants") infants: Int? = null,
        @Query("travelClass") travelClass: String? = null,
        @Query("nonStop") nonStop: Boolean? = null,
        @Query("maxPrice") maxPrice: Int? = null,
        @Query("currencyCode") currencyCode: String = "PHP",
        @Query("max") max: Int = 50
    ): FlightOffersResponse

    @POST("v1/shopping/flight-offers/pricing")
    suspend fun priceFlightOffer(@Body body: PricingRequest): PricingResponse

    @GET("v1/reference-data/locations")
    suspend fun searchLocations(
        @Query("keyword") keyword: String,
        @Query("subType") subType: String = "AIRPORT,CITY",
        @Query("page[limit]") limit: Int = 10
    ): LocationResponse
}
