package com.example.intprogactivity.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.intprogactivity.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.DATASTORE_PREFS_NAME
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_ORIGIN = stringPreferencesKey(Constants.KEY_LAST_SEARCH_ORIGIN)
    private val KEY_ORIGIN_IATA = stringPreferencesKey(Constants.KEY_LAST_SEARCH_ORIGIN_IATA)
    private val KEY_DEST = stringPreferencesKey(Constants.KEY_LAST_SEARCH_DESTINATION)
    private val KEY_DEST_IATA = stringPreferencesKey(Constants.KEY_LAST_SEARCH_DESTINATION_IATA)

    val lastSearchOrigin: Flow<String> = context.dataStore.data.map { it[KEY_ORIGIN] ?: "" }
    val lastSearchOriginIata: Flow<String> = context.dataStore.data.map { it[KEY_ORIGIN_IATA] ?: "" }
    val lastSearchDestination: Flow<String> = context.dataStore.data.map { it[KEY_DEST] ?: "" }
    val lastSearchDestinationIata: Flow<String> = context.dataStore.data.map { it[KEY_DEST_IATA] ?: "" }

    suspend fun saveLastSearch(originCity: String, originIata: String, destCity: String, destIata: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ORIGIN] = originCity
            prefs[KEY_ORIGIN_IATA] = originIata
            prefs[KEY_DEST] = destCity
            prefs[KEY_DEST_IATA] = destIata
        }
    }
}
