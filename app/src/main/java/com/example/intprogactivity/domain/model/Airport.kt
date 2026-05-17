package com.example.intprogactivity.domain.model

data class Airport(
    val iataCode: String,
    val name: String,
    val cityName: String,
    val countryName: String,
    val countryCode: String
) {
    fun displayLabel(): String = "$cityName ($iataCode)"
    fun displaySubLabel(): String = "$name · $countryName"
}
