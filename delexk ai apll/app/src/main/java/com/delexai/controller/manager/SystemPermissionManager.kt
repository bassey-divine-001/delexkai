package com.delexai.controller.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import timber.log.Timber

/**
 * Manages system permission checks and requests using native Android intent routing.
 * Handles battery optimization exemption and other system-level permission flows.
 */
class SystemPermissionManager(private val context: Context) {

    /**
     * Requests exemption from battery optimization.
     * Opens the device settings page where the user can manually grant exemption.
     */
    fun requestBatteryOptimizationBypass() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Timber.d("Opened battery optimization settings")
            } else {
                Timber.w("Battery optimization bypass intent not available")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error requesting battery optimization bypass")
        }
    }

    /**
     * Opens Android Accessibility Settings to allow user to enable the AccessibilityActionService.
     */
    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Timber.d("Opened accessibility settings")
        } catch (e: Exception) {
            Timber.e(e, "Error opening accessibility settings")
        }
    }

    /**
     * Opens Android Display Settings to allow user to enable overlay/system alert window permission.
     */
    fun openDisplaySettings() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Timber.d("Opened display overlay settings")
            } else {
                Timber.w("Display overlay settings intent not available")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error opening display settings")
        }
    }

    /**
     * Opens Android Sound Settings to configure microphone permissions.
     */
    fun openSoundSettings() {
        try {
            val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Timber.d("Opened sound settings")
        } catch (e: Exception) {
            Timber.e(e, "Error opening sound settings")
        }
    }

    /**
     * Checks if the app has overlay permission (SYSTEM_ALERT_WINDOW).
     *
     * @return true if overlay permission is granted
     */
    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Always allowed on Android < 6.0
        }
    }
}
