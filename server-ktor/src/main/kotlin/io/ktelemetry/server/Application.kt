package io.ktelemetry.server

import io.ktelemetry.server.config.ConfigLoader
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktelemetry.server.routes.telemetryRoutes
import io.ktelemetry.server.repository.TelemetryRepository
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val localProperties = ConfigLoader.loadLocalProperties()

    try {
        val host = ConfigLoader.getRequiredProperty(localProperties, "host", "HOST")
        val port = ConfigLoader.getRequiredIntProperty(localProperties, "port", "PORT")

        embeddedServer(Netty, host = host, port = port) {
            module()
        }.start(wait = true)
    } catch (e: IllegalStateException) {
        System.err.println("Configuration error: ${e.message}")
        System.exit(1)
    }
}

fun Application.module() {
    val logger = LoggerFactory.getLogger(Application::class.java)
    val localProperties = ConfigLoader.loadLocalProperties()

    try {
        val clickHouseUrl = ConfigLoader.getRequiredProperty(
            localProperties,
            "clickhouse.url",
            "CLICKHOUSE_URL"
        )

        val clickHouseUser = ConfigLoader.getRequiredProperty(
            localProperties,
            "clickhouse.user",
            "CLICKHOUSE_USER"
        )

        val clickHousePassword = ConfigLoader.getRequiredProperty(
            localProperties,
            "clickhouse.password",
            "CLICKHOUSE_PASSWORD"
        )

        logger.info("Initializing ClickHouse client with URL: $clickHouseUrl")

        val telemetryRepository = TelemetryRepository(
            url = clickHouseUrl,
            user = clickHouseUser,
            password = clickHousePassword
        )

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }

        install(CallLogging)
        routing {
            healthCheck()
            telemetryRoutes(telemetryRepository)
        }
    } catch (e: IllegalStateException) {
        logger.error("Configuration error: ${e.message}")
        throw e
    }
}

fun Routing.healthCheck() {
    get("/health") {
        call.respond(mapOf("status" to "ok"))
    }
}

