package io.epavlov.ktelemetry.core

import kotlinx.serialization.Serializable

@Serializable
data class ErrorInfo(
    val type: String,
    val message: String? = null,
    val stacktrace: String? = null,
    val isFatal: Boolean = false,
    val threadName: String? = null,
    val threads: String? = null,
)
