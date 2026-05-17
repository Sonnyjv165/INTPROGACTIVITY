package com.example.intprogactivity.presentation.rewards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.intprogactivity.databinding.ItemCoinTransactionBinding
import com.example.intprogactivity.domain.model.TransactionType
import com.example.intprogactivity.domain.model.TripCoinTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CoinHistoryAdapter : ListAdapter<TripCoinTransaction, CoinHistoryAdapter.TxViewHolder>(DIFF) {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TxViewHolder {
        val binding = ItemCoinTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TxViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TxViewHolder, position: Int) = holder.bind(getItem(position))

    inner class TxViewHolder(private val binding: ItemCoinTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tx: TripCoinTransaction) {
            binding.tvDescription.text = tx.description
            binding.tvDate.text = runCatching { dateFormat.format(Date(tx.createdAt)) }.getOrDefault("")

            val isEarned = tx.type == TransactionType.EARNED || tx.type == TransactionType.BONUS
            val prefix = if (isEarned) "+" else "-"
            binding.tvAmount.text = "$prefix${String.format("%,d", tx.amount)}"
            binding.tvAmount.setTextColor(
                binding.root.context.getColor(
                    if (isEarned) com.example.intprogactivity.R.color.success
                    else com.example.intprogactivity.R.color.error_color
                )
            )
            binding.tvTransactionIcon.text = when (tx.type) {
                TransactionType.EARNED -> "🪙"
                TransactionType.REDEEMED -> "🛍"
                TransactionType.EXPIRED -> "⏰"
                TransactionType.BONUS -> "🎁"
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TripCoinTransaction>() {
            override fun areItemsTheSame(old: TripCoinTransaction, new: TripCoinTransaction) =
                old.transactionId == new.transactionId
            override fun areContentsTheSame(old: TripCoinTransaction, new: TripCoinTransaction) =
                old == new
        }
    }
}
