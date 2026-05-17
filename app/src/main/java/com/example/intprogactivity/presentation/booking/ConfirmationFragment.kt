package com.example.intprogactivity.presentation.booking

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.databinding.FragmentConfirmationBinding
import com.example.intprogactivity.domain.repository.BookingRepository
import com.example.intprogactivity.util.formatDisplayTime
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ConfirmationFragment : Fragment() {

    private var _binding: FragmentConfirmationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingViewModel by viewModels()

    @Inject lateinit var bookingRepository: BookingRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookingId = arguments?.getString("bookingId") ?: ""
        loadBookingDetails(bookingId)

        binding.btnViewTrips.setOnClickListener {
            findNavController().navigate(R.id.tripsFragment)
        }

        binding.btnDownloadTicket.setOnClickListener {
            downloadTicket()
        }
    }

    private fun loadBookingDetails(bookingId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            bookingRepository.getBookingById(bookingId).fold(
                onSuccess = { booking ->
                    withContext(Dispatchers.Main) {
                        binding.tvPnr.text = booking.pnr
                        binding.tvPassengerName.text =
                            "${booking.passengers.firstOrNull()?.lastName?.uppercase() ?: ""} " +
                            "${booking.passengers.firstOrNull()?.firstName?.uppercase() ?: ""}"

                        val offer = viewModel.flightOffer.value
                        offer?.let {
                            val firstSeg = it.itineraries.firstOrNull()?.segments?.firstOrNull()
                            val lastSeg = it.itineraries.firstOrNull()?.segments?.lastOrNull()
                            binding.tvOriginCode.text = firstSeg?.departure?.iataCode ?: ""
                            binding.tvDestCode.text = lastSeg?.arrival?.iataCode ?: ""
                            binding.tvDepartTime.text = firstSeg?.departure?.at?.formatDisplayTime() ?: ""
                            binding.tvArriveTime.text = lastSeg?.arrival?.at?.formatDisplayTime() ?: ""
                        }

                        binding.tvTravelDate.text = booking.createdAt.toString()
                        val coins = booking.tripCoinsEarned
                        binding.tvCoinsEarned.text = "+${String.format("%,d", coins)} Trip Coins Earned!"
                    }
                },
                onFailure = { /* best-effort — ViewModel may have cached data */ }
            )
        }
    }

    private fun downloadTicket() {
        val card = binding.ticketCard
        val bitmap = Bitmap.createBitmap(card.width, card.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        card.draw(canvas)
        // In production: save bitmap to MediaStore and open share intent
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
