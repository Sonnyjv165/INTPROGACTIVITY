package com.example.intprogactivity.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.model.FlightSearchParams
import com.example.intprogactivity.domain.usecase.flight.SearchFlightsUseCase
import com.example.intprogactivity.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortMode { PRICE, DURATION, DEPARTURE }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchFlightsUseCase: SearchFlightsUseCase
) : ViewModel() {

    private val _searchState = MutableStateFlow<UiState<List<FlightOffer>>>(UiState.Idle)
    val searchState: StateFlow<UiState<List<FlightOffer>>> = _searchState.asStateFlow()

    private var allResults: List<FlightOffer> = emptyList()

    private val _sortMode = MutableStateFlow(SortMode.PRICE)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _maxStops = MutableStateFlow<Int?>(null)
    val maxStops: StateFlow<Int?> = _maxStops.asStateFlow()

    fun search(params: FlightSearchParams) {
        if (_searchState.value is UiState.Loading) return
        viewModelScope.launch {
            _searchState.value = UiState.Loading
            searchFlightsUseCase(params).fold(
                onSuccess = { offers ->
                    allResults = offers
                    applyFilters()
                },
                onFailure = { e ->
                    _searchState.value = UiState.Error(e.message ?: "Search failed")
                }
            )
        }
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
        applyFilters()
    }

    fun setMaxStops(stops: Int?) {
        _maxStops.value = stops
        applyFilters()
    }

    private fun applyFilters() {
        var results = allResults
        _maxStops.value?.let { max -> results = results.filter { it.stopCount() <= max } }
        results = when (_sortMode.value) {
            SortMode.PRICE -> results.sortedBy { it.totalPriceDouble() }
            SortMode.DURATION -> results.sortedBy {
                it.itineraries.firstOrNull()?.duration ?: ""
            }
            SortMode.DEPARTURE -> results.sortedBy {
                it.itineraries.firstOrNull()?.segments?.firstOrNull()?.departure?.at ?: ""
            }
        }
        _searchState.value = UiState.Success(results)
    }
}
