package io.ktelemetry.android.sender

import android.util.Log
import io.ktelemetry.android.database.TelemetryEventEntity
import io.ktelemetry.models.TelemetryEvent
import io.ktelemetry.models.TelemetryLevel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class EventSender(private val serverUrl: String) {
    
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    suspend fun sendEvents(events: List<TelemetryEventEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val telemetryEvents = events.map { entity ->
                    convertToTelemetryEvent(entity)
                }

                val url = "$serverUrl/telemetry/events"
                Log.d(TAG, "Sending ${telemetryEvents.size} events to $url")

                val json = Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
                val jsonString = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(TelemetryEvent.serializer()), telemetryEvents)
                
                Log.d(TAG, "JSON body (first 200 chars): ${jsonString.take(200)}...")

                val response = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(jsonString)
                }

                val success = response.status.value in 200..299
                if (success) {
                    Log.d(TAG, "Successfully sent ${telemetryEvents.size} events. Status: ${response.status.value}")
                } else {
                    Log.e(TAG, "Failed to send events. Status: ${response.status.value}")
                }
                success
            } catch (e: Exception) {
                Log.e(TAG, "Error sending events: ${e.message}", e)
                false
            }
        }
    }

    companion object {
        private const val TAG = "EventSender"
    }

    private fun convertToTelemetryEvent(entity: TelemetryEventEntity): TelemetryEvent {
        val json = Json { ignoreUnknownKeys = true }
        
        val appInfo = json.decodeFromString<io.ktelemetry.models.AppInfo>(entity.appInfo)
        val userInfo = entity.userInfo?.let { json.decodeFromString<io.ktelemetry.models.UserInfo>(it) }
        val deviceInfo = entity.deviceInfo?.let { json.decodeFromString<io.ktelemetry.models.DeviceInfo>(it) }
        val sessionInfo = entity.sessionInfo?.let { json.decodeFromString<io.ktelemetry.models.SessionInfo>(it) }
        val context = entity.context?.let { 
            json.decodeFromString<io.ktelemetry.models.EventContext>(it)
        }

        val level = TelemetryLevel.valueOf(entity.level)

        return TelemetryEvent(
            eventTime = entity.eventTime,
            eventType = entity.eventType,
            eventName = entity.eventName,
            level = level,
            app = appInfo,
            user = userInfo,
            device = deviceInfo,
            session = sessionInfo,
            context = context
        )
    }

    fun close() {
        httpClient.close()
    }
}

