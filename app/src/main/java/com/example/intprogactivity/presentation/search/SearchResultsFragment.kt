package com.example.intprogactivity.presentation.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.intprogactivity.R
import com.example.intprogactivity.databinding.FragmentSearchResultsBinding
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.model.FlightSearchParams
import com.example.intprogactivity.presentation.booking.BookingViewModel
import com.example.intprogactivity.util.UiState
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchResultsFragment : Fragment() {

    private var _binding: FragmentSearchResultsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private val bookingViewModel: BookingViewModel by activityViewModels()
    private lateinit var adapter: FlightOfferAdapter
    private val gson = Gson()
    private lateinit var searchParams: FlightSearchParams

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val paramsJson = arguments?.getString("searchParamsJson").orEmpty()
        val parsed = if (paramsJson.isNotEmpty())
            runCatching { gson.fromJson(paramsJson, FlightSearchParams::class.java) }.getOrNull()
        else null

        if (parsed == null) {
            findNavController().navigateUp()
            return
        }
        searchParams = parsed

        setupHeader()
        setupAdapter()
        setupFilters()
        observeViewModel()

        viewModel.search(searchParams)
    }

    private fun setupHeader() {
        binding.tvRoute.text = "${searchParams.origin} → ${searchParams.destination}"
        binding.tvSearchMeta.text = "${searchParams.departureDate} · ${searchParams.passengerSummary()}"
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnEditSearch.setOnClickListener { findNavController().navigateUp() }
    }

    private fun setupAdapter() {
        adapter = FlightOfferAdapter { offer ->
            navigateToDetail(offer)
        }
        binding.rvFlights.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFlights.adapter = adapter
    }

    private fun navigateToDetail(offer: FlightOffer) {
        val offerJson = gson.toJson(offer)
        val bundle = Bundle().apply {
            putString("flightOfferJson", offerJson)
            putBoolean("isRoundTrip", searchParams.isRoundTrip())
            putString("returnDate", searchParams.returnDate ?: "")
            putString("searchParamsJson", gson.toJson(searchParams))
        }
        findNavController().navigate(R.id.action_results_to_detail, bundle)
    }

    private fun setupFilters() {
        binding.chipSort.setOnClickListener {
            val modes = SortMode.values()
            val current = viewModel.sortMode.value
            val next = modes[(current.ordinal + 1) % modes.size]
            viewModel.setSortMode(next)
            binding.chipSort.text = "Sort: ${next.name.lowercase().replaceFirstChar { it.uppercase() }}"
        }
        binding.chipStops.setOnClickListener {
            val current = viewModel.maxStops.value
            val next = when (current) {
                null -> 0
                0 -> 1
                else -> null
            }
            viewModel.setMaxStops(next)
            binding.chipStops.text = when (next) {
                null -> "Stops"
                0 -> "Direct only"
                else -> "Max $next stop"
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchState.collect { state ->
                    binding.layoutLoading.isVisible = state is UiState.Loading
                    val successList = (state as? UiState.Success)?.data
                    binding.layoutEmpty.isVisible = successList != null && successList.isEmpty()
                    binding.rvFlights.isVisible = successList != null && successList.isNotEmpty()
                    binding.tvResultCount.isVisible = successList != null

                    when (state) {
                        is UiState.Success -> {
                            val offers = state.data
                            adapter.submitList(offers)
                            binding.tvResultCount.text = "${offers.size} flight${if (offers.size != 1) "s" else ""} found"
                        }
                        is UiState.Error -> {
                            binding.layoutLoading.isVisible = false
                            binding.layoutEmpty.isVisible = true
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
