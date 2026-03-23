package io.epavlov.ktelemetry.android

import android.content.Context
import android.util.Log
import io.epavlov.ktelemetry.core.ErrorInfo
import io.epavlov.ktelemetry.core.EventContext
import io.epavlov.ktelemetry.core.SessionInfo
import io.epavlov.ktelemetry.core.TelemetryEventType
import io.epavlov.ktelemetry.core.TelemetryLevel
import io.epavlov.ktelemetry.core.UserInfo

object KTelemetry {
    private const val TAG = "KTelemetry"

    private val installLock = Any()

    @Volatile
    private var client: TelemetryClient? = null

    fun install(
        context: Context,
        config: TelemetryConfig,
    ) {
        synchronized(installLock) {
            if (client != null) {
                Log.w(TAG, "Already installed, ignoring duplicate install()")
                return
            }
            val c = TelemetryClient()
            c.initialize(context, config)
            client = c
        }
        Log.i(TAG, "Installed (appId=${config.appId})")
    }

    fun track(
        eventName: String,
        eventType: TelemetryEventType = TelemetryEventType.USER_ACTION,
        level: TelemetryLevel = TelemetryLevel.INFO,
        context: EventContext? = null,
        error: ErrorInfo? = null,
    ) {
        val c = client
        if (c == null) {
            Log.d(TAG, "[not installed] track: $eventName (${eventType.name}/${level.name})")
            return
        }
        c.trackEvent(eventName, eventType, level, context, error)
    }

    fun screen(
        screenName: String,
        previousScreen: String? = null,
    ) {
        val c = client
        if (c == null) {
            Log.d(TAG, "[not installed] screen: $screenName")
            return
        }
        c.trackScreen(screenName, previousScreen)
    }

    fun breadcrumb(
        name: String,
        type: String = "log",
        screenName: String? = null,
    ) {
        val c = client
        if (c == null) {
            Log.d(TAG, "[not installed] breadcrumb: $name")
            return
        }
        c.trackBreadcrumb(name, type, screenName)
    }

    fun error(
        exception: Throwable,
        isFatal: Boolean = false,
    ) {
        val c = client
        if (c == null) {
            Log.d(TAG, "[not installed] error: ${exception.message}")
            return
        }
        c.trackError(exception, isFatal)
    }

    fun flush() {
        val c = client
        if (c == null) {
            Log.d(TAG, "[not installed] flush ignored")
            return
        }
        c.flush()
    }

    fun setUser(user: UserInfo?) {
        client?.setUser(user)
    }

    fun setSession(session: SessionInfo) {
        client?.setSession(session)
    }

    fun newSession() {
        client?.newSession()
    }

    internal suspend fun awaitFlush() {
        client?.flushSuspending()
    }

    internal fun enqueueFatalCrashAndFlush(
        error: ErrorInfo,
        onComplete: () -> Unit,
    ) {
        val c = client
        if (c == null) {
            onComplete()
            return
        }
        c.enqueueFatalCrashAndFlush(error, onComplete)
    }
}
