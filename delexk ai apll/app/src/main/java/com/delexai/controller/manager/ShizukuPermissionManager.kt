package com.delexai.controller.manager

import android.content.Context
import android.os.Build
import dev.rikka.shizuku.Shizuku
import timber.log.Timber

/**
 * Manages Shizuku permission status and requests.
 * Handles the authorization and state of the Shizuku ADB-level system command interface.
 */
class ShizukuPermissionManager(private val context: Context) {

    /**
     * Checks if the application has Shizuku permission granted.
     *
     * @return true if Shizuku permission is granted, false otherwise
     */
    fun isShizukuPermissionGranted(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == 0
        } catch (e: Exception) {
            Timber.e(e, "Error checking Shizuku permission")
            false
        }
    }

    /**
     * Requests Shizuku permission from the user via the Shizuku Manager app.
     * This will trigger the native Shizuku authorization prompt.
     */
    fun requestShizukuPermission() {
        try {
            Timber.d("Requesting Shizuku permission...")
            Shizuku.requestPermission(0) // Code 0 for permission request
        } catch (e: Exception) {
            Timber.e(e, "Error requesting Shizuku permission")
        }
    }

    /**
     * Registers listeners for Shizuku binder lifecycle events.
     *
     * @param onBinderReceived Callback when Shizuku service is available
     * @param onBinderDead Callback when Shizuku service is disconnected
     */
    fun registerShizukuListeners(
        onBinderReceived: (() -> Unit)? = null,
        onBinderDead: (() -> Unit)? = null
    ) {
        try {
            Shizuku.addBinderReceivedListener {
                Timber.d("Shizuku binder received")
                onBinderReceived?.invoke()
            }

            Shizuku.addBinderDeadListener {
                Timber.d("Shizuku binder dead")
                onBinderDead?.invoke()
            }

            Timber.d("Shizuku listeners registered successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error registering Shizuku listeners")
        }
    }

    /**
     * Checks if Shizuku is available on the device.
     *
     * @return true if Shizuku Manager is installed and accessible
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            // Attempt to check Shizuku version
            val versionCode = Shizuku.getVersion()
            versionCode > 0
        } catch (e: Exception) {
            Timber.d("Shizuku not available: ${e.message}")
            false
        }
    }
}
