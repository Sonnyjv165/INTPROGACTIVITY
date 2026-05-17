package com.example.intprogactivity.data.remote.amadeus.dto

data class PricingRequest(val data: PricingRequestData)
data class PricingRequestData(val type: String = "flight-offers", val flightOffers: List<FlightOfferDto>)

data class PricingResponse(val data: PricingDataDto = PricingDataDto())
data class PricingDataDto(val type: String = "", val flightOffers: List<FlightOfferDto> = emptyList())
