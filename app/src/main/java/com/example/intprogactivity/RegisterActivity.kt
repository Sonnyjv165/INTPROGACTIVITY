package com.example.intprogactivity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val email = findViewById<EditText>(R.id.regEmail)
        val password = findViewById<EditText>(R.id.regPassword)
        val confirmPassword = findViewById<EditText>(R.id.regConfirmPassword)
        val registerBtn = findViewById<Button>(R.id.registerBtn)
        val backToLogin = findViewById<TextView>(R.id.backToLogin)

        sharedPref = getSharedPreferences("USER_DATA", MODE_PRIVATE)

        registerBtn.setOnClickListener {
            val userEmail = email.text.toString()
            val userPass = password.text.toString()
            val confirmPass = confirmPassword.text.toString()

            if (userEmail.isEmpty() || userPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            }
            else if (userPass != confirmPass) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            }
            else {
                // ✅ SAVE DATA
                val editor = sharedPref.edit()
                editor.putString("EMAIL", userEmail)
                editor.putString("PASSWORD", userPass)
                editor.apply()

                Toast.makeText(this, "Registered Successfully!", Toast.LENGTH_SHORT).show()

                // ✅ PASS DATA BACK
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("REGISTERED_EMAIL", userEmail)
                startActivity(intent)
                finish()
            }
        }

        backToLogin.setOnClickListener {
            finish()
        }
    }
}