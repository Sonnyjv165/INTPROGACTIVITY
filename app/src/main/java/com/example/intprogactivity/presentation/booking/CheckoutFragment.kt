package com.example.intprogactivity.presentation.booking

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
import com.example.intprogactivity.databinding.FragmentCheckoutBinding
import com.example.intprogactivity.util.UiState
import com.example.intprogactivity.util.formatDisplayTime
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        populateSummary()

        binding.btnConfirmBooking.setOnClickListener {
            viewModel.confirmBooking()
        }

        observeBookingState()
    }

    private fun populateSummary() {
        val offer = viewModel.flightOffer.value ?: return
        val itinerary = offer.itineraries.firstOrNull() ?: return
        val firstSeg = itinerary.segments.firstOrNull() ?: return
        val lastSeg = itinerary.segments.lastOrNull() ?: return

        binding.tvFlightSummary.text = "${firstSeg.departure.iataCode} → ${lastSeg.arrival.iataCode}"
        binding.tvFlightTime.text = "${firstSeg.departure.at.formatDisplayTime()} → ${lastSeg.arrival.at.formatDisplayTime()}"

        val flightPrice = offer.totalPriceDouble()
        val addOnsTotal = viewModel.addOns.value.totalCost()
        val grandTotal = flightPrice + addOnsTotal

        binding.tvFlightFare.text = "$${String.format("%.2f", flightPrice)}"
        binding.tvAddOnsSubtotal.text = "$${String.format("%.2f", addOnsTotal)}"
        binding.tvGrandTotal.text = "$${String.format("%.2f", grandTotal)}"

        val coins = (grandTotal * 10).toInt()
        binding.tvCoinsEarn.text = "You'll earn ${String.format("%,d", coins)} Trip Coins"
    }

    private fun observeBookingState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bookingState.collect { state ->
                    binding.btnConfirmBooking.isEnabled = state !is UiState.Loading
                    when (state) {
                        is UiState.Success -> {
                            viewModel.resetBookingState()
                            val bundle = Bundle().apply { putString("bookingId", state.data) }
                            findNavController().navigate(R.id.action_checkout_to_confirmation, bundle)
                        }
                        is UiState.Error -> {
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        }
                        else -> Unit
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
