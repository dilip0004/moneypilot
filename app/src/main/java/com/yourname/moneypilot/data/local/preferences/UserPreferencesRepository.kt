package com.yourname.moneypilot.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val currency: String,
    val isDarkMode: Boolean?, // null means follow system
    val useDynamicColor: Boolean,
    val useTrueBlack: Boolean,
    val useBiometrics: Boolean,
    val budgetAlertThreshold: Int
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val CURRENCY = stringPreferencesKey("currency")
        val DARK_MODE = stringPreferencesKey("dark_mode") // "LIGHT", "DARK", "SYSTEM"
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val USE_TRUE_BLACK = booleanPreferencesKey("use_true_black")
        val USE_BIOMETRICS = booleanPreferencesKey("use_biometrics")
        val BUDGET_ALERT_THRESHOLD = stringPreferencesKey("budget_alert_threshold")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val currency = preferences[PreferencesKeys.CURRENCY] ?: "USD"
            val darkModeStr = preferences[PreferencesKeys.DARK_MODE] ?: "SYSTEM"
            val isDarkMode = when (darkModeStr) {
                "LIGHT" -> false
                "DARK" -> true
                else -> null
            }
            val useDynamicColor = preferences[PreferencesKeys.USE_DYNAMIC_COLOR] ?: true
            val useTrueBlack = preferences[PreferencesKeys.USE_TRUE_BLACK] ?: false
            val useBiometrics = preferences[PreferencesKeys.USE_BIOMETRICS] ?: false
            val threshold = preferences[PreferencesKeys.BUDGET_ALERT_THRESHOLD]?.toIntOrNull() ?: 90
            
            UserPreferences(currency, isDarkMode, useDynamicColor, useTrueBlack, useBiometrics, threshold)
        }

    suspend fun updateCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY] = currency
        }
    }

    suspend fun updateDarkMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = mode
        }
    }

    suspend fun updateUseDynamicColor(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLOR] = use
        }
    }

    suspend fun updateUseTrueBlack(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_TRUE_BLACK] = use
        }
    }

    suspend fun updateUseBiometrics(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_BIOMETRICS] = use
        }
    }
}
