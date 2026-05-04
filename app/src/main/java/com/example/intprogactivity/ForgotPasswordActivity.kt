package com.example.intprogactivity

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {

    lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val newPassword = findViewById<EditText>(R.id.newPassword)
        val repeatPassword = findViewById<EditText>(R.id.repeatPassword)
        val submitBtn = findViewById<Button>(R.id.submitBtn)
        val backToLogin = findViewById<TextView>(R.id.backToLogin)

        sharedPref = getSharedPreferences("USER_DATA", MODE_PRIVATE)

        submitBtn.setOnClickListener {
            val inputEmail = emailInput.text.toString()
            val newPass = newPassword.text.toString()
            val repeatPass = repeatPassword.text.toString()

            val savedEmail = sharedPref.getString("EMAIL", null)

            if (inputEmail.isEmpty() || newPass.isEmpty() || repeatPass.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            }
            else if (inputEmail != savedEmail) {
                Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show()
            }
            else if (newPass != repeatPass) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            }
            else {
                // ✅ UPDATE PASSWORD
                val editor = sharedPref.edit()
                editor.putString("PASSWORD", newPass)
                editor.apply()

                Toast.makeText(this, "Password Updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        backToLogin.setOnClickListener {
            finish()
        }
    }
}