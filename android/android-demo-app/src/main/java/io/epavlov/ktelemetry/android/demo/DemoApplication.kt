package io.epavlov.ktelemetry.android.demo

import android.app.Application
import io.epavlov.ktelemetry.android.KTelemetry
import io.epavlov.ktelemetry.android.TelemetryConfig

class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        KTelemetry.install(this, TelemetryConfig(
            serverUrl = BuildConfig.KTELEMETRY_SERVER_URL,
            apiKey = BuildConfig.KTELEMETRY_API_KEY.takeIf { it.isNotBlank() },
            batchSize = 10,
            appId = "demo-app",
            appVersion = "1.0.1337"
        ))
    }
}
