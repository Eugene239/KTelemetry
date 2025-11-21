package io.ktelemetry.models

import kotlinx.serialization.Serializable

@Serializable
enum class TelemetryLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

