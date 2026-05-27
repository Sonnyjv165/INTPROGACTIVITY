package com.example.intprogactivity.presentation.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.domain.repository.BookingRepository
import com.example.intprogactivity.util.Constants
import com.example.intprogactivity.util.UiState
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class FindBookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _searchState = MutableStateFlow<UiState<Booking>>(UiState.Idle)
    val searchState: StateFlow<UiState<Booking>> = _searchState.asStateFlow()

    fun findBooking(pnr: String) {
        if (pnr.isBlank()) return
        viewModelScope.launch {
            _searchState.value = UiState.Loading
            try {
                // Search by PNR field across all bookings (cross-platform — same collection as web)
                val snapshot = firestore.collection(Constants.FIRESTORE_BOOKINGS)
                    .whereEqualTo("pnr", pnr.uppercase().trim())
                    .limit(1)
                    .get().await()

                if (snapshot.isEmpty) {
                    _searchState.value = UiState.Error("No booking found for reference \"$pnr\"")
                    return@launch
                }

                val doc = snapshot.documents.first()
                when (val result = bookingRepository.getBookingById(doc.id)) {
                    is com.example.intprogactivity.util.Result.Success ->
                        _searchState.value = UiState.Success(result.data)
                    is com.example.intprogactivity.util.Result.Error ->
                        _searchState.value = UiState.Error(result.message ?: "Failed to load booking")
                    else -> Unit
                }
            } catch (e: Exception) {
                _searchState.value = UiState.Error("Search failed: ${e.message}")
            }
        }
    }
}
