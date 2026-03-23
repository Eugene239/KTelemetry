package io.epavlov.ktelemetry.backend.routes

import io.epavlov.ktelemetry.backend.repository.TelemetryRepository
import io.epavlov.ktelemetry.core.TelemetryEvent
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.head
import io.ktor.server.routing.post
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun Routing.telemetryRoutes(
    telemetryRepository: TelemetryRepository,
    telemetryApiKey: String?,
) {
    val logger = LoggerFactory.getLogger("io.epavlov.ktelemetry.backend.routes.TelemetryRoutes")
    val json = Json { ignoreUnknownKeys = true }
    val expectedApiKey = telemetryApiKey?.trim()?.takeIf { it.isNotEmpty() }

    head("/telemetry/events") {
        if (!call.requireApiKey(expectedApiKey, logger)) return@head
        call.respond(HttpStatusCode.NoContent)
    }

    post("/telemetry/events") {
        if (!call.requireApiKey(expectedApiKey, logger)) return@post
        try {
            val body = call.receive<String>()
            val events = json.decodeFromString(ListSerializer(TelemetryEvent.serializer()), body)

            if (events.isNotEmpty()) {
                try {
                    telemetryRepository.insertEvents(events)
                    logger.info("Inserted ${events.size} events")
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logger.error("ClickHouse insert failed: ${e.message}", e)
                }
            }

            call.respond(HttpStatusCode.Accepted)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.error("Failed to process events: ${e.message}", e)
            call.respond(HttpStatusCode.Accepted)
        }
    }
}

private suspend fun ApplicationCall.requireApiKey(
    expectedApiKey: String?,
    logger: org.slf4j.Logger,
): Boolean {
    if (expectedApiKey == null) return true

    val providedApiKey = request.headers[API_KEY_HEADER]?.trim()
    if (providedApiKey == expectedApiKey) return true

    logger.warn("Unauthorized telemetry request from ${request.local.remoteHost}")
    respond(HttpStatusCode.Unauthorized)
    return false
}

private const val API_KEY_HEADER = "X-API-Key"
