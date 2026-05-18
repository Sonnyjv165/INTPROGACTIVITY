package com.example.intprogactivity.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.CtaOrange
import com.example.intprogactivity.presentation.theme.TripFlightsTheme
import com.example.intprogactivity.util.UiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            TripFlightsTheme {
                ProfileScreen(
                    viewModel = viewModel,
                    onEditProfile = { showEditProfileDialog() },
                    onNavigateToPriceAlerts = { findNavController().navigate(R.id.priceAlertsFragment) },
                    onNavigateToRewards = { findNavController().navigate(R.id.rewardsFragment) },
                    onSignOut = { showSignOutDialog() },
                    onSignedOut = { findNavController().navigate(R.id.action_profile_to_login) }
                )
            }
        }
    }

    private fun showEditProfileDialog() {
        val user = viewModel.currentUser.value ?: return
        val nameInput = TextInputEditText(requireContext()).apply { setText(user.displayName) }
        val phoneInput = TextInputEditText(requireContext()).apply { setText(user.phone) }
        val nameLayout = TextInputLayout(requireContext()).apply { hint = "Display Name"; addView(nameInput) }
        val phoneLayout = TextInputLayout(requireContext()).apply { hint = "Phone Number"; addView(phoneInput) }
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(nameLayout)
            addView(phoneLayout.also { it.setPadding(0, 24, 0, 0) })
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Profile")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text?.toString()?.trim() ?: ""
                val phone = phoneInput.text?.toString()?.trim() ?: ""
                if (name.isNotBlank()) viewModel.updateProfile(name, phone)
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    private fun showSignOutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ -> viewModel.signOut() }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onEditProfile: () -> Unit,
    onNavigateToPriceAlerts: () -> Unit,
    onNavigateToRewards: () -> Unit,
    onSignOut: () -> Unit,
    onSignedOut: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val signOutState by viewModel.signOutState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateProfileState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(signOutState) {
        if (signOutState is UiState.Success) onSignedOut()
    }

    LaunchedEffect(updateState) {
        when (val s = updateState) {
            is UiState.Success -> {
                viewModel.resetUpdateState()
                snackbarHostState.showSnackbar("Profile updated")
            }
            is UiState.Error -> {
                viewModel.resetUpdateState()
                snackbarHostState.showSnackbar(s.message)
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandPrimary,
                    titleContentColor = Color.White
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
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(BrandPrimary, BrandPrimary.copy(alpha = 0.85f)))
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        currentUser?.displayName ?: "Guest",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text(
                        currentUser?.email ?: "",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(12.dp))
                    currentUser?.let { user ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFBBC05), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(user.membershipTier.displayName(), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onEditProfile,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Profile", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            // Stats
            currentUser?.let { user ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = "Bookings", value = user.totalBookings.toString())
                        VerticalDividerLine()
                        StatItem(label = "Trip Coins", value = String.format("%,d", user.tripCoins))
                        VerticalDividerLine()
                        StatItem(label = "Total Spend", value = "₱${String.format("%,.0f", user.totalSpend)}")
                    }
                }
            }

            // Menu
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    ProfileMenuItem(
                        icon = Icons.Filled.Star,
                        label = "Rewards & Membership",
                        onClick = onNavigateToRewards
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileMenuItem(
                        icon = Icons.Filled.NotificationsActive,
                        label = "Price Alerts",
                        onClick = onNavigateToPriceAlerts
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileMenuItem(
                        icon = Icons.Filled.Edit,
                        label = "Edit Profile",
                        onClick = onEditProfile
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = "Sign Out",
                    onClick = onSignOut,
                    iconTint = Color(0xFFEF4444),
                    labelColor = Color(0xFFEF4444)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandPrimary)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VerticalDividerLine() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = BrandPrimary,
    labelColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), color = labelColor, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

