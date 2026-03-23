package io.epavlov.ktelemetry.android.transport

import android.util.Log
import io.epavlov.ktelemetry.android.internal.data.EventJsonConverter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class HttpEventTransport(
    private val serverUrl: String,
    private val apiKey: String? = null,
) : EventTransport {
    override suspend fun isReachable(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(eventsUrl()).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                applyAuthHeaders(connection)
                connection.connectTimeout = HEAD_TIMEOUT_MS
                connection.readTimeout = HEAD_TIMEOUT_MS
                connection.doInput = true
                val code = connection.responseCode
                drainStream(connection)
                connection.disconnect()
                val ok = code in 200..299 || code == HttpURLConnection.HTTP_BAD_METHOD
                if (!ok) {
                    Log.w(TAG, "HEAD probe HTTP $code for ${eventsUrl()}")
                }
                ok
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "HEAD probe failed: ${e.message}")
                false
            }
        }

    override suspend fun send(events: List<String>): Boolean =
        withContext(Dispatchers.IO) {
            if (events.isEmpty()) return@withContext true
            if (!isReachable()) {
                Log.w(TAG, "Server unreachable, skipping send of ${events.size} events (remain queued)")
                return@withContext false
            }
            try {
                val body = EventJsonConverter.toJsonArray(events)
                val connection = URL(eventsUrl()).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                applyAuthHeaders(connection)
                connection.doOutput = true
                connection.connectTimeout = SEND_TIMEOUT_MS
                connection.readTimeout = SEND_TIMEOUT_MS

                OutputStreamWriter(connection.outputStream, "UTF-8").use { it.write(body) }

                val code = connection.responseCode
                drainStream(connection)
                connection.disconnect()

                val success = code in 200..299
                if (!success) {
                    Log.e(TAG, "Failed to send ${events.size} events, status: $code")
                }
                success
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Error sending events: ${e.message}", e)
                false
            }
        }

    private fun eventsUrl(): String = serverUrl.trimEnd('/') + "/telemetry/events"

    private fun applyAuthHeaders(connection: HttpURLConnection) {
        val key = apiKey?.trim()
        if (!key.isNullOrEmpty()) {
            connection.setRequestProperty(API_KEY_HEADER, key)
        }
    }

    private fun drainStream(connection: HttpURLConnection) {
        try {
            val stream =
                if (connection.responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }
            stream?.use { it.readBytes() }
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val TAG = "KTelemetry"
        private const val API_KEY_HEADER = "X-API-Key"
        private const val HEAD_TIMEOUT_MS = 5_000
        private const val SEND_TIMEOUT_MS = 10_000
    }
}
