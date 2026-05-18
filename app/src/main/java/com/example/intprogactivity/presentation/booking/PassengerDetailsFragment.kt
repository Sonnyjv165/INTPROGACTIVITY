package com.example.intprogactivity.presentation.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    var contactPhone by remember { mutableStateOf("") }

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
            // Passenger forms
            passengerForms.forEachIndexed { index, form ->
                val typeLabel = when (form.type) {
                    PassengerType.ADULT -> "Adult"
                    PassengerType.CHILD -> "Child"
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

                        OutlinedTextField(
                            value = form.dob,
                            onValueChange = { passengerForms[index] = form.copy(dob = it) },
                            label = { Text("Date of Birth (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
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

            // Contact info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Contact Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandPrimary)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = {
                            contactEmail = it
                            emailError = null
                        },
                        label = { Text("Email Address") },
                        singleLine = true,
                        isError = emailError != null,
                        supportingText = emailError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        placeholder = { Text("+63 9xx xxx xxxx") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        )
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
                        ) {
                            valid = false
                        }
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(contactEmail).matches()) {
                        emailError = "Enter a valid email"
                        valid = false
                    }
                    if (valid) {
                        onContinue(passengerForms.toList(), contactEmail.trim(), contactPhone.trim())
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Please fill in all required fields") }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CtaOrange)
            ) {
                Text("Continue to Add-Ons", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
