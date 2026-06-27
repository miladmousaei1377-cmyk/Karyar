package com.karland.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val ONBOARDING_KEY = booleanPreferencesKey("onboarding_completed")
    private val LAST_VERSION_KEY = intPreferencesKey("last_seen_version")

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE_KEY] ?: false
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_KEY] ?: false
    }

    val lastSeenVersionCode: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[LAST_VERSION_KEY] ?: 0
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DARK_MODE_KEY] = enabled }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs -> prefs[ONBOARDING_KEY] = true }
    }

    suspend fun markVersionSeen(versionCode: Int) {
        context.dataStore.edit { prefs -> prefs[LAST_VERSION_KEY] = versionCode }
    }
}
