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
    val keyboardBackgroundColor: Int?,
    val keyboardBoxColor: Int?,
    val useDynamicColor: Boolean,
    val useBiometrics: Boolean,
    val budgetAlertThreshold: Int,
    val dailySummaryEnabled: Boolean,
    val dailySummaryTime: String,
    val useTrueBlack: Boolean = false,
    val fontFamily: String = "DEFAULT",
    val isPrivacyModeEnabled: Boolean = false,
    val includeGoalsInNetWorth: Boolean = true,
    val lastRolloverMonth: String = "",
    val lastDistributionMonth: String = "",
    val lastAutoReserveMonth: String = ""
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val CURRENCY = stringPreferencesKey("currency")
        val THEME = stringPreferencesKey("app_theme")
        val PRIMARY_COLOR = intPreferencesKey("primary_color")
        val KEYBOARD_BG_COLOR = intPreferencesKey("keyboard_bg_color")
        val KEYBOARD_BOX_COLOR = intPreferencesKey("keyboard_box_color")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val USE_BIOMETRICS = booleanPreferencesKey("use_biometrics")
        val USE_TRUE_BLACK = booleanPreferencesKey("use_true_black")
        val BUDGET_ALERT_THRESHOLD = stringPreferencesKey("budget_alert_threshold")
        val DAILY_SUMMARY_ENABLED = booleanPreferencesKey("daily_summary_enabled")
        val DAILY_SUMMARY_TIME = stringPreferencesKey("daily_summary_time")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
        val INCLUDE_GOALS_NET_WORTH = booleanPreferencesKey("include_goals_net_worth")
        val LAST_ROLLOVER_MONTH = stringPreferencesKey("last_rollover_month")
        val LAST_DISTRIBUTION_MONTH = stringPreferencesKey("last_distribution_month")
        val LAST_AUTO_RESERVE_MONTH = stringPreferencesKey("last_auto_reserve_month")
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
            
            val primaryColor = preferences[PreferencesKeys.PRIMARY_COLOR] ?: 0xFF7B5CFA.toInt()
            val kbBgColor = preferences[PreferencesKeys.KEYBOARD_BG_COLOR]
            val kbBoxColor = preferences[PreferencesKeys.KEYBOARD_BOX_COLOR]
            
            val useDynamicColor = preferences[PreferencesKeys.USE_DYNAMIC_COLOR] ?: true
            val useBiometrics = preferences[PreferencesKeys.USE_BIOMETRICS] ?: false
            val useTrueBlack = preferences[PreferencesKeys.USE_TRUE_BLACK] ?: false
            val threshold = preferences[PreferencesKeys.BUDGET_ALERT_THRESHOLD]?.toIntOrNull() ?: 90
            val summaryEnabled = preferences[PreferencesKeys.DAILY_SUMMARY_ENABLED] ?: true
            val summaryTime = preferences[PreferencesKeys.DAILY_SUMMARY_TIME] ?: "22:00"
            val fontFamily = preferences[PreferencesKeys.FONT_FAMILY] ?: "DEFAULT"
            val privacyMode = preferences[PreferencesKeys.PRIVACY_MODE] ?: false
            val includeGoals = preferences[PreferencesKeys.INCLUDE_GOALS_NET_WORTH] ?: true
            val lastRollover = preferences[PreferencesKeys.LAST_ROLLOVER_MONTH] ?: ""
            val lastDist = preferences[PreferencesKeys.LAST_DISTRIBUTION_MONTH] ?: ""
            val lastReserve = preferences[PreferencesKeys.LAST_AUTO_RESERVE_MONTH] ?: ""

            UserPreferences(
                currency, theme, primaryColor, kbBgColor, kbBoxColor, 
                useDynamicColor, useBiometrics, threshold, summaryEnabled, 
                summaryTime, useTrueBlack, fontFamily, privacyMode, includeGoals,
                lastRollover, lastDist, lastReserve
            )
        }

    suspend fun updateCurrency(currency: String) {
        context.dataStore.edit { it[PreferencesKeys.CURRENCY] = currency }
    }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { it[PreferencesKeys.THEME] = theme.name }
    }

    suspend fun updatePrimaryColor(color: Int) {
        context.dataStore.edit { it[PreferencesKeys.PRIMARY_COLOR] = color }
    }

    suspend fun updateUseBiometrics(use: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.USE_BIOMETRICS] = use }
    }

    suspend fun updateUseDynamicColor(use: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.USE_DYNAMIC_COLOR] = use }
    }

    suspend fun updateDailySummaryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DAILY_SUMMARY_ENABLED] = enabled }
    }

    suspend fun updateDailySummaryTime(time: String) {
        context.dataStore.edit { it[PreferencesKeys.DAILY_SUMMARY_TIME] = time }
    }

    suspend fun updateUseTrueBlack(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.USE_TRUE_BLACK] = enabled }
    }

    suspend fun updateFontFamily(font: String) {
        context.dataStore.edit { it[PreferencesKeys.FONT_FAMILY] = font }
    }

    suspend fun updatePrivacyMode(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.PRIVACY_MODE] = enabled }
    }

    suspend fun updateIncludeGoalsInNetWorth(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.INCLUDE_GOALS_NET_WORTH] = enabled }
    }

    suspend fun updateLastRolloverMonth(monthStr: String) {
        context.dataStore.edit { it[PreferencesKeys.LAST_ROLLOVER_MONTH] = monthStr }
    }

    suspend fun updateLastDistributionMonth(monthStr: String) {
        context.dataStore.edit { it[PreferencesKeys.LAST_DISTRIBUTION_MONTH] = monthStr }
    }

    suspend fun updateLastAutoReserveMonth(monthStr: String) {
        context.dataStore.edit { it[PreferencesKeys.LAST_AUTO_RESERVE_MONTH] = monthStr }
    }
}
