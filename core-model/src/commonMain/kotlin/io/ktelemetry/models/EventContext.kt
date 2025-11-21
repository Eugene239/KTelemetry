package io.ktelemetry.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class EventContext(
    val feature: String? = null,
    val tags: List<String> = emptyList(),
    val featureFlags: JsonObject? = null,
    val payload: JsonObject? = null
)

