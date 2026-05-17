package com.example.intprogactivity.presentation.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.domain.model.BookingStatus
import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.domain.usecase.booking.CancelBookingUseCase
import com.example.intprogactivity.domain.usecase.booking.GetUserBookingsUseCase
import com.example.intprogactivity.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getUserBookingsUseCase: GetUserBookingsUseCase,
    private val cancelBookingUseCase: CancelBookingUseCase
) : ViewModel() {

    val bookingsState: StateFlow<UiState<List<Booking>>> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(UiState.Loading)
            else getUserBookingsUseCase(user.uid).map { UiState.Success(it) as UiState<List<Booking>> }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val upcomingBookings: StateFlow<List<Booking>> = bookingsState
        .map { state ->
            (state as? UiState.Success)?.data?.filter { it.status == BookingStatus.CONFIRMED }
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pastBookings: StateFlow<List<Booking>> = bookingsState
        .map { state ->
            (state as? UiState.Success)?.data?.filter {
                it.status == BookingStatus.COMPLETED || it.status == BookingStatus.CANCELLED
            } ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cancelState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val cancelState: StateFlow<UiState<Unit>> = _cancelState.asStateFlow()

    fun cancelBooking(bookingId: String, travelDateMs: Long) {
        _cancelState.value = UiState.Loading
        viewModelScope.launch {
            cancelBookingUseCase(bookingId, travelDateMs).fold(
                onSuccess = { _cancelState.value = UiState.Success(Unit) },
                onFailure = { _cancelState.value = UiState.Error(it.message ?: "Cancellation failed") }
            )
        }
    }

    fun resetCancelState() { _cancelState.value = UiState.Idle }
}
