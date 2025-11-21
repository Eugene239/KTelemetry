package io.ktelemetry.server.repository

import kotlinx.serialization.Serializable

@Suppress("PropertyName")
@Serializable
data class FlatTelemetryEvent(
    val event_time: Long,
    val event_type: String,
    val event_name: String,
    val level: String,
    val feature: String? = null,
    val tags: List<String> = emptyList(),
    val app_id: String,
    val app_version: String? = null,
    val app_name: String? = null,
    val app_build_number: String? = null,
    val app_environment: String? = null,
    val user_id: String,
    val anonymous_id: String,
    val user_name: String? = null,
    val user_email: String? = null,
    val user_roles: List<String> = emptyList(),
    val device_id: String,
    val device_type: String? = null,
    val device_os: String? = null,
    val device_os_version: String? = null,
    val device_manufacturer: String? = null,
    val device_model: String? = null,
    val device_screen_width: Int? = null,
    val device_screen_height: Int? = null,
    val session_id: String,
    val session_start_time: Long? = null,
    val context_feature: String? = null,
    val context_tags: List<String> = emptyList(),
    val feature_flags: String,
    val payload: String
)