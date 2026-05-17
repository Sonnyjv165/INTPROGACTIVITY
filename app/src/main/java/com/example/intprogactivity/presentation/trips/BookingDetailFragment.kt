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
import com.example.intprogactivity.databinding.FragmentBookingDetailBinding
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.domain.model.BookingStatus
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.repository.BookingRepository
import com.example.intprogactivity.util.UiState
import com.example.intprogactivity.util.formatDisplayTime
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BookingDetailFragment : Fragment() {

    private var _binding: FragmentBookingDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TripsViewModel by viewModels()
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    private var currentBooking: Booking? = null

    @Inject lateinit var bookingRepository: BookingRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        val bookingId = arguments?.getString("bookingId") ?: return
        loadBooking(bookingId)
        observeCancelState()
    }

    private fun loadBooking(bookingId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                bookingRepository.getBookingById(bookingId)
            }.fold(
                onSuccess = { booking ->
                    currentBooking = booking
                    bindBooking(booking)
                },
                onFailure = {
                    Snackbar.make(binding.root, "Failed to load booking", Snackbar.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun bindBooking(booking: Booking) {
        binding.tvPnr.text = booking.pnr
        binding.tvStatusBadge.text = booking.status.name

        runCatching {
            val offer = gson.fromJson(booking.outboundFlightJson, FlightOffer::class.java)
            val firstSeg = offer.itineraries.firstOrNull()?.segments?.firstOrNull()
            val lastSeg = offer.itineraries.firstOrNull()?.segments?.lastOrNull()
            binding.tvOriginCode.text = firstSeg?.departure?.iataCode ?: ""
            binding.tvDestCode.text = lastSeg?.arrival?.iataCode ?: ""
            binding.tvDepartTime.text = firstSeg?.departure?.at?.formatDisplayTime() ?: ""
            binding.tvArriveTime.text = lastSeg?.arrival?.at?.formatDisplayTime() ?: ""
            binding.tvDuration.text = formatDuration(offer.itineraries.firstOrNull()?.duration ?: "")
        }

        binding.tvTravelDate.text = runCatching {
            dateFormat.format(Date(booking.travelDate))
        }.getOrDefault("") + " · ${booking.passengers.size} Passenger${if (booking.passengers.size != 1) "s" else ""}"

        binding.tvPassengerName.text = booking.passengers.firstOrNull()?.let {
            "${it.lastName.uppercase()} ${it.firstName.uppercase()}"
        } ?: ""

        binding.tvTotalPrice.text = "$${String.format("%.2f", booking.totalPrice)}"

        binding.btnCancel.isVisible = booking.status == BookingStatus.CONFIRMED
        binding.btnCancel.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancel Booking")
                .setMessage("Are you sure you want to cancel? This cannot be undone.")
                .setPositiveButton("Cancel Booking") { _, _ ->
                    viewModel.cancelBooking(booking.bookingId, booking.travelDate)
                }
                .setNegativeButton("Keep") { d, _ -> d.dismiss() }
                .show()
        }
    }

    private fun observeCancelState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cancelState.collect { state ->
                    when (state) {
                        is UiState.Success -> {
                            viewModel.resetCancelState()
                            Snackbar.make(binding.root, "Booking cancelled", Snackbar.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                        is UiState.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun formatDuration(iso: String): String {
        val h = Regex("(\\d+)H").find(iso)?.groupValues?.get(1) ?: "0"
        val m = Regex("(\\d+)M").find(iso)?.groupValues?.get(1) ?: "0"
        return "${h}h ${m}m"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
