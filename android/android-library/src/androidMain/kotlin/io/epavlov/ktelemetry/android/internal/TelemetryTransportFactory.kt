package io.epavlov.ktelemetry.android.internal

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import io.epavlov.ktelemetry.android.TelemetryConfig
import io.epavlov.ktelemetry.android.transport.EventTransport
import io.epavlov.ktelemetry.android.transport.HttpEventTransport
import io.epavlov.ktelemetry.android.transport.LogcatEventTransport

internal object TelemetryTransportFactory {
    private const val TAG = "KTelemetry"

    fun create(
        context: Context,
        config: TelemetryConfig,
    ): EventTransport {
        config.eventTransport?.let {
            Log.i(TAG, "Using custom EventTransport from TelemetryConfig")
            return it
        }
        if (isHostApplicationDebuggable(context)) {
            Log.i(TAG, "Host application is debuggable: using LogcatEventTransport")
            return LogcatEventTransport()
        }
        if (config.serverUrl.isBlank()) {
            Log.w(TAG, "Release build but serverUrl is blank: using LogcatEventTransport")
            return LogcatEventTransport()
        }
        return HttpEventTransport(config.serverUrl, config.apiKey)
    }

    private fun isHostApplicationDebuggable(context: Context): Boolean =
        (context.applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
