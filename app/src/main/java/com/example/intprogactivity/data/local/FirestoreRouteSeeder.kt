package com.example.intprogactivity.data.local

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * One-time seeder that populates the Firestore `routes` collection from the local
 * flight templates if the collection is empty. Safe to call on every app launch —
 * it's a no-op once data already exists.
 */
object FirestoreRouteSeeder {

    private const val TAG = "FirestoreRouteSeeder"
    private const val COLLECTION = "routes"

    /** Seeds routes only if the collection is currently empty. */
    suspend fun seedIfEmpty(firestore: FirebaseFirestore) {
        try {
            val existing = firestore.collection(COLLECTION).limit(1).get().await()
            if (!existing.isEmpty) {
                Log.d(TAG, "Routes collection already seeded — skipping.")
                return
            }
            writeRoutes(firestore)
        } catch (e: Exception) {
            Log.e(TAG, "Seeding failed: ${e.message}", e)
        }
    }

    /** Always writes all routes (overwrites existing documents). Use once to fix missing data. */
    suspend fun seedAlways(firestore: FirebaseFirestore) {
        try {
            Log.d(TAG, "Force-seeding all routes into Firestore…")
            writeRoutes(firestore)
        } catch (e: Exception) {
            Log.e(TAG, "Force-seeding failed: ${e.message}", e)
        }
    }

    private suspend fun writeRoutes(firestore: FirebaseFirestore) {
        Log.d(TAG, "Writing ${LocalFlightData.routes.size} routes into Firestore…")

        // Firestore batches are limited to 500 operations
        val entries = LocalFlightData.routes.entries.toList()
        val batchSize = 400
        var written = 0

        entries.chunked(batchSize).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { (routeKey, templates) ->
                val ref = firestore.collection(COLLECTION).document(routeKey)
                batch.set(ref, mapOf(
                    "templates" to templates.map { t ->
                        mapOf(
                            "airlineCode"     to t.airlineCode,
                            "flightNumber"    to t.flightNumber,
                            "departureHour"   to t.departureHour,
                            "departureMinute" to t.departureMinute,
                            "durationMinutes" to t.durationMinutes,
                            "basePricePhp"    to t.basePricePhp,
                            "aircraft"        to t.aircraft
                        )
                    }
                ))
            }
            batch.commit().await()
            written += chunk.size
            Log.d(TAG, "Batch committed ($written / ${entries.size})")
        }

        Log.d(TAG, "Seeding complete.")
    }
}
