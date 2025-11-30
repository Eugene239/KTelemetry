package io.ktelemetry.android.demo

import android.app.Application
import io.ktelemetry.android.TelemetryClient
import io.ktelemetry.android.TelemetryConfig

class DemoApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        val config = TelemetryConfig(
            serverUrl =  BuildConfig.KTELEMETRY_SERVER_URL,
            batchSize = 10,
            appId = "demo-app",
            appVersion = "1.0.1337"
        )
        
        TelemetryClient.getInstance().initialize(this, config)
    }
}

