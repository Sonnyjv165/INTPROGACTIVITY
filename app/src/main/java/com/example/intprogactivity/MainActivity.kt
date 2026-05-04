package com.example.intprogactivity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val forgotPassword = findViewById<TextView>(R.id.forgotPassword)
        val registerText = findViewById<TextView>(R.id.registerText)
        val togglePassword = findViewById<ImageButton>(R.id.togglePassword)

        var isVisible = false

        togglePassword.setOnClickListener {
            isVisible = !isVisible

            if (isVisible) {
                passwordInput.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                passwordInput.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            passwordInput.setSelection(passwordInput.text.length)
        }

        val registeredEmail = intent.getStringExtra("REGISTERED_EMAIL")
        if (registeredEmail != null) {
            emailInput.setText(registeredEmail)
        }

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            val sharedPref = getSharedPreferences("USER_DATA", MODE_PRIVATE)
            val savedEmail = sharedPref.getString("EMAIL", null)
            val savedPass = sharedPref.getString("PASSWORD", null)

            if (email == savedEmail && password == savedPass) {
                val intent = Intent(this, DashboardActivity::class.java)
                intent.putExtra("USER_EMAIL", email)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show()
            }
        }

        // NAVIGATION
        registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
}