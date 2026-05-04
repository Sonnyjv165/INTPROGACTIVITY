package com.example.intprogactivity

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import android.view.View

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        val profileIcon = findViewById<ImageView>(R.id.profileIcon)
        val flightsIcon = findViewById<ImageView>(R.id.flightsIcon)

        flightsIcon.setOnClickListener {
            val intent = Intent(this, FlightsActivity::class.java)
            startActivity(intent)
        }

        val image1 = findViewById<ImageView>(R.id.promoBanner)


        val tripUrl = "https://www.trip.com/flights/"

        image1.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tripUrl)))
        }



        // ✅ Receive data from Login
        val email = intent.getStringExtra("USER_EMAIL") ?: "Guest"
        val password = intent.getStringExtra("USER_PASSWORD") ?: "No Password"

        welcomeText.text = "Welcome, $email 👋"

        // 👉 Go to Account Info
        profileIcon.setOnClickListener {
            val intent = Intent(this, AccountInfoActivity::class.java)
            intent.putExtra("USER_EMAIL", email)
            intent.putExtra("USER_PASSWORD", password)
            startActivity(intent)
        }

        // 📋 LISTVIEW & ARRAYADAPTER IMPLEMENTATION
        val bookingsListView = findViewById<ListView>(R.id.bookingsListView)
        
        // Using the Flight Data Class to create booking data
        val myBookings = arrayListOf(
            "✈️ " + Flight("Cebu Pacific", "Dec 20, 2023", 2500).let { "${it.airline}: Manila to Cebu (${it.time})" },
            "✈️ " + Flight("AirAsia", "Jan 05, 2024", 2800).let { "${it.airline}: Cebu to Bali (${it.time})" },
            "✈️ " + Flight("PH Airlines", "Mar 12, 2024", 5200).let { "${it.airline}: Manila to Tokyo (${it.time})" }
        )

        // Initialize ArrayAdapter
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1, // Default Android layout for list items
            myBookings
        )

        // Set adapter to ListView
        bookingsListView.adapter = adapter

        // Optional: Item click listener
        bookingsListView.setOnItemClickListener { _, _, position, _ ->
            Toast.makeText(this, "Selected: ${myBookings[position]}", Toast.LENGTH_SHORT).show()
        }
    }
}