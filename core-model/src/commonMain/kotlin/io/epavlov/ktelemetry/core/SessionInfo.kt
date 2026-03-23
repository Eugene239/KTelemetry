package io.epavlov.ktelemetry.core

import kotlinx.serialization.Serializable

@Serializable
data class SessionInfo(
    val sessionId: String,
    val sessionStartTime: Long? = null,
)
