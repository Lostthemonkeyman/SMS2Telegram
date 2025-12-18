package com.example.telegramforwarder.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface TelegramApi {
    @GET("sendMessage")
    suspend fun sendMessage(
        @Query("chat_id") chatId: String,
        @Query("text") text: String
    ): Any // We don't strictly need the response for this fire-and-forget logic
}
