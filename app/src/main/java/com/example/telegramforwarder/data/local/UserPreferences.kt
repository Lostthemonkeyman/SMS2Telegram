package com.example.telegramforwarder.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    companion object {
        val TELEGRAM_BOT_TOKEN = stringPreferencesKey("telegram_bot_token")
        val TELEGRAM_CHAT_ID = stringPreferencesKey("telegram_chat_id")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
    }

    val botToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TELEGRAM_BOT_TOKEN]
    }

    val chatId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TELEGRAM_CHAT_ID]
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "system"
    }

    suspend fun saveBotToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TELEGRAM_BOT_TOKEN] = token
        }
    }

    suspend fun saveChatId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[TELEGRAM_CHAT_ID] = id
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }
}
