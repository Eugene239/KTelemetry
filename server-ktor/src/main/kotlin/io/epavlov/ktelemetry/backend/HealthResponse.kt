package io.epavlov.ktelemetry.backend

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val uptimeMs: Long,
    val version: String,
    val clickhouse: ClickHouseHealth? = null,
)

@Serializable
data class ClickHouseHealth(
    val reachable: Boolean,
    val databases: List<DatabaseSize> = emptyList(),
)

@Serializable
data class DatabaseSize(
    val database: String,
    val size: String,
    val bytes: Long,
    val rows: Long,
    val parts: Long,
)
