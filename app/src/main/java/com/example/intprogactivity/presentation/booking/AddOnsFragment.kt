package com.example.intprogactivity.presentation.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddOnsFragment : Fragment() {

    private val viewModel: BookingViewModel by activityViewModels()
    private val primaryPassengerId = "1"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            TripFlightsTheme {
                AddOnsScreen(
                    viewModel = viewModel,
                    passengerId = primaryPassengerId,
                    onBack = { findNavController().navigateUp() },
                    onSelectSeat = { findNavController().navigate(R.id.action_addons_to_seat_map) },
                    onContinue = { findNavController().navigate(R.id.action_addons_to_checkout) }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val email = arguments?.getString("contactEmail") ?: ""
        val phone = arguments?.getString("contactPhone") ?: ""
        viewModel.setContactInfo(email, phone)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOnsScreen(
    viewModel: BookingViewModel,
    passengerId: String,
    onBack: () -> Unit,
    onSelectSeat: () -> Unit,
    onContinue: () -> Unit
) {
    val addOns by viewModel.addOns.collectAsStateWithLifecycle()

    var baggageKg by remember { mutableStateOf(0) }
    var mealType by remember { mutableStateOf("") }
    var insurance by remember { mutableStateOf(false) }
    val seatSelections = addOns.seatSelections.filter { it.seatNumber.isNotEmpty() }
    val seatLabel = when (seatSelections.size) {
        0    -> null
        1    -> "Seat ${seatSelections[0].seatNumber} selected"
        else -> "${seatSelections.size} seats selected (${seatSelections.joinToString { it.seatNumber }})"
    }

    val addOnsTotal = addOns.totalCost()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add-Ons") },
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
            Text(
                "Enhance your journey",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Optional add-ons to make your trip more comfortable",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Baggage
            AddOnSection(icon = Icons.Filled.Luggage, title = "Extra Baggage") {
                listOf(
                    Triple(0, "No extra baggage", "Included"),
                    Triple(20, "20 kg extra baggage", "+₱1,800"),
                    Triple(30, "30 kg extra baggage", "+₱2,800")
                ).forEach { (kg, label, price) ->
                    BaggageOption(
                        label = label,
                        price = price,
                        selected = baggageKg == kg,
                        onClick = {
                            baggageKg = kg
                            val cost = when (kg) { 20 -> 1800.0; 30 -> 2800.0; else -> 0.0 }
                            viewModel.setExtraBaggage(passengerId, kg, cost)
                        }
                    )
                    if (kg != 30) Spacer(Modifier.height(4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Meals
            AddOnSection(icon = Icons.Filled.Restaurant, title = "Meal Preference") {
                listOf(
                    Triple("", "No meal", ""),
                    Triple("REGULAR", "Regular Meal", "+₱600"),
                    Triple("VEGETARIAN", "Vegetarian Meal", "+₱600"),
                    Triple("HALAL", "Halal Meal", "+₱600")
                ).forEach { (type, label, price) ->
                    BaggageOption(
                        label = label,
                        price = price,
                        selected = mealType == type,
                        onClick = {
                            mealType = type
                            val cost = if (type.isNotEmpty()) 600.0 else 0.0
                            viewModel.setMeal(passengerId, type, cost)
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Seat selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectSeat)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AirlineSeatReclineNormal, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Seat Selection", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            seatLabel ?: "No seat selected (random assignment)",
                            fontSize = 13.sp,
                            color = if (seatLabel != null) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Insurance
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Travel Insurance", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            if (insurance) "+₱500 · Coverage included" else "Protect your trip from cancellations · +₱500",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = insurance,
                        onCheckedChange = {
                            insurance = it
                            viewModel.setInsurance(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPrimary)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Total add-ons cost
            if (addOnsTotal > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text("Add-Ons Total", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("₱${String.format("%,.0f", addOnsTotal)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CtaOrange)
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CtaOrange)
            ) {
                Text("Continue to Checkout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AddOnSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun BaggageOption(
    label: String,
    price: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (selected) Modifier.background(BrandPrimary.copy(alpha = 0.05f))
                    .border(1.dp, BrandPrimary, RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = Modifier.size(20.dp),
            colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        if (price.isNotEmpty()) {
            Text(price, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = BrandPrimary)
        }
    }
}
