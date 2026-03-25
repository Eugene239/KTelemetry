package io.epavlov.ktelemetry.backend.repository

import io.epavlov.ktelemetry.core.TelemetryEvent
import io.epavlov.ktelemetry.core.wireValue

internal fun TelemetryEvent.toFlat(): FlatTelemetryEvent {
    val ctx = context
    val dev = device
    val err = error
    return FlatTelemetryEvent(
        event_time = eventTime / 1000,
        event_time_zone_id = eventTimeZoneId,
        event_utc_offset_minutes = eventUtcOffsetMinutes,
        event_type = eventType.wireValue(),
        event_name = eventName,
        level = level.name,
        app_id = app.appId,
        app_version = app.appVersion,
        app_name = app.appName,
        app_build_number = app.buildNumber,
        app_environment = app.environment,
        user_id = user?.userId ?: "",
        anonymous_id = user?.anonymousId ?: "",
        user_name = user?.userName,
        user_email = user?.userEmail,
        user_roles = user?.userRoles ?: emptyList(),
        device_id = dev?.deviceId ?: "",
        device_type = dev?.deviceType,
        device_os = dev?.os,
        device_os_version = dev?.osVersion,
        device_manufacturer = dev?.manufacturer,
        device_model = dev?.model,
        device_screen_width = dev?.screenWidth,
        device_screen_height = dev?.screenHeight,
        device_locale = dev?.locale,
        device_orientation = dev?.orientation,
        network_type = dev?.networkType,
        battery_level = dev?.batteryLevel,
        memory_free = dev?.memoryFree,
        memory_total = dev?.memoryTotal,
        storage_free = dev?.storageFree,
        is_foreground = dev?.isForeground?.let { if (it) 1 else 0 },
        is_rooted = dev?.isRooted?.let { if (it) 1 else 0 },
        session_id = session?.sessionId ?: "",
        session_start_time = session?.sessionStartTime?.div(1000),
        feature = ctx?.feature,
        screen_name = ctx?.screenName,
        previous_screen = ctx?.previousScreen,
        breadcrumb_type = ctx?.breadcrumbType,
        duration_ms = ctx?.durationMs,
        tags = ctx?.tags ?: emptyList(),
        feature_flags = ctx?.featureFlags ?: "",
        payload = ctx?.payload ?: "",
        error_type = err?.type,
        error_message = err?.message,
        error_stacktrace = err?.stacktrace,
        error_is_fatal = if (err?.isFatal == true) 1 else 0,
        error_thread = err?.threadName,
        error_threads = err?.threads,
    )
}
