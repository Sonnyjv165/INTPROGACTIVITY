package com.example.intprogactivity.presentation.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import androidx.compose.runtime.rememberCoroutineScope
import com.example.intprogactivity.R
import com.example.intprogactivity.domain.model.Airport
import com.example.intprogactivity.domain.model.FlightSearchParams
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.CtaOrange
import com.example.intprogactivity.presentation.theme.TripFlightsTheme
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private val gson = Gson()
    private val displayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // Display dates held at fragment level so date picker can update them
    private var departDisplayDate = mutableStateOf("Select date")
    private var returnDisplayDate = mutableStateOf("Add return")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            TripFlightsTheme {
                HomeScreen(
                    viewModel = viewModel,
                    departDisplay = departDisplayDate.value,
                    returnDisplay = returnDisplayDate.value,
                    onSelectOrigin = {
                        AirportSearchBottomSheet.newInstance(
                            AirportSearchBottomSheet.MODE_ORIGIN, "Select Origin"
                        ).show(childFragmentManager, AirportSearchBottomSheet.TAG)
                    },
                    onSelectDestination = {
                        AirportSearchBottomSheet.newInstance(
                            AirportSearchBottomSheet.MODE_DESTINATION, "Select Destination"
                        ).show(childFragmentManager, AirportSearchBottomSheet.TAG)
                    },
                    onSelectDepartDate = { showDatePicker(isReturn = false) },
                    onSelectReturnDate = { if (viewModel.isRoundTrip.value) showDatePicker(isReturn = true) },
                    onSelectPassengers = {
                        PassengerPickerDialog(
                            adults = viewModel.adults.value,
                            children = viewModel.children.value,
                            infants = viewModel.infants.value
                        ) { a, c, i -> viewModel.setPassengers(a, c, i) }
                            .show(childFragmentManager, "PassengerPicker")
                    },
                    onNavigateToRewards = { findNavController().navigate(R.id.rewardsFragment) },
                    onNavigateToResults = { params ->
                        val bundle = Bundle().apply { putString("searchParamsJson", gson.toJson(params)) }
                        findNavController().navigate(R.id.action_home_to_search_results, bundle)
                    },
                    onDealClicked = { deal ->
                        viewModel.setOriginByCode(deal.originCode)
                        viewModel.setDestinationByCode(deal.destinationCode)
                    }
                )
            }
        }
    }

    private fun showDatePicker(isReturn: Boolean) {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
            .build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isReturn) "Select Return Date" else "Select Departure Date")
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { ms ->
            val date = Date(ms)
            val apiDate = apiFormat.format(date)
            val display = displayFormat.format(date)
            if (isReturn) {
                viewModel.setReturnDate(apiDate)
                returnDisplayDate.value = display
            } else {
                viewModel.setDepartDate(apiDate)
                departDisplayDate.value = display
            }
        }
        picker.show(childFragmentManager, "DatePicker")
    }

    companion object {
        val SAMPLE_DEALS = listOf(
            DealItem("Tokyo", "Manila", "₱12,000", "-25%", "", "MNL", "NRT", R.drawable.tokyo),
            DealItem("Singapore", "Manila", "₱4,999", "-30%", "", "MNL", "SIN", R.drawable.singapore),
            DealItem("Seoul", "Manila", "₱6,500", "-20%", "", "MNL", "ICN", R.drawable.tc),
            DealItem("Hong Kong", "Manila", "₱9,800", "-15%", "", "MNL", "HKG", R.drawable.hongkong),
            DealItem("Bangkok", "Manila", "₱4,500", "-35%", "", "MNL", "BKK", R.drawable.bangkok),
            DealItem("Cebu", "Manila", "₱1,899", "-10%", "", "MNL", "CEB", R.drawable.cebu),
            DealItem("Boracay", "Manila", "₱1,799", "-15%", "", "MNL", "MPH", R.drawable.boracay),
            DealItem("Dubai", "Manila", "₱22,000", "-20%", "", "MNL", "DXB", R.drawable.dubai),
        )
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    departDisplay: String,
    returnDisplay: String,
    onSelectOrigin: () -> Unit,
    onSelectDestination: () -> Unit,
    onSelectDepartDate: () -> Unit,
    onSelectReturnDate: () -> Unit,
    onSelectPassengers: () -> Unit,
    onNavigateToRewards: () -> Unit,
    onNavigateToResults: (FlightSearchParams) -> Unit,
    onDealClicked: (DealItem) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val origin by viewModel.origin.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val isRoundTrip by viewModel.isRoundTrip.collectAsStateWithLifecycle()
    val adults by viewModel.adults.collectAsStateWithLifecycle()
    val children by viewModel.children.collectAsStateWithLifecycle()
    val infants by viewModel.infants.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.navigateToSearch.collect { params ->
            onNavigateToResults(params)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(BrandPrimary, BrandPrimary.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val greeting = when {
                        currentUser != null -> {
                            val name = currentUser!!.firstName.ifBlank { currentUser!!.fullName().split(" ").first() }
                            when {
                                hour < 12 -> "Good morning, $name"
                                hour < 17 -> "Good afternoon, $name"
                                else -> "Good evening, $name"
                            }
                        }
                        else -> "Welcome to Trip.com"
                    }
                    Text(
                        text = greeting,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        text = "Where would you like to fly today?",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (currentUser != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable(onClick = onNavigateToRewards)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFFBBC05),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${currentUser!!.membershipTier.displayName()} · ${String.format("%,d", currentUser!!.loyaltyPoints)} Trip Coins",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Search Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Trip type toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppBackground),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        FilterChip(
                            selected = isRoundTrip,
                            onClick = { viewModel.setRoundTrip(true) },
                            label = { Text("Round Trip") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = !isRoundTrip,
                            onClick = { viewModel.setRoundTrip(false) },
                            label = { Text("One Way") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Origin / Destination
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AirportSelector(
                            label = "From",
                            airport = origin,
                            onClick = onSelectOrigin,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { viewModel.swapOriginDestination() },
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BrandPrimary.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                Icons.Filled.CompareArrows,
                                contentDescription = "Swap",
                                tint = BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AirportSelector(
                            label = "To",
                            airport = destination,
                            onClick = onSelectDestination,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Dates
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateSelector(
                            label = "Depart",
                            date = departDisplay,
                            onClick = onSelectDepartDate,
                            modifier = Modifier.weight(1f)
                        )
                        DateSelector(
                            label = "Return",
                            date = returnDisplay,
                            onClick = onSelectReturnDate,
                            modifier = Modifier.weight(1f),
                            dimmed = !isRoundTrip
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Passengers
                    val passengerSummary = buildPassengerSummary(adults, children, infants)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppBackground)
                            .clickable(onClick = onSelectPassengers)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.People,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Passengers & Cabin",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                passengerSummary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                val error = viewModel.onSearchClicked()
                                if (error != null) snackbarHostState.showSnackbar(error)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CtaOrange)
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Search Flights", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Flash Deals
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Flash Deals",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "See all",
                    color = BrandPrimary,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(HomeFragment.SAMPLE_DEALS) { deal ->
                    DealCard(deal = deal, onClick = { onDealClicked(deal) })
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AirportSelector(
    label: String,
    airport: Airport?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppBackground)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = airport?.iataCode ?: "---",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = if (airport != null) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = airport?.cityName ?: "Select city",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DateSelector(
    label: String,
    date: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppBackground.copy(alpha = if (dimmed) 0.5f else 1f))
            .clickable(enabled = !dimmed, onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else BrandPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                fontSize = 11.sp,
                color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = date,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DealCard(deal: DealItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            AsyncImage(
                model = deal.imageResId,
                contentDescription = deal.destination,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CtaOrange)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(deal.discount, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                deal.destination,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    deal.price,
                    color = BrandPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AirplanemodeActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "${deal.originCode}→${deal.destinationCode}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun buildPassengerSummary(adults: Int, children: Int, infants: Int): String {
    val parts = mutableListOf("$adults Adult${if (adults > 1) "s" else ""}")
    if (children > 0) parts.add("$children Child${if (children > 1) "ren" else ""}")
    if (infants > 0) parts.add("$infants Infant${if (infants > 1) "s" else ""}")
    return parts.joinToString(" · ") + " · Economy"
}
