package com.example.intprogactivity.presentation.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.intprogactivity.databinding.FragmentPriceAlertsBinding
import com.example.intprogactivity.databinding.ItemPriceAlertBinding
import com.example.intprogactivity.domain.model.PriceAlert
import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.domain.repository.PriceAlertRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PriceAlertsFragment : Fragment() {

    private var _binding: FragmentPriceAlertsBinding? = null
    private val binding get() = _binding!!
    private val adapter = PriceAlertAdapter()

    @Inject lateinit var priceAlertRepository: PriceAlertRepository
    @Inject lateinit var authRepository: AuthRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPriceAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.rvAlerts.adapter = adapter
        binding.fabAddAlert.setOnClickListener {
            // TODO: show create alert bottom sheet
        }
        loadAlerts()
    }

    private fun loadAlerts() {
        viewLifecycleOwner.lifecycleScope.launch {
            val uid = authRepository.currentUser.first()?.uid ?: return@launch
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                priceAlertRepository.getUserAlertsFlow(uid).collect { alerts ->
                    adapter.submitList(alerts)
                    binding.layoutEmpty.isVisible = alerts.isEmpty()
                    binding.rvAlerts.isVisible = alerts.isNotEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class PriceAlertAdapter : ListAdapter<PriceAlert, PriceAlertAdapter.AlertViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemPriceAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) = holder.bind(getItem(position))

    inner class AlertViewHolder(private val binding: ItemPriceAlertBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(alert: PriceAlert) {
            binding.tvRoute.text = "${alert.origin} → ${alert.destination}"
            binding.tvTargetPrice.text = "Alert when below $${String.format("%.0f", alert.targetPrice)}"
            binding.switchActive.isChecked = alert.isActive
            binding.tvActiveStatus.text = if (alert.isActive) "Active" else "Paused"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PriceAlert>() {
            override fun areItemsTheSame(old: PriceAlert, new: PriceAlert) = old.alertId == new.alertId
            override fun areContentsTheSame(old: PriceAlert, new: PriceAlert) = old == new
        }
    }
}
