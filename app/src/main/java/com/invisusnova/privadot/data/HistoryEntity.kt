package com.invisusnova.privadot.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_log")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sensorType: String, // "CAMERA", "MIC", "LOCATION"
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val durationMs: Long
)
