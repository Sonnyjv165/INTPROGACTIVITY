package com.example.intprogactivity.presentation.trips

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.Brush
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.domain.model.BookingStatus
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.model.Passenger
import com.example.intprogactivity.domain.repository.BookingRepository
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.AppError
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.BrandPrimaryDark
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
            onSuccess = { booking = it; isLoading = false },
            onFailure = { isLoading = false; snackbarHostState.showSnackbar("Failed to load booking") }
        )
    }

    LaunchedEffect(cancelState) {
        when (val s = cancelState) {
            is UiState.Success -> { viewModel.resetCancelState(); snackbarHostState.showSnackbar("Booking cancelled"); onBack() }
            is UiState.Error   -> snackbarHostState.showSnackbar(s.message)
            else               -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Booking Details", fontWeight = FontWeight.SemiBold) },
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
                val outboundOffer = runCatching { gson.fromJson(b.outboundFlightJson, FlightOffer::class.java) }.getOrNull()
                val returnOffer   = b.returnFlightJson?.takeIf { it.isNotBlank() }
                    ?.let { runCatching { gson.fromJson(it, FlightOffer::class.java) }.getOrNull() }
                val isRoundTrip   = returnOffer != null

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppBackground)
                        .verticalScroll(rememberScrollState())
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // ── Hero header card ─────────────────────────────────────────
                    BookingHeaderCard(booking = b)

                    Spacer(Modifier.height(14.dp))

                    // ── Outbound flight ──────────────────────────────────────────
                    if (outboundOffer != null) {
                        BoardingPassCard(
                            offer         = outboundOffer,
                            label         = if (isRoundTrip) "Outbound Flight" else "Flight Information",
                            isReturn      = false,
                            travelDate    = b.travelDate,
                            passengerCount= b.passengers.size,
                            cabinClass    = b.cabinClass,
                            dateFormat    = dateFormat
                        )
                    } else if (b.originIata.isNotEmpty() || b.destinationIata.isNotEmpty()) {
                        // Web-format booking: render from flat fields
                        FlatFlightCard(
                            origin       = b.originIata,
                            destination  = b.destinationIata,
                            airlineName  = b.airlineName,
                            travelDate   = b.travelDate,
                            cabinClass   = b.cabinClass,
                            isReturn     = false,
                            label        = if (isRoundTrip) "Outbound Flight" else "Flight Information",
                            passengerCount = b.passengers.size,
                            dateFormat   = dateFormat
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // ── Return flight ────────────────────────────────────────────
                    if (returnOffer != null) {
                        val returnDate = returnOffer.itineraries.firstOrNull()?.segments?.firstOrNull()
                            ?.departure?.at
                            ?.let { runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(it)?.time }.getOrNull() }
                            ?: b.travelDate
                        BoardingPassCard(
                            offer          = returnOffer,
                            label          = "Return Flight",
                            isReturn       = true,
                            travelDate     = returnDate,
                            passengerCount = b.passengers.size,
                            cabinClass     = b.cabinClass,
                            dateFormat     = dateFormat
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    // ── Passengers ───────────────────────────────────────────────
                    if (b.passengers.isNotEmpty()) {
                        PassengersCard(passengers = b.passengers, seatSelections = b.addOns.seatSelections.associate {
                            it.passengerId to it.seatNumber
                        })
                        Spacer(Modifier.height(14.dp))
                    }

                    // ── Price summary ────────────────────────────────────────────
                    PriceSummaryCard(booking = b)

                    // ── Cancel button ────────────────────────────────────────────
                    if (b.status == BookingStatus.CONFIRMED || b.status == BookingStatus.PENDING) {
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { onCancelConfirm(b) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppError)
                        ) {
                            Text("Cancel Booking", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ─── Hero Header Card ─────────────────────────────────────────────────────────

@Composable
private fun BookingHeaderCard(booking: Booking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(BrandPrimaryDark, BrandPrimary, Color(0xFF2196F3))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            "Booking Reference",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            booking.pnr.ifEmpty { "—" },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                    StatusPill(status = booking.status)
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip(label = "Passengers", value = "${booking.passengers.size.coerceAtLeast(1)}")
                    InfoChip(label = "Cabin", value = booking.cabinClass.replace("_", " ").lowercase()
                        .replaceFirstChar { it.uppercase() })
                    InfoChip(label = "Coins Earned", value = "+${booking.tripCoinsEarned}")
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: BookingStatus) {
    val (bg, fg, label) = when (status) {
        BookingStatus.CONFIRMED -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), "CONFIRMED")
        BookingStatus.CANCELLED -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), "CANCELLED")
        BookingStatus.PENDING   -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), "PENDING")
        BookingStatus.COMPLETED -> Triple(Color(0xFFD6E8FF), Color(0xFF0052B0), "COMPLETED")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (status == BookingStatus.CONFIRMED) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.65f))
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ─── Boarding Pass–Style Flight Card ─────────────────────────────────────────

@Composable
private fun BoardingPassCard(
    offer: FlightOffer,
    label: String,
    isReturn: Boolean,
    travelDate: Long,
    passengerCount: Int,
    cabinClass: String,
    dateFormat: SimpleDateFormat
) {
    val firstSeg = offer.itineraries.firstOrNull()?.segments?.firstOrNull()
    val lastSeg  = offer.itineraries.firstOrNull()?.segments?.lastOrNull()
    if (firstSeg == null || lastSeg == null) return

    val stopCount   = (offer.itineraries.firstOrNull()?.segments?.size ?: 1) - 1
    val flightNum   = "${firstSeg.carrierCode}${firstSeg.number}"
    val dateStr     = runCatching { dateFormat.format(Date(travelDate)) }.getOrDefault("")

    BoardingPassLayout(
        label        = label,
        isReturn     = isReturn,
        originIata   = firstSeg.departure.iataCode,
        destIata     = lastSeg.arrival.iataCode,
        departTime   = firstSeg.departure.at.formatDisplayTime(),
        arriveTime   = lastSeg.arrival.at.formatDisplayTime(),
        flightNum    = flightNum,
        cabinClass   = cabinClass,
        dateStr      = dateStr,
        passengerCount = passengerCount,
        stopCount    = stopCount
    )
}

@Composable
private fun FlatFlightCard(
    origin: String,
    destination: String,
    airlineName: String,
    travelDate: Long,
    cabinClass: String,
    isReturn: Boolean,
    label: String,
    passengerCount: Int,
    dateFormat: SimpleDateFormat
) {
    val dateStr = runCatching { dateFormat.format(Date(travelDate)) }.getOrDefault("")
    BoardingPassLayout(
        label          = label,
        isReturn       = isReturn,
        originIata     = origin,
        destIata       = destination,
        departTime     = "—:——",
        arriveTime     = "—:——",
        flightNum      = airlineName,
        cabinClass     = cabinClass,
        dateStr        = dateStr,
        passengerCount = passengerCount,
        stopCount      = 0
    )
}

@Composable
private fun BoardingPassLayout(
    label: String,
    isReturn: Boolean,
    originIata: String,
    destIata: String,
    departTime: String,
    arriveTime: String,
    flightNum: String,
    cabinClass: String,
    dateStr: String,
    passengerCount: Int,
    stopCount: Int
) {
    val accentColor = if (isReturn) Color(0xFF0052B0) else BrandPrimary

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column {
            // ── Coloured top accent bar ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(listOf(accentColor, Color(0xFF2196F3)))
                    )
            )

            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isReturn) Icons.Filled.FlightLand else Icons.Filled.AirplanemodeActive,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentColor)
                    }
                    if (isReturn) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFEFF6FF))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("Return", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Times & airports ────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Origin
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(departTime, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Color(0xFF1A1A1A))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            originIata,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = accentColor
                        )
                    }

                    // Centre: dashes + plane icon
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(Color(0xFFD1D5DB))
                            )
                            Icon(
                                imageVector = if (isReturn) Icons.Filled.FlightLand else Icons.Filled.FlightTakeoff,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(22.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(Color(0xFFD1D5DB))
                            )
                        }
                        if (stopCount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "$stopCount Stop${if (stopCount > 1) "s" else ""}",
                                fontSize = 10.sp,
                                color = Color(0xFFCA8A04),
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Spacer(Modifier.height(4.dp))
                            Text("Direct", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Destination
                    Column(horizontalAlignment = Alignment.End) {
                        Text(arriveTime, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Color(0xFF1A1A1A))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            destIata,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = accentColor
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Dashed divider ──────────────────────────────────────────
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0xFFE5E7EB)
                )

                Spacer(Modifier.height(12.dp))

                // ── Bottom info row ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FlightDetailChip(
                        icon  = Icons.Filled.Schedule,
                        label = "Date",
                        value = dateStr
                    )
                    FlightDetailChip(
                        icon  = Icons.Filled.AirplanemodeActive,
                        label = "Flight",
                        value = flightNum
                    )
                    FlightDetailChip(
                        icon  = Icons.Filled.Luggage,
                        label = "Cabin",
                        value = cabinClass.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() }
                    )
                    FlightDetailChip(
                        icon  = Icons.Filled.Person,
                        label = "Pax",
                        value = "$passengerCount"
                    )
                }
            }
        }
    }
}

@Composable
private fun FlightDetailChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 9.sp, color = Color(0xFF9CA3AF))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
    }
}

// ─── Passengers Card ─────────────────────────────────────────────────────────

@Composable
private fun PassengersCard(
    passengers: List<Passenger>,
    seatSelections: Map<String, String>
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Passengers",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BrandPrimary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("${passengers.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                }
            }

            Spacer(Modifier.height(14.dp))

            passengers.forEachIndexed { idx, p ->
                if (idx > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = Color(0xFFF3F4F6)
                    )
                }
                val seat = seatSelections[idx.toString()]
                PassengerRow(passenger = p, seatNumber = seat, index = idx)
            }
        }
    }
}

@Composable
private fun PassengerRow(passenger: Passenger, seatNumber: String?, index: Int) {
    val avatarColors = listOf(
        Color(0xFF006CE4), Color(0xFF7C3AED), Color(0xFFDB2777), Color(0xFF059669)
    )
    val avatarColor = avatarColors[index % avatarColors.size]
    val initials = buildString {
        if (passenger.firstName.isNotEmpty()) append(passenger.firstName.first().uppercaseChar())
        if (passenger.lastName.isNotEmpty()) append(passenger.lastName.first().uppercaseChar())
    }.ifEmpty { "P" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${passenger.lastName.uppercase()} ${passenger.firstName.uppercase()}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color(0xFF1A1A1A)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                passenger.type.name.lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 11.sp,
                color = Color(0xFF6B7280)
            )
        }

        // Seat badge
        if (!seatNumber.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEFF6FF))
                    .border(1.dp, BrandPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "Seat $seatNumber",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            }
        }
    }
}

// ─── Price Summary Card ───────────────────────────────────────────────────────

@Composable
private fun PriceSummaryCard(booking: Booking) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text("Payment Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1A1A1A))

            Spacer(Modifier.height(14.dp))

            if (booking.paymentMethod.isNotEmpty()) {
                PriceRow(label = "Payment Method", value = booking.paymentMethod, isHighlight = false)
                Spacer(Modifier.height(6.dp))
            }

            if (!booking.promoCode.isNullOrEmpty()) {
                PriceRow(label = "Promo Code", value = booking.promoCode, isHighlight = false)
                Spacer(Modifier.height(6.dp))
            }

            if (booking.tripCoinsEarned > 0) {
                PriceRow(label = "Trip Coins Earned", value = "+${booking.tripCoinsEarned} coins", isHighlight = false, valueColor = Color(0xFF10B981))
                Spacer(Modifier.height(6.dp))
            }

            HorizontalDivider(color = Color(0xFFF3F4F6), modifier = Modifier.padding(vertical = 6.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Paid", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A1A1A))
                Text(
                    "₱${String.format("%,.0f", booking.totalPrice)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = CtaOrange
                )
            }
        }
    }
}

@Composable
private fun PriceRow(
    label: String,
    value: String,
    isHighlight: Boolean,
    valueColor: Color = Color(0xFF6B7280)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFF6B7280))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun statusBgColor(status: BookingStatus): Color = when (status) {
    BookingStatus.CONFIRMED -> Color(0xFFD1FAE5)
    BookingStatus.CANCELLED -> Color(0xFFFEE2E2)
    BookingStatus.PENDING   -> Color(0xFFFEF3C7)
    BookingStatus.COMPLETED -> Color(0xFFD6E8FF)
}

private fun statusFgColor(status: BookingStatus): Color = when (status) {
    BookingStatus.CONFIRMED -> Color(0xFF065F46)
    BookingStatus.CANCELLED -> Color(0xFF991B1B)
    BookingStatus.PENDING   -> Color(0xFF92400E)
    BookingStatus.COMPLETED -> Color(0xFF0052B0)
}
