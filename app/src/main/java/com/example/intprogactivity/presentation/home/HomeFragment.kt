package com.example.intprogactivity.presentation.home

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
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.databinding.FragmentHomeBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private val dealsAdapter = DealsAdapter { deal ->
        // Pre-fill search with deal's airports
    }
    private val gson = Gson()
    private val displayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDeals()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupDeals() {
        binding.rvDeals.adapter = dealsAdapter
        dealsAdapter.submitList(SAMPLE_DEALS)
    }

    private fun setupClickListeners() {
        binding.tripTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.setRoundTrip(checkedId == R.id.btnRoundTrip)
            }
        }
        binding.btnOneWay.isChecked = true

        binding.layoutOrigin.setOnClickListener {
            AirportSearchBottomSheet.newInstance(
                AirportSearchBottomSheet.MODE_ORIGIN, "Select Origin"
            ).show(childFragmentManager, AirportSearchBottomSheet.TAG)
        }

        binding.layoutDestination.setOnClickListener {
            AirportSearchBottomSheet.newInstance(
                AirportSearchBottomSheet.MODE_DESTINATION, "Select Destination"
            ).show(childFragmentManager, AirportSearchBottomSheet.TAG)
        }

        binding.fabSwap.setOnClickListener { viewModel.swapOriginDestination() }

        binding.layoutDepartDate.setOnClickListener { showDatePicker(isReturn = false) }
        binding.layoutReturnDate.setOnClickListener {
            if (viewModel.isRoundTrip.value) showDatePicker(isReturn = true)
        }

        binding.layoutPassengers.setOnClickListener {
            PassengerPickerDialog(
                adults = viewModel.adults.value,
                children = viewModel.children.value,
                infants = viewModel.infants.value
            ) { adults, children, infants ->
                viewModel.setPassengers(adults, children, infants)
            }.show(childFragmentManager, "PassengerPicker")
        }

        binding.btnSearchFlights.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val error = viewModel.onSearchClicked()
                if (error != null) {
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        binding.tierBanner.setOnClickListener {
            findNavController().navigate(R.id.rewardsFragment)
        }
    }

    private fun showDatePicker(isReturn: Boolean) {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
            .build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isReturn) "Select Return Date" else "Select Departure Date")
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { ms ->
            val date = Date(ms)
            val apiDate = apiFormat.format(date)
            val displayDate = displayFormat.format(date)
            if (isReturn) {
                viewModel.setReturnDate(apiDate)
                binding.tvReturnDate.text = displayDate
            } else {
                viewModel.setDepartDate(apiDate)
                binding.tvDepartDate.text = displayDate
            }
        }
        picker.show(childFragmentManager, "DatePicker")
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentUser.collect { user ->
                        if (user != null) {
                            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                            val greeting = when {
                                hour < 12 -> "Good morning, ${user.displayName.split(" ").first()}!"
                                hour < 17 -> "Good afternoon, ${user.displayName.split(" ").first()}!"
                                else -> "Good evening, ${user.displayName.split(" ").first()}!"
                            }
                            binding.tvGreeting.text = greeting
                            binding.tierBanner.isVisible = true
                            binding.tvTierBadge.text = user.membershipTier.displayName()
                            binding.tvTierCoins.text = "${user.tripCoins} Trip Coins"
                            binding.tvTierBadge.setBackgroundColor(
                                requireContext().getColor(user.membershipTier.colorRes())
                            )
                        } else {
                            binding.tvGreeting.text = "Welcome to TripFlights!"
                            binding.tierBanner.isVisible = false
                        }
                    }
                }

                launch {
                    viewModel.origin.collect { airport ->
                        binding.tvOriginCode.text = airport?.iataCode ?: "---"
                        binding.tvOriginName.text = airport?.cityName ?: "Select city"
                    }
                }

                launch {
                    viewModel.destination.collect { airport ->
                        binding.tvDestCode.text = airport?.iataCode ?: "---"
                        binding.tvDestName.text = airport?.cityName ?: "Select city"
                    }
                }

                launch {
                    viewModel.isRoundTrip.collect { roundTrip ->
                        binding.layoutReturnDate.alpha = if (roundTrip) 1.0f else 0.4f
                        if (!roundTrip) {
                            binding.tvReturnDate.text = "Add return"
                        }
                    }
                }

                launch {
                    viewModel.adults.collect { updatePassengerSummary() }
                }

                launch {
                    viewModel.navigateToSearch.collect { params ->
                        val json = gson.toJson(params)
                        val bundle = Bundle().apply { putString("searchParamsJson", json) }
                        findNavController().navigate(R.id.action_home_to_search_results, bundle)
                    }
                }
            }
        }
    }

    private fun updatePassengerSummary() {
        val adults = viewModel.adults.value
        val children = viewModel.children.value
        val infants = viewModel.infants.value
        val parts = mutableListOf("$adults Adult${if (adults > 1) "s" else ""}")
        if (children > 0) parts.add("$children Child${if (children > 1) "ren" else ""}")
        if (infants > 0) parts.add("$infants Infant${if (infants > 1) "s" else ""}")
        binding.tvPassengerSummary.text = parts.joinToString(" · ") + " · Economy"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val SAMPLE_DEALS = listOf(
            DealItem("Tokyo", "Manila", "$299", "-25%", "🗼", "MNL", "NRT"),
            DealItem("Singapore", "Manila", "$120", "-30%", "🦁", "MNL", "SIN"),
            DealItem("Seoul", "Manila", "$249", "-20%", "🏯", "MNL", "ICN"),
            DealItem("Hong Kong", "Manila", "$180", "-15%", "🌃", "MNL", "HKG"),
            DealItem("Bangkok", "Manila", "$99", "-35%", "🏛", "MNL", "BKK"),
        )
    }
}
