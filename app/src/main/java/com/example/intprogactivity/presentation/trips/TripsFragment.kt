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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.intprogactivity.R
import com.example.intprogactivity.databinding.FragmentTripsBinding
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.util.UiState
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TripsFragment : Fragment() {

    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TripsViewModel by viewModels()
    private val gson = Gson()
    private lateinit var upcomingAdapter: BookingCardAdapter
    private lateinit var pastAdapter: BookingCardAdapter
    private lateinit var upcomingRv: RecyclerView
    private lateinit var pastRv: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTripsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        setupAdapters()
        observeViewModel()

        binding.btnSearchFlight.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Upcoming"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Past"))
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isUpcoming = tab?.position == 0
                upcomingRv.isVisible = isUpcoming
                pastRv.isVisible = !isUpcoming
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
    }

    private fun setupAdapters() {
        val onBookingClick: (Booking) -> Unit = { booking ->
            val bundle = Bundle().apply { putString("bookingId", booking.bookingId) }
            findNavController().navigate(R.id.action_trips_to_booking_detail, bundle)
        }

        upcomingAdapter = BookingCardAdapter(onBookingClick)
        pastAdapter = BookingCardAdapter(onBookingClick)

        // Dynamically added RecyclerViews within the ViewPager2's view
        // Using the ViewPager2's content area — for simplicity, use the rvFlights as the base
        upcomingRv = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = upcomingAdapter
        }
        pastRv = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pastAdapter
            isVisible = false
        }

        val contentContainer = binding.viewPager.parent as? ViewGroup
        contentContainer?.addView(upcomingRv)
        contentContainer?.addView(pastRv)
        binding.viewPager.isVisible = false
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.bookingsState.collect { state ->
                        binding.progressBar.isVisible = state is UiState.Loading
                        binding.layoutEmpty.isVisible =
                            state is UiState.Success && (state.data as List<*>).isEmpty()
                    }
                }
                launch {
                    viewModel.upcomingBookings.collect { bookings ->
                        upcomingAdapter.submitList(bookings)
                    }
                }
                launch {
                    viewModel.pastBookings.collect { bookings ->
                        pastAdapter.submitList(bookings)
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
