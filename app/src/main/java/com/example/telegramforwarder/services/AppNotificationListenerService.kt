package com.example.telegramforwarder.services

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.telegramforwarder.data.LogRepository
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
    private lateinit var logger: LogRepository

    override fun onCreate() {
        super.onCreate()
        logger = LogRepository(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val packageName = sbn?.packageName ?: return

        // Log all notifications for debugging purposes if needed, or filter strictly
        // logger.logDebug("NotificationListener", "Notification from $packageName")

        if (packageName == "com.google.android.gm") {
             scope.launch {
                val preferences = UserPreferences(applicationContext)
                val isEnabled = preferences.isEmailEnabled.first()

                if (!isEnabled) {
                    // logger.logDebug("NotificationListener", "Email received but forwarding disabled.")
                    return@launch
                }

                val extras = sbn.notification.extras
                val title = extras.getString(Notification.EXTRA_TITLE) ?: "No Title"
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "No Content"

                logger.logInfo("NotificationListener", "Detected Gmail notification: $title")

                processIncomingMessage(applicationContext, "Gmail: $title", text, "EMAIL", logger)
            }
        }
    }

    private suspend fun processIncomingMessage(
        context: Context,
        sender: String,
        content: String,
        type: String,
        logger: LogRepository
    ) {
        val database = AppDatabase.getDatabase(context)
        val preferences = UserPreferences(context)

        // Save to local DB
        try {
            database.messageDao().insertMessage(
                MessageEntity(
                    sender = sender,
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    type = type
                )
            )
            logger.logDebug("NotificationListener", "Message saved to local DB")
        } catch (e: Exception) {
            logger.logError("NotificationListener", "Failed to save to local DB", e)
        }

        // Send to Telegram
        val botToken = preferences.botToken.first()
        val chatId = preferences.chatId.first()

        if (!botToken.isNullOrEmpty() && !chatId.isNullOrEmpty()) {
            val formattedMessage = "📧 *New $type*\n\n*From:* $sender\n\n$content"
            logger.logInfo("NotificationListener", "Sending email notification to Telegram...")

            val result = TelegramRepository.sendMessage(botToken, chatId, formattedMessage)
            if (result.success) {
                logger.logInfo("NotificationListener", "Successfully sent email to Telegram")
            } else {
                logger.logError("NotificationListener", "Failed to send email to Telegram: ${result.message}")
            }
        } else {
            logger.logError("NotificationListener", "Telegram credentials not set.")
        }
    }
}
