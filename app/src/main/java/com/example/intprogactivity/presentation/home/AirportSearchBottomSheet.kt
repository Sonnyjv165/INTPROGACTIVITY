package com.example.intprogactivity.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.intprogactivity.databinding.FragmentAirportSearchBottomSheetBinding
import com.example.intprogactivity.databinding.ItemAirportBinding
import com.example.intprogactivity.domain.model.Airport
import com.example.intprogactivity.util.UiState
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class AirportSearchBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "AirportSearchBottomSheet"
        const val MODE_ORIGIN = "origin"
        const val MODE_DESTINATION = "destination"
        private const val ARG_MODE = "mode"
        private const val ARG_TITLE = "title"

        fun newInstance(mode: String, title: String) = AirportSearchBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_MODE, mode)
                putString(ARG_TITLE, title)
            }
        }
    }

    private var _binding: FragmentAirportSearchBottomSheetBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: AirportListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAirportSearchBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mode = arguments?.getString(ARG_MODE) ?: MODE_ORIGIN
        binding.tvTitle.text = arguments?.getString(ARG_TITLE) ?: "Select Airport"

        adapter = AirportListAdapter { airport ->
            if (mode == MODE_ORIGIN) homeViewModel.setOrigin(airport)
            else homeViewModel.setDestination(airport)
            dismiss()
        }
        binding.rvAirports.adapter = adapter

        binding.etSearch.doAfterTextChanged { text ->
            homeViewModel.onAirportQueryChanged(text?.toString() ?: "")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.airportSearchState.collect { state ->
                    binding.progressBar.isVisible = state is UiState.Loading
                    binding.tvEmpty.isVisible = state is UiState.Idle
                    when (state) {
                        is UiState.Success -> adapter.submitList(state.data)
                        is UiState.Idle -> adapter.submitList(emptyList())
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        sheet?.let {
            BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
        }
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class AirportListAdapter(
    private val onAirportClick: (Airport) -> Unit
) : ListAdapter<Airport, AirportListAdapter.AirportViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AirportViewHolder {
        val binding = ItemAirportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AirportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AirportViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class AirportViewHolder(private val binding: ItemAirportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(airport: Airport) {
            binding.tvIata.text = airport.iataCode
            binding.tvAirportName.text = airport.name
            binding.tvCityCountry.text = "${airport.cityName}, ${airport.countryName}"
            binding.root.setOnClickListener { onAirportClick(airport) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Airport>() {
            override fun areItemsTheSame(old: Airport, new: Airport) = old.iataCode == new.iataCode
            override fun areContentsTheSame(old: Airport, new: Airport) = old == new
        }
    }
}
