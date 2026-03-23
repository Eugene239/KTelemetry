package io.epavlov.ktelemetry.backend.repository

import io.epavlov.ktelemetry.backend.ClickHouseHealth
import io.epavlov.ktelemetry.backend.DatabaseSize
import io.epavlov.ktelemetry.core.TelemetryEvent
import io.epavlov.ktelemetry.core.wireValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

class TelemetryRepository(
    private val url: String,
    private val user: String,
    private val password: String,
) {
    private val logger = LoggerFactory.getLogger(TelemetryRepository::class.java)

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val endpointUrl: String = url.trimEnd('/') + "/"

    private fun flattenEvent(event: TelemetryEvent): FlatTelemetryEvent {
        val ctx = event.context
        val dev = event.device
        val err = event.error
        return FlatTelemetryEvent(
            event_time = event.eventTime / 1000,
            event_time_zone_id = event.eventTimeZoneId,
            event_utc_offset_minutes = event.eventUtcOffsetMinutes,
            event_type = event.eventType.wireValue(),
            event_name = event.eventName,
            level = event.level.name,
            app_id = event.app.appId,
            app_version = event.app.appVersion,
            app_name = event.app.appName,
            app_build_number = event.app.buildNumber,
            app_environment = event.app.environment,
            user_id = event.user?.userId ?: "",
            anonymous_id = event.user?.anonymousId ?: "",
            user_name = event.user?.userName,
            user_email = event.user?.userEmail,
            user_roles = event.user?.userRoles ?: emptyList(),
            device_id = dev?.deviceId ?: "",
            device_type = dev?.deviceType,
            device_os = dev?.os,
            device_os_version = dev?.osVersion,
            device_manufacturer = dev?.manufacturer,
            device_model = dev?.model,
            device_screen_width = dev?.screenWidth,
            device_screen_height = dev?.screenHeight,
            device_locale = dev?.locale,
            device_orientation = dev?.orientation,
            network_type = dev?.networkType,
            battery_level = dev?.batteryLevel,
            memory_free = dev?.memoryFree,
            memory_total = dev?.memoryTotal,
            storage_free = dev?.storageFree,
            is_foreground = dev?.isForeground?.let { if (it) 1 else 0 },
            is_rooted = dev?.isRooted?.let { if (it) 1 else 0 },
            session_id = event.session?.sessionId ?: "",
            session_start_time = event.session?.sessionStartTime?.div(1000),
            feature = ctx?.feature,
            screen_name = ctx?.screenName,
            previous_screen = ctx?.previousScreen,
            breadcrumb_type = ctx?.breadcrumbType,
            duration_ms = ctx?.durationMs,
            tags = ctx?.tags ?: emptyList(),
            feature_flags = ctx?.featureFlags ?: "",
            payload = ctx?.payload ?: "",
            error_type = err?.type,
            error_message = err?.message,
            error_stacktrace = err?.stacktrace,
            error_is_fatal = if (err?.isFatal == true) 1 else 0,
            error_thread = err?.threadName,
            error_threads = err?.threads,
        )
    }

    suspend fun insertEvents(events: List<TelemetryEvent>) {
        if (events.isEmpty()) return

        val flatEvents = events.map { flattenEvent(it) }
        val jsonLines =
            flatEvents.joinToString("\n") { event ->
                json.encodeToString(FlatTelemetryEvent.serializer(), event)
            }

        logger.info("Inserting ${events.size} events into ClickHouse")

        withContext(Dispatchers.IO) {
            val queryUrl = buildQueryUrl("INSERT INTO telemetry.events FORMAT JSONEachRow")
            val connection = createConnection(queryUrl)
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

    suspend fun getHealth(): ClickHouseHealth {
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
                val obj = json.parseToJsonElement(line) as kotlinx.serialization.json.JsonObject
                DatabaseSize(
                    database = obj["database"]!!.asString(),
                    size = obj["size"]!!.asString(),
                    bytes = obj["bytes"]!!.asLong(),
                    rows = obj["rows"]!!.asLong(),
                    parts = obj["parts"]!!.asLong(),
                )
            }
    }

    private fun kotlinx.serialization.json.JsonElement.asString(): String = (this as kotlinx.serialization.json.JsonPrimitive).content

    private fun kotlinx.serialization.json.JsonElement.asLong(): Long = (this as kotlinx.serialization.json.JsonPrimitive).content.toLong()

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
        if (responseCode in 200..299) {
            logger.info("Inserted $eventCount events (status: $responseCode)")
        } else {
            val errorMessage = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            logger.error("ClickHouse returned $responseCode: $errorMessage")
            throw RuntimeException("ClickHouse error: $responseCode - $errorMessage")
        }
    }
}
