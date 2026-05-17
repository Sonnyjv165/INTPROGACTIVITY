package com.example.intprogactivity.presentation.booking

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.intprogactivity.R
import com.example.intprogactivity.databinding.ItemSeatBinding

enum class SeatState { AVAILABLE, SELECTED, TAKEN, AISLE }

data class SeatItem(
    val label: String,
    val state: SeatState
)

class SeatAdapter(
    private val onSeatSelected: (seatLabel: String) -> Unit
) : RecyclerView.Adapter<SeatAdapter.SeatViewHolder>() {

    private val seats: MutableList<SeatItem> = generateSeats()
    private var selectedPosition = -1

    private fun generateSeats(): MutableList<SeatItem> {
        val list = mutableListOf<SeatItem>()
        val takenSeats = setOf(
            "2A","2C","3B","5E","5F","7A","8D","9B","10F","11C",
            "12A","13E","14B","15D","16F","17A","18C","19B","20E",
            "21A","22F","23C","24D","25B","26E","27A","28C","29F","30D"
        )
        val columns = listOf("A","B","C","D","E","F")
        for (row in 1..30) {
            for (colIndex in 0..6) {
                if (colIndex == 3) {
                    list.add(SeatItem("", SeatState.AISLE))
                } else {
                    val actualCol = if (colIndex < 3) colIndex else colIndex - 1
                    val col = columns[actualCol]
                    val label = "$row$col"
                    val state = if (label in takenSeats) SeatState.TAKEN else SeatState.AVAILABLE
                    list.add(SeatItem(label, state))
                }
            }
        }
        return list
    }

    fun setSelectedSeat(seatLabel: String) {
        val prev = selectedPosition
        selectedPosition = seats.indexOfFirst { it.label == seatLabel && it.state != SeatState.TAKEN && it.state != SeatState.AISLE }
        if (prev >= 0) notifyItemChanged(prev)
        if (selectedPosition >= 0) notifyItemChanged(selectedPosition)
    }

    fun clearSelection() {
        val prev = selectedPosition
        selectedPosition = -1
        if (prev >= 0) notifyItemChanged(prev)
    }

    fun getSelectedSeat(): String? = if (selectedPosition >= 0) seats[selectedPosition].label else null

    override fun getItemCount() = seats.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val binding = ItemSeatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SeatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        holder.bind(seats[position], position == selectedPosition)
    }

    inner class SeatViewHolder(private val binding: ItemSeatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SeatItem, isSelected: Boolean) {
            if (item.state == SeatState.AISLE) {
                binding.tvSeat.text = ""
                binding.root.background = null
                binding.root.isClickable = false
                return
            }

            binding.tvSeat.text = item.label
            val ctx = binding.root.context
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 6f * ctx.resources.displayMetrics.density
            }

            when {
                isSelected -> {
                    drawable.setColor(ctx.getColor(R.color.brand_primary))
                    drawable.setStroke(0, Color.TRANSPARENT)
                    binding.tvSeat.setTextColor(Color.WHITE)
                }
                item.state == SeatState.TAKEN -> {
                    drawable.setColor(ctx.getColor(R.color.divider))
                    drawable.setStroke(0, Color.TRANSPARENT)
                    binding.tvSeat.setTextColor(ctx.getColor(R.color.text_secondary))
                    binding.root.isClickable = false
                }
                else -> {
                    drawable.setColor(Color.WHITE)
                    drawable.setStroke(
                        (1 * ctx.resources.displayMetrics.density).toInt(),
                        ctx.getColor(R.color.divider)
                    )
                    binding.tvSeat.setTextColor(ctx.getColor(R.color.text_primary))
                    binding.root.isClickable = true
                }
            }
            binding.root.background = drawable

            if (item.state != SeatState.TAKEN) {
                binding.root.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos == RecyclerView.NO_ID.toInt()) return@setOnClickListener
                    val prev = selectedPosition
                    selectedPosition = pos
                    if (prev >= 0) notifyItemChanged(prev)
                    notifyItemChanged(pos)
                    onSeatSelected(item.label)
                }
            }
        }
    }
}
