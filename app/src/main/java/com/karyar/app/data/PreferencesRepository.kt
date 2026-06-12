package com.karyar.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val ONBOARDING_KEY = booleanPreferencesKey("onboarding_completed")

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE_KEY] ?: false
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_KEY] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DARK_MODE_KEY] = enabled }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs -> prefs[ONBOARDING_KEY] = true }
    }
}

