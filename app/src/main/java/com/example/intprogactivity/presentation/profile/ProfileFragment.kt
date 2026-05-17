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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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

        binding.ivAvatar.setImageResource(R.drawable.profile)
        binding.ivAvatar.imageTintList = null

        binding.btnEditProfile.setOnClickListener { showEditProfileDialog() }
        binding.itemEditProfile.setOnClickListener { showEditProfileDialog() }

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

    private fun showEditProfileDialog() {
        val user = viewModel.currentUser.value ?: return
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            android.R.layout.simple_list_item_2, null
        )

        val nameInput = TextInputEditText(requireContext()).apply { setText(user.displayName) }
        val phoneInput = TextInputEditText(requireContext()).apply { setText(user.phone) }

        val nameLayout = TextInputLayout(requireContext()).apply {
            hint = "Display Name"
            addView(nameInput)
        }
        val phoneLayout = TextInputLayout(requireContext()).apply {
            hint = "Phone Number"
            addView(phoneInput)
        }

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
                if (name.isNotBlank()) {
                    viewModel.updateProfile(name, phone)
                } else {
                    Snackbar.make(binding.root, "Name cannot be empty", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
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
                            binding.tvTotalSpend.text = "₱${String.format("%,.0f", user.totalSpend)}"
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

                launch {
                    viewModel.updateProfileState.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                Snackbar.make(binding.root, "Profile updated", Snackbar.LENGTH_SHORT).show()
                                viewModel.resetUpdateState()
                            }
                            is UiState.Error -> {
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                                viewModel.resetUpdateState()
                            }
                            else -> Unit
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
