package io.ktelemetry.android.device

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import io.ktelemetry.models.DeviceInfo
import java.util.UUID

class DeviceInfoCollector(private val context: Context) {
    
    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: UUID.randomUUID().toString()
    }

    fun collectDeviceInfo(): DeviceInfo {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        return DeviceInfo(
            deviceId = deviceId,
            deviceType = "mobile",
            os = "Android",
            osVersion = Build.VERSION.RELEASE,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            screenWidth = displayMetrics.widthPixels,
            screenHeight = displayMetrics.heightPixels
        )
    }
}

