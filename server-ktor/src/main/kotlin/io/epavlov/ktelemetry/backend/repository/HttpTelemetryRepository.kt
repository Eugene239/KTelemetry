package io.epavlov.ktelemetry.backend.repository

import io.epavlov.ktelemetry.backend.ClickHouseHealth
import io.epavlov.ktelemetry.backend.DatabaseSize
import io.epavlov.ktelemetry.core.TelemetryEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Minimal HTTP transport (legacy). Prefer [ClickHouseClientTelemetryRepository].
 */
class HttpTelemetryRepository(
    private val url: String,
    private val user: String,
    private val password: String,
) : TelemetryRepository {
    private val logger = LoggerFactory.getLogger(HttpTelemetryRepository::class.java)

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val endpointUrl: String = url.trimEnd('/') + "/"

    override suspend fun insertEvents(events: List<TelemetryEvent>) {
        if (events.isEmpty()) return

        val flatEvents = events.map { it.toFlat() }
        val jsonLines =
            flatEvents.joinToString("\n") { event ->
                json.encodeToString(FlatTelemetryEvent.serializer(), event)
            }

        logger.info("Inserting ${events.size} events into ClickHouse (HTTP)")

        withContext(Dispatchers.IO) {
            val queryUrl = buildQueryUrl("INSERT INTO telemetry.events FORMAT JSONEachRow")
            val connection = createConnection(queryUrl)
            connection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8")
            try {
                sendData(connection, jsonLines.toByteArray(StandardCharsets.UTF_8))
                validateResponse(connection, events.size)
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
        val query =
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
            """.trimIndent()

        val queryUrl = buildQueryUrl("$query FORMAT JSONEachRow")
        val connection = createConnection(queryUrl)
        connection.requestMethod = "GET"
        connection.doOutput = false

        val code = connection.responseCode
        if (code !in 200..299) {
            val err = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw RuntimeException("ClickHouse error $code: $err")
        }

        val body = connection.inputStream.bufferedReader().readText()
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

    private fun buildQueryUrl(query: String): String {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)
        return "$endpointUrl?query=$encodedQuery"
    }

    private fun createConnection(queryUrl: String): HttpURLConnection {
        val connection = URI(queryUrl).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        val credentials = "$user:$password"
        val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray(StandardCharsets.UTF_8))
        connection.setRequestProperty("Authorization", "Basic $encoded")

        return connection
    }

    private fun sendData(
        connection: HttpURLConnection,
        dataBytes: ByteArray,
    ) {
        connection.outputStream.use {
            it.write(dataBytes)
            it.flush()
        }
    }

    private fun validateResponse(
        connection: HttpURLConnection,
        eventCount: Int,
    ) {
        val responseCode = connection.responseCode
        val summary = connection.getHeaderField("X-ClickHouse-Summary")
        val responseBody =
            try {
                val stream =
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                stream?.bufferedReader()?.use { it.readText() }?.trim().orEmpty()
            } catch (_: Exception) {
                ""
            }
        if (responseCode in 200..299) {
            if (summary != null) {
                logger.info(
                    "ClickHouse insert OK (HTTP $responseCode), request batch size=$eventCount, summary=$summary",
                )
            } else {
                logger.info("ClickHouse insert OK (HTTP $responseCode), request batch size=$eventCount")
            }
            if (responseBody.isNotEmpty()) {
                logger.debug("ClickHouse response body: $responseBody")
            }
        } else {
            logger.error("ClickHouse returned $responseCode: $responseBody")
            throw RuntimeException("ClickHouse error: $responseCode - $responseBody")
        }
    }
}
