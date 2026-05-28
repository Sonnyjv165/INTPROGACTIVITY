package com.example.intprogactivity.presentation.trips

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.AirplaneTicket
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.domain.model.BookingStatus
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.AppSuccess
import com.example.intprogactivity.presentation.theme.AppWarning
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.CtaOrange
import com.example.intprogactivity.presentation.theme.TripFlightsTheme
import com.example.intprogactivity.util.UiState
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TripsFragment : Fragment() {

    private val viewModel: TripsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            TripFlightsTheme {
                TripsScreen(
                    viewModel = viewModel,
                    onBookingClick = { booking ->
                        val bundle = Bundle().apply { putString("bookingId", booking.bookingId) }
                        findNavController().navigate(R.id.action_trips_to_booking_detail, bundle)
                    },
                    onSearchFlight = { findNavController().navigate(R.id.homeFragment) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    viewModel: TripsViewModel,
    onBookingClick: (Booking) -> Unit,
    onSearchFlight: () -> Unit
) {
    val bookingsState by viewModel.bookingsState.collectAsStateWithLifecycle()
    val upcoming by viewModel.upcomingBookings.collectAsStateWithLifecycle()
    val past by viewModel.pastBookings.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Upcoming", "Past")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Trips") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandPrimary,
                    titleContentColor = Color.White
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = BrandPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BrandPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = BrandPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (bookingsState is UiState.Loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandPrimary)
                }
            } else {
                val displayList = if (selectedTab == 0) upcoming else past
                if (displayList.isEmpty()) {
                    EmptyTripsState(
                        isUpcoming = selectedTab == 0,
                        onSearchFlight = onSearchFlight
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayList, key = { it.bookingId }) { booking ->
                            BookingCard(booking = booking, onClick = { onBookingClick(booking) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(booking: Booking, onClick: () -> Unit) {
    val gson = remember { Gson() }
    val outboundOffer = remember(booking.outboundFlightJson) {
        runCatching { gson.fromJson(booking.outboundFlightJson, FlightOffer::class.java) }.getOrNull()
    }
    val returnOffer = remember(booking.returnFlightJson) {
        booking.returnFlightJson?.takeIf { it.isNotBlank() }
            ?.let { runCatching { gson.fromJson(it, FlightOffer::class.java) }.getOrNull() }
    }
    val firstSeg = outboundOffer?.itineraries?.firstOrNull()?.segments?.firstOrNull()
    val lastSeg  = outboundOffer?.itineraries?.firstOrNull()?.segments?.lastOrNull()
    val retFirstSeg = returnOffer?.itineraries?.firstOrNull()?.segments?.firstOrNull()
    val retLastSeg  = returnOffer?.itineraries?.firstOrNull()?.segments?.lastOrNull()

    // Derive outbound display values
    val originCode   = firstSeg?.departure?.iataCode?.takeIf { it.isNotEmpty() } ?: booking.originIata
    val destCode     = lastSeg?.arrival?.iataCode?.takeIf { it.isNotEmpty() } ?: booking.destinationIata
    val airlineLabel = firstSeg?.carrierCode?.takeIf { it.isNotEmpty() } ?: booking.airlineName

    // Derive return display values
    val retOrigin = retFirstSeg?.departure?.iataCode?.takeIf { it.isNotEmpty() }
    val retDest   = retLastSeg?.arrival?.iataCode?.takeIf { it.isNotEmpty() }
    val isRoundTrip = retOrigin != null && retDest != null

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── PNR + status ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        booking.pnr,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BrandPrimary
                    )
                    if (isRoundTrip) {
                        Text(
                            "Round Trip",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor(booking.status).copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        booking.status.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor(booking.status)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Outbound route ─────────────────────────────────────────────
            if (originCode.isNotEmpty() || destCode.isNotEmpty()) {
                RouteRow(
                    origin      = originCode.ifEmpty { "—" },
                    destination = destCode.ifEmpty { "—" },
                    label       = airlineLabel,
                    isReturn    = false
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Flight details unavailable", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Return route (round trips only) ────────────────────────────
            if (isRoundTrip) {
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.HorizontalDivider(color = Color(0xFFF3F4F6))
                Spacer(Modifier.height(8.dp))
                RouteRow(
                    origin      = retOrigin ?: "—",
                    destination = retDest ?: "—",
                    label       = retFirstSeg?.carrierCode ?: airlineLabel,
                    isReturn    = true
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Price + details link ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "₱${String.format("%,.0f", booking.totalPrice)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = CtaOrange
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Details", fontSize = 13.sp, color = BrandPrimary)
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RouteRow(origin: String, destination: String, label: String, isReturn: Boolean) {
    val tint = if (isReturn) Color(0xFF0052B0) else BrandPrimary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(origin, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isReturn) Icons.Filled.FlightLand else Icons.Filled.FlightTakeoff,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Text(
                if (isReturn) "Return" else "Outbound",
                fontSize = 9.sp,
                color = tint,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(destination, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable
private fun EmptyTripsState(isUpcoming: Boolean, onSearchFlight: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                if (isUpcoming) Icons.Filled.AirplaneTicket else Icons.Filled.AirplanemodeActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (isUpcoming) "No upcoming trips" else "No past trips",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Text(
                if (isUpcoming) "Book your next adventure!" else "Your completed trips will appear here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (isUpcoming) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onSearchFlight,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Search Flights")
                }
            }
        }
    }
}

private fun statusColor(status: BookingStatus): Color = when (status) {
    BookingStatus.CONFIRMED -> AppSuccess
    BookingStatus.CANCELLED -> Color(0xFFEF4444)
    BookingStatus.PENDING -> AppWarning
    BookingStatus.COMPLETED -> BrandPrimary
}
