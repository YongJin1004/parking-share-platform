package com.parking.share.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val TOKEN_TYPE_KEY = stringPreferencesKey("token_type")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN_KEY]
    }

    val tokenType: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_TYPE_KEY]
    }

    suspend fun saveToken(accessToken: String, tokenType: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[TOKEN_TYPE_KEY] = tokenType
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(TOKEN_TYPE_KEY)
        }
    }

    suspend fun getAuthHeader(): String? {
        val preferences = context.dataStore.data.first()
        val token = preferences[ACCESS_TOKEN_KEY]
        val type = preferences[TOKEN_TYPE_KEY] ?: "Bearer"
        return if (token != null) {
            "$type $token"
        } else {
            null
        }
    }
}
