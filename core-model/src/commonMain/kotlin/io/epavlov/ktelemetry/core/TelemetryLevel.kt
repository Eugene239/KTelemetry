package io.epavlov.ktelemetry.core

import kotlinx.serialization.Serializable

@Serializable
enum class TelemetryLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL,
}
