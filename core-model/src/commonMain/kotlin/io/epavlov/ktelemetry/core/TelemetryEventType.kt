package io.epavlov.ktelemetry.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

@Serializable
enum class TelemetryEventType {
    @SerialName("user_action")
    USER_ACTION,

    @SerialName("lifecycle")
    LIFECYCLE,

    @SerialName("screen_view")
    SCREEN_VIEW,

    @SerialName("breadcrumb")
    BREADCRUMB,

    @SerialName("crash")
    CRASH,

    @SerialName("error")
    ERROR,

    @SerialName("custom")
    CUSTOM,
}

private val jsonWire = Json { encodeDefaults = true }

fun TelemetryEventType.wireValue(): String =
    (jsonWire.encodeToJsonElement(TelemetryEventType.serializer(), this) as JsonPrimitive).content
