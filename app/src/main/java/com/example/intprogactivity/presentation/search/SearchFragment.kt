package com.example.intprogactivity.presentation.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.domain.model.Airport
import com.example.intprogactivity.domain.model.FlightSearchParams
import com.example.intprogactivity.presentation.home.AirportSearchBottomSheet
import com.example.intprogactivity.presentation.home.HomeViewModel
import com.example.intprogactivity.presentation.home.PassengerPickerDialog
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
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.runtime.mutableStateOf

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private val gson = Gson()
    private val displayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

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
                SearchScreen(
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
                    onNavigateToResults = { params ->
                        val bundle = Bundle().apply { putString("searchParamsJson", gson.toJson(params)) }
                        findNavController().navigate(R.id.action_search_to_results, bundle)
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
}

@Composable
fun SearchScreen(
    viewModel: HomeViewModel,
    departDisplay: String,
    returnDisplay: String,
    onSelectOrigin: () -> Unit,
    onSelectDestination: () -> Unit,
    onSelectDepartDate: () -> Unit,
    onSelectReturnDate: () -> Unit,
    onSelectPassengers: () -> Unit,
    onNavigateToResults: (FlightSearchParams) -> Unit
) {
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

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                "Search Flights",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Find the best fares for your trip",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Trip type
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(4.dp)
            ) {
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
            }

            Spacer(Modifier.height(12.dp))

            // Origin / Destination row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchAirportField(
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
                SearchAirportField(
                    label = "To",
                    airport = destination,
                    onClick = onSelectDestination,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Dates
            Row(modifier = Modifier.fillMaxWidth()) {
                SearchDateField(
                    label = "Departure",
                    date = departDisplay,
                    onClick = onSelectDepartDate,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                SearchDateField(
                    label = "Return",
                    date = returnDisplay,
                    onClick = onSelectReturnDate,
                    modifier = Modifier.weight(1f),
                    dimmed = !isRoundTrip
                )
            }

            Spacer(Modifier.height(12.dp))

            // Passengers
            val paxSummary = buildSearchPaxSummary(adults, children, infants)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .clickable(onClick = onSelectPassengers)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.People, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Passengers & Cabin", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(paxSummary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        val error = viewModel.onSearchClicked()
                        if (error != null) snackbarHostState.showSnackbar(error)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CtaOrange)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Search Flights", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SearchAirportField(
    label: String,
    airport: Airport?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
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
private fun SearchDateField(
    label: String,
    date: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (dimmed) Color.White.copy(alpha = 0.5f) else Color.White)
            .clickable(enabled = !dimmed, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.CalendarMonth,
            contentDescription = null,
            tint = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else BrandPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (dimmed) 0.4f else 1f
                )
            )
            Text(
                date,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dimmed) 0.4f else 1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildSearchPaxSummary(adults: Int, children: Int, infants: Int): String {
    val parts = mutableListOf("$adults Adult${if (adults > 1) "s" else ""}")
    if (children > 0) parts.add("$children Child${if (children > 1) "ren" else ""}")
    if (infants > 0) parts.add("$infants Infant${if (infants > 1) "s" else ""}")
    return parts.joinToString(" · ") + " · Economy"
}
