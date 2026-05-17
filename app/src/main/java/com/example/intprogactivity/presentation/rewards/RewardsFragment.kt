package com.example.intprogactivity.presentation.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.intprogactivity.R
import com.example.intprogactivity.databinding.FragmentRewardsBinding
import com.example.intprogactivity.domain.model.MembershipTier
import com.example.intprogactivity.domain.model.User
import com.example.intprogactivity.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RewardsFragment : Fragment() {

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RewardsViewModel by viewModels()
    private val adapter = CoinHistoryAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvCoinHistory.adapter = adapter
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentUser.collect { user ->
                        if (user != null) {
                            binding.tvCoinBalance.text = String.format("%,d", user.tripCoins)
                            binding.tvTierMultiplier.text = "${user.membershipTier.coinMultiplier()}×"
                            binding.tvCurrentTier.text = user.membershipTier.displayName()
                            updateTierProgress(user)
                            buildTierCards(user.membershipTier)
                        }
                    }
                }
                launch {
                    viewModel.coinHistoryState.collect { state ->
                        if (state is UiState.Success) adapter.submitList(state.data)
                    }
                }
            }
        }
    }

    private fun updateTierProgress(user: User) {
        val tier = user.membershipTier
        val bookings = user.totalBookings
        val spend = user.totalSpend

        val (nextTierName, progress, hint) = when (tier) {
            MembershipTier.SILVER -> Triple(
                "Gold",
                minOf((bookings / 5.0 * 100).toInt(), 100),
                if (bookings >= 5) "Tier upgrade pending" else "Book ${5 - bookings} more flight${if (5 - bookings != 1) "s" else ""} to reach Gold"
            )
            MembershipTier.GOLD -> {
                val bookProgress = (bookings / 10.0 * 100).toInt()
                val spendProgress = (spend / 50_000.0 * 100).toInt()
                Triple(
                    "Platinum",
                    minOf((bookProgress + spendProgress) / 2, 100),
                    "Need 10 bookings (${bookings}/10) + ₱50,000 spend (₱${String.format("%,.0f", spend)}/₱50,000) for Platinum"
                )
            }
            MembershipTier.PLATINUM -> {
                val bookProgress = (bookings / 25.0 * 100).toInt()
                val spendProgress = (spend / 500_000.0 * 100).toInt()
                Triple(
                    "Diamond",
                    minOf((bookProgress + spendProgress) / 2, 100),
                    "Need 25 bookings (${bookings}/25) + ₱500,000 spend for Diamond"
                )
            }
            MembershipTier.DIAMOND -> Triple(
                "Diamond+",
                minOf((spend / 2_000_000.0 * 100).toInt(), 100),
                "Need ₱2,000,000 annual spend for Diamond+ (₱${String.format("%,.0f", spend)} so far)"
            )
            MembershipTier.DIAMOND_PLUS -> Triple("Black Diamond", 100, "Diamond+ — invite-only above")
            MembershipTier.BLACK_DIAMOND -> Triple("MAX TIER", 100, "You've reached the highest tier!")
            MembershipTier.GUEST -> Triple("Silver", 0, "Register to start earning Trip Coins")
        }

        binding.tvNextTier.text = nextTierName
        binding.tierProgress.progress = progress
        binding.tvTierHint.text = hint
    }

    private fun buildTierCards(currentTier: MembershipTier) {
        binding.llTierCards.removeAllViews()
        val tiers = listOf(
            MembershipTier.SILVER,
            MembershipTier.GOLD,
            MembershipTier.PLATINUM,
            MembershipTier.DIAMOND,
            MembershipTier.DIAMOND_PLUS,
            MembershipTier.BLACK_DIAMOND
        )
        tiers.forEach { tier ->
            val cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_tier_card, binding.llTierCards, false)

            val accentBar = cardView.findViewById<View>(R.id.tierAccentBar)
            val tvName = cardView.findViewById<TextView>(R.id.tvTierName)
            val tvBadge = cardView.findViewById<TextView>(R.id.tvCurrentBadge)
            val tvMultiplier = cardView.findViewById<TextView>(R.id.tvMultiplier)
            val tvDiscount = cardView.findViewById<TextView>(R.id.tvDiscount)
            val llBenefits = cardView.findViewById<LinearLayout>(R.id.llBenefits)

            val colorRes = tier.colorRes()
            accentBar.setBackgroundColor(ContextCompat.getColor(requireContext(), colorRes))
            tvName.text = tier.displayName()
            tvName.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
            tvMultiplier.text = "${tier.coinMultiplier()}× coins"
            tvDiscount.text = tier.discountLabel()

            if (tier == currentTier) {
                tvBadge.visibility = View.VISIBLE
            }

            if (tier == MembershipTier.BLACK_DIAMOND) {
                // Dim locked tiers the user hasn't reached
                if (currentTier != MembershipTier.BLACK_DIAMOND) {
                    cardView.alpha = 0.6f
                }
            } else if (tier.ordinal > currentTier.ordinal) {
                cardView.alpha = 0.6f
            }

            // Populate benefits
            tier.benefits().forEach { benefit ->
                val tv = TextView(requireContext()).apply {
                    text = "• $benefit"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    val dp4 = (4 * resources.displayMetrics.density).toInt()
                    setPadding(0, dp4, 0, 0)
                }
                llBenefits.addView(tv)
            }

            binding.llTierCards.addView(cardView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
