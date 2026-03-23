package io.epavlov.ktelemetry.core

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
    val screenHeight: Int? = null,
    val locale: String? = null,
    val orientation: String? = null,
    val networkType: String? = null,
    val batteryLevel: Int? = null,
    val memoryFree: Long? = null,
    val memoryTotal: Long? = null,
    val storageFree: Long? = null,
    val isForeground: Boolean? = null,
    val isRooted: Boolean? = null,
)
