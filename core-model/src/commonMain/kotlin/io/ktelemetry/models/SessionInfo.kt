package io.ktelemetry.models

import kotlinx.serialization.Serializable

@Serializable
data class SessionInfo(
    val sessionId: String,
    val sessionStartTime: Long? = null
)

