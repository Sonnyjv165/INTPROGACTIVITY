package com.example.intprogactivity.presentation.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.domain.model.MembershipTier
import com.example.intprogactivity.domain.model.TransactionType
import com.example.intprogactivity.domain.model.TripCoinTransaction
import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.TripFlightsTheme
import com.example.intprogactivity.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class RewardsFragment : Fragment() {

    private val viewModel: RewardsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            TripFlightsTheme {
                RewardsScreen(
                    viewModel = viewModel,
                    onBack = { findNavController().navigateUp() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val coinHistoryState by viewModel.coinHistoryState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rewards & Membership") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val user = currentUser
            if (user != null) {
                item { CoinBalanceCard(user) }
                item { TierProgressCard(user) }
                item {
                    Text(
                        "Membership Tiers",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                val tiers = listOf(
                    MembershipTier.SILVER,
                    MembershipTier.GOLD,
                    MembershipTier.PLATINUM,
                    MembershipTier.DIAMOND,
                    MembershipTier.DIAMOND_PLUS,
                    MembershipTier.BLACK_DIAMOND
                )
                items(tiers) { tier ->
                    TierCard(tier = tier, currentTier = user.membershipTier)
                }
            }

            if (coinHistoryState is UiState.Success) {
                val history = (coinHistoryState as UiState.Success<List<TripCoinTransaction>>).data
                if (history.isNotEmpty()) {
                    item {
                        Text(
                            "Coin History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(history, key = { it.transactionId }) { tx ->
                        CoinTransactionItem(tx)
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun CoinBalanceCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandPrimary),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = Color(0xFFFBBC05), modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Trip Coins Balance", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                Text(
                    String.format("%,d", user.loyaltyPoints),
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Color.White
                )
                Text(
                    "${user.membershipTier.coinMultiplier()}× multiplier · ${user.membershipTier.displayName()}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun TierProgressCard(user: User) {
    val tier      = user.membershipTier
    val confirmed = user.confirmedBookings   // only CONFIRMED bookings count toward Gold
    val bookings  = user.totalBookings       // used for display/other tiers
    val spend     = user.totalSpend

    val (nextTierName, progress, hint) = when (tier) {
        MembershipTier.SILVER -> Triple(
            "Gold",
            minOf((confirmed / 5.0f), 1f),
            if (confirmed >= 5) "Tier upgrade pending"
            else "${confirmed}/5 confirmed bookings — ${5 - confirmed} more to reach Gold"
        )
        MembershipTier.GOLD -> {
            val p = minOf(((confirmed / 10.0f) + (spend.toFloat() / 50_000.0f)) / 2f, 1f)
            Triple(
                "Platinum",
                p,
                "Need 10 confirmed bookings ($confirmed/10) + ₱50,000 spend (₱${String.format("%,.0f", spend)}/₱50,000)"
            )
        }
        MembershipTier.PLATINUM -> {
            val p = minOf(((confirmed / 25.0f) + (spend.toFloat() / 500_000.0f)) / 2f, 1f)
            Triple("Diamond", p, "Need 25 confirmed bookings ($confirmed/25) + ₱500,000 spend for Diamond")
        }
        MembershipTier.DIAMOND -> {
            val p = minOf((spend.toFloat() / 2_000_000.0f), 1f)
            Triple("Diamond+", p, "Need ₱2,000,000 annual spend for Diamond+")
        }
        MembershipTier.DIAMOND_PLUS -> Triple("Black Diamond", 1f, "Diamond+ — invite-only above")
        MembershipTier.BLACK_DIAMOND -> Triple("MAX TIER", 1f, "You've reached the highest tier!")
        MembershipTier.GUEST -> Triple("Silver", 0f, "Register to start earning Trip Coins")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(tier.displayName(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = tierColor(tier))
                Text(nextTierName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = tierColor(tier),
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TierCard(tier: MembershipTier, currentTier: MembershipTier) {
    val isCurrent = tier == currentTier
    val isLocked = tier.ordinal > currentTier.ordinal
    val color = tierColor(tier)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isLocked) Modifier else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(if (isCurrent) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isLocked) Modifier.background(Color.White.copy(alpha = 0.6f)) else Modifier)
                .padding(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(if (isLocked) color.copy(alpha = 0.3f) else color)
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tier.displayName(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isLocked) color.copy(alpha = 0.5f) else color
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("Current", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(Icons.Filled.Star, contentDescription = null, tint = if (isLocked) Color.LightGray else color, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${tier.coinMultiplier()}×", fontSize = 13.sp, color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    tier.discountLabel(),
                    fontSize = 12.sp,
                    color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tier.benefits().isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))
                    tier.benefits().forEach { benefit ->
                        Text(
                            "· $benefit",
                            fontSize = 12.sp,
                            color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoinTransactionItem(tx: TripCoinTransaction) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val isEarned = tx.type == TransactionType.EARNED || tx.type == TransactionType.BONUS
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.description, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(
                    dateFormat.format(Date(tx.createdAt)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${if (isEarned) "+" else "-"}${String.format("%,d", tx.amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isEarned) Color(0xFF10B981) else Color(0xFFEF4444)
            )
        }
    }
}

private fun tierColor(tier: MembershipTier): Color = when (tier) {
    MembershipTier.GUEST, MembershipTier.SILVER -> Color(0xFF9E9E9E)
    MembershipTier.GOLD -> Color(0xFFFBBC05)
    MembershipTier.PLATINUM -> Color(0xFF00BCD4)
    MembershipTier.DIAMOND -> Color(0xFF2196F3)
    MembershipTier.DIAMOND_PLUS -> Color(0xFF9C27B0)
    MembershipTier.BLACK_DIAMOND -> Color(0xFF212121)
}
