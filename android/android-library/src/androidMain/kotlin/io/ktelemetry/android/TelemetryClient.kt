package io.ktelemetry.android

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.ktelemetry.android.crash.CrashHandler
import io.ktelemetry.android.database.TelemetryDatabase
import io.ktelemetry.android.device.DeviceInfoCollector
import io.ktelemetry.android.lifecycle.ApplicationLifecycleManager
import io.ktelemetry.android.sender.EventSender
import io.ktelemetry.android.worker.TelemetryWorker
import io.ktelemetry.models.AppInfo
import io.ktelemetry.models.DeviceInfo
import io.ktelemetry.models.EventContext
import io.ktelemetry.models.TelemetryEvent
import io.ktelemetry.models.TelemetryLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class TelemetryClient private constructor() {
    
    private var config: TelemetryConfig? = null
    private var database: TelemetryDatabase? = null
    private var eventSender: EventSender? = null
    private var deviceInfoCollector: DeviceInfoCollector? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private const val TAG = "TelemetryClient"
        
        @Volatile
        private var INSTANCE: TelemetryClient? = null

        fun getInstance(): TelemetryClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TelemetryClient().also { INSTANCE = it }
            }
        }
    }

    fun initialize(context: Context, config: TelemetryConfig) {
        this.config = config
        this.database = TelemetryDatabase.getDatabase(context)
        this.eventSender = EventSender(config.serverUrl)
        this.deviceInfoCollector = DeviceInfoCollector(context)
        schedulePeriodicWork(context)
        CrashHandler.install(context)
        
        if (context is android.app.Application) {
            ApplicationLifecycleManager.register(context)
        }
    }

    private fun schedulePeriodicWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<TelemetryWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ktelemetry_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun trackEvent(
        eventName: String,
        eventType: String = "user_action",
        level: TelemetryLevel = TelemetryLevel.INFO,
        context: EventContext? = null
    ) {
        val config = this.config ?: return
        val database = this.database ?: return
        val deviceInfoCollector = this.deviceInfoCollector ?: return

        scope.launch {
            try {
                val deviceInfo = deviceInfoCollector.collectDeviceInfo()
                val appInfo = AppInfo(
                    appId = config.appId,
                    appVersion = config.appVersion
                )

                val event = TelemetryEvent(
                    eventTime = System.currentTimeMillis(),
                    eventType = eventType,
                    eventName = eventName,
                    level = level,
                    app = appInfo,
                    user = null,
                    device = deviceInfo,
                    session = null,
                    context = context
                )

                val entity = convertToEntity(event)
                database.telemetryEventDao().insert(entity)
                Log.d(TAG, "Event saved: $eventName (total count: ${database.telemetryEventDao().getEventCount()})")

                checkAndSendBatch()
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking event: ${e.message}", e)
            }
        }
    }

    fun flush() {
        scope.launch {
            Log.d(TAG, "Flush called")
            sendAllPendingEvents()
        }
    }

    private suspend fun checkAndSendBatch() {
        val config = this.config ?: return
        val database = this.database ?: return
        
        val count = database.telemetryEventDao().getEventCount()
        Log.d(TAG, "Checking batch: count=$count, batchSize=${config.batchSize}")
        if (count >= config.batchSize) {
            Log.d(TAG, "Batch size reached, sending events...")
            sendPendingEvents(config.batchSize)
        }
    }

    private suspend fun sendAllPendingEvents() {
        val config = this.config ?: return
        val database = this.database ?: return
        
        Log.d(TAG, "Starting to send all pending events")
        while (true) {
            val events = database.telemetryEventDao().getPendingEvents(config.batchSize)
            if (events.isEmpty()) {
                Log.d(TAG, "No more pending events")
                break
            }
            
            Log.d(TAG, "Sending batch of ${events.size} events")
            val success = sendPendingEvents(events)
            if (!success) {
                Log.e(TAG, "Failed to send batch, stopping")
                break
            }
        }
    }

    private suspend fun sendPendingEvents(batchSize: Int) {
        val database = this.database ?: return

        val events = database.telemetryEventDao().getPendingEvents(batchSize)
        if (events.isEmpty()) {
            return
        }

        sendPendingEvents(events)
    }

    private suspend fun sendPendingEvents(events: List<io.ktelemetry.android.database.TelemetryEventEntity>): Boolean {
        val database = this.database ?: return false
        val sender = this.eventSender ?: return false

        Log.d(TAG, "Attempting to send ${events.size} events")
        val success = sender.sendEvents(events)
        if (success) {
            val deletedCount = database.telemetryEventDao().deleteEventsByIds(events.map { it.id })
            Log.d(TAG, "Successfully sent and deleted ${events.size} events")
        } else {
            Log.e(TAG, "Failed to send events, keeping them in database")
        }
        return success
    }

    private fun convertToEntity(event: TelemetryEvent): io.ktelemetry.android.database.TelemetryEventEntity {
        val appInfoJson = json.encodeToString(event.app)
        val userInfoJson = event.user?.let { json.encodeToString(it) }
        val deviceInfoJson = event.device?.let { json.encodeToString(it) }
        val sessionInfoJson = event.session?.let { json.encodeToString(it) }
        val contextJson = event.context?.let { 
            json.encodeToString(it)
        }

        return io.ktelemetry.android.database.TelemetryEventEntity(
            eventTime = event.eventTime,
            eventType = event.eventType,
            eventName = event.eventName,
            level = event.level.name,
            appInfo = appInfoJson,
            userInfo = userInfoJson,
            deviceInfo = deviceInfoJson,
            sessionInfo = sessionInfoJson,
            context = contextJson
        )
    }
}

