package io.epavlov.ktelemetry.backend

import io.epavlov.ktelemetry.backend.config.ConfigLoader
import io.epavlov.ktelemetry.backend.repository.ClickHouseClientTelemetryRepository
import io.epavlov.ktelemetry.backend.repository.TelemetryRepository
import io.epavlov.ktelemetry.backend.routes.telemetryRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

private val logger = LoggerFactory.getLogger("io.epavlov.ktelemetry.backend")

fun main() {
    val props = ConfigLoader.loadLocalProperties()

    val host = ConfigLoader.getProperty(props, "host", "HOST", "0.0.0.0")
    val port = ConfigLoader.getIntProperty(props, "port", "PORT", 8080)
    val clickHouseUrl = ConfigLoader.getRequiredProperty(props, "clickhouse.url", "CLICKHOUSE_URL")
    val clickHouseUser = ConfigLoader.getProperty(props, "clickhouse.user", "CLICKHOUSE_USER", "default")
    val clickHousePassword = ConfigLoader.getProperty(props, "clickhouse.password", "CLICKHOUSE_PASSWORD", "")
    val telemetryApiKey = ConfigLoader.getProperty(props, "telemetry.apiKey", "TELEMETRY_API_KEY", "")

    val apiKeyAuth = if (telemetryApiKey.isBlank()) "disabled" else "enabled"
    logger.info(
        "Starting KTelemetry server v${BuildInfo.version}, ClickHouse: $clickHouseUrl, API key auth: $apiKeyAuth",
    )

    val telemetryRepository: TelemetryRepository =
        ClickHouseClientTelemetryRepository(
            endpoint = clickHouseUrl.trimEnd('/'),
            user = clickHouseUser,
            password = clickHousePassword,
        )

    embeddedServer(Netty, host = host, port = port) {
        module(telemetryRepository, telemetryApiKey)
    }.start(wait = true)
}

fun Application.module(
    telemetryRepository: TelemetryRepository,
    telemetryApiKey: String?,
) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
        )
    }

    install(CallLogging)

    routing {
        healthCheck(telemetryRepository)
        telemetryRoutes(telemetryRepository, telemetryApiKey)
    }
}

fun Routing.healthCheck(repository: TelemetryRepository) {
    get("/health") {
        val chHealth = repository.getHealth()
        call.respond(
            HealthResponse(
                status = if (chHealth.reachable) "ok" else "degraded",
                uptimeMs = ManagementFactory.getRuntimeMXBean().uptime,
                version = BuildInfo.version,
                clickhouse = chHealth,
            ),
        )
    }
}
