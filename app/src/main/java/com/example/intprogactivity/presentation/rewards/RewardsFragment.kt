package com.example.intprogactivity.presentation.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.intprogactivity.databinding.FragmentRewardsBinding
import com.example.intprogactivity.domain.model.MembershipTier
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
                            updateTierProgress(user.membershipTier, user.totalBookings, user.totalSpend)
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

    private fun updateTierProgress(tier: MembershipTier, totalBookings: Int, totalSpend: Double) {
        val (nextTierName, progress, hint) = when (tier) {
            MembershipTier.SILVER -> Triple("GOLD", minOf(totalBookings * 100, 100), "Book ${maxOf(1 - totalBookings, 0)} more flight to reach Gold")
            MembershipTier.GOLD -> Triple("PLATINUM", minOf(totalBookings * 33, 100), "Need 3+ bookings in 12 months for Platinum")
            MembershipTier.PLATINUM -> Triple("DIAMOND", minOf((totalBookings / 8.0 * 100).toInt(), 100), "Need 8+ bookings & \$1,000 spend for Diamond")
            MembershipTier.DIAMOND -> Triple("DIAMOND+", minOf((totalSpend / 100).toInt(), 100), "Need \$10,000 annual spend for Diamond+")
            MembershipTier.DIAMOND_PLUS -> Triple("BLACK DIAMOND", 100, "Diamond+ tier — invite-only above")
            MembershipTier.BLACK_DIAMOND -> Triple("MAX TIER", 100, "You've reached the highest tier!")
            MembershipTier.GUEST -> Triple("SILVER", 0, "Register to start earning Trip Coins")
        }
        binding.tvNextTier.text = nextTierName
        binding.tierProgress.progress = progress
        binding.tvTierHint.text = hint
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
