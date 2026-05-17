package com.example.intprogactivity.presentation.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.intprogactivity.R
import com.example.intprogactivity.databinding.FragmentFlightDetailBinding
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.model.FlightSearchParams
import com.example.intprogactivity.presentation.booking.BookingViewModel
import com.example.intprogactivity.util.Constants
import com.example.intprogactivity.util.formatDisplayTime
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class FlightDetailFragment : Fragment() {

    private var _binding: FragmentFlightDetailBinding? = null
    private val binding get() = _binding!!
    private val bookingViewModel: BookingViewModel by activityViewModels()
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    private lateinit var offer: FlightOffer
    private var selectedCabin = "ECONOMY"
    private var isRoundTrip = false
    private var searchParams: FlightSearchParams? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFlightDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val offerJson = arguments?.getString("flightOfferJson") ?: ""
        offer = gson.fromJson(offerJson, FlightOffer::class.java)
        isRoundTrip = arguments?.getBoolean("isRoundTrip", false) ?: false
        val spJson = arguments?.getString("searchParamsJson") ?: ""
        if (spJson.isNotEmpty()) {
            searchParams = runCatching { gson.fromJson(spJson, FlightSearchParams::class.java) }.getOrNull()
        }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        setupCabinChips()
        bindFlightOffer(offer)

        binding.btnBookNow.setOnClickListener {
            bookingViewModel.setCabinClass(selectedCabin)

            val bundle = Bundle().apply {
                putString("flightOfferJson", offerJson)
                putString("returnFlightJson", "")
                putInt("adults", searchParams?.adults ?: 1)
                putInt("children", searchParams?.children ?: 0)
                putInt("infants", searchParams?.infants ?: 0)
            }

            // For round trip, generate matching return flight and pass it
            if (isRoundTrip && searchParams != null) {
                val returnDate = searchParams!!.returnDate ?: ""
                if (returnDate.isNotEmpty()) {
                    val returnParams = searchParams!!.copy(
                        origin = searchParams!!.destination,
                        destination = searchParams!!.origin,
                        departureDate = returnDate,
                        returnDate = null
                    )
                    val returnOffers = com.example.intprogactivity.data.local.LocalFlightData.searchFlights(returnParams)
                    val returnFlight = returnOffers.firstOrNull()
                    if (returnFlight != null) {
                        bundle.putString("returnFlightJson", gson.toJson(returnFlight))
                    }
                }
            }

            findNavController().navigate(R.id.action_detail_to_passenger, bundle)
        }
    }

    private fun setupCabinChips() {
        val chips = listOf(
            binding.chipEconomy,
            binding.chipPremiumEconomy,
            binding.chipBusiness,
            binding.chipFirst
        )
        val cabinValues = listOf("ECONOMY", "PREMIUM_ECONOMY", "BUSINESS", "FIRST")
        val cabinMultipliers = mapOf(
            "ECONOMY" to 1.0,
            "PREMIUM_ECONOMY" to 1.4,
            "BUSINESS" to 2.5,
            "FIRST" to 4.0
        )

        chips.forEachIndexed { index, chip ->
            chip.setOnClickListener {
                chips.forEach { it.isChecked = false }
                chip.isChecked = true
                selectedCabin = cabinValues[index]
                val multiplier = cabinMultipliers[selectedCabin] ?: 1.0
                updatePrices(multiplier)
            }
        }
        binding.chipEconomy.isChecked = true
    }

    private fun updatePrices(multiplier: Double) {
        val travelerPrice = offer.travelerPricings.firstOrNull()?.price
        val base = (travelerPrice?.base?.toDoubleOrNull() ?: 0.0) * multiplier
        val total = offer.totalPriceDouble() * multiplier
        val taxes = total - base

        binding.tvBaseFare.text = "₱${String.format("%,.0f", base)}"
        binding.tvTaxes.text = "₱${String.format("%,.0f", taxes)}"
        binding.tvTotalPrice.text = "₱${String.format("%,.0f", total)}"

        val coinsEstimate = (total * 10).toInt()
        binding.tvCoinsEarn.text = "Earn ${String.format("%,d", coinsEstimate)} Trip Coins with this booking"
    }

    private fun bindFlightOffer(offer: FlightOffer) {
        val itinerary = offer.itineraries.firstOrNull() ?: return
        val firstSeg = itinerary.segments.firstOrNull() ?: return
        val lastSeg = itinerary.segments.lastOrNull() ?: return

        val airlineCode = offer.primaryAirlineCode()
        Glide.with(binding.ivAirlineLogo)
            .load(Constants.AMADEUS_AIRLINE_LOGO_URL.format(airlineCode))
            .placeholder(android.R.drawable.ic_menu_myplaces)
            .into(binding.ivAirlineLogo)

        binding.tvAirlineName.text = firstSeg.carrierCode
        binding.tvFlightNumber.text = "${firstSeg.carrierCode} ${firstSeg.number}"

        binding.tvDepartTime.text = firstSeg.departure.at.formatDisplayTime()
        binding.tvDepartCode.text = firstSeg.departure.iataCode
        binding.tvArriveTime.text = lastSeg.arrival.at.formatDisplayTime()
        binding.tvArriveCode.text = lastSeg.arrival.iataCode

        runCatching { dateFormat.format(isoFormat.parse(firstSeg.departure.at)!!) }
            .onSuccess { binding.tvDepartDate.text = it }
        runCatching { dateFormat.format(isoFormat.parse(lastSeg.arrival.at)!!) }
            .onSuccess { binding.tvArriveDate.text = it }

        binding.tvDuration.text = formatDuration(itinerary.duration)

        val stops = offer.stopCount()
        binding.tvStops.text = when (stops) {
            0 -> "Nonstop"
            1 -> "1 stop"
            else -> "$stops stops"
        }

        val baggage = offer.travelerPricings.firstOrNull()
            ?.fareDetailsBySegment?.firstOrNull()
            ?.includedCheckedBags
        binding.tvBaggage.text = baggage?.displayText() ?: "Carry-on only"

        // Trip type label
        if (isRoundTrip) {
            binding.btnBookNow.text = "Book Round Trip"
        }

        updatePrices(1.0)
    }

    private fun formatDuration(isoDuration: String): String {
        val h = Regex("(\\d+)H").find(isoDuration)?.groupValues?.get(1) ?: "0"
        val m = Regex("(\\d+)M").find(isoDuration)?.groupValues?.get(1) ?: "0"
        return "${h}h ${m}m"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
