package com.example.intprogactivity.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.databinding.FragmentProfileBinding
import com.example.intprogactivity.util.UiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.itemPriceAlerts.setOnClickListener {
            findNavController().navigate(R.id.priceAlertsFragment)
        }

        binding.itemRewards.setOnClickListener {
            findNavController().navigate(R.id.rewardsFragment)
        }

        binding.itemSignOut.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ -> viewModel.signOut() }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentUser.collect { user ->
                        if (user != null) {
                            binding.tvDisplayName.text = user.displayName
                            binding.tvEmail.text = user.email
                            binding.tvTierBadge.text = user.membershipTier.displayName()
                            binding.tvTierBadge.setBackgroundColor(
                                requireContext().getColor(user.membershipTier.colorRes())
                            )
                            binding.tvTotalBookings.text = user.totalBookings.toString()
                            binding.tvTripCoins.text = String.format("%,d", user.tripCoins)
                            binding.tvTotalSpend.text = "$${String.format("%.0f", user.totalSpend)}"
                        }
                    }
                }

                launch {
                    viewModel.signOutState.collect { state ->
                        if (state is UiState.Success) {
                            findNavController().navigate(R.id.action_profile_to_login)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
