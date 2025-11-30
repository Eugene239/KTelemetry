package io.ktelemetry.android.lifecycle

import android.app.Application
import io.ktelemetry.android.TelemetryClient

class ApplicationLifecycleCallback : Application.ActivityLifecycleCallbacks {
    
    override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {
    }

    override fun onActivityStarted(activity: android.app.Activity) {
    }

    override fun onActivityResumed(activity: android.app.Activity) {
    }

    override fun onActivityPaused(activity: android.app.Activity) {
    }

    override fun onActivityStopped(activity: android.app.Activity) {
    }

    override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {
    }

    override fun onActivityDestroyed(activity: android.app.Activity) {
    }
}

object ApplicationLifecycleManager {
    
    fun register(application: Application) {
        application.registerComponentCallbacks(object : android.content.ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
            }

            override fun onLowMemory() {
                TelemetryClient.getInstance().flush()
            }

            override fun onTrimMemory(level: Int) {
                if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE) {
                    TelemetryClient.getInstance().flush()
                }
            }
        })
    }
}

