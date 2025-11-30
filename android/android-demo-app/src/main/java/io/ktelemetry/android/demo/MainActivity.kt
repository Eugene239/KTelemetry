package io.ktelemetry.android.demo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.ktelemetry.android.TelemetryClient
import io.ktelemetry.models.EventContext
import io.ktelemetry.models.TelemetryLevel
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MainActivity : AppCompatActivity() {
    
    private lateinit var btnInfoEvent: Button
    private lateinit var btnDebugEvent: Button
    private lateinit var btnWarnEvent: Button
    private lateinit var btnErrorEvent: Button
    private lateinit var btnCustomEvent: Button
    private lateinit var btnTriggerCrash: Button
    private lateinit var btnFlushEvents: Button
    private lateinit var tvStatus: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        btnInfoEvent = findViewById(R.id.btnInfoEvent)
        btnDebugEvent = findViewById(R.id.btnDebugEvent)
        btnWarnEvent = findViewById(R.id.btnWarnEvent)
        btnErrorEvent = findViewById(R.id.btnErrorEvent)
        btnCustomEvent = findViewById(R.id.btnCustomEvent)
        btnTriggerCrash = findViewById(R.id.btnTriggerCrash)
        btnFlushEvents = findViewById(R.id.btnFlushEvents)
        tvStatus = findViewById(R.id.tvStatus)
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        btnInfoEvent.setOnClickListener {
            TelemetryClient.getInstance().trackEvent(
                eventName = "button_click",
                eventType = "user_action",
                level = TelemetryLevel.INFO,
                context = EventContext(
                    feature = "main_screen",
                    tags = listOf("button", "info")
                )
            )
            showToast("INFO event sent!")
        }
        
        btnDebugEvent.setOnClickListener {
            TelemetryClient.getInstance().trackEvent(
                eventName = "debug_action",
                eventType = "user_action",
                level = TelemetryLevel.DEBUG,
                context = EventContext(
                    feature = "main_screen",
                    tags = listOf("button", "debug")
                )
            )
            showToast("DEBUG event sent!")
        }
        
        btnWarnEvent.setOnClickListener {
            TelemetryClient.getInstance().trackEvent(
                eventName = "warning_action",
                eventType = "user_action",
                level = TelemetryLevel.WARN,
                context = EventContext(
                    feature = "main_screen",
                    tags = listOf("button", "warning")
                )
            )
            showToast("WARN event sent!")
        }
        
        btnErrorEvent.setOnClickListener {
            TelemetryClient.getInstance().trackEvent(
                eventName = "error_action",
                eventType = "user_action",
                level = TelemetryLevel.ERROR,
                context = EventContext(
                    feature = "main_screen",
                    tags = listOf("button", "error"),
                    payload = buildJsonObject {
                        put("error_code", "DEMO_ERROR")
                        put("error_message", "This is a demo error event")
                    }
                )
            )
            showToast("ERROR event sent!")
        }
        
        btnCustomEvent.setOnClickListener {
            TelemetryClient.getInstance().trackEvent(
                eventName = "custom_event",
                eventType = "custom",
                level = TelemetryLevel.INFO,
                context = EventContext(
                    feature = "main_screen",
                    tags = listOf("custom", "demo"),
                    payload = buildJsonObject {
                        put("custom_field", "custom_value")
                        put("timestamp", System.currentTimeMillis())
                    }
                )
            )
            showToast("Custom event sent!")
        }
        
        btnTriggerCrash.setOnClickListener {
            throw RuntimeException("Demo crash triggered by user")
        }
        
        btnFlushEvents.setOnClickListener {
            TelemetryClient.getInstance().flush()
            showToast("Events flushed!")
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        tvStatus.text = message
    }
}

