package com.example.intprogactivity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AccountInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_info)

        val emailText = findViewById<TextView>(R.id.emailText)
        val passwordText = findViewById<TextView>(R.id.passwordText)
        val backBtn = findViewById<Button>(R.id.backBtn)
        val toggleBtn = findViewById<TextView>(R.id.togglePasswordBtn)

        // ✅ Receive data from Dashboard
        val email = intent.getStringExtra("USER_EMAIL") ?: "No Email"

        val sharedPref = getSharedPreferences("USER_DATA", MODE_PRIVATE)
        val password = sharedPref.getString("PASSWORD", "") ?: ""

        emailText.text = email
        passwordText.setText(password)
        passwordText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)

        logoutBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        // Toggle password visibility
        var isPasswordVisible = false

        toggleBtn.setOnClickListener {
            if (isPasswordVisible) {
                // HIDE PASSWORD
                passwordText.inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleBtn.text = "Show Passowrd"
            } else {
                // SHOW PASSWORD
                passwordText.inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                toggleBtn.text = "Hide Password"
            }

            backBtn.setOnClickListener { finish() }
        }
    }
}