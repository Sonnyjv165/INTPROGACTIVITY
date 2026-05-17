package com.example.intprogactivity.presentation.booking

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.R
import com.example.intprogactivity.databinding.FragmentPassengerDetailsBinding
import com.example.intprogactivity.domain.model.FlightOffer
import com.example.intprogactivity.domain.model.PassengerType
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PassengerDetailsFragment : Fragment() {

    private var _binding: FragmentPassengerDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingViewModel by activityViewModels()
    private val gson = Gson()

    private data class PassengerFormViews(
        val root: View,
        val tvTitle: TextView,
        val tilFirstName: TextInputLayout,
        val etFirstName: TextInputEditText,
        val tilLastName: TextInputLayout,
        val etLastName: TextInputEditText,
        val tilDob: TextInputLayout,
        val etDob: TextInputEditText,
        val tilPassport: TextInputLayout,
        val etPassport: TextInputEditText,
        val tilNationality: TextInputLayout,
        val etNationality: TextInputEditText,
        val rgGender: RadioGroup,
        val type: PassengerType
    )

    private val passengerForms = mutableListOf<PassengerFormViews>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPassengerDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val offerJson = arguments?.getString("flightOfferJson") ?: ""
        val offer = gson.fromJson(offerJson, FlightOffer::class.java)
        val adults = arguments?.getInt("adults", 1) ?: 1
        val children = arguments?.getInt("children", 0) ?: 0
        val infants = arguments?.getInt("infants", 0) ?: 0

        viewModel.resetForNewBooking()
        viewModel.setFlightOffer(offer)
        viewModel.initPassengers(adults, children, infants)

        val returnJson = arguments?.getString("returnFlightJson") ?: ""
        if (returnJson.isNotEmpty()) {
            runCatching { gson.fromJson(returnJson, FlightOffer::class.java) }
                .onSuccess { viewModel.setReturnFlightOffer(it) }
        }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        buildPassengerForms(adults, children, infants)
        setupContactPhoneFormatting()

        binding.btnContinue.setOnClickListener {
            if (validateAllForms()) {
                collectAllPassengers()
                val email = binding.etContactEmail.text?.toString()?.trim() ?: ""
                val phone = binding.etContactPhone.text?.toString()?.trim() ?: ""
                findNavController().navigate(R.id.action_passenger_to_addons, Bundle().apply {
                    putString("contactEmail", email)
                    putString("contactPhone", phone)
                })
            }
        }
    }

    private fun buildPassengerForms(adults: Int, children: Int, infants: Int) {
        passengerForms.clear()
        binding.llPassengerForms.removeAllViews()

        val types = mutableListOf<PassengerType>()
        repeat(adults) { types.add(PassengerType.ADULT) }
        repeat(children) { types.add(PassengerType.CHILD) }
        repeat(infants) { types.add(PassengerType.INFANT) }

        types.forEachIndexed { index, type ->
            val formView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_passenger_form, binding.llPassengerForms, false)

            val form = PassengerFormViews(
                root = formView,
                tvTitle = formView.findViewById(R.id.tvPassengerTitle),
                tilFirstName = formView.findViewById(R.id.tilFirstName),
                etFirstName = formView.findViewById(R.id.etFirstName),
                tilLastName = formView.findViewById(R.id.tilLastName),
                etLastName = formView.findViewById(R.id.etLastName),
                tilDob = formView.findViewById(R.id.tilDateOfBirth),
                etDob = formView.findViewById(R.id.etDateOfBirth),
                tilPassport = formView.findViewById(R.id.tilPassportNumber),
                etPassport = formView.findViewById(R.id.etPassportNumber),
                tilNationality = formView.findViewById(R.id.tilNationality),
                etNationality = formView.findViewById(R.id.etNationality),
                rgGender = formView.findViewById(R.id.rgGender),
                type = type
            )

            val typeLabel = when (type) {
                PassengerType.ADULT -> "Adult"
                PassengerType.CHILD -> "Child"
                PassengerType.INFANT -> "Infant"
            }
            form.tvTitle.text = "Passenger ${index + 1} ($typeLabel)"
            setupValidationClearers(form)
            passengerForms.add(form)
            binding.llPassengerForms.addView(formView)
        }
    }

    private fun setupValidationClearers(form: PassengerFormViews) {
        listOf(
            form.etFirstName to form.tilFirstName,
            form.etLastName to form.tilLastName,
            form.etDob to form.tilDob,
            form.etPassport to form.tilPassport,
            form.etNationality to form.tilNationality
        ).forEach { (editText, layout) ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    layout.error = null
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun setupContactPhoneFormatting() {
        binding.tilContactPhone.hint = "+63 | 9xx xxx xxxx"
        binding.etContactPhone.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true
                var digits = s.toString().filter { it.isDigit() }
                if (digits.startsWith("63")) digits = digits.substring(2)
                if (digits.startsWith("0")) digits = digits.substring(1)
                if (digits.length > 10) digits = digits.substring(0, 10)
                val formatted = when {
                    digits.length > 6 -> "${digits.substring(0, 3)} ${digits.substring(3, 6)} ${digits.substring(6)}"
                    digits.length > 3 -> "${digits.substring(0, 3)} ${digits.substring(3)}"
                    else -> digits
                }
                s.replace(0, s.length, formatted)
                isFormatting = false
            }
        })
        binding.etContactEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilContactEmail.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.etContactPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilContactPhone.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun validateAllForms(): Boolean {
        var valid = true

        passengerForms.forEach { form ->
            val firstName = form.etFirstName.text?.toString()?.trim() ?: ""
            if (firstName.isBlank()) {
                form.tilFirstName.error = "First name is required"
                valid = false
            }
            val lastName = form.etLastName.text?.toString()?.trim() ?: ""
            if (lastName.isBlank()) {
                form.tilLastName.error = "Last name is required"
                valid = false
            }
            val dob = form.etDob.text?.toString()?.trim() ?: ""
            if (dob.isBlank()) {
                form.tilDob.error = "Date of birth is required"
                valid = false
            } else if (!dob.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                form.tilDob.error = "Use format YYYY-MM-DD"
                valid = false
            }
            val passport = form.etPassport.text?.toString()?.trim() ?: ""
            if (passport.isBlank()) {
                form.tilPassport.error = "Passport number is required"
                valid = false
            }
            val nationality = form.etNationality.text?.toString()?.trim() ?: ""
            if (nationality.isBlank()) {
                form.tilNationality.error = "Nationality is required (e.g. PH)"
                valid = false
            }
        }

        val email = binding.etContactEmail.text?.toString()?.trim() ?: ""
        if (email.isBlank()) {
            binding.tilContactEmail.error = "Email is required"
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilContactEmail.error = "Enter a valid email address"
            valid = false
        }

        return valid
    }

    private fun collectAllPassengers() {
        val email = binding.etContactEmail.text?.toString()?.trim() ?: ""
        val phone = binding.etContactPhone.text?.toString()?.trim() ?: ""
        passengerForms.forEachIndexed { index, form ->
            val firstName = form.etFirstName.text?.toString()?.trim() ?: ""
            val lastName = form.etLastName.text?.toString()?.trim() ?: ""
            val dob = form.etDob.text?.toString()?.trim() ?: ""
            val passport = form.etPassport.text?.toString()?.trim() ?: ""
            val nationality = form.etNationality.text?.toString()?.trim() ?: ""
            val gender = if ((form.rgGender.checkedRadioButtonId == R.id.rbMale)) "MALE" else "FEMALE"
            viewModel.setPassenger(index, firstName, lastName, dob, gender, passport, nationality)
        }
        viewModel.setContactInfo(email, phone)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
