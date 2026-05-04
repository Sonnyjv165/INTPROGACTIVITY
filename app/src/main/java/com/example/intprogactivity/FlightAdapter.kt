package com.example.intprogactivity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
data class Flight(
    val airline: String,
    val time: String,
    val price: Int
)
class FlightAdapter(private var flights: List<Flight>) :
    RecyclerView.Adapter<FlightAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val airline: TextView = view.findViewById(R.id.airlineText)
        val time: TextView = view.findViewById(R.id.timeText)
        val price: TextView = view.findViewById(R.id.priceText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.flight_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = flights.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val flight = flights[position]
        holder.airline.text = flight.airline
        holder.time.text = flight.time
        holder.price.text = "₱${flight.price}"
    }

    fun updateList(newList: List<Flight>) {
        flights = newList
        notifyDataSetChanged()
    }
}