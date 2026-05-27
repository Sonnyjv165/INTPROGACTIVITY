package com.example.intprogactivity.presentation.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.model.PassengerType
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.CtaOrange
import com.example.intprogactivity.presentation.theme.TripFlightsTheme
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class PassengerDetailsFragment : Fragment() {

    private val viewModel: BookingViewModel by activityViewModels()
    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val offerJson = arguments?.getString("flightOfferJson") ?: ""
        val offer = gson.fromJson(offerJson, FlightOffer::class.java)
        val adults = arguments?.getInt("adults", 1) ?: 1
        val children = arguments?.getInt("children", 0) ?: 0
        val infants = arguments?.getInt("infants", 0) ?: 0

        viewModel.resetForNewBooking()
        viewModel.setFlightOffer(offer)
        viewModel.initPassengers(adults, children, infants)

        val returnJson = arguments?.getString("returnFlightJson") ?: ""
        if (returnJson.isNotEmpty()) {
            runCatching { gson.fromJson(returnJson, FlightOffer::class.java) }
                .onSuccess { viewModel.setReturnFlightOffer(it) }
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TripFlightsTheme {
                    PassengerDetailsScreen(
                        adults = adults,
                        children = children,
                        infants = infants,
                        onBack = { findNavController().navigateUp() },
                        onContinue = { passengers, email, phone ->
                            passengers.forEachIndexed { index, p ->
                                viewModel.setPassenger(
                                    index, p.firstName, p.lastName, p.dob, p.gender, p.passport, p.nationality
                                )
                            }
                            viewModel.setContactInfo(email, phone)
                            findNavController().navigate(
                                R.id.action_passenger_to_addons,
                                Bundle().apply {
                                    putString("contactEmail", email)
                                    putString("contactPhone", phone)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

data class PassengerFormState(
    var firstName: String = "",
    var lastName: String = "",
    var dob: String = "",
    var passport: String = "",
    var nationality: String = "",
    var gender: String = "MALE",
    val type: PassengerType = PassengerType.ADULT
)

// ── Phone number field with +63 country-code prefix ──────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneInputField(
    digits: String,              // raw digit-only string, 10 chars max
    onDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Format "9171234567" → "917 123 4567"  (groups: 3 – 3 – 4)
    val formatted = buildString {
        digits.forEachIndexed { i, c ->
            if (i == 3 || i == 6) append(' ')
            append(c)
        }
    }

    OutlinedTextField(
        value = formatted,
        onValueChange = { input ->
            // Strip spaces/non-digits, cap at 10 digits
            val raw = input.filter { it.isDigit() }.take(10)
            onDigitsChange(raw)
        },
        label = { Text("Phone Number") },
        prefix = {
            // "+63 │ " prefix that can't be edited
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    "+63",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
                Spacer(Modifier.width(8.dp))
            }
        },
        placeholder = { Text("9XX XXX XXXX") },
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        )
    )
}

// ── Date-of-Birth picker field ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DobPickerField(
    dob: String,
    passengerType: PassengerType,
    onDobSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Compute sensible default year based on passenger type
    val defaultMillis = remember(passengerType) {
        val cal = Calendar.getInstance()
        when (passengerType) {
            PassengerType.ADULT  -> cal.add(Calendar.YEAR, -25)
            PassengerType.CHILD  -> cal.add(Calendar.YEAR, -8)
            PassengerType.INFANT -> cal.add(Calendar.YEAR, -1)
        }
        cal.timeInMillis
    }

    val initialMillis = remember(dob) {
        if (dob.isNotBlank()) {
            try { fmt.parse(dob)?.time ?: defaultMillis }
            catch (_: Exception) { defaultMillis }
        } else defaultMillis
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        // Restrict selectable range: no future dates
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                utcTimeMillis <= System.currentTimeMillis()
        }
    )

    // Tappable read-only field
    Box(modifier = modifier) {
        OutlinedTextField(
            value = dob.ifBlank { "" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Date of Birth") },
            placeholder = { Text("Tap to select") },
            trailingIcon = {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = "Pick date",
                    tint = BrandPrimary,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTrailingIconColor = BrandPrimary,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            enabled = false  // prevents keyboard, field looks normal via custom colors
        )
        // Invisible overlay to capture taps (enabled=false blocks clicks on TextField itself)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showPicker = true }
        )
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDobSelected(fmt.format(Date(millis)))
                        }
                        showPicker = false
                    }
                ) { Text("OK", color = BrandPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true,   // lets user switch to manual-type mode
                title = {
                    Text(
                        "Select Date of Birth",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                headline = {
                    DatePickerDefaults.DatePickerHeadline(
                        selectedDateMillis = datePickerState.selectedDateMillis,
                        displayMode = datePickerState.displayMode,
                        dateFormatter = DatePickerDefaults.dateFormatter(),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            )
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerDetailsScreen(
    adults: Int,
    children: Int,
    infants: Int,
    onBack: () -> Unit,
    onContinue: (List<PassengerFormState>, String, String) -> Unit
) {
    val passengerTypes = buildList {
        repeat(adults) { add(PassengerType.ADULT) }
        repeat(children) { add(PassengerType.CHILD) }
        repeat(infants) { add(PassengerType.INFANT) }
    }

    val passengerForms = remember {
        mutableStateListOf(*Array(passengerTypes.size) {
            PassengerFormState(type = passengerTypes[it])
        })
    }

    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }   // raw digits only, no country code
    var emailError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Passenger Details") },
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
            // ── Passenger cards ──────────────────────────────────────────────
            passengerForms.forEachIndexed { index, form ->
                val typeLabel = when (form.type) {
                    PassengerType.ADULT  -> "Adult"
                    PassengerType.CHILD  -> "Child"
                    PassengerType.INFANT -> "Infant"
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Passenger ${index + 1} ($typeLabel)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandPrimary
                        )
                        Spacer(Modifier.height(12.dp))

                        // First / Last name
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = form.firstName,
                                onValueChange = { passengerForms[index] = form.copy(firstName = it) },
                                label = { Text("First Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = form.lastName,
                                onValueChange = { passengerForms[index] = form.copy(lastName = it) },
                                label = { Text("Last Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // ── Date of Birth — calendar picker ──────────────────
                        DobPickerField(
                            dob = form.dob,
                            passengerType = form.type,
                            onDobSelected = { passengerForms[index] = form.copy(dob = it) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = form.passport,
                            onValueChange = { passengerForms[index] = form.copy(passport = it) },
                            label = { Text("Passport Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = form.nationality,
                            onValueChange = { passengerForms[index] = form.copy(nationality = it) },
                            label = { Text("Nationality (e.g. PH)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )

                        Spacer(Modifier.height(8.dp))

                        Text("Gender", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row {
                            FilterChip(
                                selected = form.gender == "MALE",
                                onClick = { passengerForms[index] = form.copy(gender = "MALE") },
                                label = { Text("Male") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = form.gender == "FEMALE",
                                onClick = { passengerForms[index] = form.copy(gender = "FEMALE") },
                                label = { Text("Female") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Contact info ─────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Contact Information",
                        fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandPrimary
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it; emailError = null },
                        label = { Text("Email Address") },
                        singleLine = true,
                        isError = emailError != null,
                        supportingText = emailError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email, imeAction = ImeAction.Next
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    PhoneInputField(
                        digits = contactPhone,
                        onDigitsChange = { contactPhone = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    var valid = true
                    passengerForms.forEach { form ->
                        if (form.firstName.isBlank() || form.lastName.isBlank() ||
                            form.dob.isBlank() || form.passport.isBlank() || form.nationality.isBlank()
                        ) valid = false
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(contactEmail).matches()) {
                        emailError = "Enter a valid email"
                        valid = false
                    }
                    if (valid) {
                        // Store full international number: "+63" + 10-digit local number
                        val fullPhone = if (contactPhone.length == 10) "+63$contactPhone"
                                        else contactPhone
                        onContinue(passengerForms.toList(), contactEmail.trim(), fullPhone)
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Please fill in all required fields") }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CtaOrange)
            ) {
                Text("Continue to Add-Ons", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
