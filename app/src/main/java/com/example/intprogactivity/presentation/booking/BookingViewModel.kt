package com.example.intprogactivity.presentation.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intprogactivity.domain.model.AddOns
import com.example.intprogactivity.domain.model.BaggageOption
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.model.MealOption
import com.example.intprogactivity.domain.model.MembershipTier
import com.example.intprogactivity.domain.model.Passenger
import com.example.intprogactivity.domain.model.PassengerType
import com.example.intprogactivity.domain.model.SeatSelection
import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.domain.usecase.booking.CreateBookingUseCase
import com.example.intprogactivity.util.Result
import com.example.intprogactivity.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val createBookingUseCase: CreateBookingUseCase
) : ViewModel() {

    private val _flightOffer = MutableStateFlow<FlightOffer?>(null)
    val flightOffer: StateFlow<FlightOffer?> = _flightOffer.asStateFlow()

    private val _passengers = MutableStateFlow<List<Passenger>>(emptyList())
    val passengers: StateFlow<List<Passenger>> = _passengers.asStateFlow()

    private val _addOns = MutableStateFlow(AddOns())
    val addOns: StateFlow<AddOns> = _addOns.asStateFlow()

    private val _bookingState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val bookingState: StateFlow<UiState<String>> = _bookingState.asStateFlow()

    fun setFlightOffer(offer: FlightOffer) { _flightOffer.value = offer }

    fun setContactInfo(email: String, phone: String) {
        val list = _passengers.value.toMutableList()
        if (list.isNotEmpty()) {
            list[0] = list[0].copy(email = email, phone = phone)
            _passengers.value = list
        }
    }

    fun initPassengers(adults: Int, children: Int, infants: Int) {
        val list = mutableListOf<Passenger>()
        repeat(adults) { list.add(Passenger(type = PassengerType.ADULT)) }
        repeat(children) { list.add(Passenger(type = PassengerType.CHILD)) }
        repeat(infants) { list.add(Passenger(type = PassengerType.INFANT)) }
        _passengers.value = list
    }

    fun setPassenger(
        index: Int,
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        gender: String,
        passportNumber: String,
        nationality: String
    ) {
        val list = _passengers.value.toMutableList()
        val existingType = list.getOrNull(index)?.type ?: PassengerType.ADULT
        val passenger = Passenger(
            type = existingType,
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
            gender = gender,
            passportNumber = passportNumber,
            nationality = nationality
        )
        if (index < list.size) list[index] = passenger else list.add(passenger)
        _passengers.value = list
    }

    fun setExtraBaggage(passengerId: String, weightKg: Int, price: Double) {
        val current = _addOns.value
        val updated = current.checkedBaggage.toMutableList()
        updated.removeAll { it.passengerId == passengerId }
        if (weightKg > 0) updated.add(BaggageOption(passengerId, weightKg, price))
        _addOns.value = current.copy(checkedBaggage = updated)
    }

    fun setMeal(passengerId: String, mealType: String, price: Double) {
        val current = _addOns.value
        val updated = current.meals.toMutableList()
        updated.removeAll { it.passengerId == passengerId }
        if (mealType.isNotEmpty()) updated.add(MealOption(passengerId, mealType, price))
        _addOns.value = current.copy(meals = updated)
    }

    fun setInsurance(enabled: Boolean) {
        _addOns.value = _addOns.value.copy(travelInsurance = enabled)
    }

    fun setSeatSelection(passengerIndex: Int, seatNumber: String) {
        val current = _addOns.value
        val updated = current.seatSelections.toMutableList()
        updated.removeAll { it.passengerId == passengerIndex.toString() }
        if (seatNumber.isNotEmpty()) {
            updated.add(SeatSelection(
                passengerId = passengerIndex.toString(),
                segmentId = "SEG1",
                seatNumber = seatNumber,
                price = 0.0
            ))
        }
        _addOns.value = current.copy(seatSelections = updated)
    }

    fun clearSeatSelection() {
        _addOns.value = _addOns.value.copy(seatSelections = emptyList())
    }

    fun getSeatForPassenger(index: Int): String? =
        _addOns.value.seatSelections.find { it.passengerId == index.toString() }?.seatNumber

    fun selectedSeat(): String? = _addOns.value.seatSelections.firstOrNull()?.seatNumber

    fun confirmBooking() {
        val offer = _flightOffer.value ?: return
        val passengers = _passengers.value
        if (passengers.isEmpty()) return
        _bookingState.value = UiState.Loading
        viewModelScope.launch {
            val user = authRepository.currentUser.first()
            if (user == null) {
                _bookingState.value = UiState.Error("Please sign in to book flights")
                return@launch
            }
            when (val result = createBookingUseCase(
                flightOffer = offer,
                returnFlight = _returnFlightOffer.value,
                passengers = passengers,
                addOns = _addOns.value,
                user = user,
                overrideTotalPrice = totalPrice()
            )) {
                is Result.Success -> _bookingState.value = UiState.Success(result.data.bookingId)
                is Result.Error -> _bookingState.value = UiState.Error(result.exception.message ?: "Booking failed")
                is Result.Loading -> Unit
            }
        }
    }

    private val _returnFlightOffer = MutableStateFlow<FlightOffer?>(null)
    val returnFlightOffer: StateFlow<FlightOffer?> = _returnFlightOffer.asStateFlow()

    val currentUserTier: StateFlow<MembershipTier> = authRepository.currentUser
        .map { it?.membershipTier ?: MembershipTier.SILVER }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MembershipTier.SILVER)

    private val _selectedCabinClass = MutableStateFlow("ECONOMY")
    val selectedCabinClass: StateFlow<String> = _selectedCabinClass.asStateFlow()

    fun setReturnFlightOffer(offer: FlightOffer?) { _returnFlightOffer.value = offer }
    fun setCabinClass(cabin: String) { _selectedCabinClass.value = cabin }

    fun resetBookingState() { _bookingState.value = UiState.Idle }

    fun resetForNewBooking() {
        _flightOffer.value = null
        _returnFlightOffer.value = null
        _passengers.value = emptyList()
        _addOns.value = AddOns()
        _bookingState.value = UiState.Idle
        _selectedCabinClass.value = "ECONOMY"
    }

    fun totalPrice(): Double {
        val cabinMultiplier = when (_selectedCabinClass.value) {
            "PREMIUM_ECONOMY" -> 1.4
            "BUSINESS" -> 2.5
            "FIRST" -> 4.0
            else -> 1.0
        }
        val discountFactor = 1.0 - currentUserTier.value.discountPercent()
        val flightPrice = (_flightOffer.value?.totalPriceDouble() ?: 0.0) * cabinMultiplier * discountFactor
        val returnPrice = (_returnFlightOffer.value?.totalPriceDouble() ?: 0.0) * cabinMultiplier * discountFactor
        return flightPrice + returnPrice + _addOns.value.totalCost()
    }

    fun flightDiscount(): Double {
        val cabinMultiplier = when (_selectedCabinClass.value) {
            "PREMIUM_ECONOMY" -> 1.4
            "BUSINESS" -> 2.5
            "FIRST" -> 4.0
            else -> 1.0
        }
        val discount = currentUserTier.value.discountPercent()
        val flightBase = (_flightOffer.value?.totalPriceDouble() ?: 0.0) * cabinMultiplier
        val returnBase = (_returnFlightOffer.value?.totalPriceDouble() ?: 0.0) * cabinMultiplier
        return (flightBase + returnBase) * discount
    }
}
