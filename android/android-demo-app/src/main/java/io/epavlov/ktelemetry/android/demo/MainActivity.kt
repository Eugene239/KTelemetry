package io.epavlov.ktelemetry.android.demo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.epavlov.ktelemetry.android.KTelemetry
import io.epavlov.ktelemetry.core.EventContext
import io.epavlov.ktelemetry.core.TelemetryEventType
import io.epavlov.ktelemetry.core.TelemetryLevel
import io.epavlov.ktelemetry.core.UserInfo
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TelemetryDemoScreen()
            }
        }
    }
}

@Composable
fun TelemetryDemoScreen() {
    var statusMessage by remember { mutableStateOf("") }
    var loggedIn by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "KTelemetry Demo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Auth (demo)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (loggedIn) "SDK user: Alice (demo_user_alice)" else "SDK user: none",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Events store user at track time; after logout, queued events still carry the old user JSON.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            KTelemetry.setUser(
                                UserInfo(
                                    userId = "demo_user_alice",
                                    userName = "Alice",
                                    userEmail = "alice@demo.local",
                                ),
                            )
                            loggedIn = true
                            statusMessage = "Logged in — next track() calls attach Alice"
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !loggedIn,
                    ) {
                        Text("Login")
                    }
                    OutlinedButton(
                        onClick = {
                            KTelemetry.setUser(null)
                            loggedIn = false
                            statusMessage = "Logged out — new events have no user; old queue unchanged"
                        },
                        modifier = Modifier.weight(1f),
                        enabled = loggedIn,
                    ) {
                        Text("Logout")
                    }
                }
            }
        }

        EventButton("Send INFO Event") {
            KTelemetry.track("button_click", context = EventContext(
                feature = "main_screen",
                tags = listOf("button", "info")
            ))
            statusMessage = "INFO event sent!"
        }

        EventButton("Send DEBUG Event") {
            KTelemetry.track("debug_action", level = TelemetryLevel.DEBUG, context = EventContext(
                feature = "main_screen",
                tags = listOf("button", "debug")
            ))
            statusMessage = "DEBUG event sent!"
        }

        EventButton("Send WARN Event") {
            KTelemetry.track("warning_action", level = TelemetryLevel.WARN, context = EventContext(
                feature = "main_screen",
                tags = listOf("button", "warning")
            ))
            statusMessage = "WARN event sent!"
        }

        EventButton("Send ERROR Event") {
            KTelemetry.track(
                eventName = "error_action",
                level = TelemetryLevel.ERROR,
                context = EventContext(
                    feature = "main_screen",
                    tags = listOf("button", "error"),
                    payload = JSONObject().apply {
                        put("error_code", "DEMO_ERROR")
                        put("error_message", "This is a demo error event")
                    }.toString()
                )
            )
            statusMessage = "ERROR event sent!"
        }

        EventButton("Send Custom Event") {
            KTelemetry.track(
                eventName = "custom_event",
                eventType = TelemetryEventType.CUSTOM,
                context = EventContext(
                    feature = "main_screen",
                    tags = listOf("custom", "demo"),
                    payload = JSONObject().apply {
                        put("custom_field", "custom_value")
                        put("timestamp", System.currentTimeMillis())
                    }.toString()
                )
            )
            statusMessage = "Custom event sent!"
        }

        Button(
            onClick = { throw RuntimeException("Demo crash triggered by user") },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Trigger Crash")
        }

        Spacer(modifier = Modifier.height(16.dp))

        EventButton("Flush Events") {
            KTelemetry.flush()
            statusMessage = "Events flushed!"
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EventButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}
