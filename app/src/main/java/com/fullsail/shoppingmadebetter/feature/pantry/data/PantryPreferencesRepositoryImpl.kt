package com.fullsail.shoppingmadebetter.feature.pantry.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class PantryPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PantryPreferencesRepository {

    override fun skipRemoveConfirmation(): Flow<Boolean> = dataStore.data
        // A failed read (e.g. corrupt/absent file) falls back to defaults rather
        // than crashing the collector — DataStore surfaces I/O errors in the flow.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[SKIP_REMOVE_CONFIRMATION] ?: false }

    override suspend fun setSkipRemoveConfirmation(skip: Boolean) {
        dataStore.edit { prefs -> prefs[SKIP_REMOVE_CONFIRMATION] = skip }
    }

    private companion object {
        val SKIP_REMOVE_CONFIRMATION = booleanPreferencesKey("pantry_skip_remove_confirmation")
    }
}