package io.epavlov.ktelemetry.backend.repository

import io.epavlov.ktelemetry.backend.ClickHouseHealth
import io.epavlov.ktelemetry.core.TelemetryEvent

interface TelemetryRepository {
    suspend fun insertEvents(events: List<TelemetryEvent>)

    suspend fun getHealth(): ClickHouseHealth
}
