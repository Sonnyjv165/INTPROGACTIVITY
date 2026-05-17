package com.example.intprogactivity.presentation.trips

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
import com.example.intprogactivity.databinding.FragmentTripsBinding
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.util.UiState
import android.widget.TextView
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TripsFragment : Fragment() {

    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TripsViewModel by viewModels()
    private lateinit var upcomingAdapter: BookingCardAdapter
    private lateinit var pastAdapter: BookingCardAdapter

    // Track the latest lists so tab switches show correct state
    private var latestUpcoming: List<Booking> = emptyList()
    private var latestPast: List<Booking> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTripsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        setupTabs()
        observeViewModel()

        binding.btnSearchFlight.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun setupAdapters() {
        val onBookingClick: (Booking) -> Unit = { booking ->
            val bundle = Bundle().apply { putString("bookingId", booking.bookingId) }
            findNavController().navigate(R.id.action_trips_to_booking_detail, bundle)
        }
        upcomingAdapter = BookingCardAdapter(onBookingClick)
        pastAdapter = BookingCardAdapter(onBookingClick)
        binding.rvUpcoming.adapter = upcomingAdapter
        binding.rvPast.adapter = pastAdapter
    }

    private var isUpcomingSelected = true

    private fun setupTabs() {
        selectTab(isUpcoming = true)
        binding.tabUpcoming.setOnClickListener {
            if (!isUpcomingSelected) {
                isUpcomingSelected = true
                selectTab(isUpcoming = true)
                applyVisibility(true, latestUpcoming)
            }
        }
        binding.tabPast.setOnClickListener {
            if (isUpcomingSelected) {
                isUpcomingSelected = false
                selectTab(isUpcoming = false)
                applyVisibility(false, latestPast)
            }
        }
    }

    private fun selectTab(isUpcoming: Boolean) {
        val selectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_selected)
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        val unselectedTextColor = 0xCCFFFFFF.toInt()

        if (isUpcoming) {
            binding.tabUpcoming.background = selectedBg
            binding.tabUpcoming.setTextColor(selectedTextColor)
            binding.tabUpcoming.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.tabPast.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.tabPast.setTextColor(unselectedTextColor)
            binding.tabPast.setTypeface(null, android.graphics.Typeface.NORMAL)
        } else {
            binding.tabPast.background = selectedBg
            binding.tabPast.setTextColor(selectedTextColor)
            binding.tabPast.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.tabUpcoming.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.tabUpcoming.setTextColor(unselectedTextColor)
            binding.tabUpcoming.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.bookingsState.collect { state ->
                        binding.progressBar.isVisible = state is UiState.Loading
                    }
                }
                launch {
                    viewModel.upcomingBookings.collect { bookings ->
                        latestUpcoming = bookings
                        upcomingAdapter.submitList(bookings)
                        if (isUpcomingSelected) applyVisibility(isUpcomingTab = true, list = bookings)
                    }
                }
                launch {
                    viewModel.pastBookings.collect { bookings ->
                        latestPast = bookings
                        pastAdapter.submitList(bookings)
                        if (!isUpcomingSelected) applyVisibility(isUpcomingTab = false, list = bookings)
                    }
                }
            }
        }
    }

    private fun applyVisibility(isUpcomingTab: Boolean, list: List<Booking>) {
        val isEmpty = list.isEmpty()
        binding.layoutEmpty.isVisible = isEmpty
        binding.rvUpcoming.isVisible = isUpcomingTab && !isEmpty
        binding.rvPast.isVisible = !isUpcomingTab && !isEmpty
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
