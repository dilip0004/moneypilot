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

enum class AppTheme {
    LIGHT, DARK, SYSTEM, OLED
}

data class UserPreferences(
    val currency: String,
    val theme: AppTheme,
    val primaryColor: Int,
    val useDynamicColor: Boolean,
    val useBiometrics: Boolean,
    val budgetAlertThreshold: Int,
    val dailySummaryEnabled: Boolean,
    val dailySummaryTime: String
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val CURRENCY = stringPreferencesKey("currency")
        val THEME = stringPreferencesKey("app_theme")
        val PRIMARY_COLOR = intPreferencesKey("primary_color")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val USE_BIOMETRICS = booleanPreferencesKey("use_biometrics")
        val BUDGET_ALERT_THRESHOLD = stringPreferencesKey("budget_alert_threshold")
        val DAILY_SUMMARY_ENABLED = booleanPreferencesKey("daily_summary_enabled")
        val DAILY_SUMMARY_TIME = stringPreferencesKey("daily_summary_time")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val currency = preferences[PreferencesKeys.CURRENCY] ?: "INR"
            val themeStr = preferences[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name
            val theme = try { AppTheme.valueOf(themeStr) } catch(e: Exception) { AppTheme.SYSTEM }
            
            val primaryColor = preferences[PreferencesKeys.PRIMARY_COLOR] ?: 0xFF7F3DFF.toInt() // Default Purple
            val useDynamicColor = preferences[PreferencesKeys.USE_DYNAMIC_COLOR] ?: true
            val useBiometrics = preferences[PreferencesKeys.USE_BIOMETRICS] ?: false
            val threshold = preferences[PreferencesKeys.BUDGET_ALERT_THRESHOLD]?.toIntOrNull() ?: 90
            val summaryEnabled = preferences[PreferencesKeys.DAILY_SUMMARY_ENABLED] ?: true
            val summaryTime = preferences[PreferencesKeys.DAILY_SUMMARY_TIME] ?: "22:00"
            
            UserPreferences(currency, theme, primaryColor, useDynamicColor, useBiometrics, threshold, summaryEnabled, summaryTime)
        }

    suspend fun updateCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY] = currency
        }
    }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun updatePrimaryColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_COLOR] = color
        }
    }

    suspend fun updateUseDynamicColor(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLOR] = use
        }
    }

    suspend fun updateUseBiometrics(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_BIOMETRICS] = use
        }
    }

    suspend fun updateDailySummaryEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_SUMMARY_ENABLED] = enabled
        }
    }

    suspend fun updateDailySummaryTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_SUMMARY_TIME] = time
        }
    }
}
