package io.epavlov.ktelemetry.android.internal.domain

import io.epavlov.ktelemetry.core.SessionInfo
import io.epavlov.ktelemetry.core.UserInfo

internal class TelemetryState {
    @Volatile
    var user: UserInfo? = null

    @Volatile
    var session: SessionInfo? = null
}
