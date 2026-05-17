package com.example.intprogactivity.presentation.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.databinding.FragmentAddOnsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddOnsFragment : Fragment() {

    private var _binding: FragmentAddOnsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingViewModel by viewModels()

    // Passenger ID for add-on association (single adult for now)
    private val primaryPassengerId = "1"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddOnsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Store contact info passed from passenger details
        val email = arguments?.getString("contactEmail") ?: ""
        val phone = arguments?.getString("contactPhone") ?: ""
        viewModel.setContactInfo(email, phone)

        setupListeners()

        binding.btnContinue.setOnClickListener {
            findNavController().navigate(R.id.action_addons_to_checkout)
        }
    }

    private fun setupListeners() {
        binding.rgBaggage.setOnCheckedChangeListener { _, checkedId ->
            val (kg, price) = when (checkedId) {
                R.id.rbBag20 -> Pair(20, 35.0)
                R.id.rbBag30 -> Pair(30, 55.0)
                else -> Pair(0, 0.0)
            }
            viewModel.setExtraBaggage(primaryPassengerId, kg, price)
            updateTotal()
        }

        binding.rgMeal.setOnCheckedChangeListener { _, checkedId ->
            val (type, price) = when (checkedId) {
                R.id.rbMealRegular -> Pair("REGULAR", 12.0)
                R.id.rbMealVeg -> Pair("VEGETARIAN", 12.0)
                R.id.rbMealHalal -> Pair("HALAL", 12.0)
                else -> Pair("", 0.0)
            }
            viewModel.setMeal(primaryPassengerId, type, price)
            updateTotal()
        }

        binding.switchInsurance.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setInsurance(isChecked)
            updateTotal()
        }

        binding.cardSeatSelect.setOnClickListener {
            findNavController().navigate(R.id.action_addons_to_seat_map)
        }
    }

    private fun updateTotal() {
        val total = viewModel.addOns.value.totalCost()
        binding.tvAddOnsTotal.text = "$${String.format("%.2f", total)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
