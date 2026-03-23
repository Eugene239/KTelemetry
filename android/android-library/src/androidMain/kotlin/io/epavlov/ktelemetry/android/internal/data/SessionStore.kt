package io.epavlov.ktelemetry.android.internal.data

import android.content.Context
import androidx.core.content.edit
import io.epavlov.ktelemetry.android.TelemetryConfig
import io.epavlov.ktelemetry.core.SessionInfo
import java.util.UUID

internal class SessionStore(
    context: Context,
) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadOrCreate(config: TelemetryConfig): SessionInfo? {
        if (!config.autoSession) {
            return config.session
        }
        if (config.session != null) {
            persist(config.session)
            return config.session
        }
        var sessionId = prefs.getString(KEY_SESSION_ID, null)
        var sessionStart = prefs.getLong(KEY_SESSION_START, -1L)
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString()
            prefs.edit(commit = true) { putString(KEY_SESSION_ID, sessionId) }
        }
        if (sessionStart <= 0L) {
            sessionStart = System.currentTimeMillis()
            prefs.edit(commit = true) { putLong(KEY_SESSION_START, sessionStart) }
        }
        return SessionInfo(sessionId = sessionId, sessionStartTime = sessionStart)
    }

    fun persist(session: SessionInfo) {
        prefs.edit(commit = true) {
            putString(KEY_SESSION_ID, session.sessionId)
            putLong(KEY_SESSION_START, session.sessionStartTime ?: System.currentTimeMillis())
        }
    }

    fun clear() {
        prefs.edit(commit = true) {
            remove(KEY_SESSION_ID)
            remove(KEY_SESSION_START)
        }
    }

    private companion object {
        private const val PREFS_NAME = "io.epavlov.ktelemetry.android.session"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_SESSION_START = "session_start_utc_ms"
    }
}
