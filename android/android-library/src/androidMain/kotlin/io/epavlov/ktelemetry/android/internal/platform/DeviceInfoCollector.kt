package io.epavlov.ktelemetry.android.internal.platform

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import io.epavlov.ktelemetry.core.DeviceInfo
import java.io.File
import java.util.Locale
import java.util.UUID

internal class DeviceInfoCollector(context: Context) {
    private val appContext = context.applicationContext

    private val deviceId: String by lazy { resolveStableDeviceId() }

    private fun resolveStableDeviceId(): String {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY_DEVICE_ID, null)?.let { if (it.isNotBlank()) return it }
        prefs.getString(LEGACY_FALLBACK_DEVICE_ID, null)?.let { legacy ->
            if (legacy.isNotBlank()) {
                prefs.edit(commit = true) {
                    putString(KEY_DEVICE_ID, legacy)
                    remove(LEGACY_FALLBACK_DEVICE_ID)
                }
                return legacy
            }
        }
        val generated = UUID.randomUUID().toString()
        prefs.edit(commit = true) { putString(KEY_DEVICE_ID, generated) }
        return generated
    }

    private companion object {
        private const val PREFS_NAME = "io.epavlov.ktelemetry.android.device"
        private const val KEY_DEVICE_ID = "device_id"
        private const val LEGACY_FALLBACK_DEVICE_ID = "fallback_device_id"
    }

    fun collectDeviceInfo(): DeviceInfo {
        val (screenW, screenH) = getScreenSize()

        return DeviceInfo(
            deviceId = deviceId,
            deviceType = "mobile",
            os = "Android",
            osVersion = Build.VERSION.RELEASE,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            screenWidth = screenW,
            screenHeight = screenH,
            locale = Locale.getDefault().toLanguageTag(),
            orientation = getOrientation(),
            networkType = getNetworkType(),
            batteryLevel = getBatteryLevel(),
            memoryFree = getMemoryFree(),
            memoryTotal = getMemoryTotal(),
            storageFree = getStorageFree(),
            isForeground =
                ProcessLifecycleOwner.get().lifecycle.currentState
                    .isAtLeast(Lifecycle.State.STARTED),
            isRooted = checkRooted(),
        )
    }

    @Suppress("DEPRECATION")
    private fun getScreenSize(): Pair<Int, Int> {
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = android.util.DisplayMetrics()
            wm.defaultDisplay.getMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }

    private fun getOrientation(): String {
        return when (appContext.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> "landscape"
            else -> "portrait"
        }
    }

    @SuppressLint("MissingPermission")
    private fun getNetworkType(): String {
        val cm =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return "unknown"
        val network = cm.activeNetwork ?: return "none"
        val caps = cm.getNetworkCapabilities(network) ?: return "none"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }
    }

    private fun getBatteryLevel(): Int? {
        val bm =
            appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                ?: return null
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (level in 0..100) level else null
    }

    private fun getMemoryFree(): Long? {
        val am =
            appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return null
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.availMem
    }

    private fun getMemoryTotal(): Long? {
        val am =
            appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return null
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem
    }

    private fun getStorageFree(): Long? {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            null
        }
    }

    private fun checkRooted(): Boolean {
        val paths =
            arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
            )
        return paths.any { File(it).exists() }
    }
}
