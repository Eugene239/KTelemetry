package io.epavlov.ktelemetry.android.internal.data

import io.epavlov.ktelemetry.android.internal.data.local.EventDatabaseHelper
import io.epavlov.ktelemetry.android.internal.domain.EventRepository
import io.epavlov.ktelemetry.android.internal.domain.PendingEvent
import io.epavlov.ktelemetry.core.TelemetryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class EventRepositoryImpl(
    private val database: EventDatabaseHelper,
) : EventRepository {
    override suspend fun save(event: TelemetryEvent) =
        withContext(Dispatchers.IO) {
            database.insertEvent(EventJsonConverter.toJson(event))
        }

    override suspend fun getPending(limit: Int): List<PendingEvent> =
        withContext(Dispatchers.IO) {
            database.getPendingEvents(limit)
        }

    override suspend fun delete(ids: List<Long>) =
        withContext(Dispatchers.IO) {
            database.deleteByIds(ids)
        }

    override suspend fun count(): Int =
        withContext(Dispatchers.IO) {
            database.getEventCount()
        }
}
