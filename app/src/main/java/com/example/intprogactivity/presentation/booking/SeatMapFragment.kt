package com.example.intprogactivity.presentation.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.intprogactivity.databinding.FragmentSeatMapBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SeatMapFragment : Fragment() {

    private var _binding: FragmentSeatMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingViewModel by activityViewModels()
    private lateinit var seatAdapter: SeatAdapter

    private var currentPassengerIndex = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSeatMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        seatAdapter = SeatAdapter { /* selection handled inside adapter */ }
        binding.rvSeats.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvSeats.adapter = seatAdapter

        currentPassengerIndex = 0
        updatePassengerLabel()

        binding.btnConfirmSeat.setOnClickListener {
            val selected = seatAdapter.getSelectedSeat()
            if (selected != null) {
                viewModel.setSeatSelection(currentPassengerIndex, selected)
            }
            currentPassengerIndex++
            val total = viewModel.passengers.value.size
            if (currentPassengerIndex < total) {
                updatePassengerLabel()
                seatAdapter.clearSelection()
                // Restore seat if already chosen for this passenger
                val existingSeat = viewModel.getSeatForPassenger(currentPassengerIndex)
                if (existingSeat != null) seatAdapter.setSelectedSeat(existingSeat)
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun updatePassengerLabel() {
        val passengers = viewModel.passengers.value
        val total = passengers.size
        if (total == 0) {
            binding.tvPassengerLabel.text = "Select your seat"
            return
        }
        val current = passengers.getOrNull(currentPassengerIndex) ?: return
        val typeLabel = current.type.name.lowercase().replaceFirstChar { it.uppercase() }
        val name = if (current.firstName.isNotBlank()) current.firstName else "Passenger ${currentPassengerIndex + 1}"
        binding.tvPassengerLabel.text = "Seat for $name ($typeLabel) · ${currentPassengerIndex + 1} of $total"

        // Restore existing seat selection for this passenger
        val existing = viewModel.getSeatForPassenger(currentPassengerIndex)
        if (existing != null) seatAdapter.setSelectedSeat(existing) else seatAdapter.clearSelection()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
