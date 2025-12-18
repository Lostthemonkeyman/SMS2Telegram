package com.example.telegramforwarder.data

import android.content.Context
import com.example.telegramforwarder.data.local.AppDatabase
import com.example.telegramforwarder.data.local.LogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LogRepository(context: Context) {
    private val logDao = AppDatabase.getDatabase(context).logDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun logInfo(tag: String, message: String) {
        log("INFO", tag, message)
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val msg = if (throwable != null) "$message\n${throwable.stackTraceToString()}" else message
        log("ERROR", tag, msg)
    }

    fun logDebug(tag: String, message: String) {
        log("DEBUG", tag, message)
    }

    private fun log(level: String, tag: String, message: String) {
        scope.launch {
            logDao.insertLog(
                LogEntity(
                    timestamp = System.currentTimeMillis(),
                    tag = tag,
                    message = message,
                    level = level
                )
            )
        }
    }

    fun getAllLogs() = logDao.getAllLogs()

    suspend fun clearLogs() = logDao.clearLogs()
}
