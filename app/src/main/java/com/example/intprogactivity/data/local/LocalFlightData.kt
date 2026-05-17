package com.example.intprogactivity.data.local

import com.example.intprogactivity.domain.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LocalFlightData {

    private data class RouteTemplate(
        val airlineCode: String,
        val flightNumber: String,
        val departureHour: Int,
        val departureMinute: Int,
        val durationMinutes: Int,
        val basePricePhp: Double,
        val aircraft: String = "320"
    )

    private val routes: Map<String, List<RouteTemplate>> = mapOf(
        "MNL-CEB" to listOf(
            RouteTemplate("5J", "561", 5, 50, 80, 1899.0),
            RouteTemplate("5J", "563", 8, 10, 80, 2299.0),
            RouteTemplate("PR", "481", 10, 30, 85, 3500.0, "321"),
            RouteTemplate("Z2", "281", 13, 45, 80, 1699.0),
            RouteTemplate("5J", "567", 16, 20, 80, 2499.0),
            RouteTemplate("PR", "485", 19, 0, 85, 3200.0, "321"),
        ),
        "MNL-DVO" to listOf(
            RouteTemplate("5J", "571", 6, 0, 105, 2199.0),
            RouteTemplate("PR", "491", 9, 30, 110, 4200.0, "321"),
            RouteTemplate("Z2", "291", 12, 15, 105, 1999.0),
            RouteTemplate("5J", "575", 15, 30, 105, 2499.0),
            RouteTemplate("PR", "495", 18, 45, 110, 3800.0, "321"),
        ),
        "MNL-ILO" to listOf(
            RouteTemplate("5J", "621", 6, 30, 70, 1799.0),
            RouteTemplate("PR", "461", 9, 0, 70, 3200.0, "321"),
            RouteTemplate("5J", "623", 13, 0, 70, 2099.0),
            RouteTemplate("Z2", "311", 16, 45, 70, 1599.0),
        ),
        "MNL-KLO" to listOf(
            RouteTemplate("5J", "681", 7, 0, 65, 1599.0),
            RouteTemplate("5J", "683", 11, 30, 65, 1899.0),
            RouteTemplate("PR", "451", 15, 0, 65, 2800.0, "321"),
        ),
        "MNL-MPH" to listOf(
            RouteTemplate("5J", "691", 6, 45, 60, 1799.0),
            RouteTemplate("PR", "445", 10, 0, 60, 3000.0, "321"),
            RouteTemplate("5J", "693", 14, 30, 60, 2099.0),
        ),
        "MNL-PPS" to listOf(
            RouteTemplate("5J", "651", 7, 30, 80, 2099.0),
            RouteTemplate("PR", "471", 11, 0, 80, 3500.0, "321"),
            RouteTemplate("Z2", "321", 15, 0, 80, 1899.0),
        ),
        "MNL-BCD" to listOf(
            RouteTemplate("5J", "641", 8, 0, 70, 1899.0),
            RouteTemplate("PR", "455", 13, 30, 70, 3200.0, "321"),
        ),
        "CEB-DVO" to listOf(
            RouteTemplate("5J", "861", 7, 0, 55, 1499.0),
            RouteTemplate("PR", "821", 11, 30, 55, 2800.0, "320"),
            RouteTemplate("Z2", "611", 15, 45, 55, 1299.0),
        ),
        "MNL-SIN" to listOf(
            RouteTemplate("5J", "71", 7, 30, 210, 4999.0, "321"),
            RouteTemplate("SQ", "911", 9, 0, 210, 12500.0, "787"),
            RouteTemplate("PR", "521", 12, 0, 215, 8500.0, "321"),
            RouteTemplate("Z2", "411", 15, 30, 210, 3999.0),
            RouteTemplate("SQ", "919", 19, 30, 210, 11000.0, "787"),
        ),
        "MNL-HKG" to listOf(
            RouteTemplate("CX", "911", 8, 0, 145, 9800.0, "350"),
            RouteTemplate("PR", "501", 10, 30, 150, 7500.0, "321"),
            RouteTemplate("CX", "915", 14, 15, 145, 10500.0, "350"),
            RouteTemplate("5J", "81", 17, 0, 150, 4500.0, "321"),
        ),
        "MNL-NRT" to listOf(
            RouteTemplate("PR", "551", 9, 0, 260, 12000.0, "77W"),
            RouteTemplate("NH", "871", 11, 30, 255, 15500.0, "787"),
            RouteTemplate("5J", "91", 14, 0, 260, 7500.0, "321"),
            RouteTemplate("PR", "553", 21, 0, 260, 11500.0, "77W"),
        ),
        "MNL-KIX" to listOf(
            RouteTemplate("PR", "555", 10, 0, 240, 11500.0, "77W"),
            RouteTemplate("5J", "93", 14, 30, 240, 7000.0, "321"),
        ),
        "MNL-ICN" to listOf(
            RouteTemplate("PR", "541", 8, 30, 235, 11000.0, "77W"),
            RouteTemplate("5J", "101", 11, 0, 235, 6500.0, "321"),
            RouteTemplate("KE", "621", 14, 30, 230, 14000.0, "73H"),
            RouteTemplate("PR", "543", 20, 0, 235, 10500.0, "77W"),
        ),
        "MNL-KUL" to listOf(
            RouteTemplate("AK", "161", 7, 0, 240, 3999.0),
            RouteTemplate("PR", "531", 10, 0, 240, 8000.0, "321"),
            RouteTemplate("AK", "165", 14, 30, 240, 4299.0),
            RouteTemplate("MH", "701", 18, 0, 245, 9500.0, "738"),
        ),
        "MNL-BKK" to listOf(
            RouteTemplate("5J", "111", 8, 0, 210, 4500.0, "321"),
            RouteTemplate("TG", "621", 10, 30, 205, 11000.0, "788"),
            RouteTemplate("PR", "511", 14, 0, 210, 7500.0, "321"),
            RouteTemplate("TG", "625", 19, 0, 205, 10500.0, "788"),
        ),
        "MNL-DXB" to listOf(
            RouteTemplate("EK", "335", 21, 30, 540, 22000.0, "77W"),
            RouteTemplate("PR", "661", 23, 0, 540, 18000.0, "77W"),
        ),
        "MNL-SYD" to listOf(
            RouteTemplate("PR", "211", 22, 30, 510, 28000.0, "77W"),
            RouteTemplate("QF", "121", 20, 0, 510, 32000.0, "789"),
        ),
        "MNL-LAX" to listOf(
            RouteTemplate("PR", "101", 0, 30, 840, 55000.0, "77W"),
            RouteTemplate("UA", "891", 23, 30, 840, 62000.0, "789"),
        ),
        // Philippine domestic — additional cities
        "MNL-CRM" to listOf(
            RouteTemplate("5J", "631", 6, 0, 95, 1999.0),
            RouteTemplate("PR", "461", 10, 30, 95, 3500.0, "321"),
            RouteTemplate("5J", "633", 15, 0, 95, 2299.0),
        ),
        "MNL-GES" to listOf(
            RouteTemplate("5J", "701", 5, 50, 120, 2299.0),
            RouteTemplate("PR", "471", 9, 0, 120, 4000.0, "321"),
            RouteTemplate("5J", "703", 14, 0, 120, 2599.0),
        ),
        "MNL-ZAM" to listOf(
            RouteTemplate("5J", "711", 6, 30, 110, 2099.0),
            RouteTemplate("PR", "481", 11, 0, 110, 3800.0, "321"),
        ),
        "MNL-LGP" to listOf(
            RouteTemplate("5J", "721", 7, 0, 60, 1699.0),
            RouteTemplate("Z2", "351", 12, 30, 60, 1499.0),
            RouteTemplate("5J", "723", 17, 0, 60, 1899.0),
        ),
        "MNL-TAC" to listOf(
            RouteTemplate("5J", "731", 6, 45, 90, 1999.0),
            RouteTemplate("PR", "491", 10, 0, 90, 3500.0, "321"),
            RouteTemplate("5J", "733", 15, 30, 90, 2299.0),
        ),
        "MNL-TAG" to listOf(
            RouteTemplate("5J", "741", 7, 30, 75, 2299.0),
            RouteTemplate("PR", "501", 12, 0, 75, 3800.0, "321"),
        ),
        "MNL-CYP" to listOf(
            RouteTemplate("5J", "751", 8, 0, 80, 1999.0),
            RouteTemplate("Z2", "361", 14, 0, 80, 1799.0),
        ),
        // More Philippine domestic cross-routes
        "CEB-ILO" to listOf(
            RouteTemplate("5J", "871", 8, 30, 45, 1299.0),
            RouteTemplate("Z2", "621", 14, 0, 45, 1099.0),
        ),
        "CEB-CRM" to listOf(
            RouteTemplate("5J", "881", 7, 0, 50, 1399.0),
            RouteTemplate("PR", "831", 13, 30, 50, 2800.0, "320"),
        ),
        // Southeast Asia
        "MNL-TPE" to listOf(
            RouteTemplate("CI", "711", 8, 30, 165, 8500.0, "333"),
            RouteTemplate("PR", "561", 11, 0, 170, 7000.0, "321"),
            RouteTemplate("CI", "715", 15, 30, 165, 9000.0, "333"),
            RouteTemplate("5J", "121", 19, 0, 170, 5500.0, "321"),
        ),
        "MNL-CGK" to listOf(
            RouteTemplate("GA", "871", 9, 0, 270, 9500.0, "738"),
            RouteTemplate("PR", "571", 12, 30, 275, 8000.0, "321"),
            RouteTemplate("GA", "875", 17, 0, 270, 10000.0, "738"),
        ),
        "MNL-SGN" to listOf(
            RouteTemplate("VN", "641", 7, 30, 180, 6500.0, "321"),
            RouteTemplate("5J", "131", 10, 0, 180, 4500.0, "321"),
            RouteTemplate("PR", "581", 14, 30, 185, 7500.0, "321"),
            RouteTemplate("VN", "645", 19, 0, 180, 7000.0, "321"),
        ),
        "MNL-HAN" to listOf(
            RouteTemplate("VN", "661", 8, 0, 195, 7000.0, "321"),
            RouteTemplate("5J", "141", 12, 30, 195, 4999.0, "321"),
            RouteTemplate("PR", "591", 16, 0, 200, 8000.0, "321"),
        ),
        "MNL-PVG" to listOf(
            RouteTemplate("MU", "551", 9, 30, 195, 11000.0, "321"),
            RouteTemplate("PR", "601", 13, 0, 195, 9000.0, "321"),
            RouteTemplate("MU", "555", 18, 0, 195, 11500.0, "321"),
        ),
        "MNL-PEK" to listOf(
            RouteTemplate("CA", "841", 10, 0, 210, 13000.0, "738"),
            RouteTemplate("PR", "611", 14, 30, 215, 10500.0, "321"),
        ),
        // Cross-Asia routes
        "SIN-BKK" to listOf(
            RouteTemplate("SQ", "711", 7, 30, 150, 8000.0, "320"),
            RouteTemplate("TG", "411", 12, 0, 150, 9500.0, "788"),
            RouteTemplate("SQ", "715", 18, 0, 150, 8500.0, "320"),
        ),
        "SIN-KUL" to listOf(
            RouteTemplate("SQ", "105", 6, 0, 55, 3500.0, "320"),
            RouteTemplate("AK", "721", 9, 30, 55, 1800.0),
            RouteTemplate("SQ", "109", 14, 0, 55, 3800.0, "320"),
            RouteTemplate("AK", "725", 18, 30, 55, 2000.0),
        ),
        "HKG-ICN" to listOf(
            RouteTemplate("CX", "411", 9, 0, 210, 12000.0, "350"),
            RouteTemplate("KE", "601", 14, 30, 210, 14500.0, "73H"),
        ),
        "HKG-NRT" to listOf(
            RouteTemplate("CX", "511", 10, 0, 230, 13000.0, "350"),
            RouteTemplate("NH", "811", 15, 30, 230, 16000.0, "787"),
        ),
        "ICN-NRT" to listOf(
            RouteTemplate("KE", "701", 8, 30, 130, 11000.0, "73H"),
            RouteTemplate("NH", "901", 11, 0, 130, 13000.0, "787"),
            RouteTemplate("KE", "705", 16, 0, 130, 11500.0, "73H"),
        ),
        // Europe
        "MNL-LHR" to listOf(
            RouteTemplate("BA", "11", 21, 0, 795, 65000.0, "789"),
            RouteTemplate("EK", "351", 23, 30, 840, 58000.0, "77W"),
        ),
        "MNL-CDG" to listOf(
            RouteTemplate("AF", "131", 22, 0, 825, 62000.0, "788"),
        ),
        "MNL-FRA" to listOf(
            RouteTemplate("LH", "741", 21, 30, 810, 60000.0, "744"),
        ),
        // More Australia
        "MNL-MEL" to listOf(
            RouteTemplate("QF", "141", 21, 0, 540, 30000.0, "789"),
            RouteTemplate("PR", "221", 23, 0, 540, 26000.0, "77W"),
        ),
        // More North America
        "MNL-SFO" to listOf(
            RouteTemplate("UA", "871", 22, 0, 870, 65000.0, "789"),
            RouteTemplate("PR", "111", 0, 0, 870, 58000.0, "77W"),
        ),
        // Europe cross-routes
        "DXB-LHR" to listOf(
            RouteTemplate("EK", "3", 8, 0, 420, 28000.0, "388"),
            RouteTemplate("BA", "107", 14, 30, 420, 32000.0, "789"),
        ),
        "DXB-CDG" to listOf(
            RouteTemplate("EK", "71", 9, 30, 405, 26000.0, "388"),
            RouteTemplate("AF", "611", 15, 0, 405, 28000.0, "788"),
        ),
        "DXB-FRA" to listOf(
            RouteTemplate("EK", "47", 10, 0, 390, 25000.0, "388"),
            RouteTemplate("LH", "601", 16, 0, 390, 27000.0, "744"),
        ),
        // Australia cross-routes
        "SYD-MEL" to listOf(
            RouteTemplate("QF", "401", 6, 30, 85, 8000.0, "738"),
            RouteTemplate("VA", "301", 9, 0, 85, 7000.0, "738"),
            RouteTemplate("QF", "407", 13, 30, 85, 8500.0, "738"),
            RouteTemplate("VA", "305", 18, 0, 85, 7500.0, "738"),
        ),
        // North America cross-routes
        "LAX-SFO" to listOf(
            RouteTemplate("UA", "101", 7, 0, 80, 5000.0, "738"),
            RouteTemplate("AA", "201", 11, 30, 80, 5500.0, "738"),
            RouteTemplate("UA", "105", 16, 0, 80, 4800.0, "738"),
        ),
    )

    fun searchFlights(params: FlightSearchParams): List<FlightOffer> {
        val key = "${params.origin.uppercase()}-${params.destination.uppercase()}"
        val reverseKey = "${params.destination.uppercase()}-${params.origin.uppercase()}"
        val isReverse = !routes.containsKey(key) && routes.containsKey(reverseKey)
        val templates = routes[key] ?: routes[reverseKey] ?: return emptyList()

        val origin = if (isReverse) params.destination.uppercase() else params.origin.uppercase()
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

            val adultPrice = t.basePricePhp
            val childPrice  = t.basePricePhp * 0.75
            val infantPrice = t.basePricePhp * 0.10
            val total = adultPrice * params.adults +
                childPrice * params.children +
                infantPrice * params.infants
            val base = total * 0.82
            val tax  = total - base

            fun d(v: Double) = "%.2f".format(v)

            val segment = Segment(
                id = "S1",
                departure = FlightEndpoint(origin, null, departure.format(dtFmt)),
                arrival   = FlightEndpoint(destination, null, arrival.format(dtFmt)),
                carrierCode    = t.airlineCode,
                number         = t.flightNumber,
                aircraft       = t.aircraft,
                duration       = isoDuration(t.durationMinutes),
                numberOfStops  = 0
            )

            FlightOffer(
                id = "${index + 1}",
                source = "LOCAL",
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
            cabin = cabin,
            fareBasis = basis,
            includedCheckedBags = BaggageAllowance(weight = 20, weightUnit = "KG")
        )
        repeat(params.adults) { i ->
            add(TravelerPricing("${i + 1}", "STANDARD", "ADULT",
                Price("PHP", d(adultPrice), d(adultPrice * 0.82), emptyList(), d(adultPrice)),
                listOf(fareDetail(params.travelClass, "YLOW"))))
        }
        repeat(params.children) { i ->
            add(TravelerPricing("${params.adults + i + 1}", "STANDARD", "CHILD",
                Price("PHP", d(childPrice), d(childPrice * 0.82), emptyList(), d(childPrice)),
                listOf(fareDetail(params.travelClass, "YCHILD"))))
        }
        repeat(params.infants) { i ->
            add(TravelerPricing("${params.adults + params.children + i + 1}", "STANDARD", "HELD_INFANT",
                Price("PHP", d(infantPrice), d(infantPrice * 0.82), emptyList(), d(infantPrice)),
                listOf(FareDetail("S1", params.travelClass, "YINF"))))
        }
    }

    private fun isoDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "PT${h}H" else "PT${h}H${m}M"
    }
}
