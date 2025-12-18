package com.example.telegramforwarder.data.remote

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TelegramRepository {

    private const val BASE_URL = "https://api.telegram.org/"

    suspend fun sendMessage(botToken: String, chatId: String, message: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("$BASE_URL$botToken/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(TelegramApi::class.java)

        try {
            api.sendMessage(chatId, message)
            Log.d("TelegramRepository", "Message sent successfully")
        } catch (e: Exception) {
            Log.e("TelegramRepository", "Failed to send message", e)
        }
    }
}
