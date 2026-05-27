package com.example.intprogactivity.data.remote.firebase

import android.util.Log
import com.example.intprogactivity.domain.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreFlightSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val TAG = "FirestoreFlightSource"

    /**
     * Queries the `flights` collection for flights matching the given origin,
     * destination and departure date.
     *
     * Schema per document:
     *   airlineCode, airlineName, arrival (IATA), depart (IATA),
     *   departDate (Timestamp), arriveDate (Timestamp),
     *   fare (Number), flightNumber (String), seatsAvailable (Number),
     *   totalSeats (Number), status ("SCHEDULED" | …)
     */
    internal suspend fun searchFlights(params: FlightSearchParams): List<FlightOffer> {
        val origin      = params.origin.uppercase().trim()
        val destination = params.destination.uppercase().trim()

        val targetDate = runCatching {
            LocalDate.parse(params.departureDate, DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrElse { LocalDate.now().plusDays(1) }

        Log.d(TAG, "Querying flights: $origin → $destination on $targetDate")

        // Query by depart only (avoids needing a composite index).
        // Filter arrival + date client-side.
        val snapshot = firestore.collection("flights")
            .whereEqualTo("depart", origin)
            .get().await()

        Log.d(TAG, "Raw docs from Firestore: ${snapshot.size()}")

        val dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val zoneId = ZoneId.systemDefault()

        return snapshot.documents.mapIndexedNotNull { index, doc ->
            try {
                // Client-side filters
                val docArrival = doc.getString("arrival")?.uppercase()?.trim()
                if (docArrival != destination) return@mapIndexedNotNull null

                val status = doc.getString("status") ?: "SCHEDULED"
                if (status != "SCHEDULED") return@mapIndexedNotNull null

                val departTs: Timestamp = doc.getTimestamp("departDate")
                    ?: return@mapIndexedNotNull null
                val arriveTs: Timestamp = doc.getTimestamp("arriveDate")
                    ?: return@mapIndexedNotNull null

                val departDT: LocalDateTime = departTs.toDate().toInstant()
                    .atZone(zoneId).toLocalDateTime()
                val arriveDT: LocalDateTime = arriveTs.toDate().toInstant()
                    .atZone(zoneId).toLocalDateTime()

                // Only include flights on the requested date
                if (departDT.toLocalDate() != targetDate) return@mapIndexedNotNull null

                val airlineCode      = doc.getString("airlineCode") ?: return@mapIndexedNotNull null
                val fullFlightNumber = doc.getString("flightNumber") ?: return@mapIndexedNotNull null
                // Strip airline prefix so "5J102" → "102"; keeps display as "5J 102"
                val flightNum = fullFlightNumber.removePrefix(airlineCode).trim()
                    .ifEmpty { fullFlightNumber }

                val fare = doc.getDouble("fare")
                    ?: doc.getLong("fare")?.toDouble()
                    ?: return@mapIndexedNotNull null

                val seatsAvailable = doc.getLong("seatsAvailable")?.toInt() ?: 10

                val durationMinutes = java.time.Duration.between(departDT, arriveDT)
                    .toMinutes().toInt().coerceAtLeast(1)

                // Passenger pricing
                val adultPrice  = fare
                val childPrice  = fare * 0.75
                val infantPrice = fare * 0.10
                val total = adultPrice  * params.adults   +
                            childPrice  * params.children  +
                            infantPrice * params.infants
                val base  = total * 0.82
                val tax   = total - base
                fun d(v: Double) = "%.2f".format(v)

                val segment = Segment(
                    id            = "S1",
                    departure     = FlightEndpoint(origin, null, departDT.format(dtFmt)),
                    arrival       = FlightEndpoint(destination, null, arriveDT.format(dtFmt)),
                    carrierCode   = airlineCode,
                    number        = flightNum,
                    aircraft      = "320",
                    duration      = isoDuration(durationMinutes),
                    numberOfStops = 0
                )

                FlightOffer(
                    id     = doc.id,
                    source = "FIRESTORE",
                    itineraries = listOf(Itinerary(
                        duration = isoDuration(durationMinutes),
                        segments = listOf(segment)
                    )),
                    price = Price(
                        currency   = "PHP",
                        total      = d(total),
                        base       = d(base),
                        fees       = listOf(Fee(amount = d(tax), type = "SUPPLIER")),
                        grandTotal = d(total)
                    ),
                    validatingAirlineCodes = listOf(airlineCode),
                    travelerPricings = buildTravelerPricings(
                        params, adultPrice, childPrice, infantPrice
                    ),
                    numberOfBookableSeats = seatsAvailable,
                    rawJson = ""
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse flight doc ${doc.id}: ${e.message}")
                null
            }
        }.also {
            Log.d(TAG, "Matched ${it.size} flight(s) for $origin→$destination on $targetDate")
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

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
