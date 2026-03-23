package io.epavlov.ktelemetry.android.internal.platform

import java.util.TimeZone

internal object EventTimeSource {
    fun now(): EventTimeSnapshot {
        val utcMillis = System.currentTimeMillis()
        val tz = TimeZone.getDefault()
        val offsetMinutes = tz.getOffset(utcMillis) / 60_000
        val zoneId = tz.id
        return EventTimeSnapshot(
            utcMillis = utcMillis,
            timeZoneId = zoneId,
            utcOffsetMinutes = offsetMinutes,
        )
    }
}

internal data class EventTimeSnapshot(
    val utcMillis: Long,
    val timeZoneId: String,
    val utcOffsetMinutes: Int,
)
