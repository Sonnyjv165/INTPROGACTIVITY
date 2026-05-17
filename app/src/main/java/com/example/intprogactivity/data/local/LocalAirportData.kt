package com.example.intprogactivity.data.local

import com.example.intprogactivity.domain.model.Airport

object LocalAirportData {

    val airports = listOf(
        // Philippine domestic
        Airport("MNL", "Ninoy Aquino International Airport", "Manila", "Philippines", "PH"),
        Airport("CEB", "Mactan-Cebu International Airport", "Cebu", "Philippines", "PH"),
        Airport("DVO", "Francisco Bangoy International Airport", "Davao", "Philippines", "PH"),
        Airport("ILO", "Iloilo International Airport", "Iloilo", "Philippines", "PH"),
        Airport("KLO", "Kalibo International Airport", "Kalibo", "Philippines", "PH"),
        Airport("TAG", "Francisco B. Reyes Airport", "Coron", "Philippines", "PH"),
        Airport("CYP", "Calbayog Airport", "Calbayog", "Philippines", "PH"),
        Airport("MPH", "Caticlan Airport", "Boracay", "Philippines", "PH"),
        Airport("CRM", "National Airport", "Cagayan de Oro", "Philippines", "PH"),
        Airport("GES", "General Santos Airport", "General Santos", "Philippines", "PH"),
        Airport("BCD", "Bacolod-Silay Airport", "Bacolod", "Philippines", "PH"),
        Airport("ZAM", "Zamboanga International Airport", "Zamboanga", "Philippines", "PH"),
        Airport("LGP", "Legazpi Airport", "Legazpi", "Philippines", "PH"),
        Airport("PPS", "Puerto Princesa International Airport", "Puerto Princesa", "Philippines", "PH"),
        Airport("TAC", "Daniel Z. Romualdez Airport", "Tacloban", "Philippines", "PH"),
        // Asia
        Airport("SIN", "Singapore Changi Airport", "Singapore", "Singapore", "SG"),
        Airport("HKG", "Hong Kong International Airport", "Hong Kong", "Hong Kong", "HK"),
        Airport("NRT", "Narita International Airport", "Tokyo", "Japan", "JP"),
        Airport("KIX", "Kansai International Airport", "Osaka", "Japan", "JP"),
        Airport("ICN", "Incheon International Airport", "Seoul", "South Korea", "KR"),
        Airport("KUL", "Kuala Lumpur International Airport", "Kuala Lumpur", "Malaysia", "MY"),
        Airport("BKK", "Suvarnabhumi Airport", "Bangkok", "Thailand", "TH"),
        Airport("TPE", "Taoyuan International Airport", "Taipei", "Taiwan", "TW"),
        Airport("CGK", "Soekarno-Hatta International Airport", "Jakarta", "Indonesia", "ID"),
        Airport("SGN", "Tan Son Nhat International Airport", "Ho Chi Minh City", "Vietnam", "VN"),
        Airport("HAN", "Noi Bai International Airport", "Hanoi", "Vietnam", "VN"),
        Airport("PVG", "Pudong International Airport", "Shanghai", "China", "CN"),
        Airport("PEK", "Beijing Capital International Airport", "Beijing", "China", "CN"),
        Airport("DXB", "Dubai International Airport", "Dubai", "UAE", "AE"),
        // Rest of world
        Airport("SYD", "Sydney Kingsford Smith Airport", "Sydney", "Australia", "AU"),
        Airport("MEL", "Melbourne Airport", "Melbourne", "Australia", "AU"),
        Airport("LAX", "Los Angeles International Airport", "Los Angeles", "USA", "US"),
        Airport("SFO", "San Francisco International Airport", "San Francisco", "USA", "US"),
        Airport("LHR", "Heathrow Airport", "London", "United Kingdom", "GB"),
        Airport("CDG", "Charles de Gaulle Airport", "Paris", "France", "FR"),
        Airport("FRA", "Frankfurt Airport", "Frankfurt", "Germany", "DE"),
    )

    fun search(query: String): List<Airport> {
        if (query.length < 2) return emptyList()
        val q = query.lowercase().trim()
        return airports.filter {
            it.iataCode.lowercase().startsWith(q) ||
            it.cityName.lowercase().contains(q) ||
            it.name.lowercase().contains(q) ||
            it.countryName.lowercase().contains(q)
        }.sortedWith(compareByDescending {
            when {
                it.iataCode.lowercase().startsWith(q) -> 3
                it.cityName.lowercase().startsWith(q) -> 2
                else -> 1
            }
        }).take(10)
    }
}
