package com.example.intprogactivity.presentation.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
    val flightOffer        by viewModel.flightOffer.collectAsStateWithLifecycle()
    val returnFlightOffer  by viewModel.returnFlightOffer.collectAsStateWithLifecycle()
    val addOns             by viewModel.addOns.collectAsStateWithLifecycle()
    val bookingState       by viewModel.bookingState.collectAsStateWithLifecycle()
    val tier               by viewModel.currentUserTier.collectAsStateWithLifecycle()
    val appliedPromo       by viewModel.appliedPromo.collectAsStateWithLifecycle()
    val promoState         by viewModel.promoState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var paymentMethod by remember { mutableStateOf("CARD") }
    var cardNumber    by remember { mutableStateOf("") }
    var cardExpiry    by remember { mutableStateOf("") }
    var cardCvv       by remember { mutableStateOf("") }
    var gcashNumber   by remember { mutableStateOf("") }
    var mayaNumber    by remember { mutableStateOf("") }
    var promoCode     by remember { mutableStateOf("") }

    val isLoading = bookingState is UiState.Loading

    // Handle promo feedback
    LaunchedEffect(promoState) {
        when (val s = promoState) {
            is UiState.Success -> {
                val p = s.data
                val msg = if (p.type == "percentage")
                    "✅ Promo applied — ${p.value.toInt()}% off (−₱${String.format("%,.0f", p.discountAmount)})"
                else
                    "✅ Promo applied — −₱${String.format("%,.0f", p.discountAmount)} off"
                snackbarHostState.showSnackbar(msg)
                viewModel.resetPromoState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetPromoState()
            }
            else -> Unit
        }
    }

    // Handle booking result
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

            // ── Flight summary ──────────────────────────────────────────────
            flightOffer?.let { offer ->
                val firstSeg = offer.itineraries.firstOrNull()?.segments?.firstOrNull()
                val lastSeg  = offer.itineraries.firstOrNull()?.segments?.lastOrNull()
                SummaryCard {
                    Text("Your Flight", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    if (firstSeg != null && lastSeg != null) {
                        Text("${firstSeg.departure.iataCode} → ${lastSeg.arrival.iataCode}",
                            fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("${firstSeg.departure.at.formatDisplayTime()} → ${lastSeg.arrival.at.formatDisplayTime()}",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    returnFlightOffer?.let { ret ->
                        val r1 = ret.itineraries.firstOrNull()?.segments?.firstOrNull()
                        val r2 = ret.itineraries.firstOrNull()?.segments?.lastOrNull()
                        if (r1 != null && r2 != null) {
                            Spacer(Modifier.height(8.dp))
                            Text("Return flight", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${r1.departure.iataCode} → ${r2.arrival.iataCode}",
                                fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("${r1.departure.at.formatDisplayTime()} → ${r2.arrival.at.formatDisplayTime()}",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Price breakdown ─────────────────────────────────────────────
            val grandTotal    = viewModel.totalPrice()
            val addOnsTotal   = addOns.totalCost()
            val tierDiscount  = viewModel.flightDiscount()
            val promoDiscount = viewModel.promoDiscount()
            val flightTotal   = grandTotal - addOnsTotal + tierDiscount + promoDiscount
            val coins = (grandTotal * Constants.BASE_COIN_EARN_RATE * tier.coinMultiplier()).toInt()

            SummaryCard {
                Text("Price Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                PriceRow("Flight Fare", "₱${String.format("%,.0f", flightTotal)}")
                if (addOnsTotal > 0) {
                    Spacer(Modifier.height(4.dp))
                    PriceRow("Add-Ons", "₱${String.format("%,.0f", addOnsTotal)}")
                }
                if (tierDiscount > 0) {
                    Spacer(Modifier.height(4.dp))
                    PriceRow("${tier.displayName()} Discount",
                        "−₱${String.format("%,.0f", tierDiscount)}", Color(0xFF10B981))
                }
                if (promoDiscount > 0) {
                    Spacer(Modifier.height(4.dp))
                    PriceRow("Promo (${appliedPromo?.code})",
                        "−₱${String.format("%,.0f", promoDiscount)}", Color(0xFF10B981))
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                PriceRow("Total", "₱${String.format("%,.0f", grandTotal)}", isTotal = true)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MonetizationOn, null,
                        tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("You'll earn ${String.format("%,d", coins)} Trip Coins",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Promo code ──────────────────────────────────────────────────
            SummaryCard {
                Text("Promo Code", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                if (appliedPromo != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null,
                                tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(appliedPromo!!.code, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        TextButton(onClick = { viewModel.removePromoCode(); promoCode = "" }) {
                            Text("Remove", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = promoCode,
                            onValueChange = { promoCode = it.uppercase() },
                            label = { Text("Enter promo code") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.applyPromoCode(promoCode) },
                            enabled = promoCode.isNotBlank() && promoState !is UiState.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (promoState is UiState.Loading)
                                CircularProgressIndicator(color = Color.White,
                                    strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            else Text("Apply")
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Payment method ──────────────────────────────────────────────
            SummaryCard {
                Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))

                PaymentMethodRow(Icons.Filled.CreditCard, "Credit / Debit Card",
                    paymentMethod == "CARD") { paymentMethod = "CARD" }
                Spacer(Modifier.height(4.dp))
                PaymentMethodRow(Icons.Filled.PhoneAndroid, "GCash",
                    paymentMethod == "GCASH") { paymentMethod = "GCASH" }
                Spacer(Modifier.height(4.dp))
                PaymentMethodRow(Icons.Filled.AccountBalanceWallet, "Maya",
                    paymentMethod == "MAYA") { paymentMethod = "MAYA" }

                when (paymentMethod) {
                    "CARD" -> {
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
                                    cardExpiry = if (digits.length >= 2)
                                        "${digits.substring(0, 2)}/${digits.substring(2)}" else digits
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
                    }
                    "GCASH" -> {
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
                    "MAYA" -> {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = mayaNumber,
                            onValueChange = { if (it.length <= 10) mayaNumber = it.filter { c -> c.isDigit() } },
                            label = { Text("Maya Number (9xxxxxxxxx)") },
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
                        "CARD"  -> cardNumber.replace(" ", "").length == 16 &&
                                   cardExpiry.matches(Regex("\\d{2}/\\d{2}")) &&
                                   cardCvv.length >= 3
                        "GCASH" -> gcashNumber.length == 10 && gcashNumber.startsWith("9")
                        "MAYA"  -> mayaNumber.length == 10  && mayaNumber.startsWith("9")
                        else    -> true
                    }
                    if (valid) viewModel.confirmBooking()
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CtaOrange)
            ) {
                if (isLoading)
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else
                    Text("Confirm Booking", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) { Column(modifier = Modifier.padding(16.dp), content = content) }
}

@Composable
private fun PriceRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    isTotal: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label,
            fontSize = if (isTotal) 15.sp else 13.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value,
            fontSize = if (isTotal) 15.sp else 13.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isTotal -> CtaOrange
                valueColor != Color.Unspecified -> valueColor
                else -> MaterialTheme.colorScheme.onSurface
            })
    }
}

@Composable
private fun PaymentMethodRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary))
        Icon(icon, null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
