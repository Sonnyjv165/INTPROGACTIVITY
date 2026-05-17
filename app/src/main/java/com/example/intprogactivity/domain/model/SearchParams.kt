package com.example.intprogactivity.domain.model

data class FlightSearchParams(
    val origin: String,
    val originCity: String = "",
    val destination: String,
    val destinationCity: String = "",
    val departureDate: String,
    val returnDate: String? = null,
    val adults: Int = 1,
    val children: Int = 0,
    val infants: Int = 0,
    val travelClass: String = "ECONOMY",
    val nonStop: Boolean = false,
    val maxPrice: Int? = null,
    val currencyCode: String = "PHP"
) {
    fun isRoundTrip(): Boolean = returnDate != null
    fun totalPassengers(): Int = adults + children + infants
    fun routeLabel(): String = "$origin → $destination"
    fun passengerSummary(): String = buildString {
        append("$adults Adult${if (adults != 1) "s" else ""}")
        if (children > 0) append(", $children Child${if (children != 1) "ren" else ""}")
        if (infants > 0) append(", $infants Infant${if (infants != 1) "s" else ""}")
    }
}
