package io.ktelemetry.android.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_events")
data class TelemetryEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventTime: Long,
    val eventType: String,
    val eventName: String,
    val level: String,
    val appInfo: String,
    val userInfo: String?,
    val deviceInfo: String?,
    val sessionInfo: String?,
    val context: String?,
    val createdAt: Long = System.currentTimeMillis()
)

