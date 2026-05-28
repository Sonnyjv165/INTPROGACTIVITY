package com.example.intprogactivity.domain.usecase.booking

import com.example.intprogactivity.domain.model.*
import com.example.intprogactivity.domain.repository.BookingRepository
import com.example.intprogactivity.domain.repository.UserRepository
import com.example.intprogactivity.util.Constants
import com.example.intprogactivity.util.Result
import com.example.intprogactivity.util.containsRyanair
import com.example.intprogactivity.util.isDepartingWithin2Hours
import com.google.gson.Gson
import java.util.UUID
import javax.inject.Inject

class CreateBookingUseCase @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val userRepository: UserRepository,
    private val gson: Gson
) {
    suspend operator fun invoke(
        flightOffer: FlightOffer,
        returnFlight: FlightOffer? = null,
        passengers: List<Passenger>,
        addOns: AddOns,
        user: User,
        overrideTotalPrice: Double? = null,
        paymentMethod: String = "GCash",
        promoCode: String? = null,
        cabinClass: String = "ECONOMY"
    ): Result<Booking> {
        // Business rule: Guests cannot book
        if (user.membershipTier == MembershipTier.GUEST)
            return Result.Error(Exception("Guests cannot book flights. Please create a free account first."))

        // Business rule: no booking within 2 hours of departure
        if (flightOffer.isDepartingWithin2Hours())
            return Result.Error(Exception("Flights departing within 2 hours cannot be booked."))

        // Business rule: no Ryanair
        if (flightOffer.containsRyanair())
            return Result.Error(Exception("Ryanair bookings are not supported under our service guarantee."))

        val basePrice = flightOffer.totalPriceDouble()
        val totalPrice = overrideTotalPrice ?: (basePrice + addOns.totalCost())
        val coinsEarned = (totalPrice * Constants.BASE_COIN_EARN_RATE * user.membershipTier.coinMultiplier()).toInt()

        val firstSeg = flightOffer.itineraries.firstOrNull()?.segments?.firstOrNull()

        val booking = Booking(
            bookingId = UUID.randomUUID().toString(),
            userId = user.uid,
            userEmail = user.email,
            userName = user.fullName().ifBlank { user.email },
            pnr = generateConfirmCode(),
            status = BookingStatus.PENDING,
            outboundFlightJson = gson.toJson(flightOffer),
            returnFlightJson = returnFlight?.let { gson.toJson(it) },
            passengers = passengers,
            totalPrice = totalPrice,
            currency = flightOffer.price.currency,
            addOns = addOns,
            tripCoinsEarned = coinsEarned,
            createdAt = System.currentTimeMillis(),
            travelDate = firstSeg?.departure?.at?.let { parseTimestamp(it) } ?: 0L,
            originIata = firstSeg?.departure?.iataCode ?: "",
            destinationIata = flightOffer.itineraries.firstOrNull()?.segments?.lastOrNull()?.arrival?.iataCode ?: "",
            airlineName = flightOffer.primaryAirlineCode(),
            cabinClass = cabinClass,
            paymentMethod = paymentMethod,
            paymentStatus = "PENDING",
            promoCode = promoCode
        )

        val result = bookingRepository.createBooking(booking)
        if (result is Result.Success) {
            userRepository.incrementBookingStats(user.uid, totalPrice)
            if (coinsEarned > 0) {
                userRepository.addTripCoins(user.uid, coinsEarned, "Flight booking ${booking.pnr}", booking.bookingId)
            }
            evaluateTierUpgrade(user)
        }
        return result
    }

    private suspend fun evaluateTierUpgrade(user: User) {
        val updatedUser = when (val r = userRepository.getUserProfile(user.uid)) {
            is Result.Success -> r.data
            else -> return
        }
        val newTier = MembershipTier.evaluate(
            currentTier       = updatedUser.membershipTier,
            totalBookings     = updatedUser.totalBookings,
            confirmedBookings = updatedUser.confirmedBookings,
            bookingsInLastYear = updatedUser.confirmedBookings,
            totalSpend        = updatedUser.totalSpend,
            spendInLastYear   = updatedUser.totalSpend
        )
        if (newTier != updatedUser.membershipTier) {
            userRepository.updateMembershipTier(user.uid, newTier)
        }
    }

    /** Generates a 10-character alphanumeric confirm code matching web format, e.g. "TR7C553FD7" */
    private fun generateConfirmCode(): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..10).map { chars.random() }.joinToString("")
    }

    private fun parseTimestamp(isoDatetime: String): Long = try {
        java.time.ZonedDateTime.parse(isoDatetime).toInstant().toEpochMilli()
    } catch (_: Exception) {
        try {
            java.time.LocalDateTime.parse(isoDatetime).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }
}
