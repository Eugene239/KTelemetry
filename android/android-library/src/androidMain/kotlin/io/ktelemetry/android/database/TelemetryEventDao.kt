package io.ktelemetry.android.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TelemetryEventDao {
    @Insert
    suspend fun insert(event: TelemetryEventEntity): Long

    @Query("SELECT * FROM telemetry_events ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingEvents(limit: Int): List<TelemetryEventEntity>

    @Query("SELECT COUNT(*) FROM telemetry_events")
    suspend fun getEventCount(): Int

    @Delete
    suspend fun deleteEvents(events: List<TelemetryEventEntity>)

    @Query("DELETE FROM telemetry_events WHERE id IN (:ids)")
    suspend fun deleteEventsByIds(ids: List<Long>)
}

