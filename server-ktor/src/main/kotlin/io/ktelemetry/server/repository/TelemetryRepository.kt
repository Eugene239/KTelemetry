package io.ktelemetry.server.repository

import io.ktelemetry.models.TelemetryEvent
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Base64

class TelemetryRepository(
    private val url: String,
    private val user: String,
    private val password: String
) {
    private val logger = LoggerFactory.getLogger(TelemetryRepository::class.java)
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val endpointUrl: String = normalizeUrl(url)
    
    private fun normalizeUrl(url: String): String {
        val normalized = url.trimEnd('/')
        return if (normalized.endsWith("/")) normalized else "$normalized/"
    }
    
    private fun flattenEvent(event: TelemetryEvent): FlatTelemetryEvent {
        val context = event.context
        return FlatTelemetryEvent(
            event_time = event.eventTime / 1000,
            event_type = event.eventType,
            event_name = event.eventName,
            level = event.level.name,
            feature = context?.feature,
            tags = context?.tags ?: emptyList(),
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
            device_id = event.device?.deviceId ?: "",
            device_type = event.device?.deviceType,
            device_os = event.device?.os,
            device_os_version = event.device?.osVersion,
            device_manufacturer = event.device?.manufacturer,
            device_model = event.device?.model,
            device_screen_width = event.device?.screenWidth,
            device_screen_height = event.device?.screenHeight,
            session_id = event.session?.sessionId ?: "",
            session_start_time = event.session?.sessionStartTime?.div(1000),
            context_feature = context?.feature,
            context_tags = context?.tags ?: emptyList(),
            feature_flags = context?.featureFlags?.toString() ?: "",
            payload = context?.payload?.toString() ?: ""
        )
    }
    
    suspend fun insertEvents(events: List<TelemetryEvent>) {
        if (events.isEmpty()) {
            logger.warn("Attempted to insert empty events list")
            return
        }
        
        try {
            val flatEvents = events.map { flattenEvent(it) }
            val jsonLines = flatEvents.joinToString("\n") { event ->
                json.encodeToString(FlatTelemetryEvent.serializer(), event)
            }
            
            logger.info("Prepared ${events.size} events for ClickHouse insertion")
            logger.debug("JSON data (first 300 chars): ${jsonLines.take(300)}")
            
            withContext(Dispatchers.IO) {
                val insertQuery = "INSERT INTO telemetry.events FORMAT JSONEachRow"
                val queryUrl = buildQueryUrl(insertQuery)
                
                logger.debug("Sending data to ClickHouse: $queryUrl")

                val dataBytes = jsonLines.toByteArray(StandardCharsets.UTF_8)
                val connection = createConnection(queryUrl)
                
                try {
                    sendData(connection, dataBytes)
                    validateResponse(connection, events.size)
                } catch (e: Exception) {
                    logger.error("Error executing INSERT query: ${e.message}", e)
                    throw e
                }
            }
        } catch (e: Exception) {
            logger.error("Error inserting events into ClickHouse: ${e.message}", e)
            logger.error("Exception stack trace:", e)
            throw e
        }
    }
    
    private fun buildQueryUrl(query: String): String {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)
        return "$endpointUrl?query=$encodedQuery"
    }
    
    private fun createConnection(queryUrl: String): HttpURLConnection {
        val connection = URL(queryUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        
        val credentials = "$user:$password"
        val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray(StandardCharsets.UTF_8))
        connection.setRequestProperty("Authorization", "Basic $encoded")
        
        return connection
    }
    
    private fun sendData(connection: HttpURLConnection, dataBytes: ByteArray) {
        connection.outputStream.use { outputStream ->
            outputStream.write(dataBytes)
            outputStream.flush()
        }
    }
    
    private fun validateResponse(connection: HttpURLConnection, eventCount: Int) {
        val responseCode = connection.responseCode
        if (responseCode in 200..299) {
            logger.info("Successfully inserted $eventCount events into ClickHouse (status: $responseCode)")
        } else {
            val errorMessage = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            logger.error("ClickHouse returned status $responseCode: $errorMessage")
            throw RuntimeException("ClickHouse error: $responseCode - $errorMessage")
        }
    }
}
