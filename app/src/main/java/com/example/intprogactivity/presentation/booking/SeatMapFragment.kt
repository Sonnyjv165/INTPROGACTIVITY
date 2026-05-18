package com.example.intprogactivity.presentation.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.CtaOrange
import com.example.intprogactivity.presentation.theme.Divider
import com.example.intprogactivity.presentation.theme.TripFlightsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SeatMapFragment : Fragment() {

    private val viewModel: BookingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            TripFlightsTheme {
                SeatMapScreen(
                    viewModel = viewModel,
                    onBack = { findNavController().navigateUp() },
                    onDone = { findNavController().navigateUp() }
                )
            }
        }
    }
}

private val TAKEN_SEATS = setOf(
    "2A","2C","3B","5E","5F","7A","8D","9B","10F","11C",
    "12A","13E","14B","15D","16F","17A","18C","19B","20E",
    "21A","22F","23C","24D","25B","26E","27A","28C","29F","30D"
)

private val COLUMNS = listOf("A","B","C","D","E","F")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatMapScreen(
    viewModel: BookingViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val passengers by viewModel.passengers.collectAsStateWithLifecycle()
    var currentPassengerIndex by remember { mutableStateOf(0) }
    var selectedSeat by remember { mutableStateOf<String?>(null) }

    val passengerLabel = if (passengers.isNotEmpty()) {
        val p = passengers.getOrNull(currentPassengerIndex)
        val name = if (p?.firstName?.isNotBlank() == true) p.firstName else "Passenger ${currentPassengerIndex + 1}"
        val type = p?.type?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""
        "Seat for $name ($type) · ${currentPassengerIndex + 1} of ${passengers.size}"
    } else {
        "Select your seat"
    }

    // Generate seat items (7 columns: A B C [aisle] D E F)
    val seatItems = remember {
        buildList {
            for (row in 1..30) {
                for (colIdx in 0..6) {
                    if (colIdx == 3) {
                        add(SeatData("", SeatType.AISLE))
                    } else {
                        val actualCol = if (colIdx < 3) colIdx else colIdx - 1
                        val label = "$row${COLUMNS[actualCol]}"
                        add(SeatData(label, if (label in TAKEN_SEATS) SeatType.TAKEN else SeatType.AVAILABLE))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Seat") },
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
                .padding(padding)
        ) {
            // Passenger label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(passengerLabel, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(color = Color.White, borderColor = Divider, label = "Available")
                LegendItem(color = BrandPrimary, borderColor = BrandPrimary, label = "Selected", textColor = Color.White)
                LegendItem(color = Divider, borderColor = Divider, label = "Taken")
            }

            Spacer(Modifier.height(8.dp))

            // Seat grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                items(seatItems) { seat ->
                    SeatCell(
                        seat = seat,
                        isSelected = seat.label == selectedSeat,
                        onClick = {
                            if (seat.type == SeatType.AVAILABLE) {
                                selectedSeat = if (selectedSeat == seat.label) null else seat.label
                            }
                        }
                    )
                }
            }

            // Bottom bar
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (selectedSeat != null) {
                    Text(
                        "Selected: $selectedSeat",
                        fontWeight = FontWeight.SemiBold,
                        color = BrandPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Button(
                    onClick = {
                        if (selectedSeat != null) {
                            viewModel.setSeatSelection(currentPassengerIndex, selectedSeat!!)
                        }
                        currentPassengerIndex++
                        val total = passengers.size
                        if (currentPassengerIndex < total) {
                            selectedSeat = viewModel.getSeatForPassenger(currentPassengerIndex)
                        } else {
                            onDone()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CtaOrange)
                ) {
                    val total = passengers.size
                    Text(
                        if (currentPassengerIndex < total - 1) "Next Passenger" else "Confirm Seats",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class SeatData(val label: String, val type: SeatType)
enum class SeatType { AVAILABLE, TAKEN, AISLE }

@Composable
private fun SeatCell(seat: SeatData, isSelected: Boolean, onClick: () -> Unit) {
    if (seat.type == SeatType.AISLE) {
        Box(Modifier.aspectRatio(1f))
        return
    }

    val bgColor = when {
        isSelected -> BrandPrimary
        seat.type == SeatType.TAKEN -> Divider
        else -> Color.White
    }
    val textColor = when {
        isSelected -> Color.White
        seat.type == SeatType.TAKEN -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .then(
                if (!isSelected && seat.type != SeatType.TAKEN)
                    Modifier.border(1.dp, Divider, RoundedCornerShape(4.dp))
                else Modifier
            )
            .clickable(enabled = seat.type == SeatType.AVAILABLE, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = seat.label,
            fontSize = 8.sp,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    borderColor: Color,
    label: String,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
                .border(1.dp, borderColor, RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = textColor)
    }
}
