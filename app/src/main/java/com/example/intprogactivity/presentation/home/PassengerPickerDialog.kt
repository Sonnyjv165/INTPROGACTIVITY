package com.example.intprogactivity.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import com.example.intprogactivity.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PassengerPickerDialog(
    private val adults: Int,
    private val children: Int,
    private val infants: Int,
    private val onConfirm: (adults: Int, children: Int, infants: Int) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.dialog_passenger_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val npAdults = view.findViewById<NumberPicker>(R.id.npAdults)
        val npChildren = view.findViewById<NumberPicker>(R.id.npChildren)
        val npInfants = view.findViewById<NumberPicker>(R.id.npInfants)

        npAdults.minValue = 1; npAdults.maxValue = 9; npAdults.value = adults
        npChildren.minValue = 0; npChildren.maxValue = 8; npChildren.value = children
        npInfants.minValue = 0; npInfants.maxValue = 4; npInfants.value = infants

        view.findViewById<View>(R.id.btnConfirmPassengers).setOnClickListener {
            onConfirm(npAdults.value, npChildren.value, npInfants.value)
            dismiss()
        }
    }
}
