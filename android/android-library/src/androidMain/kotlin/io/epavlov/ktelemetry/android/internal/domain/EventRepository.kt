package io.epavlov.ktelemetry.android.internal.domain

import io.epavlov.ktelemetry.core.TelemetryEvent

internal data class PendingEvent(val id: Long, val json: String)

internal interface EventRepository {
    suspend fun save(event: TelemetryEvent)

    suspend fun getPending(limit: Int): List<PendingEvent>

    suspend fun delete(ids: List<Long>)

    suspend fun count(): Int
}
