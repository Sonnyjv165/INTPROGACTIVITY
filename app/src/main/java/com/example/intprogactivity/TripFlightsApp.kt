package com.example.intprogactivity

import android.app.Application
import com.example.intprogactivity.data.local.FirestoreRouteSeeder
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class TripFlightsApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Seed Firestore route data on first launch (no-op if already seeded)
        appScope.launch {
            FirestoreRouteSeeder.seedIfEmpty(FirebaseFirestore.getInstance())
        }
    }
}
