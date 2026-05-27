package com.example.intprogactivity.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.intprogactivity.domain.model.Airport
import com.example.intprogactivity.domain.model.FlightSearchParams
import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.domain.usecase.flight.SearchAirportsUseCase
import com.example.intprogactivity.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val searchAirportsUseCase: SearchAirportsUseCase
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _origin = MutableStateFlow<Airport?>(null)
    val origin: StateFlow<Airport?> = _origin.asStateFlow()

    private val _destination = MutableStateFlow<Airport?>(null)
    val destination: StateFlow<Airport?> = _destination.asStateFlow()

    private val _isRoundTrip = MutableStateFlow(true)
    val isRoundTrip: StateFlow<Boolean> = _isRoundTrip.asStateFlow()

    private val _departDate = MutableStateFlow<String?>(null)
    val departDate: StateFlow<String?> = _departDate.asStateFlow()

    private val _returnDate = MutableStateFlow<String?>(null)
    val returnDate: StateFlow<String?> = _returnDate.asStateFlow()

    private val _adults = MutableStateFlow(1)
    val adults: StateFlow<Int> = _adults.asStateFlow()

    private val _children = MutableStateFlow(0)
    val children: StateFlow<Int> = _children.asStateFlow()

    private val _infants = MutableStateFlow(0)
    val infants: StateFlow<Int> = _infants.asStateFlow()

    private val _airportSearchState = MutableStateFlow<UiState<List<Airport>>>(UiState.Idle)
    val airportSearchState: StateFlow<UiState<List<Airport>>> = _airportSearchState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    private val _navigateToSearch = MutableSharedFlow<FlightSearchParams>()
    val navigateToSearch = _navigateToSearch.asSharedFlow()

    init {
        observeAirportSearch()
    }

    @OptIn(FlowPreview::class)
    private fun observeAirportSearch() {
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length < 2) {
                    _airportSearchState.value = UiState.Idle
                    return@onEach
                }
                _airportSearchState.value = UiState.Loading
                searchAirportsUseCase(query).fold(
                    onSuccess = { _airportSearchState.value = UiState.Success(it) },
                    onFailure = { _airportSearchState.value = UiState.Error(it.message ?: "Search failed") }
                )
            }
            .launchIn(viewModelScope)
    }

    fun onAirportQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setOrigin(airport: Airport) { _origin.value = airport }
    fun setDestination(airport: Airport) { _destination.value = airport }

    fun setOriginByCode(iataCode: String) {
        val airport = com.example.intprogactivity.data.local.LocalAirportData.airports
            .find { it.iataCode == iataCode }
        if (airport != null) _origin.value = airport
    }

    fun setDestinationByCode(iataCode: String) {
        val airport = com.example.intprogactivity.data.local.LocalAirportData.airports
            .find { it.iataCode == iataCode }
        if (airport != null) _destination.value = airport
    }

    fun swapOriginDestination() {
        val temp = _origin.value
        _origin.value = _destination.value
        _destination.value = temp
    }

    fun setRoundTrip(roundTrip: Boolean) {
        _isRoundTrip.value = roundTrip
        if (!roundTrip) _returnDate.value = null
    }

    fun setDepartDate(date: String) { _departDate.value = date }
    fun setReturnDate(date: String) { _returnDate.value = date }

    fun setPassengers(adults: Int, children: Int, infants: Int) {
        _adults.value = adults
        _children.value = children
        _infants.value = infants
    }

    suspend fun onSearchClicked(): String? {
        val origin = _origin.value ?: return "Please select an origin city"
        val destination = _destination.value ?: return "Please select a destination city"
        val departDate = _departDate.value ?: return "Please select a departure date"
        if (_isRoundTrip.value && _returnDate.value == null) return "Please select a return date"
        if (origin.iataCode == destination.iataCode) return "Origin and destination cannot be the same"

        val params = FlightSearchParams(
            origin = origin.iataCode,
            originCity = origin.cityName,
            destination = destination.iataCode,
            destinationCity = destination.cityName,
            departureDate = departDate,
            returnDate = _returnDate.value,
            adults = _adults.value,
            children = _children.value,
            infants = _infants.value
        )
        _navigateToSearch.emit(params)
        return null
    }
}
