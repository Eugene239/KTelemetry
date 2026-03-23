package io.epavlov.ktelemetry.android.internal.platform

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import io.epavlov.ktelemetry.android.KTelemetry
import io.epavlov.ktelemetry.core.EventContext
import io.epavlov.ktelemetry.core.TelemetryEventType
import io.epavlov.ktelemetry.core.TelemetryLevel

internal object LifecycleManager {
    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(ActivityTracker())

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    KTelemetry.track(eventName = "app_background", eventType = TelemetryEventType.LIFECYCLE)
                    KTelemetry.flush()
                }
            },
        )

        application.registerComponentCallbacks(
            object : ComponentCallbacks2 {
                override fun onConfigurationChanged(newConfig: Configuration) {}

                @Deprecated("Deprecated in ComponentCallbacks")
                override fun onLowMemory() {
                    KTelemetry.track(
                        eventName = "low_memory",
                        eventType = TelemetryEventType.LIFECYCLE,
                        level = TelemetryLevel.WARN,
                    )
                    KTelemetry.flush()
                }

                @Suppress("DEPRECATION")
                override fun onTrimMemory(level: Int) {
                    if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE) {
                        KTelemetry.flush()
                    }
                }
            },
        )
    }
}

private class ActivityTracker : Application.ActivityLifecycleCallbacks {
    private var currentScreen: String? = null
    private var screenStartTime: Long = 0L

    override fun onActivityResumed(activity: Activity) {
        val previousScreen = currentScreen
        currentScreen = activity.javaClass.simpleName
        screenStartTime = System.currentTimeMillis()

        KTelemetry.track(
            eventName = currentScreen!!,
            eventType = TelemetryEventType.SCREEN_VIEW,
            context =
                EventContext(
                    screenName = currentScreen,
                    previousScreen = previousScreen,
                ),
        )
    }

    override fun onActivityPaused(activity: Activity) {
        val screenName = activity.javaClass.simpleName
        val duration = System.currentTimeMillis() - screenStartTime

        if (duration > 0) {
            KTelemetry.track(
                eventName = "${screenName}_duration",
                eventType = TelemetryEventType.SCREEN_VIEW,
                level = TelemetryLevel.DEBUG,
                context =
                    EventContext(
                        screenName = screenName,
                        durationMs = duration,
                    ),
            )
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
