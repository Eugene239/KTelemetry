package io.epavlov.ktelemetry.android.internal.domain

import io.epavlov.ktelemetry.android.transport.EventTransport
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class FlushEventsUseCase(
    private val repository: EventRepository,
    private val transport: EventTransport,
    private val batchSize: Int,
) {
    private val flushMutex = Mutex()

    suspend fun execute() {
        flushMutex.withLock {
            while (true) {
                val events = repository.getPending(batchSize)
                if (events.isEmpty()) break
                if (transport.send(events.map { it.json })) {
                    repository.delete(events.map { it.id })
                } else {
                    break
                }
            }
        }
    }

    suspend fun flushIfBatchReady() {
        flushMutex.withLock {
            if (repository.count() < batchSize) return@withLock
            val events = repository.getPending(batchSize)
            if (events.isNotEmpty() && transport.send(events.map { it.json })) {
                repository.delete(events.map { it.id })
            }
        }
    }
}