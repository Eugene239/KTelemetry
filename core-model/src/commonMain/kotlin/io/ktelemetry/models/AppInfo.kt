package io.ktelemetry.models

import kotlinx.serialization.Serializable

@Serializable
data class AppInfo(
    val appId: String,
    val appVersion: String? = null,
    val appName: String? = null,
    val buildNumber: String? = null,
    val environment: String? = null
)

