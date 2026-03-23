package io.epavlov.ktelemetry.android

import io.epavlov.ktelemetry.android.transport.EventTransport
import io.epavlov.ktelemetry.core.SessionInfo
import io.epavlov.ktelemetry.core.UserInfo
import kotlinx.coroutines.CoroutineScope

data class TelemetryConfig(
    val serverUrl: String = "",
    val apiKey: String? = null,
    val batchSize: Int = 100,
    val appId: String,
    val appVersion: String? = null,
    val coroutineScope: CoroutineScope? = null,
    val user: UserInfo? = null,
    val session: SessionInfo? = null,
    val autoSession: Boolean = true,
    val eventTransport: EventTransport? = null,
)
