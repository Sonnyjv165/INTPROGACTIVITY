package com.example.intprogactivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.intprogactivity.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        val hiddenFromNav = setOf(
            R.id.splashFragment,
            R.id.loginFragment,
            R.id.registerFragment,
            R.id.forgotPasswordFragment,
            R.id.flightDetailFragment,
            R.id.passengerDetailsFragment,
            R.id.addOnsFragment,
            R.id.seatMapFragment,
            R.id.checkoutFragment,
            R.id.confirmationFragment,
            R.id.bookingDetailFragment,
            R.id.priceAlertsFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible = destination.id !in hiddenFromNav
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp() || super.onSupportNavigateUp()
}
