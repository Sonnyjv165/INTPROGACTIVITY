package com.example.intprogactivity.presentation.booking

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.databinding.FragmentCheckoutBinding
import com.example.intprogactivity.util.Constants
import com.example.intprogactivity.util.UiState
import com.example.intprogactivity.util.formatDisplayTime
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        populateSummary()
        setupPaymentToggle()
        setupCardNumberFormatting()

        binding.btnConfirmBooking.setOnClickListener {
            if (validatePayment()) viewModel.confirmBooking()
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

        val returnOffer = viewModel.returnFlightOffer.value
        if (returnOffer != null) {
            val retItinerary = returnOffer.itineraries.firstOrNull()
            val retFirst = retItinerary?.segments?.firstOrNull()
            val retLast = retItinerary?.segments?.lastOrNull()
            if (retFirst != null && retLast != null) {
                binding.layoutReturnFlight.isVisible = true
                binding.tvReturnFlightSummary.text = "${retFirst.departure.iataCode} → ${retLast.arrival.iataCode}"
                binding.tvReturnFlightTime.text = "${retFirst.departure.at.formatDisplayTime()} → ${retLast.arrival.at.formatDisplayTime()}"
            }
        }

        val grandTotal = viewModel.totalPrice()
        val addOnsTotal = viewModel.addOns.value.totalCost()
        val discount = viewModel.flightDiscount()
        val flightTotal = grandTotal - addOnsTotal

        binding.tvFlightFare.text = "₱${String.format("%,.0f", flightTotal)}"
        binding.tvAddOnsSubtotal.text = "₱${String.format("%,.0f", addOnsTotal)}"
        binding.tvGrandTotal.text = "₱${String.format("%,.0f", grandTotal)}"

        val tier = viewModel.currentUserTier.value
        if (discount > 0.0) {
            binding.layoutTierDiscount.visibility = android.view.View.VISIBLE
            binding.tvTierDiscountLabel.text = "${tier.displayName()} discount (${(tier.discountPercent() * 100).toInt()}%)"
            binding.tvTierDiscountAmount.text = "-₱${String.format("%,.0f", discount)}"
        } else {
            binding.layoutTierDiscount.visibility = android.view.View.GONE
        }

        val coins = (grandTotal * Constants.BASE_COIN_EARN_RATE * tier.coinMultiplier()).toInt()
        binding.tvCoinsEarn.text = "You'll earn ${String.format("%,d", coins)} Trip Coins"
    }

    private fun setupPaymentToggle() {
        binding.rgPayment.setOnCheckedChangeListener { _, checkedId ->
            binding.layoutCardFields.isVisible = checkedId == R.id.rbCreditCard
            binding.layoutGcashFields.isVisible = checkedId == R.id.rbGcash
            clearPaymentErrors()
        }
    }

    private fun setupCardNumberFormatting() {
        binding.etCardNumber.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true
                val digits = s.toString().replace(" ", "")
                val formatted = StringBuilder()
                for (i in digits.indices) {
                    if (i > 0 && i % 4 == 0) formatted.append(' ')
                    formatted.append(digits[i])
                }
                s.replace(0, s.length, formatted)
                isFormatting = false
            }
        })

        binding.etCardExpiry.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true
                val digits = s.toString().replace("/", "")
                val formatted = when {
                    digits.length >= 2 -> "${digits.substring(0, 2)}/${digits.substring(2)}"
                    else -> digits
                }
                s.replace(0, s.length, formatted)
                isFormatting = false
            }
        })
    }

    private fun validatePayment(): Boolean {
        clearPaymentErrors()
        return when (binding.rgPayment.checkedRadioButtonId) {
            R.id.rbCreditCard -> validateCardFields()
            R.id.rbGcash -> validateGcashField()
            else -> true
        }
    }

    private fun validateCardFields(): Boolean {
        var valid = true
        val cardDigits = binding.etCardNumber.text?.toString()?.replace(" ", "") ?: ""
        if (cardDigits.length < 16) {
            binding.tilCardNumber.error = "Enter a valid 16-digit card number"
            valid = false
        }
        val expiry = binding.etCardExpiry.text?.toString() ?: ""
        if (!expiry.matches(Regex("\\d{2}/\\d{2}"))) {
            binding.tilCardExpiry.error = "Enter expiry as MM/YY"
            valid = false
        }
        val cvv = binding.etCardCvv.text?.toString() ?: ""
        if (cvv.length < 3) {
            binding.tilCardCvv.error = "Enter CVV"
            valid = false
        }
        return valid
    }

    private fun validateGcashField(): Boolean {
        val number = binding.etGcashNumber.text?.toString() ?: ""
        if (number.length != 10 || !number.startsWith("9")) {
            binding.tilGcashNumber.error = "Enter a valid 10-digit GCash number (e.g. 9171234567)"
            return false
        }
        return true
    }

    private fun clearPaymentErrors() {
        binding.tilCardNumber.error = null
        binding.tilCardExpiry.error = null
        binding.tilCardCvv.error = null
        binding.tilGcashNumber.error = null
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
