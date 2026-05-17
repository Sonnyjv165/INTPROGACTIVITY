package com.example.intprogactivity.presentation.trips

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.intprogactivity.databinding.ItemBookingCardBinding
import com.example.intprogactivity.domain.model.Booking
import com.example.intprogactivity.util.formatDisplayTime
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookingCardAdapter(
    private val onBookingClick: (Booking) -> Unit
) : ListAdapter<Booking, BookingCardAdapter.BookingViewHolder>(DIFF) {

    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class BookingViewHolder(private val binding: ItemBookingCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.tvStatus.text = booking.status.name
            binding.tvPnr.text = booking.pnr

            // Parse outbound flight JSON for display
            runCatching {
                val offer = gson.fromJson(
                    booking.outboundFlightJson,
                    com.example.intprogactivity.domain.model.FlightOffer::class.java
                )
                val itinerary = offer.itineraries.firstOrNull()
                val firstSeg = itinerary?.segments?.firstOrNull()
                val lastSeg = itinerary?.segments?.lastOrNull()

                binding.tvOriginCode.text = firstSeg?.departure?.iataCode ?: ""
                binding.tvDestCode.text = lastSeg?.arrival?.iataCode ?: ""
                binding.tvDepartTime.text = firstSeg?.departure?.at?.formatDisplayTime() ?: ""
                binding.tvArriveTime.text = lastSeg?.arrival?.at?.formatDisplayTime() ?: ""
                binding.tvDuration.text = formatDuration(itinerary?.duration ?: "")
            }

            binding.tvTravelDate.text = runCatching {
                dateFormat.format(booking.travelDate)
            }.getOrDefault("")

            val paxCount = booking.passengers.size.takeIf { it > 0 } ?: 1
            binding.tvPassengerCount.text = "$paxCount Passenger${if (paxCount != 1) "s" else ""}"
            binding.tvPrice.text = "₱${String.format("%,.0f", booking.totalPrice)}"

            binding.root.setOnClickListener { onBookingClick(booking) }
        }

        private fun formatDuration(iso: String): String {
            val h = Regex("(\\d+)H").find(iso)?.groupValues?.get(1) ?: "0"
            val m = Regex("(\\d+)M").find(iso)?.groupValues?.get(1) ?: "0"
            return "${h}h ${m}m"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Booking>() {
            override fun areItemsTheSame(old: Booking, new: Booking) = old.bookingId == new.bookingId
            override fun areContentsTheSame(old: Booking, new: Booking) = old == new
        }
    }
}
