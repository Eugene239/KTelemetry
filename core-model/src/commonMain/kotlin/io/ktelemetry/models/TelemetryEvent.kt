package io.ktelemetry.models

import kotlinx.serialization.Serializable

@Serializable
data class TelemetryEvent(
    val eventTime: Long,
    val eventType: String,
    val eventName: String,
    val level: TelemetryLevel,
    val app: AppInfo,
    val user: UserInfo? = null,
    val device: DeviceInfo? = null,
    val session: SessionInfo? = null,
    val context: EventContext? = null
)

