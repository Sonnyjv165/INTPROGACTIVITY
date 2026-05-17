package com.example.intprogactivity.presentation.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.intprogactivity.databinding.ItemFlightOfferBinding
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.util.Constants
import com.example.intprogactivity.util.formatDisplayTime

class FlightOfferAdapter(
    private val onFlightClick: (FlightOffer) -> Unit
) : ListAdapter<FlightOffer, FlightOfferAdapter.FlightViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlightViewHolder {
        val binding = ItemFlightOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FlightViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FlightViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class FlightViewHolder(private val binding: ItemFlightOfferBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(offer: FlightOffer) {
            val itinerary = offer.itineraries.firstOrNull() ?: return
            val firstSeg = itinerary.segments.firstOrNull() ?: return
            val lastSeg = itinerary.segments.lastOrNull() ?: return

            val airlineCode = offer.primaryAirlineCode()
            Glide.with(binding.ivAirlineLogo)
                .load(Constants.AMADEUS_AIRLINE_LOGO_URL.format(airlineCode))
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .into(binding.ivAirlineLogo)

            binding.tvAirlineName.text = firstSeg.carrierCode
            binding.tvDepartTime.text = firstSeg.departure.at.formatDisplayTime()
            binding.tvDepartCode.text = firstSeg.departure.iataCode
            binding.tvArriveTime.text = lastSeg.arrival.at.formatDisplayTime()
            binding.tvArriveCode.text = lastSeg.arrival.iataCode
            binding.tvDuration.text = formatDuration(itinerary.duration)

            val stops = offer.stopCount()
            binding.tvStopsLabel.text = when (stops) {
                0 -> "Nonstop"
                1 -> "1 stop"
                else -> "$stops stops"
            }
            binding.tvStopsBadge.text = when (stops) {
                0 -> "Direct"
                1 -> "1 Stop"
                else -> "$stops Stops"
            }

            val price = offer.totalPriceDouble()
            binding.tvPrice.text = "$${String.format("%.0f", price)}"
            binding.tvPriceLabel.text = "per person · ${offer.price.currency}"

            binding.btnSelectFlight.setOnClickListener { onFlightClick(offer) }
            binding.root.setOnClickListener { onFlightClick(offer) }
        }

        private fun formatDuration(isoDuration: String): String {
            val hoursMatch = Regex("(\\d+)H").find(isoDuration)
            val minutesMatch = Regex("(\\d+)M").find(isoDuration)
            val h = hoursMatch?.groupValues?.get(1) ?: "0"
            val m = minutesMatch?.groupValues?.get(1) ?: "0"
            return "${h}h ${m}m"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FlightOffer>() {
            override fun areItemsTheSame(old: FlightOffer, new: FlightOffer) = old.id == new.id
            override fun areContentsTheSame(old: FlightOffer, new: FlightOffer) = old == new
        }
    }
}
