package io.epavlov.ktelemetry.android.internal.data

import io.epavlov.ktelemetry.core.AppInfo
import io.epavlov.ktelemetry.core.DeviceInfo
import io.epavlov.ktelemetry.core.ErrorInfo
import io.epavlov.ktelemetry.core.EventContext
import io.epavlov.ktelemetry.core.SessionInfo
import io.epavlov.ktelemetry.core.TelemetryEvent
import io.epavlov.ktelemetry.core.wireValue
import io.epavlov.ktelemetry.core.UserInfo
import org.json.JSONArray
import org.json.JSONObject

internal object EventJsonConverter {
    fun toJson(event: TelemetryEvent): String = toJsonObject(event).toString()

    fun toJsonArray(jsonStrings: List<String>): String {
        val arr = JSONArray()
        for (s in jsonStrings) arr.put(JSONObject(s))
        return arr.toString()
    }

    private fun toJsonObject(event: TelemetryEvent): JSONObject =
        JSONObject().apply {
            put("eventTime", event.eventTime)
            putOpt("eventTimeZoneId", event.eventTimeZoneId)
            if (event.eventUtcOffsetMinutes != null) {
                put("eventUtcOffsetMinutes", event.eventUtcOffsetMinutes)
            }
            put("eventType", event.eventType.wireValue())
            put("eventName", event.eventName)
            put("level", event.level.name)
            put("app", toJsonObject(event.app))
            putOpt("user", event.user?.let { toJsonObject(it) })
            putOpt("device", event.device?.let { toJsonObject(it) })
            putOpt("session", event.session?.let { toJsonObject(it) })
            putOpt("context", event.context?.let { toJsonObject(it) })
            putOpt("error", event.error?.let { toJsonObject(it) })
        }

    private fun toJsonObject(app: AppInfo): JSONObject =
        JSONObject().apply {
            put("appId", app.appId)
            putOpt("appVersion", app.appVersion)
            putOpt("appName", app.appName)
            putOpt("buildNumber", app.buildNumber)
            putOpt("environment", app.environment)
        }

    private fun toJsonObject(user: UserInfo): JSONObject =
        JSONObject().apply {
            putOpt("userId", user.userId)
            putOpt("anonymousId", user.anonymousId)
            putOpt("userName", user.userName)
            putOpt("userEmail", user.userEmail)
            if (user.userRoles.isNotEmpty()) put("userRoles", JSONArray(user.userRoles))
        }

    private fun toJsonObject(device: DeviceInfo): JSONObject =
        JSONObject().apply {
            putOpt("deviceId", device.deviceId)
            putOpt("deviceType", device.deviceType)
            putOpt("os", device.os)
            putOpt("osVersion", device.osVersion)
            putOpt("manufacturer", device.manufacturer)
            putOpt("model", device.model)
            putOpt("screenWidth", device.screenWidth)
            putOpt("screenHeight", device.screenHeight)
            putOpt("locale", device.locale)
            putOpt("orientation", device.orientation)
            putOpt("networkType", device.networkType)
            putOpt("batteryLevel", device.batteryLevel)
            putOpt("memoryFree", device.memoryFree)
            putOpt("memoryTotal", device.memoryTotal)
            putOpt("storageFree", device.storageFree)
            putOpt("isForeground", device.isForeground)
            putOpt("isRooted", device.isRooted)
        }

    private fun toJsonObject(session: SessionInfo): JSONObject =
        JSONObject().apply {
            put("sessionId", session.sessionId)
            putOpt("sessionStartTime", session.sessionStartTime)
        }

    private fun toJsonObject(ctx: EventContext): JSONObject =
        JSONObject().apply {
            putOpt("feature", ctx.feature)
            putOpt("screenName", ctx.screenName)
            putOpt("previousScreen", ctx.previousScreen)
            putOpt("breadcrumbType", ctx.breadcrumbType)
            putOpt("durationMs", ctx.durationMs)
            if (ctx.tags.isNotEmpty()) put("tags", JSONArray(ctx.tags))
            if (ctx.featureFlags != null) put("featureFlags", JSONObject(ctx.featureFlags!!))
            if (ctx.payload != null) put("payload", JSONObject(ctx.payload!!))
        }

    private fun toJsonObject(error: ErrorInfo): JSONObject =
        JSONObject().apply {
            put("type", error.type)
            putOpt("message", error.message)
            putOpt("stacktrace", error.stacktrace)
            put("isFatal", error.isFatal)
            putOpt("threadName", error.threadName)
            putOpt("threads", error.threads)
        }
}
