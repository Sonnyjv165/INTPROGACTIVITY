package com.example.intprogactivity.data.remote.firebase

import com.example.intprogactivity.data.local.RouteTemplate
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreFlightSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // In-memory cache — routes don't change at runtime so one fetch per app session is enough
    private var cachedRoutes: Map<String, List<RouteTemplate>>? = null
    private val mutex = Mutex()

    internal suspend fun getRoutes(): Map<String, List<RouteTemplate>> {
        cachedRoutes?.let { return it }
        return mutex.withLock {
            // Double-check after acquiring the lock
            cachedRoutes?.let { return it }
            val snapshot = firestore.collection("routes").get().await()
            val routes = snapshot.documents.associate { doc ->
                doc.id to parseTemplates(doc)
            }
            cachedRoutes = routes
            routes
        }
    }

    /** Clear the cache — call this if routes are updated in Firestore at runtime */
    fun invalidateCache() {
        cachedRoutes = null
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseTemplates(doc: DocumentSnapshot): List<RouteTemplate> {
        val rawList = doc.get("templates") as? List<Map<String, Any>> ?: return emptyList()
        return rawList.mapNotNull { map ->
            try {
                RouteTemplate(
                    airlineCode     = map["airlineCode"] as String,
                    flightNumber    = map["flightNumber"] as String,
                    departureHour   = (map["departureHour"] as Long).toInt(),
                    departureMinute = (map["departureMinute"] as Long).toInt(),
                    durationMinutes = (map["durationMinutes"] as Long).toInt(),
                    basePricePhp    = (map["basePricePhp"] as? Double)
                        ?: (map["basePricePhp"] as Long).toDouble(),
                    aircraft        = map["aircraft"] as? String ?: "320"
                )
            } catch (e: Exception) {
                null // skip malformed entries
            }
        }
    }
}
