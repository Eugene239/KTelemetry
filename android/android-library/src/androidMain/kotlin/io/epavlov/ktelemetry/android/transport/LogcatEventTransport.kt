package io.epavlov.ktelemetry.android.transport

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LogcatEventTransport : EventTransport {
    override suspend fun isReachable(): Boolean = true

    override suspend fun send(events: List<String>): Boolean =
        withContext(Dispatchers.IO) {
            try {
                for (json in events) {
                    Log.d(TAG, "event $json")
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "LogcatEventTransport.send failed: ${e.message}", e)
                false
            }
        }

    companion object {
        private const val TAG = "KTelemetry"
    }
}
