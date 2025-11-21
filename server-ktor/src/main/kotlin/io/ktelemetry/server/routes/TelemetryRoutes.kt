package io.ktelemetry.server.routes

import io.ktelemetry.models.TelemetryEvent
import io.ktelemetry.server.repository.TelemetryRepository
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.Logger

fun Routing.telemetryRoutes(telemetryRepository: TelemetryRepository) {
    val logger: Logger = LoggerFactory.getLogger("io.ktelemetry.server.routes.TelemetryRoutes")
    val json = Json { ignoreUnknownKeys = true }

    post("/telemetry/events") {
        try {
            logger.info("Received POST request to /telemetry/events")
            val body = call.receive<String>()
            logger.debug("Received body: ${body.take(200)}...")

            val events = json.decodeFromString(ListSerializer(TelemetryEvent.serializer()), body)
            logger.info("Successfully deserialized ${events.size} events")

            if (events.isEmpty()) {
                logger.warn("Received empty events list")
                call.respond(io.ktor.http.HttpStatusCode.Accepted)
                return@post
            }

            logger.info("Calling insertEvents for ${events.size} events")
            try {
                telemetryRepository.insertEvents(events)
                logger.info("Successfully inserted events")
            } catch (e: Exception) {
                logger.error("Error inserting events to ClickHouse: ${e.message}", e)
            }

            call.respond(io.ktor.http.HttpStatusCode.Accepted)
        } catch (e: Exception) {
            logger.error("Error processing telemetry events: ${e.message}", e)
            call.respond(io.ktor.http.HttpStatusCode.Accepted)
        }
    }
}

