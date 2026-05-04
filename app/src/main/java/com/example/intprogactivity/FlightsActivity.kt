package com.example.intprogactivity

import com.google.android.material.datepicker.MaterialDatePicker
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class FlightsActivity : AppCompatActivity() {

    lateinit var dateText: TextView

    var startDate = ""
    var endDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flights)

        val fromInput = findViewById<EditText>(R.id.fromInput)
        val toInput = findViewById<EditText>(R.id.toInput)
        val searchBtn = findViewById<Button>(R.id.searchBtn)
        val tripTypeGroup = findViewById<RadioGroup>(R.id.tripTypeGroup)

        val recycler = findViewById<RecyclerView>(R.id.flightRecycler)
        recycler.layoutManager = LinearLayoutManager(this)

        val flights = listOf(
            Flight("Cebu Pacific", "6:00 AM - 7:30 AM", 2500),
            Flight("Philippine Airlines", "9:00 AM - 10:30 AM", 3200),
            Flight("AirAsia", "1:00 PM - 2:30 PM", 2800)
        )

        val adapter = FlightAdapter(flights)
        recycler.adapter = adapter

        dateText = findViewById(R.id.dateText)


        // 📅 DATE PICKER (START + END)
        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select travel dates")
            .build()

        dateText.setOnClickListener {
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        datePicker.addOnPositiveButtonClickListener { selection ->
            startDate = selection.first.toString()
            endDate = selection.second.toString()

            dateText.text = "Selected: $startDate - $endDate"
        }
        val filterGroup = findViewById<RadioGroup>(R.id.filterGroup)

        filterGroup.setOnCheckedChangeListener { _, checkedId ->

            when (checkedId) {

                R.id.filterDefault -> {
                    adapter.updateList(flights) // original list
                }

                R.id.filterPrice -> {
                    val sorted = flights.sortedBy { it.price }
                    adapter.updateList(sorted)
                }

                R.id.filterTime -> {
                    val sorted = flights.sortedBy { it.time }
                    adapter.updateList(sorted)
                }
            }
        }
        // 🔍 SEARCH BUTTON
        searchBtn.setOnClickListener {
            val from = fromInput.text.toString()
            val to = toInput.text.toString()

            if (from.isEmpty() || to.isEmpty() || startDate.isEmpty()) {
                Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // SHOW RESULTS USING RECYCLER
            recycler.adapter = adapter

            Toast.makeText(this, "Flights found!", Toast.LENGTH_SHORT).show()
        }
    }


    // EXTENSION FOR MARGIN
    private fun TextView.setMarginBottom(margin: Int) {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, margin)
        layoutParams = params
    }
}