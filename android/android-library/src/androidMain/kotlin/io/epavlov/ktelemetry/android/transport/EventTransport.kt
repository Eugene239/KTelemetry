package io.epavlov.ktelemetry.android.transport

interface EventTransport {
    suspend fun isReachable(): Boolean

    suspend fun send(events: List<String>): Boolean
}
