package com.example.intprogactivity.presentation.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.databinding.FragmentPassengerDetailsBinding
import com.example.intprogactivity.domain.model.FlightOffer
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PassengerDetailsFragment : Fragment() {

    private var _binding: FragmentPassengerDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingViewModel by viewModels()
    private val gson = Gson()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPassengerDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val offerJson = arguments?.getString("flightOfferJson") ?: ""
        val offer = gson.fromJson(offerJson, FlightOffer::class.java)
        viewModel.setFlightOffer(offer)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnContinue.setOnClickListener {
            val firstName = binding.etFirstName.text?.toString()?.trim() ?: ""
            val lastName = binding.etLastName.text?.toString()?.trim() ?: ""
            val dob = binding.etDateOfBirth.text?.toString()?.trim() ?: ""
            val passport = binding.etPassportNumber.text?.toString()?.trim() ?: ""
            val nationality = binding.etNationality.text?.toString()?.trim() ?: ""
            val email = binding.etContactEmail.text?.toString()?.trim() ?: ""

            if (firstName.isBlank() || lastName.isBlank()) {
                Snackbar.make(binding.root, "Please enter passenger name", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isBlank()) {
                Snackbar.make(binding.root, "Please enter contact email", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gender = if (binding.rbMale.isChecked) "MALE" else "FEMALE"
            viewModel.setPassenger(0, firstName, lastName, dob, gender, passport, nationality)

            findNavController().navigate(R.id.action_passenger_to_addons, Bundle().apply {
                putString("contactEmail", email)
                putString("contactPhone", binding.etContactPhone.text?.toString()?.trim() ?: "")
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
