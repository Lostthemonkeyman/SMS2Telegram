package com.example.telegramforwarder.data.remote

import android.util.Log

data class TelegramResponse(
    val success: Boolean,
    val message: String? = null
)

object TelegramRepository {

    private const val BASE_URL = "https://api.telegram.org/"

    suspend fun sendMessage(botToken: String, chatId: String, message: String): TelegramResponse {
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("${BASE_URL}bot$botToken/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()

        val api = retrofit.create(TelegramApi::class.java)

        return try {
            api.sendMessage(chatId, message)
            TelegramResponse(true, "Message sent successfully")
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown error"
            TelegramResponse(false, errorMsg)
        }
    }
}
