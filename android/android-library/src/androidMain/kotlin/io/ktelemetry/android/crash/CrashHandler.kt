package io.ktelemetry.android.crash

import android.content.Context
import io.ktelemetry.android.TelemetryClient
import io.ktelemetry.models.EventContext
import io.ktelemetry.models.TelemetryLevel
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.PrintWriter
import java.io.StringWriter

class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            val stackTrace = StringWriter()
            exception.printStackTrace(PrintWriter(stackTrace))
            
            val crashContext = EventContext(
                payload = buildJsonObject {
                    put("exception", exception.javaClass.name)
                    put("message", exception.message ?: "")
                    put("stackTrace", stackTrace.toString())
                }
            )

            TelemetryClient.getInstance().trackEvent(
                eventName = "app_crash",
                eventType = "crash",
                level = TelemetryLevel.ERROR,
                context = crashContext
            )

            TelemetryClient.getInstance().flush()
            
            Thread.sleep(2000)
        } catch (e: Exception) {
        } finally {
            defaultHandler?.uncaughtException(thread, exception)
        }
    }

    companion object {
        fun install(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            val crashHandler = CrashHandler(context, defaultHandler)
            Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        }
    }
}

