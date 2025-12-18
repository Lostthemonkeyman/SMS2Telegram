package com.example.telegramforwarder.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.telegramforwarder.data.local.AppDatabase
import com.example.telegramforwarder.data.local.MessageEntity
import com.example.telegramforwarder.data.local.UserPreferences
import com.example.telegramforwarder.data.remote.TelegramRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val pendingResult = goAsync()

            scope.launch {
                try {
                    messages?.forEach { sms ->
                        val sender = sms.originatingAddress ?: "Unknown"
                        val messageBody = sms.messageBody ?: ""

                        processIncomingMessage(context, sender, messageBody, "SMS")
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun processIncomingMessage(context: Context, sender: String, content: String, type: String) {
        val database = AppDatabase.getDatabase(context)
        val preferences = UserPreferences(context)

        // Save to local DB
        database.messageDao().insertMessage(
            MessageEntity(
                sender = sender,
                content = content,
                timestamp = System.currentTimeMillis(),
                type = type
            )
        )

        // Send to Telegram
        val botToken = preferences.botToken.first()
        val chatId = preferences.chatId.first()

        if (!botToken.isNullOrEmpty() && !chatId.isNullOrEmpty()) {
            val formattedMessage = "📩 *New $type*\n\n*From:* $sender\n\n$content"
            TelegramRepository.sendMessage(botToken, chatId, formattedMessage)
        } else {
            Log.w("SmsReceiver", "Telegram credentials not set.")
        }
    }
}
