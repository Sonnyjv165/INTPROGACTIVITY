package com.example.intprogactivity.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.intprogactivity.databinding.ItemDealBinding

data class DealItem(
    val destination: String,
    val originCity: String,
    val price: String,
    val discount: String,
    val emoji: String,
    val originCode: String,
    val destinationCode: String
)

class DealsAdapter(
    private val onDealClick: (DealItem) -> Unit
) : ListAdapter<DealItem, DealsAdapter.DealViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DealViewHolder {
        val binding = ItemDealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DealViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DealViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DealViewHolder(private val binding: ItemDealBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(deal: DealItem) {
            binding.tvDealDestination.text = deal.destination
            binding.tvDealOrigin.text = "From ${deal.originCity}"
            binding.tvDealPrice.text = deal.price
            binding.tvDiscount.text = deal.discount
            binding.tvDestinationEmoji.text = deal.emoji
            binding.root.setOnClickListener { onDealClick(deal) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DealItem>() {
            override fun areItemsTheSame(old: DealItem, new: DealItem) =
                old.destinationCode == new.destinationCode
            override fun areContentsTheSame(old: DealItem, new: DealItem) = old == new
        }
    }
}
