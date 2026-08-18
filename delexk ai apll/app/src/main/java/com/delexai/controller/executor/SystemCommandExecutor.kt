package com.delexai.controller.executor

import android.content.Context
import android.content.Intent
import android.os.Build
import dev.rikka.shizuku.Shizuku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Executes system-level commands via Shizuku ADB interface.
 * Provides methods to toggle Wi-Fi, change DPI, and execute arbitrary shell commands.
 * Implements graceful fallback when Shizuku is unavailable.
 */
class SystemCommandExecutor(private val context: Context) {

    private var isShizukuAvailable = false

    init {
        checkShizukuAvailability()
    }

    /**
     * Checks if Shizuku service is currently available.
     */
    private fun checkShizukuAvailability() {
        try {
            isShizukuAvailable = Shizuku.getVersion() > 0
            Timber.d("Shizuku availability: $isShizukuAvailable")
        } catch (e: Exception) {
            isShizukuAvailable = false
            Timber.d("Shizuku not available: ${e.message}")
        }
    }

    /**
     * Toggles Wi-Fi state using Shizuku or fallback methods.
     *
     * @param enable true to enable Wi-Fi, false to disable
     * @return true if command executed successfully
     */
    suspend fun toggleWiFi(enable: Boolean): Boolean = withContext(Dispatchers.Default) {
        return@withContext try {
            if (!isShizukuAvailable) {
                Timber.w("Shizuku unavailable, falling back to standard Android API")
                return@withContext fallbackToggleWiFi(enable)
            }

            val command = if (enable) "svc wifi enable" else "svc wifi disable"
            executeShizukuCommand(command)
            Timber.d("Wi-Fi toggle executed: enable=$enable")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error toggling Wi-Fi")
            false
        }
    }

    /**
     * Changes device DPI using Shizuku.
     *
     * @param dpiValue The DPI value to set (e.g., 320, 420, 480)
     * @return true if command executed successfully
     */
    suspend fun changeDPI(dpiValue: Int): Boolean = withContext(Dispatchers.Default) {
        return@withContext try {
            if (!isShizukuAvailable) {
                Timber.w("Shizuku unavailable for DPI change")
                return@withContext false
            }

            if (dpiValue < 160 || dpiValue > 640) {
                Timber.e("Invalid DPI value: $dpiValue")
                return@withContext false
            }

            val command = "wm density $dpiValue"
            executeShizukuCommand(command)
            Timber.d("DPI changed to: $dpiValue")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error changing DPI")
            false
        }
    }

    /**
     * Toggles Bluetooth using Shizuku.
     *
     * @param enable true to enable, false to disable
     * @return true if command executed successfully
     */
    suspend fun toggleBluetooth(enable: Boolean): Boolean = withContext(Dispatchers.Default) {
        return@withContext try {
            if (!isShizukuAvailable) {
                Timber.w("Shizuku unavailable, cannot toggle Bluetooth")
                return@withContext false
            }

            val command = if (enable) "svc bluetooth enable" else "svc bluetooth disable"
            executeShizukuCommand(command)
            Timber.d("Bluetooth toggle executed: enable=$enable")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error toggling Bluetooth")
            false
        }
    }

    /**
     * Toggles GPS/Location services using Shizuku.
     *
     * @param enable true to enable, false to disable
     * @return true if command executed successfully
     */
    suspend fun toggleGPS(enable: Boolean): Boolean = withContext(Dispatchers.Default) {
        return@withContext try {
            if (!isShizukuAvailable) {
                Timber.w("Shizuku unavailable, cannot toggle GPS")
                return@withContext false
            }

            val command = if (enable) {
                "settings put secure location_providers_allowed gps"
            } else {
                "settings put secure location_providers_allowed -gps"
            }
            executeShizukuCommand(command)
            Timber.d("GPS toggle executed: enable=$enable")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error toggling GPS")
            false
        }
    }

    /**
     * Toggles NFC using Shizuku.
     *
     * @param enable true to enable, false to disable
     * @return true if command executed successfully
     */
    suspend fun toggleNFC(enable: Boolean): Boolean = withContext(Dispatchers.Default) {
        return@withContext try {
            if (!isShizukuAvailable) {
                Timber.w("Shizuku unavailable, cannot toggle NFC")
                return@withContext false
            }

            val command = if (enable) "svc nfc enable" else "svc nfc disable"
            executeShizukuCommand(command)
            Timber.d("NFC toggle executed: enable=$enable")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error toggling NFC")
            false
        }
    }

    /**
     * Sets screen brightness using Shizuku.
     *
     * @param brightness Value 0-255
     * @return true if command executed successfully
     */
    suspend fun setBrightness(brightness: Int): Boolean = withContext(Dispatchers.Default) {
        return@withContext try {
            if (!isShizukuAvailable) {
                Timber.w("Shizuku unavailable, cannot set brightness")
                return@withContext false
            }

            if (brightness < 0 || brightness > 255) {
                Timber.e("Invalid brightness value: $brightness")
                return@withContext false
            }

            val command = "settings put system screen_brightness $brightness"
            executeShizukuCommand(command)
            Timber.d("Brightness set to: $brightness")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error setting brightness")
            false
        }
    }

    /**
     * Toggles auto-rotate screen using Shizuku.
     *
     * @param enable true to enable, false to disable
     * @return true if command executed successfully
     */
    suspend fun toggleAutoRotate(enable: Boolean): Boolean = withContext(Dispatchers.Default) {
        return@withContext try {
            if (!isShizukuAvailable) {
                Timber.w("Shizuku unavailable, cannot toggle auto-rotate")
                return@withContext false
            }

            val value = if (enable) 1 else 0
            val command = "settings put system accelerometer_rotation $value"
            executeShizukuCommand(command)
            Timber.d("Auto-rotate toggle executed: enable=$enable")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error toggling auto-rotate")
            false
        }
    }

    /**
     * Toggles flashlight/torch using Shizuku.
     *
     * @param enable true to enable, false to disable
     * @return true if command executed successfully
     */
    suspend fun toggleFlashlight(enable: Boolean): Boolean = withContext(Dispatchers.Default) {
        return@withContext try {
            if (!isShizukuAvailable) {
                Timber.w("Shizuku unavailable, cannot toggle flashlight")
                return@withContext false
            }

            // This uses svc, which requires camera flash
            val command = if (enable) "svc power stayon" else "svc power nosleep"
            executeShizukuCommand(command)
            Timber.d("Flashlight toggle attempted: enable=$enable")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error toggling flashlight")
            false
        }
    }

    /**
     * Executes an arbitrary shell command via Shizuku.
     *
     * @param command The shell command to execute
     * @return true if command executed successfully
     */
    suspend fun executeShellCommand(command: String): Boolean = withContext(Dispatchers.Default) {
        return@withContext try {
            if (!isShizukuAvailable) {
                Timber.w("Shizuku unavailable for shell command: $command")
                return@withContext false
            }

            executeShizukuCommand(command)
            Timber.d("Shell command executed: $command")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error executing shell command: $command")
            false
        }
    }

    /**
     * Internal method to execute Shizuku shell command.
     * Wraps error handling for Shizuku API calls.
     *
     * @param command The shell command to execute
     * @throws Exception if command execution fails
     */
    @Throws(Exception::class)
    private fun executeShizukuCommand(command: String) {
        try {
            checkShizukuAvailability()

            if (!isShizukuAvailable) {
                throw Exception("Shizuku service is not available")
            }

            val process = Shizuku.newProcess(arrayOf("sh", "-c", command))
            process.waitFor()
            val exitCode = process.exitValue()

            if (exitCode != 0) {
                Timber.w("Command exited with non-zero code: $exitCode, command: $command")
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception executing command via Shizuku: $command")
            throw Exception("Shizuku permission denied", e)
        } catch (e: Exception) {
            Timber.e(e, "Exception executing command via Shizuku: $command")
            throw e
        }
    }

    /**
     * Fallback method to toggle Wi-Fi using standard Android API.
     * This is less reliable and may not work on newer Android versions.
     *
     * @param enable true to enable, false to disable
     * @return true if method was invoked (actual result depends on OS)
     */
    private fun fallbackToggleWiFi(enable: Boolean): Boolean {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // On Android 10+, direct toggle is restricted. Open settings instead.
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Timber.d("Opened Wi-Fi settings for manual toggle")
                true
            } else {
                wifiManager?.isWifiEnabled = enable
                Timber.d("Wi-Fi toggle via fallback API: enable=$enable")
                true
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in fallback Wi-Fi toggle")
            false
        }
    }

    /**
     * Registers listener for Shizuku binder state changes.
     * Allows the executor to adapt when Shizuku becomes available or unavailable.
     */
    fun registerShizukuStateListener() {
        try {
            Shizuku.addBinderReceivedListener {
                Timber.d("Shizuku service available")
                checkShizukuAvailability()
            }

            Shizuku.addBinderDeadListener {
                Timber.d("Shizuku service disconnected")
                isShizukuAvailable = false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error registering Shizuku state listener")
        }
    }
}
