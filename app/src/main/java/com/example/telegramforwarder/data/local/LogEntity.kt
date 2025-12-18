package com.example.telegramforwarder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val tag: String,
    val message: String,
    val level: String // "INFO", "ERROR", "DEBUG", "WARN"
)
