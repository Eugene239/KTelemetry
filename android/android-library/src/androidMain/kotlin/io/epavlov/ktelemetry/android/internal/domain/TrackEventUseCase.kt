package io.epavlov.ktelemetry.android.internal.domain

import android.util.Log
import io.epavlov.ktelemetry.android.TelemetryConfig
import io.epavlov.ktelemetry.android.internal.platform.DeviceInfoCollector
import io.epavlov.ktelemetry.android.internal.platform.EventTimeSource
import io.epavlov.ktelemetry.core.AppInfo
import io.epavlov.ktelemetry.core.ErrorInfo
import io.epavlov.ktelemetry.core.EventContext
import io.epavlov.ktelemetry.core.TelemetryEvent
import io.epavlov.ktelemetry.core.TelemetryEventType
import io.epavlov.ktelemetry.core.TelemetryLevel

internal class TrackEventUseCase(
    private val config: TelemetryConfig,
    private val repository: EventRepository,
    private val deviceInfoCollector: DeviceInfoCollector,
    private val flushEvents: FlushEventsUseCase,
    private val telemetryState: TelemetryState,
) {
    suspend fun execute(
        eventName: String,
        eventType: TelemetryEventType,
        level: TelemetryLevel,
        context: EventContext?,
        error: ErrorInfo?,
    ) {
        val time = EventTimeSource.now()
        val userSnapshot = telemetryState.user
        val sessionSnapshot = telemetryState.session
        val event =
            TelemetryEvent(
                eventTime = time.utcMillis,
                eventTimeZoneId = time.timeZoneId,
                eventUtcOffsetMinutes = time.utcOffsetMinutes,
                eventType = eventType,
                eventName = eventName,
                level = level,
                app = AppInfo(appId = config.appId, appVersion = config.appVersion),
                user = userSnapshot,
                session = sessionSnapshot,
                device = deviceInfoCollector.collectDeviceInfo(),
                context = context,
                error = error,
            )
        repository.save(event)
        Log.d(TAG, "Event saved: $eventName")
        flushEvents.flushIfBatchReady()
    }

    companion object {
        private const val TAG = "KTelemetry"
    }
}
