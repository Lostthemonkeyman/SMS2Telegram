package com.example.telegramforwarder.services

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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

class AppNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val packageName = sbn?.packageName ?: return

        // Filter for Gmail or specific apps if needed. The prompt implies "gmail or something like that".
        // Capturing all emails implies capturing notifications from email apps.
        // com.google.android.gm is Gmail.
        if (packageName == "com.google.android.gm") {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: "No Title"
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "No Content"

            // Gmail notifications often group messages. We try to get the best representation.
            // Often Title is the sender or subject, text is the snippet.

            processIncomingMessage(applicationContext, "Gmail: $title", text, "EMAIL")
        }
    }

    private fun processIncomingMessage(context: Context, sender: String, content: String, type: String) {
        scope.launch {
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
                val formattedMessage = "📧 *New $type*\n\n*From:* $sender\n\n$content"
                TelegramRepository.sendMessage(botToken, chatId, formattedMessage)
            } else {
                Log.w("NotificationListener", "Telegram credentials not set.")
            }
        }
    }
}
