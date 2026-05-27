package com.example.intprogactivity.presentation.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.model.FlightSearchParams
import com.example.intprogactivity.presentation.booking.BookingViewModel
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.CtaOrange
import com.example.intprogactivity.presentation.theme.TripFlightsTheme
import com.example.intprogactivity.util.formatDisplayTime
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class FlightDetailFragment : Fragment() {

    private val bookingViewModel: BookingViewModel by activityViewModels()
    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val offerJson = arguments?.getString("flightOfferJson") ?: ""
        val offer = gson.fromJson(offerJson, FlightOffer::class.java)
        val isRoundTrip = arguments?.getBoolean("isRoundTrip", false) ?: false
        val isSelectingReturn = arguments?.getBoolean("isSelectingReturn", false) ?: false
        val spJson = arguments?.getString("searchParamsJson") ?: ""
        val searchParams = if (spJson.isNotEmpty())
            runCatching { gson.fromJson(spJson, FlightSearchParams::class.java) }.getOrNull()
        else null

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TripFlightsTheme {
                    FlightDetailScreen(
                        offer = offer,
                        isRoundTrip = isRoundTrip,
                        isSelectingReturn = isSelectingReturn,
                        searchParams = searchParams,
                        onBack = { findNavController().navigateUp() },
                        onBookNow = { selectedCabin ->
                            bookingViewModel.setCabinClass(selectedCabin)

                            when {
                                // Step 1 of round-trip: outbound selected → navigate to return flight selection
                                isRoundTrip && !isSelectingReturn && searchParams != null -> {
                                    bookingViewModel.setFlightOffer(offer)
                                    val returnDate = searchParams.returnDate ?: ""
                                    val returnParams = searchParams.copy(
                                        origin = searchParams.destination,
                                        destination = searchParams.origin,
                                        departureDate = returnDate.ifEmpty { searchParams.departureDate },
                                        returnDate = null
                                    )
                                    val bundle = Bundle().apply {
                                        putString("searchParamsJson", gson.toJson(returnParams))
                                        putBoolean("isSelectingReturn", true)
                                    }
                                    findNavController().navigate(R.id.action_detail_to_return_results, bundle)
                                }

                                // Step 2 of round-trip: return flight confirmed → go to passenger details
                                isSelectingReturn -> {
                                    val outboundOffer = bookingViewModel.flightOffer.value
                                    val outboundJson = if (outboundOffer != null) gson.toJson(outboundOffer) else offerJson
                                    val bundle = Bundle().apply {
                                        putString("flightOfferJson", outboundJson)
                                        putString("returnFlightJson", offerJson)
                                        putInt("adults", searchParams?.adults ?: 1)
                                        putInt("children", searchParams?.children ?: 0)
                                        putInt("infants", searchParams?.infants ?: 0)
                                    }
                                    findNavController().navigate(R.id.action_detail_to_passenger, bundle)
                                }

                                // One-way booking: go directly to passenger details
                                else -> {
                                    val bundle = Bundle().apply {
                                        putString("flightOfferJson", offerJson)
                                        putString("returnFlightJson", "")
                                        putInt("adults", searchParams?.adults ?: 1)
                                        putInt("children", searchParams?.children ?: 0)
                                        putInt("infants", searchParams?.infants ?: 0)
                                    }
                                    findNavController().navigate(R.id.action_detail_to_passenger, bundle)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailScreen(
    offer: FlightOffer,
    isRoundTrip: Boolean,
    isSelectingReturn: Boolean = false,
    searchParams: FlightSearchParams?,
    onBack: () -> Unit,
    onBookNow: (String) -> Unit
) {
    val itinerary = offer.itineraries.firstOrNull() ?: return
    val firstSeg = itinerary.segments.firstOrNull() ?: return
    val lastSeg = itinerary.segments.lastOrNull() ?: return
    val stops = offer.stopCount()
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val isoFormat = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }

    val cabinOptions = listOf(
        Triple("ECONOMY", "Economy", 1.0),
        Triple("PREMIUM_ECONOMY", "Premium Eco", 1.4),
        Triple("BUSINESS", "Business", 2.5),
        Triple("FIRST", "First Class", 4.0)
    )
    var selectedCabinIndex by remember { mutableStateOf(0) }
    val multiplier = cabinOptions[selectedCabinIndex].third

    val travelerPrice = offer.travelerPricings.firstOrNull()?.price
    val basePrice = (travelerPrice?.base?.toDoubleOrNull() ?: 0.0) * multiplier
    val totalPrice = offer.totalPriceDouble() * multiplier
    val taxesPrice = totalPrice - basePrice
    val coinsEstimate = (totalPrice * 10).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isSelectingReturn) "Return Flight Details" else "Flight Details")
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
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Main flight card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Airline row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.AirplanemodeActive,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                firstSeg.carrierCode,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                "${firstSeg.carrierCode} ${firstSeg.number}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Route timeline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(firstSeg.departure.at.formatDisplayTime(), fontWeight = FontWeight.Bold, fontSize = 28.sp)
                            }
                            Text(firstSeg.departure.iataCode, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                runCatching { dateFormat.format(isoFormat.parse(firstSeg.departure.at)!!) }.getOrDefault(""),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(formatDuration(itinerary.duration), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (stops == 0) "Nonstop" else "$stops stop${if (stops > 1) "s" else ""}",
                                fontSize = 11.sp,
                                color = if (stops == 0) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.FlightLand, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(lastSeg.arrival.at.formatDisplayTime(), fontWeight = FontWeight.Bold, fontSize = 28.sp)
                            }
                            Text(lastSeg.arrival.iataCode, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                runCatching { dateFormat.format(isoFormat.parse(lastSeg.arrival.at)!!) }.getOrDefault(""),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))

                    // Baggage
                    val baggage = offer.travelerPricings.firstOrNull()
                        ?.fareDetailsBySegment?.firstOrNull()
                        ?.includedCheckedBags
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Luggage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(baggage?.displayText() ?: "Carry-on only", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Cabin class selection
            Text("Cabin Class", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cabinOptions.forEachIndexed { index, (_, label, _) ->
                    FilterChip(
                        selected = index == selectedCabinIndex,
                        onClick = { selectedCabinIndex = index },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Price breakdown
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Price Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    PriceRow("Base Fare", "₱${String.format("%,.0f", basePrice)}")
                    Spacer(Modifier.height(6.dp))
                    PriceRow("Taxes & Fees", "₱${String.format("%,.0f", taxesPrice)}")
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    PriceRow(
                        label = "Total",
                        value = "₱${String.format("%,.0f", totalPrice)}",
                        isTotal = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Earn ${String.format("%,d", coinsEstimate)} Trip Coins with this booking",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onBookNow(cabinOptions[selectedCabinIndex].first) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CtaOrange)
            ) {
                Text(
                    when {
                        isSelectingReturn -> "Confirm Return Flight"
                        isRoundTrip -> "Select Return Flight"
                        else -> "Book Now"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PriceRow(label: String, value: String, isTotal: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            fontSize = if (isTotal) 15.sp else 13.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontSize = if (isTotal) 15.sp else 13.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) CtaOrange else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatDuration(isoDuration: String): String {
    val h = Regex("(\\d+)H").find(isoDuration)?.groupValues?.get(1) ?: "0"
    val m = Regex("(\\d+)M").find(isoDuration)?.groupValues?.get(1) ?: "0"
    return "${h}h ${m}m"
}
