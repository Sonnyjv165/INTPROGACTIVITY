package com.example.intprogactivity.presentation.trips

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.domain.model.BookingStatus
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.CtaOrange
import com.example.intprogactivity.presentation.theme.TripFlightsTheme
import com.example.intprogactivity.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class FindBookingFragment : Fragment() {

    private val viewModel: FindBookingViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TripFlightsTheme {
                    FindBookingScreen(
                        viewModel = viewModel,
                        onBack = { findNavController().navigateUp() },
                        onViewDetail = { bookingId ->
                            val bundle = Bundle().apply { putString("bookingId", bookingId) }
                            findNavController().navigate(
                                com.example.intprogactivity.R.id.action_find_booking_to_detail,
                                bundle
                            )
                        }
                    )
                }
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindBookingScreen(
    viewModel: FindBookingViewModel,
    onBack: () -> Unit,
    onViewDetail: (String) -> Unit
) {
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    var pnr by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find My Booking") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Icon(Icons.Filled.ConfirmationNumber, null,
                modifier = Modifier.size(56.dp), tint = BrandPrimary)
            Spacer(Modifier.height(12.dp))
            Text("Enter your booking reference", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text("Find your booking using the PNR / reference code from your confirmation.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)

            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = pnr,
                onValueChange = { pnr = it.uppercase().trim() },
                label = { Text("Booking Reference (PNR)") },
                placeholder = { Text("e.g. TF-ABC123") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Filled.Search, null) }
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.findBooking(pnr) },
                enabled = pnr.length >= 3 && searchState !is UiState.Loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CtaOrange)
            ) {
                if (searchState is UiState.Loading)
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp))
                else
                    Text("Find Booking", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(24.dp))

            when (val s = searchState) {
                is UiState.Error   -> ErrorCard(s.message)
                is UiState.Success -> BookingResultCard(booking = s.data, onViewDetail = onViewDetail)
                else               -> Unit
            }
        }
    }
}

@Composable
private fun BookingResultCard(booking: Booking, onViewDetail: (String) -> Unit) {
    val fmt = remember { SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Booking Found", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                BookingStatusBadge(booking.status)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            BookingInfoRow("PNR", booking.pnr)
            BookingInfoRow("Route", "${booking.originIata} → ${booking.destinationIata}")
            BookingInfoRow("Date", fmt.format(Date(booking.travelDate)))
            BookingInfoRow("Passengers", booking.passengers.size.toString())
            BookingInfoRow("Total", "₱${String.format("%,.0f", booking.totalPrice)}")
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onViewDetail(booking.bookingId) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) { Text("View Full Details") }
        }
    }
}

@Composable
private fun BookingInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BookingStatusBadge(status: BookingStatus) {
    val (bg, fg) = when (status) {
        BookingStatus.CONFIRMED  -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        BookingStatus.CANCELLED  -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
        BookingStatus.COMPLETED  -> Color(0xFFEFF6FF) to Color(0xFF2563EB)
        BookingStatus.PENDING    -> Color(0xFFFEF9C3) to Color(0xFFCA8A04)
    }
    Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
        Text(status.name.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
    ) {
        Text(message,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            color = Color(0xFFDC2626), fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}
