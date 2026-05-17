package com.example.intprogactivity.data.remote.amadeus.dto

import com.google.gson.annotations.SerializedName

data class FlightOffersResponse(
    val data: List<FlightOfferDto> = emptyList(),
    val meta: MetaDto? = null
)

data class MetaDto(val count: Int = 0)

data class FlightOfferDto(
    val id: String = "",
    val source: String = "",
    val itineraries: List<ItineraryDto> = emptyList(),
    val price: PriceDto = PriceDto(),
    val validatingAirlineCodes: List<String> = emptyList(),
    val travelerPricings: List<TravelerPricingDto> = emptyList(),
    val numberOfBookableSeats: Int = 0
)

data class ItineraryDto(
    val duration: String = "",
    val segments: List<SegmentDto> = emptyList()
)

data class SegmentDto(
    val id: String = "",
    val departure: EndpointDto = EndpointDto(),
    val arrival: EndpointDto = EndpointDto(),
    @SerializedName("carrierCode") val carrierCode: String = "",
    val number: String = "",
    val aircraft: AircraftDto = AircraftDto(),
    val duration: String = "",
    val numberOfStops: Int = 0,
    val blacklistedInEU: Boolean = false
)

data class EndpointDto(
    val iataCode: String = "",
    val terminal: String? = null,
    val at: String = ""
)

data class AircraftDto(val code: String = "")

data class PriceDto(
    val currency: String = "PHP",
    val total: String = "0",
    val base: String = "0",
    val fees: List<FeeDto>? = null,
    val grandTotal: String = "0"
)

data class FeeDto(val amount: String = "0", val type: String = "")

data class TravelerPricingDto(
    val travelerId: String = "",
    val fareOption: String = "",
    val travelerType: String = "ADULT",
    val price: PriceDto = PriceDto(),
    val fareDetailsBySegment: List<FareDetailDto> = emptyList()
)

data class FareDetailDto(
    val segmentId: String = "",
    val cabin: String = "ECONOMY",
    val fareBasis: String = "",
    val brandedFare: String? = null,
    val includedCheckedBags: BaggageDto? = null
)

data class BaggageDto(
    val quantity: Int? = null,
    val weight: Int? = null,
    val weightUnit: String? = null
)
