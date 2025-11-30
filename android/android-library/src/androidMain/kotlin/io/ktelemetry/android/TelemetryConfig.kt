package io.ktelemetry.android

data class TelemetryConfig(
    val serverUrl: String,
    val batchSize: Int = 100,
    val appId: String,
    val appVersion: String? = null
)

