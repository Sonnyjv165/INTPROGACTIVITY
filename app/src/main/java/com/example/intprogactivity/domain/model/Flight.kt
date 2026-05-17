package com.example.intprogactivity.domain.model

data class FlightOffer(
    val id: String,
    val source: String,
    val itineraries: List<Itinerary>,
    val price: Price,
    val validatingAirlineCodes: List<String>,
    val travelerPricings: List<TravelerPricing>,
    val numberOfBookableSeats: Int,
    val rawJson: String = ""
) {
    fun primaryAirlineCode(): String = validatingAirlineCodes.firstOrNull() ?: ""
    fun totalPriceDouble(): Double = price.grandTotal.toDoubleOrNull() ?: 0.0
    fun firstDepartureTime(): String =
        itineraries.firstOrNull()?.segments?.firstOrNull()?.departure?.at ?: ""
    fun firstArrivalTime(): String =
        itineraries.firstOrNull()?.segments?.lastOrNull()?.arrival?.at ?: ""
    fun stopCount(): Int =
        (itineraries.firstOrNull()?.segments?.size ?: 1) - 1
}

data class Itinerary(
    val duration: String,
    val segments: List<Segment>
)

data class Segment(
    val id: String = "",
    val departure: FlightEndpoint,
    val arrival: FlightEndpoint,
    val carrierCode: String,
    val number: String,
    val aircraft: String,
    val duration: String,
    val numberOfStops: Int,
    val blacklistedInEU: Boolean = false
) {
    fun flightNumber(): String = "$carrierCode$number"
}

data class FlightEndpoint(
    val iataCode: String,
    val terminal: String?,
    val at: String
)

data class Price(
    val currency: String,
    val total: String,
    val base: String,
    val fees: List<Fee> = emptyList(),
    val grandTotal: String
)

data class Fee(val amount: String, val type: String)

data class TravelerPricing(
    val travelerId: String,
    val fareOption: String,
    val travelerType: String,
    val price: Price,
    val fareDetailsBySegment: List<FareDetail>
)

data class FareDetail(
    val segmentId: String,
    val cabin: String,
    val fareBasis: String,
    val brandedFare: String? = null,
    val includedCheckedBags: BaggageAllowance? = null
)

data class BaggageAllowance(
    val quantity: Int? = null,
    val weight: Int? = null,
    val weightUnit: String? = null
) {
    fun displayText(): String = when {
        quantity != null -> "$quantity pc"
        weight != null -> "${weight}${weightUnit ?: "kg"}"
        else -> "Not included"
    }
}
