package io.ktelemetry.models

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val deviceId: String? = null,
    val deviceType: String? = null,
    val os: String? = null,
    val osVersion: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val screenWidth: Int? = null,
    val screenHeight: Int? = null
)

