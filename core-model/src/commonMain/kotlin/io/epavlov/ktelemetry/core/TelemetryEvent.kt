package io.epavlov.ktelemetry.core

import kotlinx.serialization.Serializable

@Serializable
data class TelemetryEvent(
    /** Wall-clock instant as Unix epoch milliseconds in UTC (Instant / epoch semantics). */
    val eventTime: Long,
    /** IANA time zone id on the device when the event was recorded (e.g. `Europe/Berlin`). */
    val eventTimeZoneId: String? = null,
    /** Offset from UTC in minutes at [eventTime] for the default zone (includes DST if applicable). */
    val eventUtcOffsetMinutes: Int? = null,
    val eventType: TelemetryEventType,
    val eventName: String,
    val level: TelemetryLevel,
    val app: AppInfo,
    val user: UserInfo? = null,
    val device: DeviceInfo? = null,
    val session: SessionInfo? = null,
    val context: EventContext? = null,
    val error: ErrorInfo? = null,
)
