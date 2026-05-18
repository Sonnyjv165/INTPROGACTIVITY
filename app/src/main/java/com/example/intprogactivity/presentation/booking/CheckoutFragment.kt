package com.example.intprogactivity.presentation.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PhoneAndroid
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.CtaOrange
import com.example.intprogactivity.presentation.theme.TripFlightsTheme
import com.example.intprogactivity.util.Constants
import com.example.intprogactivity.util.UiState
import com.example.intprogactivity.util.formatDisplayTime
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CheckoutFragment : Fragment() {

    private val viewModel: BookingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            TripFlightsTheme {
                CheckoutScreen(
                    viewModel = viewModel,
                    onBack = { findNavController().navigateUp() },
                    onConfirmed = { bookingId ->
                        val bundle = Bundle().apply { putString("bookingId", bookingId) }
                        findNavController().navigate(R.id.action_checkout_to_confirmation, bundle)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: BookingViewModel,
    onBack: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    val flightOffer by viewModel.flightOffer.collectAsStateWithLifecycle()
    val returnFlightOffer by viewModel.returnFlightOffer.collectAsStateWithLifecycle()
    val addOns by viewModel.addOns.collectAsStateWithLifecycle()
    val bookingState by viewModel.bookingState.collectAsStateWithLifecycle()
    val tier by viewModel.currentUserTier.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var paymentMethod by remember { mutableStateOf("CARD") }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var gcashNumber by remember { mutableStateOf("") }
    val isLoading = bookingState is UiState.Loading

    LaunchedEffect(bookingState) {
        when (val s = bookingState) {
            is UiState.Success -> {
                viewModel.resetBookingState()
                onConfirmed(s.data)
            }
            is UiState.Error -> snackbarHostState.showSnackbar(s.message)
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
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
            // Flight summary
            flightOffer?.let { offer ->
                val firstSeg = offer.itineraries.firstOrNull()?.segments?.firstOrNull()
                val lastSeg = offer.itineraries.firstOrNull()?.segments?.lastOrNull()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Your Flight", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        if (firstSeg != null && lastSeg != null) {
                            Text(
                                "${firstSeg.departure.iataCode} → ${lastSeg.arrival.iataCode}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Text(
                                "${firstSeg.departure.at.formatDisplayTime()} → ${lastSeg.arrival.at.formatDisplayTime()}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        returnFlightOffer?.let { ret ->
                            val retFirst = ret.itineraries.firstOrNull()?.segments?.firstOrNull()
                            val retLast = ret.itineraries.firstOrNull()?.segments?.lastOrNull()
                            if (retFirst != null && retLast != null) {
                                Spacer(Modifier.height(8.dp))
                                Text("Return flight", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${retFirst.departure.iataCode} → ${retLast.arrival.iataCode}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "${retFirst.departure.at.formatDisplayTime()} → ${retLast.arrival.at.formatDisplayTime()}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Price breakdown
            val grandTotal = viewModel.totalPrice()
            val addOnsTotal = addOns.totalCost()
            val discount = viewModel.flightDiscount()
            val flightTotal = grandTotal - addOnsTotal
            val coins = (grandTotal * Constants.BASE_COIN_EARN_RATE * tier.coinMultiplier()).toInt()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Price Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    CheckoutPriceRow("Flight Fare", "₱${String.format("%,.0f", flightTotal)}")
                    if (addOnsTotal > 0) {
                        Spacer(Modifier.height(4.dp))
                        CheckoutPriceRow("Add-Ons", "₱${String.format("%,.0f", addOnsTotal)}")
                    }
                    if (discount > 0) {
                        Spacer(Modifier.height(4.dp))
                        CheckoutPriceRow(
                            "${tier.displayName()} Discount",
                            "-₱${String.format("%,.0f", discount)}",
                            valueColor = Color(0xFF10B981)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    CheckoutPriceRow(
                        "Total",
                        "₱${String.format("%,.0f", grandTotal)}",
                        isTotal = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "You'll earn ${String.format("%,d", coins)} Trip Coins",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Payment method
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))

                    PaymentMethodRow(
                        icon = Icons.Filled.CreditCard,
                        label = "Credit / Debit Card",
                        selected = paymentMethod == "CARD",
                        onClick = { paymentMethod = "CARD" }
                    )
                    Spacer(Modifier.height(4.dp))
                    PaymentMethodRow(
                        icon = Icons.Filled.PhoneAndroid,
                        label = "GCash",
                        selected = paymentMethod == "GCASH",
                        onClick = { paymentMethod = "GCASH" }
                    )

                    if (paymentMethod == "CARD") {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { v ->
                                val digits = v.replace(" ", "").filter { it.isDigit() }.take(16)
                                cardNumber = digits.chunked(4).joinToString(" ")
                            },
                            label = { Text("Card Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = cardExpiry,
                                onValueChange = { v ->
                                    val digits = v.replace("/", "").filter { it.isDigit() }.take(4)
                                    cardExpiry = if (digits.length >= 2) "${digits.substring(0, 2)}/${digits.substring(2)}" else digits
                                },
                                label = { Text("MM/YY") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = cardCvv,
                                onValueChange = { if (it.length <= 4) cardCvv = it.filter { c -> c.isDigit() } },
                                label = { Text("CVV") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                            )
                        }
                    } else {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = gcashNumber,
                            onValueChange = { if (it.length <= 10) gcashNumber = it.filter { c -> c.isDigit() } },
                            label = { Text("GCash Number (9xxxxxxxxx)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val valid = when (paymentMethod) {
                        "CARD" -> cardNumber.replace(" ", "").length == 16 &&
                                cardExpiry.matches(Regex("\\d{2}/\\d{2}")) &&
                                cardCvv.length >= 3
                        "GCASH" -> gcashNumber.length == 10 && gcashNumber.startsWith("9")
                        else -> true
                    }
                    if (valid) {
                        viewModel.confirmBooking()
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CtaOrange)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Confirm Booking", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CheckoutPriceRow(
    label: String,
    value: String,
    isTotal: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
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
            color = if (isTotal) CtaOrange else if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PaymentMethodRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
        )
        Icon(icon, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
