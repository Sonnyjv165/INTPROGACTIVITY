package com.example.intprogactivity.data.remote.amadeus.dto

data class LocationResponse(
    val data: List<LocationDto> = emptyList()
)

data class LocationDto(
    val type: String = "",
    val subType: String = "",
    val name: String = "",
    val iataCode: String = "",
    val address: AddressDto = AddressDto()
)

data class AddressDto(
    val cityName: String = "",
    val countryName: String = "",
    val countryCode: String = ""
)
