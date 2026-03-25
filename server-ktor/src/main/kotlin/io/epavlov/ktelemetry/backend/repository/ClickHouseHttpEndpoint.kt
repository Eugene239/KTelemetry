package io.epavlov.ktelemetry.backend.repository

import java.net.URI

internal fun normalizedClickHouseHttpEndpoint(url: String): String {
    val trimmed = url.trim().trimEnd('/')
    val uri = URI(trimmed)
    val scheme = uri.scheme ?: "http"
    val host = uri.host ?: error("Invalid clickhouse.url: missing host")
    val port = uri.port.takeIf { it > 0 } ?: if (scheme == "https") 443 else 8123
    val path = uri.path?.takeIf { it.isNotEmpty() && it != "/" } ?: ""
    return if (path.isEmpty()) {
        "$scheme://$host:$port/"
    } else {
        "$scheme://$host:$port$path"
    }
}
