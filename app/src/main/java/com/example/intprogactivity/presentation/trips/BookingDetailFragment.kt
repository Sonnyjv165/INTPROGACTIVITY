package com.example.intprogactivity.presentation.trips

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
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.domain.model.BookingStatus
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.repository.BookingRepository
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.AppError
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.CtaOrange
import com.example.intprogactivity.presentation.theme.TripFlightsTheme
import com.example.intprogactivity.util.UiState
import com.example.intprogactivity.util.formatDisplayTime
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BookingDetailFragment : Fragment() {

    private val viewModel: TripsViewModel by viewModels()

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
                    BookingDetailScreen(
                        bookingId = bookingId,
                        bookingRepository = bookingRepository,
                        viewModel = viewModel,
                        onBack = { findNavController().navigateUp() },
                        onCancelConfirm = { booking ->
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Cancel Booking")
                                .setMessage("Are you sure you want to cancel? This cannot be undone.")
                                .setPositiveButton("Cancel Booking") { _, _ ->
                                    viewModel.cancelBooking(booking.bookingId, booking.travelDate)
                                }
                                .setNegativeButton("Keep") { d, _ -> d.dismiss() }
                                .show()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    bookingId: String,
    bookingRepository: BookingRepository,
    viewModel: TripsViewModel,
    onBack: () -> Unit,
    onCancelConfirm: (Booking) -> Unit
) {
    val cancelState by viewModel.cancelState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var booking by remember { mutableStateOf<Booking?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val gson = remember { Gson() }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }

    LaunchedEffect(bookingId) {
        withContext(Dispatchers.IO) {
            bookingRepository.getBookingById(bookingId)
        }.fold(
            onSuccess = {
                booking = it
                isLoading = false
            },
            onFailure = {
                isLoading = false
                snackbarHostState.showSnackbar("Failed to load booking")
            }
        )
    }

    LaunchedEffect(cancelState) {
        when (val s = cancelState) {
            is UiState.Success -> {
                viewModel.resetCancelState()
                snackbarHostState.showSnackbar("Booking cancelled")
                onBack()
            }
            is UiState.Error -> snackbarHostState.showSnackbar(s.message)
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Booking Details") },
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
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else {
            val b = booking
            if (b == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Booking not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val offer = runCatching { gson.fromJson(b.outboundFlightJson, FlightOffer::class.java) }.getOrNull()
                val firstSeg = offer?.itineraries?.firstOrNull()?.segments?.firstOrNull()
                val lastSeg = offer?.itineraries?.firstOrNull()?.segments?.lastOrNull()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppBackground)
                        .verticalScroll(rememberScrollState())
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // PNR & status
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Booking Reference", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        b.pnr,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = BrandPrimary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusBgColor(b.status))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        b.status.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusFgColor(b.status)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Flight info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AirplanemodeActive, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Flight Information", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(Modifier.height(12.dp))

                            if (firstSeg != null && lastSeg != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(firstSeg.departure.at.formatDisplayTime(), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                        Text(firstSeg.departure.iataCode, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                    Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(24.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(lastSeg.arrival.at.formatDisplayTime(), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                        Text(lastSeg.arrival.iataCode, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    runCatching { dateFormat.format(Date(b.travelDate)) }.getOrDefault("") +
                                            " · ${b.passengers.size.coerceAtLeast(1)} Passenger${if (b.passengers.size != 1) "s" else ""}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Passengers
                    if (b.passengers.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Passengers", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Spacer(Modifier.height(10.dp))
                                b.passengers.forEachIndexed { idx, p ->
                                    if (idx > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                    val seat = b.addOns.seatSelections.find { it.passengerId == idx.toString() }?.seatNumber
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${p.lastName.uppercase()} ${p.firstName.uppercase()}",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            buildString {
                                                append(p.type.name.lowercase().replaceFirstChar { it.uppercase() })
                                                if (seat != null) append(" · Seat $seat")
                                            },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Total price
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Paid", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "₱${String.format("%,.0f", b.totalPrice)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = CtaOrange
                            )
                        }
                    }

                    if (b.status == BookingStatus.CONFIRMED) {
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { onCancelConfirm(b) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppError)
                        ) {
                            Text("Cancel Booking", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun statusBgColor(status: BookingStatus): Color = when (status) {
    BookingStatus.CONFIRMED -> Color(0xFFD1FAE5)
    BookingStatus.CANCELLED -> Color(0xFFFEE2E2)
    BookingStatus.PENDING -> Color(0xFFFEF3C7)
    BookingStatus.COMPLETED -> Color(0xFFD6E8FF)
}

private fun statusFgColor(status: BookingStatus): Color = when (status) {
    BookingStatus.CONFIRMED -> Color(0xFF065F46)
    BookingStatus.CANCELLED -> Color(0xFF991B1B)
    BookingStatus.PENDING -> Color(0xFF92400E)
    BookingStatus.COMPLETED -> Color(0xFF0052B0)
}
