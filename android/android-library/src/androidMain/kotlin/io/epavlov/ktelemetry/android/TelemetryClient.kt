package io.epavlov.ktelemetry.android

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.epavlov.ktelemetry.android.internal.data.EventRepositoryImpl
import io.epavlov.ktelemetry.android.internal.data.SessionStore
import io.epavlov.ktelemetry.android.internal.data.local.EventDatabaseHelper
import io.epavlov.ktelemetry.android.internal.domain.FlushEventsUseCase
import io.epavlov.ktelemetry.android.internal.TelemetryTransportFactory
import io.epavlov.ktelemetry.android.internal.domain.TelemetryState
import io.epavlov.ktelemetry.android.internal.domain.TrackEventUseCase
import io.epavlov.ktelemetry.android.internal.platform.CrashHandler
import io.epavlov.ktelemetry.android.internal.platform.DeviceInfoCollector
import io.epavlov.ktelemetry.android.internal.platform.FlushWorker
import io.epavlov.ktelemetry.android.internal.platform.LifecycleManager
import io.epavlov.ktelemetry.core.ErrorInfo
import io.epavlov.ktelemetry.core.EventContext
import io.epavlov.ktelemetry.core.SessionInfo
import io.epavlov.ktelemetry.core.TelemetryEventType
import io.epavlov.ktelemetry.core.TelemetryLevel
import io.epavlov.ktelemetry.core.UserInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class TelemetryClient {
    private data class Runtime(
        val scope: CoroutineScope,
        val trackEventUseCase: TrackEventUseCase,
        val flushEventsUseCase: FlushEventsUseCase,
        val telemetryState: TelemetryState,
        val sessionStore: SessionStore,
    )

    private var runtime: Runtime? = null

    companion object {
        private const val TAG = "KTelemetry"
        private const val FLUSH_INTERVAL_MIN = 15L
    }

    fun initialize(
        context: Context,
        config: TelemetryConfig,
    ) {
        val scope =
            config.coroutineScope
                ?: CoroutineScope(
                    SupervisorJob() +
                        Dispatchers.Default +
                        CoroutineExceptionHandler { _, e ->
                            Log.e(TAG, "Coroutine failure", e)
                        },
                )

        val database = EventDatabaseHelper(context)
        val transport = TelemetryTransportFactory.create(context, config)
        val repository = EventRepositoryImpl(database)

        val flushEventsUseCase = FlushEventsUseCase(repository, transport, config.batchSize)
        val telemetryState =
            TelemetryState().apply {
                user = config.user
            }
        val sessionStore = SessionStore(context)
        telemetryState.session = sessionStore.loadOrCreate(config)
        val trackEventUseCase =
            TrackEventUseCase(
                config = config,
                repository = repository,
                deviceInfoCollector = DeviceInfoCollector(context),
                flushEvents = flushEventsUseCase,
                telemetryState = telemetryState,
            )

        runtime = Runtime(scope, trackEventUseCase, flushEventsUseCase, telemetryState, sessionStore)

        CrashHandler.install()
        if (context is Application) {
            LifecycleManager.register(context)
        }
        schedulePeriodicFlush(context)
    }

    private fun schedulePeriodicFlush(context: Context) {
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val request =
            PeriodicWorkRequestBuilder<FlushWorker>(FLUSH_INTERVAL_MIN, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            FlushWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun trackEvent(
        eventName: String,
        eventType: TelemetryEventType = TelemetryEventType.USER_ACTION,
        level: TelemetryLevel = TelemetryLevel.INFO,
        context: EventContext? = null,
        error: ErrorInfo? = null,
    ) {
        val r = runtime ?: return
        r.scope.launch {
            try {
                r.trackEventUseCase.execute(eventName, eventType, level, context, error)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Error tracking event: ${e.message}", e)
            }
        }
    }

    fun trackBreadcrumb(
        name: String,
        breadcrumbType: String = "log",
        screenName: String? = null,
    ) {
        trackEvent(
            eventName = name,
            eventType = TelemetryEventType.BREADCRUMB,
            level = TelemetryLevel.DEBUG,
            context = EventContext(breadcrumbType = breadcrumbType, screenName = screenName),
        )
    }

    fun trackScreen(
        screenName: String,
        previousScreen: String? = null,
    ) {
        trackEvent(
            eventName = screenName,
            eventType = TelemetryEventType.SCREEN_VIEW,
            context = EventContext(screenName = screenName, previousScreen = previousScreen),
        )
    }

    fun trackError(
        exception: Throwable,
        isFatal: Boolean = false,
    ) {
        val sw = StringWriter()
        exception.printStackTrace(PrintWriter(sw))

        trackEvent(
            eventName = exception.javaClass.simpleName,
            eventType = if (isFatal) TelemetryEventType.CRASH else TelemetryEventType.ERROR,
            level = if (isFatal) TelemetryLevel.FATAL else TelemetryLevel.ERROR,
            error =
                ErrorInfo(
                    type = exception.javaClass.name,
                    message = exception.message,
                    stacktrace = sw.toString(),
                    isFatal = isFatal,
                    threadName = Thread.currentThread().name,
                ),
        )
    }

    fun flush() {
        val r = runtime ?: return
        r.scope.launch {
            flushSuspending()
        }
    }

    fun setUser(user: UserInfo?) {
        runtime?.telemetryState?.user = user
    }

    fun setSession(session: SessionInfo) {
        val r = runtime ?: return
        r.telemetryState.session = session
        r.sessionStore.persist(session)
    }

    fun newSession() {
        val r = runtime ?: return
        val session =
            SessionInfo(
                sessionId = UUID.randomUUID().toString(),
                sessionStartTime = System.currentTimeMillis(),
            )
        r.telemetryState.session = session
        r.sessionStore.persist(session)
    }

    internal fun enqueueFatalCrashAndFlush(
        error: ErrorInfo,
        onComplete: () -> Unit,
    ) {
        val r = runtime ?: run {
            onComplete()
            return
        }
        r.scope.launch {
            try {
                r.trackEventUseCase.execute(
                    eventName = "app_crash",
                    eventType = TelemetryEventType.CRASH,
                    level = TelemetryLevel.FATAL,
                    context = null,
                    error = error,
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Error reporting crash: ${e.message}", e)
            } finally {
                try {
                    flushSuspending()
                } finally {
                    onComplete()
                }
            }
        }
    }

    suspend fun flushSuspending() {
        val r = runtime ?: return
        try {
            r.flushEventsUseCase.execute()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error flushing events: ${e.message}", e)
        }
    }
}
