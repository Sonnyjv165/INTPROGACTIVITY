package com.example.intprogactivity.domain.model

import java.util.UUID

data class Booking(
    val bookingId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val pnr: String = "",           // maps to confirmCode in Firestore
    val status: BookingStatus = BookingStatus.PENDING,
    val outboundFlightJson: String = "",
    val returnFlightJson: String? = null,
    val passengers: List<Passenger> = emptyList(),
    val totalPrice: Double = 0.0,
    val currency: String = "PHP",
    val addOns: AddOns = AddOns(),
    val tripCoinsEarned: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val travelDate: Long = 0L,
    val originIata: String = "",
    val destinationIata: String = "",
    val airlineName: String = "",
    val cabinClass: String = "ECONOMY",
    val paymentMethod: String = "GCash",
    val paymentStatus: String = "PENDING",
    val promoCode: String? = null
)

enum class BookingStatus { CONFIRMED, PENDING, CANCELLED, COMPLETED }

data class Passenger(
    val id: String = UUID.randomUUID().toString(),
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val email: String = "",
    val phone: String = "",
    val nationality: String = "",
    val passportNumber: String = "",
    val passportExpiry: String = "",
    val type: PassengerType = PassengerType.ADULT
) {
    fun fullName(): String = "$firstName $lastName"
}

enum class PassengerType { ADULT, CHILD, INFANT }

data class AddOns(
    val checkedBaggage: List<BaggageOption> = emptyList(),
    val meals: List<MealOption> = emptyList(),
    val travelInsurance: Boolean = false,
    val seatSelections: List<SeatSelection> = emptyList()
) {
    fun totalCost(): Double =
        checkedBaggage.sumOf { it.price } +
        meals.sumOf { it.price } +
        (if (travelInsurance) 1200.0 else 0.0) +
        seatSelections.sumOf { it.price }
}

data class BaggageOption(
    val passengerId: String,
    val weightKg: Int,
    val price: Double
)

data class MealOption(
    val passengerId: String,
    val mealType: String,
    val price: Double
)

data class SeatSelection(
    val passengerId: String,
    val segmentId: String,
    val seatNumber: String,
    val price: Double
)

data class SeatInfo(
    val seatNumber: String,
    val row: Int,
    val column: String,
    val isAvailable: Boolean,
    val isSelected: Boolean = false,
    val isExitRow: Boolean = false,
    val isExtraLegroom: Boolean = false,
    val price: Double = 0.0
)
