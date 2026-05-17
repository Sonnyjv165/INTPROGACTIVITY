package com.example.intprogactivity.util

object Constants {
    const val AMADEUS_BASE_URL = "https://test.api.amadeus.com/"
    const val AMADEUS_AIRLINE_LOGO_URL = "https://pics.avs.io/200/200/%s.png"

    const val FIRESTORE_USERS = "users"
    const val FIRESTORE_BOOKINGS = "bookings"
    const val FIRESTORE_ALERTS = "price_alerts"
    const val FIRESTORE_COIN_HISTORY = "coin_history"

    const val MIN_BOOKING_HOURS_ADVANCE = 2L
    const val MIN_CANCEL_HOURS_BEFORE = 24L
    const val RYANAIR_IATA_CODE = "FR"

    const val BASE_COIN_EARN_RATE = 0.05 // 1 coin per ₱20 spent

    const val DATASTORE_PREFS_NAME = "trip_flights_prefs"
    const val KEY_LAST_SEARCH_ORIGIN = "last_search_origin"
    const val KEY_LAST_SEARCH_DESTINATION = "last_search_destination"
    const val KEY_LAST_SEARCH_ORIGIN_IATA = "last_search_origin_iata"
    const val KEY_LAST_SEARCH_DESTINATION_IATA = "last_search_destination_iata"
    const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
}
