package com.example.intprogactivity.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.TripFlightsTheme
import com.example.intprogactivity.util.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TripFlightsTheme {
                    ProfileScreen(
                        viewModel = viewModel,
                        onNavigateToPriceAlerts  = { findNavController().navigate(R.id.priceAlertsFragment) },
                        onNavigateToRewards      = { findNavController().navigate(R.id.rewardsFragment) },
                        onNavigateToSupport      = { findNavController().navigate(R.id.supportFragment) },
                        onNavigateToFindBooking  = { findNavController().navigate(R.id.findBookingFragment) },
                        onSignedOut              = { findNavController().navigate(R.id.action_profile_to_login) }
                    )
                }
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToPriceAlerts: () -> Unit,
    onNavigateToRewards: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToFindBooking: () -> Unit,
    onSignedOut: () -> Unit
) {
    val currentUser       by viewModel.currentUser.collectAsStateWithLifecycle()
    val signOutState      by viewModel.signOutState.collectAsStateWithLifecycle()
    val updateState       by viewModel.updateProfileState.collectAsStateWithLifecycle()
    val passwordState     by viewModel.changePasswordState.collectAsStateWithLifecycle()
    val snackbar          = remember { SnackbarHostState() }

    var showEditDialog    by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(signOutState) { if (signOutState is UiState.Success) onSignedOut() }

    LaunchedEffect(updateState) {
        when (val s = updateState) {
            is UiState.Success -> { snackbar.showSnackbar("Profile updated"); viewModel.resetUpdateState() }
            is UiState.Error   -> { snackbar.showSnackbar(s.message); viewModel.resetUpdateState() }
            else -> Unit
        }
    }

    LaunchedEffect(passwordState) {
        when (val s = passwordState) {
            is UiState.Success -> { snackbar.showSnackbar("Password changed successfully"); viewModel.resetPasswordState(); showPasswordDialog = false }
            is UiState.Error   -> { snackbar.showSnackbar(s.message); viewModel.resetPasswordState() }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandPrimary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(BrandPrimary, BrandPrimary.copy(alpha = 0.85f))))
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(currentUser?.fullName() ?: "Guest", fontWeight = FontWeight.Bold,
                        fontSize = 20.sp, color = Color.White)
                    Text(currentUser?.email ?: "", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    currentUser?.let { user ->
                        Spacer(Modifier.height(6.dp))
                        if (!user.nationality.isNullOrBlank()) {
                            Text("🌍 ${user.nationality}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Star, null, tint = Color(0xFFFBBC05), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(user.membershipTier.displayName(), color = Color.White,
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { showEditDialog = true }, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Filled.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Profile", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            // ── Stats ────────────────────────────────────────────────────────
            currentUser?.let { user ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("Bookings", user.totalBookings.toString())
                        VerticalDividerLine()
                        StatItem("Trip Coins", String.format("%,d", user.loyaltyPoints))
                        VerticalDividerLine()
                        StatItem("Spent", "₱${String.format("%,.0f", user.totalSpend)}")
                    }
                }
            }

            // ── Travel & Booking ─────────────────────────────────────────────
            MenuCard {
                ProfileMenuItem(Icons.Filled.Star, "Rewards & Membership", onClick = onNavigateToRewards)
                Divider()
                ProfileMenuItem(Icons.Filled.NotificationsActive, "Price Alerts", onClick = onNavigateToPriceAlerts)
                Divider()
                ProfileMenuItem(Icons.Filled.Search, "Find My Booking", onClick = onNavigateToFindBooking)
            }

            Spacer(Modifier.height(12.dp))

            // ── Account settings ─────────────────────────────────────────────
            MenuCard {
                ProfileMenuItem(Icons.Filled.Edit, "Edit Profile") { showEditDialog = true }
                Divider()
                ProfileMenuItem(Icons.Filled.Lock, "Change Password") { showPasswordDialog = true }
                Divider()
                ProfileMenuItem(Icons.Filled.HelpOutline, "Support Tickets", onClick = onNavigateToSupport)
            }

            Spacer(Modifier.height(12.dp))

            // ── Sign out ──────────────────────────────────────────────────────
            MenuCard {
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = "Sign Out",
                    onClick = { showSignOutDialog = true },
                    iconTint = Color(0xFFEF4444),
                    labelColor = Color(0xFFEF4444)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────
    if (showEditDialog) {
        EditProfileDialog(
            user = currentUser,
            isLoading = updateState is UiState.Loading,
            onDismiss = { showEditDialog = false },
            onSave = { firstName, lastName, middleInitial, suffix, phone, nationality, dob ->
                viewModel.updateProfile(firstName, lastName, middleInitial, suffix, phone, nationality, dob)
                showEditDialog = false
            }
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            isLoading = passwordState is UiState.Loading,
            onDismiss = { showPasswordDialog = false; viewModel.resetPasswordState() },
            onSave    = { current, newPw -> viewModel.changePassword(current, newPw) }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text  = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = { showSignOutDialog = false; viewModel.signOut() }) {
                    Text("Sign Out", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Edit Profile Dialog ───────────────────────────────────────────────────────
@Composable
private fun EditProfileDialog(
    user: User?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    // matches web schema: firstName, lastName, middleInitial, suffix, phone, nationality, dob
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    var firstName     by remember { mutableStateOf(user?.firstName ?: "") }
    var lastName      by remember { mutableStateOf(user?.lastName ?: "") }
    var middleInitial by remember { mutableStateOf(user?.middleInitial ?: "") }
    var suffix        by remember { mutableStateOf(user?.suffix ?: "") }
    var phone         by remember { mutableStateOf(user?.phone ?: "") }
    var nationality   by remember { mutableStateOf(user?.nationality ?: "") }
    var dob           by remember { mutableStateOf(user?.dob ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(16.dp))

                // Name row: First + Last
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileTextField(
                        label = "First Name", value = firstName,
                        icon = Icons.Filled.Person, modifier = Modifier.weight(1f)
                    ) { firstName = it }
                    ProfileTextField(
                        label = "Last Name", value = lastName,
                        icon = null, modifier = Modifier.weight(1f)
                    ) { lastName = it }
                }
                Spacer(Modifier.height(10.dp))

                // Middle initial + suffix row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileTextField(
                        label = "Middle Initial", value = middleInitial,
                        icon = null, modifier = Modifier.weight(1f)
                    ) { if (it.length <= 1) middleInitial = it }
                    ProfileTextField(
                        label = "Suffix (e.g. Jr.)", value = suffix,
                        icon = null, modifier = Modifier.weight(1f)
                    ) { suffix = it }
                }
                Spacer(Modifier.height(10.dp))

                ProfileTextField("Phone Number", phone, Icons.Filled.Phone,
                    keyboardType = KeyboardType.Phone) { phone = it }
                Spacer(Modifier.height(10.dp))
                ProfileTextField("Nationality", nationality, Icons.Filled.Public) { nationality = it }
                Spacer(Modifier.height(10.dp))
                ProfileTextField("Date of Birth (yyyy-MM-dd)", dob, Icons.Filled.CalendarMonth,
                    keyboardType = KeyboardType.Number) { dob = it }

                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(firstName.trim(), lastName.trim(),
                            middleInitial.trim(), suffix.trim(),
                            phone.trim(), nationality.trim(), dob.trim()) },
                        enabled = firstName.isNotBlank() && lastName.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White,
                            strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        else Text("Save")
                    }
                }
            }
        }
    }
}

// ── Change Password Dialog ────────────────────────────────────────────────────
@Composable
private fun ChangePasswordDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var current  by remember { mutableStateOf("") }
    var newPw    by remember { mutableStateOf("") }
    var confirm  by remember { mutableStateOf("") }
    var showCurr by remember { mutableStateOf(false) }
    var showNew  by remember { mutableStateOf(false) }
    val mismatch = confirm.isNotBlank() && newPw != confirm

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Change Password", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = current, onValueChange = { current = it },
                    label = { Text("Current Password") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    visualTransformation = if (showCurr) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurr = !showCurr }) {
                            Icon(if (showCurr) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = newPw, onValueChange = { newPw = it },
                    label = { Text("New Password") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNew = !showNew }) {
                            Icon(if (showNew) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirm, onValueChange = { confirm = it },
                    label = { Text("Confirm New Password") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    isError = mismatch,
                    supportingText = { if (mismatch) Text("Passwords do not match", color = MaterialTheme.colorScheme.error) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(current, newPw) },
                        enabled = current.isNotBlank() && newPw.length >= 6 && newPw == confirm && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White,
                            strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        else Text("Change")
                    }
                }
            }
        }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────
@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) }, singleLine = true,
        modifier = modifier, shape = RoundedCornerShape(8.dp),
        leadingIcon = icon?.let { { Icon(it, null, modifier = Modifier.size(20.dp)) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
private fun MenuCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) { Column(content = content) }
}

@Composable
private fun Divider() = HorizontalDivider(
    modifier = Modifier.padding(horizontal = 16.dp),
    color = MaterialTheme.colorScheme.outlineVariant
)

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandPrimary)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VerticalDividerLine() {
    Box(modifier = Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector, label: String,
    iconTint: Color = BrandPrimary, labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit   // last so trailing-lambda syntax works
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), color = labelColor,
            fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Icon(Icons.Filled.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}
