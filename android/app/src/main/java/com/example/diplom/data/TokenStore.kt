package com.example.diplom.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("auth")

class TokenStore(private val context: Context) {
    private val keyToken = stringPreferencesKey("jwt")

    suspend fun getToken(): String? {
        return context.dataStore.data.map { it[keyToken] }.first()
    }

    suspend fun setToken(token: String?) {
        context.dataStore.edit { prefs ->
            if (token == null) prefs.remove(keyToken)
            else prefs[keyToken] = token
        }
    }
}
