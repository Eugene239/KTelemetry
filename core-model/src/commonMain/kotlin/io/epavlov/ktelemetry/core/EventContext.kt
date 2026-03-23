package io.epavlov.ktelemetry.core

import kotlinx.serialization.Serializable

@Serializable
data class EventContext(
    val feature: String? = null,
    val screenName: String? = null,
    val previousScreen: String? = null,
    val breadcrumbType: String? = null,
    val durationMs: Long? = null,
    val tags: List<String> = emptyList(),
    val featureFlags: String? = null,
    val payload: String? = null,
)
