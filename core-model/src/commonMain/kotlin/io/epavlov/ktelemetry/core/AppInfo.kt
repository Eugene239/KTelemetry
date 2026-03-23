package io.epavlov.ktelemetry.core

import kotlinx.serialization.Serializable

@Serializable
data class AppInfo(
    val appId: String,
    val appVersion: String? = null,
    val appName: String? = null,
    val buildNumber: String? = null,
    val environment: String? = null,
)
