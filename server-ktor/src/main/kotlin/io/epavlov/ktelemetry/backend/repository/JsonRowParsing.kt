package io.epavlov.ktelemetry.backend.repository

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal fun JsonElement.asString(): String = (this as JsonPrimitive).content

internal fun JsonElement.asLong(): Long = (this as JsonPrimitive).content.toLong()

internal fun JsonObject.requireString(key: String): String =
    get(key)?.asString() ?: error("missing JSON field: $key")

internal fun JsonObject.requireLong(key: String): Long =
    get(key)?.asLong() ?: error("missing JSON field: $key")
