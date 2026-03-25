package io.epavlov.ktelemetry.backend.repository

import com.clickhouse.client.api.Client
import com.clickhouse.client.api.insert.InsertSettings
import com.clickhouse.data.ClickHouseFormat
import io.epavlov.ktelemetry.backend.ClickHouseHealth
import io.epavlov.ktelemetry.backend.DatabaseSize
import io.epavlov.ktelemetry.core.TelemetryEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class ClickHouseClientTelemetryRepository(
    endpoint: String,
    private val user: String,
    private val password: String,
    private val database: String = "telemetry",
    private val eventsTable: String = "events",
) : TelemetryRepository {
    private val logger = LoggerFactory.getLogger(ClickHouseClientTelemetryRepository::class.java)

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val client: Client =
        Client.Builder()
            .addEndpoint(normalizedClickHouseHttpEndpoint(endpoint))
            .setUsername(user)
            .setPassword(password)
            .setDefaultDatabase(database)
            .compressServerResponse(true)
            .build()

    override suspend fun insertEvents(events: List<TelemetryEvent>) {
        if (events.isEmpty()) return

        val flatEvents = events.map { it.toFlat() }
        val jsonLines =
            flatEvents.joinToString("\n") { event ->
                json.encodeToString(FlatTelemetryEvent.serializer(), event)
            }
        val bytes = jsonLines.toByteArray(StandardCharsets.UTF_8)

        logger.info("Inserting ${events.size} events into ClickHouse (official client)")

        withContext(Dispatchers.IO) {
            try {
                val stream = ByteArrayInputStream(bytes)
                stream.mark(bytes.size)
                val insertSettings = InsertSettings()
                client.insert(eventsTable, stream, ClickHouseFormat.JSONEachRow, insertSettings).get(60, TimeUnit.SECONDS)
                    .use { response ->
                        logger.info(
                            "ClickHouse insert OK, request batch size=${events.size}, written_rows=${response.writtenRows}",
                        )
                    }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.error("ClickHouse insert failed: ${e.message}", e)
                throw e
            }
        }
    }

    override suspend fun getHealth(): ClickHouseHealth {
        return withContext(Dispatchers.IO) {
            try {
                val databases = queryDatabaseSizes()
                ClickHouseHealth(reachable = true, databases = databases)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.warn("ClickHouse health check failed: ${e.message}")
                ClickHouseHealth(reachable = false)
            }
        }
    }

    private fun queryDatabaseSizes(): List<DatabaseSize> {
        val sql =
            """
            SELECT
                database,
                formatReadableSize(sum(bytes_on_disk)) AS size,
                sum(bytes_on_disk) AS bytes,
                sum(rows) AS rows,
                count() AS parts
            FROM system.parts
            WHERE active
            GROUP BY database
            ORDER BY bytes DESC
            FORMAT JSONEachRow
            """.trimIndent()

        client.query(sql).get(15, TimeUnit.SECONDS).use { response ->
            val body = response.inputStream.bufferedReader().readText()
            return body.lines()
                .filter { it.isNotBlank() }
                .map { line ->
                    val obj = json.parseToJsonElement(line) as JsonObject
                    DatabaseSize(
                        database = obj.requireString("database"),
                        size = obj.requireString("size"),
                        bytes = obj.requireLong("bytes"),
                        rows = obj.requireLong("rows"),
                        parts = obj.requireLong("parts"),
                    )
                }
        }
    }
}
