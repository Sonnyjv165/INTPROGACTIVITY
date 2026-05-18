package com.example.intprogactivity.presentation.booking

import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.domain.repository.BookingRepository
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.AppSuccess
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.CtaOrange
import com.example.intprogactivity.presentation.theme.TripFlightsTheme
import com.example.intprogactivity.util.formatDisplayTime
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ConfirmationFragment : Fragment() {

    private val viewModel: BookingViewModel by viewModels()

    @Inject
    lateinit var bookingRepository: BookingRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bookingId = arguments?.getString("bookingId") ?: ""
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TripFlightsTheme {
                    ConfirmationScreen(
                        bookingId = bookingId,
                        bookingRepository = bookingRepository,
                        viewModel = viewModel,
                        onViewTrips = { findNavController().navigate(R.id.tripsFragment) },
                        onDownloadTicket = { /* no-op – bitmap capture not possible in Compose easily */ }
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmationScreen(
    bookingId: String,
    bookingRepository: BookingRepository,
    viewModel: BookingViewModel,
    onViewTrips: () -> Unit,
    onDownloadTicket: () -> Unit
) {
    var booking by remember { mutableStateOf<Booking?>(null) }

    LaunchedEffect(bookingId) {
        withContext(Dispatchers.IO) {
            bookingRepository.getBookingById(bookingId)
        }.onSuccess { booking = it }
    }

    val offer = viewModel.flightOffer.value
    val firstSeg = offer?.itineraries?.firstOrNull()?.segments?.firstOrNull()
    val lastSeg = offer?.itineraries?.firstOrNull()?.segments?.lastOrNull()

    Scaffold(
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(AppSuccess.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = AppSuccess,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Booking Confirmed!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Your e-ticket has been sent to your email",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )

            // Ticket card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandPrimary, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AirplanemodeActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("TripFlights", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "PNR: ${booking?.pnr ?: "------"}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Flight info
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (firstSeg != null && lastSeg != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(firstSeg.departure.at.formatDisplayTime(), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                                    Text(firstSeg.departure.iataCode, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(28.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(lastSeg.arrival.at.formatDisplayTime(), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                                    Text(lastSeg.arrival.iataCode, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        booking?.let { b ->
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(12.dp))
                            val passenger = b.passengers.firstOrNull()
                            if (passenger != null) {
                                TicketRow(
                                    "Passenger",
                                    "${passenger.lastName.uppercase()} ${passenger.firstName.uppercase()}"
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            TicketRow("Status", b.status.name)
                            Spacer(Modifier.height(8.dp))
                            TicketRow("Total Paid", "₱${String.format("%,.0f", b.totalPrice)}")
                            if (b.tripCoinsEarned > 0) {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "+${String.format("%,d", b.tripCoinsEarned)} Trip Coins Earned!",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onViewTrips,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("View My Trips", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onDownloadTicket,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download E-Ticket", fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TicketRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
