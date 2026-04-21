package com.yourname.moneypilot.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_prefs")

class SecurityPreferences(private val context: Context) {

    private val PIN_HASH_KEY = stringPreferencesKey("pin_hash")

    suspend fun setPin(pin: String) {
        val hash = hashPin(pin)
        context.dataStore.edit { prefs ->
            prefs[PIN_HASH_KEY] = hash
        }
    }

    suspend fun isPinSet(): Boolean {
        return context.dataStore.data.map { prefs ->
            prefs[PIN_HASH_KEY] != null
        }.first()
    }

    suspend fun verifyPin(pin: String): Boolean {
        val storedHash = context.dataStore.data.map { prefs ->
            prefs[PIN_HASH_KEY]
        }.first() ?: return false
        return storedHash == hashPin(pin)
    }

    suspend fun clearPin() {
        context.dataStore.edit { prefs ->
            prefs.remove(PIN_HASH_KEY)
        }
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}