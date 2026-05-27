package com.example.intprogactivity.data.repository

import android.util.Log
import com.example.intprogactivity.data.local.LocalAirportData
import com.example.intprogactivity.data.local.LocalFlightData
import com.example.intprogactivity.data.local.RouteTemplate
import com.example.intprogactivity.data.remote.firebase.FirestoreFlightSource
import com.example.intprogactivity.domain.model.*
import com.example.intprogactivity.domain.repository.FlightRepository
import com.example.intprogactivity.util.Result
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightRepositoryImpl @Inject constructor(
    private val flightSource: FirestoreFlightSource
) : FlightRepository {

    private val TAG = "FlightRepository"

    override suspend fun searchFlights(params: FlightSearchParams): Result<List<FlightOffer>> {
        return try {
            // Try Firestore first; fall back to local data if unavailable
            val routes = try {
                val remote = flightSource.getRoutes()
                if (remote.isEmpty()) {
                    Log.w(TAG, "Firestore returned empty routes — using local fallback")
                    LocalFlightData.routes
                } else {
                    remote
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore unavailable — using local fallback: ${e.message}")
                LocalFlightData.routes
            }

            val offers = buildFlightOffers(routes, params)
            Result.Success(offers)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to search flights")
        }
    }

    override suspend fun getFlightPrice(flightOffer: FlightOffer): Result<FlightOffer> {
        // Price is already included in the offer from search
        return Result.Success(flightOffer)
    }

    override suspend fun searchAirports(keyword: String): Result<List<Airport>> {
        return try {
            Result.Success(LocalAirportData.search(keyword))
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Airport search failed")
        }
    }

    // ─── Flight building logic ────────────────────────────────────────────────

    private fun buildFlightOffers(
        routes: Map<String, List<RouteTemplate>>,
        params: FlightSearchParams
    ): List<FlightOffer> {
        val key        = "${params.origin.uppercase()}-${params.destination.uppercase()}"
        val reverseKey = "${params.destination.uppercase()}-${params.origin.uppercase()}"
        val isReverse  = !routes.containsKey(key) && routes.containsKey(reverseKey)
        val templates  = routes[key] ?: routes[reverseKey] ?: return emptyList()

        val origin      = if (isReverse) params.destination.uppercase() else params.origin.uppercase()
        val destination = if (isReverse) params.origin.uppercase() else params.destination.uppercase()

        val date = runCatching {
            LocalDate.parse(params.departureDate, DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrElse { LocalDate.now().plusDays(1) }

        val dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        return templates.mapIndexed { index, t ->
            val departure = LocalDateTime.of(
                date.year, date.monthValue, date.dayOfMonth,
                t.departureHour, t.departureMinute
            )
            val arrival = departure.plusMinutes(t.durationMinutes.toLong())

            val adultPrice  = t.basePricePhp
            val childPrice  = t.basePricePhp * 0.75
            val infantPrice = t.basePricePhp * 0.10
            val total = adultPrice  * params.adults   +
                        childPrice  * params.children  +
                        infantPrice * params.infants
            val base  = total * 0.82
            val tax   = total - base

            fun d(v: Double) = "%.2f".format(v)

            val segment = Segment(
                id          = "S1",
                departure   = FlightEndpoint(origin, null, departure.format(dtFmt)),
                arrival     = FlightEndpoint(destination, null, arrival.format(dtFmt)),
                carrierCode = t.airlineCode,
                number      = t.flightNumber,
                aircraft    = t.aircraft,
                duration    = isoDuration(t.durationMinutes),
                numberOfStops = 0
            )

            FlightOffer(
                id     = "${index + 1}",
                source = "FIRESTORE",
                itineraries = listOf(Itinerary(
                    duration = isoDuration(t.durationMinutes),
                    segments = listOf(segment)
                )),
                price = Price(
                    currency   = "PHP",
                    total      = d(total),
                    base       = d(base),
                    fees       = listOf(Fee(amount = d(tax), type = "SUPPLIER")),
                    grandTotal = d(total)
                ),
                validatingAirlineCodes = listOf(t.airlineCode),
                travelerPricings = buildTravelerPricings(params, adultPrice, childPrice, infantPrice),
                numberOfBookableSeats = (10..50).random(),
                rawJson = ""
            )
        }
    }

    private fun buildTravelerPricings(
        params: FlightSearchParams,
        adultPrice: Double,
        childPrice: Double,
        infantPrice: Double
    ): List<TravelerPricing> = buildList {
        fun d(v: Double) = "%.2f".format(v)
        fun fareDetail(cabin: String, basis: String) = FareDetail(
            segmentId = "S1",
            cabin     = cabin,
            fareBasis = basis,
            includedCheckedBags = BaggageAllowance(weight = 20, weightUnit = "KG")
        )
        repeat(params.adults) { i ->
            add(TravelerPricing(
                "${i + 1}", "STANDARD", "ADULT",
                Price("PHP", d(adultPrice), d(adultPrice * 0.82), emptyList(), d(adultPrice)),
                listOf(fareDetail(params.travelClass, "YLOW"))
            ))
        }
        repeat(params.children) { i ->
            add(TravelerPricing(
                "${params.adults + i + 1}", "STANDARD", "CHILD",
                Price("PHP", d(childPrice), d(childPrice * 0.82), emptyList(), d(childPrice)),
                listOf(fareDetail(params.travelClass, "YCHILD"))
            ))
        }
        repeat(params.infants) { i ->
            add(TravelerPricing(
                "${params.adults + params.children + i + 1}", "STANDARD", "HELD_INFANT",
                Price("PHP", d(infantPrice), d(infantPrice * 0.82), emptyList(), d(infantPrice)),
                listOf(FareDetail("S1", params.travelClass, "YINF"))
            ))
        }
    }

    private fun isoDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "PT${h}H" else "PT${h}H${m}M"
    }
}
