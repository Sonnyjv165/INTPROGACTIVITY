package com.example.intprogactivity.presentation.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.model.FlightSearchParams
import com.example.intprogactivity.presentation.booking.BookingViewModel
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.AppSuccess
import com.example.intprogactivity.presentation.theme.AppWarning
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.CtaOrange
import com.example.intprogactivity.presentation.theme.TripFlightsTheme
import com.example.intprogactivity.util.UiState
import com.example.intprogactivity.util.formatDisplayTime
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchResultsFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModels()
    private val bookingViewModel: BookingViewModel by activityViewModels()
    private val gson = Gson()
    private lateinit var searchParams: FlightSearchParams

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val paramsJson = arguments?.getString("searchParamsJson").orEmpty()
        val parsed = if (paramsJson.isNotEmpty())
            runCatching { gson.fromJson(paramsJson, FlightSearchParams::class.java) }.getOrNull()
        else null

        if (parsed == null) {
            return ComposeView(requireContext()).also {
                it.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                it.setContent { TripFlightsTheme { Box(Modifier.fillMaxSize()) } }
            }
        }
        searchParams = parsed
        viewModel.search(searchParams)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TripFlightsTheme {
                    SearchResultsScreen(
                        viewModel = viewModel,
                        searchParams = searchParams,
                        onBack = { findNavController().navigateUp() },
                        onOfferClick = { offer ->
                            val bundle = Bundle().apply {
                                putString("flightOfferJson", gson.toJson(offer))
                                putBoolean("isRoundTrip", searchParams.isRoundTrip())
                                putString("returnDate", searchParams.returnDate ?: "")
                                putString("searchParamsJson", gson.toJson(searchParams))
                            }
                            findNavController().navigate(R.id.action_results_to_detail, bundle)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    viewModel: SearchViewModel,
    searchParams: FlightSearchParams,
    onBack: () -> Unit,
    onOfferClick: (FlightOffer) -> Unit
) {
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val maxStops by viewModel.maxStops.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(searchState) {
        if (searchState is UiState.Error) {
            snackbarHostState.showSnackbar((searchState as UiState.Error).message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "${searchParams.origin} → ${searchParams.destination}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "${searchParams.departureDate} · ${searchParams.passengerSummary()}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(padding)
        ) {
            // Filter / sort row
            LazyRow(
                modifier = Modifier
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val modes = SortMode.values()
                    val current = sortMode
                    FilterChip(
                        selected = current != SortMode.PRICE,
                        onClick = {
                            val next = modes[(current.ordinal + 1) % modes.size]
                            viewModel.setSortMode(next)
                        },
                        label = {
                            Text(
                                "Sort: ${current.name.lowercase().replaceFirstChar { it.uppercase() }}"
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = maxStops != null,
                        onClick = {
                            viewModel.setMaxStops(when (maxStops) {
                                null -> 0
                                0 -> 1
                                else -> null
                            })
                        },
                        label = {
                            Text(when (maxStops) {
                                null -> "Stops"
                                0 -> "Direct"
                                else -> "Max $maxStops stop"
                            })
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            when (val state = searchState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BrandPrimary)
                            Spacer(Modifier.height(12.dp))
                            Text("Searching flights...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                is UiState.Success -> {
                    val offers = state.data
                    if (offers.isEmpty()) {
                        EmptyFlightsState()
                    } else {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Text(
                                "${offers.size} flight${if (offers.size != 1) "s" else ""} found",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp, vertical = 8.dp
                            )
                        ) {
                            items(offers, key = { it.id }) { offer ->
                                FlightOfferCard(offer = offer, onClick = { onOfferClick(offer) })
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    EmptyFlightsState()
                }
                else -> Unit
            }
        }
    }
}

@Composable
fun FlightOfferCard(offer: FlightOffer, onClick: () -> Unit) {
    val itinerary = offer.itineraries.firstOrNull() ?: return
    val firstSeg = itinerary.segments.firstOrNull() ?: return
    val lastSeg = itinerary.segments.lastOrNull() ?: return
    val stops = offer.stopCount()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Airline + price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(BrandPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AirplanemodeActive,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            firstSeg.carrierCode,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            "${firstSeg.carrierCode} ${firstSeg.number}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₱${String.format("%,.0f", offer.totalPriceDouble())}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CtaOrange
                    )
                    Text("per person", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Route row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.FlightTakeoff,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            firstSeg.departure.at.formatDisplayTime(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Text(
                        firstSeg.departure.iataCode,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatDuration(itinerary.duration),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (stops) {
                                    0 -> AppSuccess.copy(alpha = 0.15f)
                                    1 -> AppWarning.copy(alpha = 0.15f)
                                    else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (stops) { 0 -> "Direct"; 1 -> "1 stop"; else -> "$stops stops" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (stops) {
                                0 -> AppSuccess
                                1 -> AppWarning
                                else -> Color(0xFFEF4444)
                            }
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.FlightLand,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            lastSeg.arrival.at.formatDisplayTime(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Text(
                        lastSeg.arrival.iataCode,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }

            // Baggage info
            val baggage = offer.travelerPricings.firstOrNull()
                ?.fareDetailsBySegment?.firstOrNull()
                ?.includedCheckedBags
            if (baggage != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Luggage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        baggage.displayText(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFlightsState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No flights found",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Text(
                "Try adjusting your search filters",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp)
            )
        }
    }
}

private fun formatDuration(isoDuration: String): String {
    val h = Regex("(\\d+)H").find(isoDuration)?.groupValues?.get(1) ?: "0"
    val m = Regex("(\\d+)M").find(isoDuration)?.groupValues?.get(1) ?: "0"
    return "${h}h ${m}m"
}
