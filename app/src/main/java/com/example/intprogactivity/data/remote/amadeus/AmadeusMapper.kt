package com.example.intprogactivity.data.remote.amadeus

import com.example.intprogactivity.data.remote.amadeus.dto.*
import com.example.intprogactivity.domain.model.*

object AmadeusMapper {

    fun FlightOfferDto.toDomain(rawJson: String = ""): FlightOffer = FlightOffer(
        id = id,
        source = source,
        itineraries = itineraries.map { it.toDomain() },
        price = price.toDomain(),
        validatingAirlineCodes = validatingAirlineCodes,
        travelerPricings = travelerPricings.map { it.toDomain() },
        numberOfBookableSeats = numberOfBookableSeats,
        rawJson = rawJson
    )

    fun ItineraryDto.toDomain(): Itinerary = Itinerary(
        duration = duration,
        segments = segments.map { it.toDomain() }
    )

    fun SegmentDto.toDomain(): Segment = Segment(
        id = id,
        departure = departure.toDomain(),
        arrival = arrival.toDomain(),
        carrierCode = carrierCode,
        number = number,
        aircraft = aircraft.code,
        duration = duration,
        numberOfStops = numberOfStops,
        blacklistedInEU = blacklistedInEU
    )

    fun EndpointDto.toDomain(): FlightEndpoint = FlightEndpoint(
        iataCode = iataCode,
        terminal = terminal,
        at = at
    )

    fun PriceDto.toDomain(): Price = Price(
        currency = currency,
        total = total,
        base = base,
        fees = fees?.map { Fee(it.amount, it.type) } ?: emptyList(),
        grandTotal = grandTotal
    )

    fun TravelerPricingDto.toDomain(): TravelerPricing = TravelerPricing(
        travelerId = travelerId,
        fareOption = fareOption,
        travelerType = travelerType,
        price = price.toDomain(),
        fareDetailsBySegment = fareDetailsBySegment.map { it.toDomain() }
    )

    fun FareDetailDto.toDomain(): FareDetail = FareDetail(
        segmentId = segmentId,
        cabin = cabin,
        fareBasis = fareBasis,
        brandedFare = brandedFare,
        includedCheckedBags = includedCheckedBags?.toDomain()
    )

    fun BaggageDto.toDomain(): BaggageAllowance = BaggageAllowance(
        quantity = quantity,
        weight = weight,
        weightUnit = weightUnit
    )

    fun LocationDto.toDomain(): Airport = Airport(
        iataCode = iataCode,
        name = name,
        cityName = address.cityName,
        countryName = address.countryName,
        countryCode = address.countryCode
    )
}
