package com.delexai.controller.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.delexai.controller.R
import com.delexai.controller.databinding.ActivityMainBinding
import com.delexai.controller.manager.ShizukuPermissionManager
import com.delexai.controller.manager.SystemPermissionManager
import com.delexai.controller.service.FloatingBubbleService
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main Dashboard Activity for Delex AI Controller.
 * Provides the Master Toggle switch to control the floating bubble service.
 * Manages Shizuku and system permission verification.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var shizukuPermissionManager: ShizukuPermissionManager
    private lateinit var systemPermissionManager: SystemPermissionManager
    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shizukuPermissionManager = ShizukuPermissionManager(this)
        systemPermissionManager = SystemPermissionManager(this)

        setupUI()
        checkPermissions()
    }

    /**
     * Initializes UI components and sets up listeners.
     */
    private fun setupUI() {
        // Master Toggle Switch
        binding.masterToggle.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                if (isChecked) {
                    startFloatingBubbleService()
                } else {
                    stopFloatingBubbleService()
                }
            }
        }

        // Shizuku Permission Button
        binding.shizukuPermissionButton.setOnClickListener {
            shizukuPermissionManager.requestShizukuPermission()
        }

        // Battery Optimization Bypass Button
        binding.batteryOptimizationButton.setOnClickListener {
            systemPermissionManager.requestBatteryOptimizationBypass()
        }

        // Update UI state
        updateUIState()
    }

    /**
     * Checks permissions and updates UI to show permission status.
     */
    private fun checkPermissions() {
        lifecycleScope.launch {
            // Check Shizuku
            val hasShizukuPermission = shizukuPermissionManager.isShizukuPermissionGranted()
            binding.shizukuPermissionButton.isEnabled = !hasShizukuPermission
            binding.shizukuStatusText.text = if (hasShizukuPermission) {
                "Shizuku: Authorized"
            } else {
                "Shizuku: Not Authorized"
            }

            // Check overlay permission
            val canDrawOverlays = systemPermissionManager.canDrawOverlays()
            binding.overlayStatusText.text = if (canDrawOverlays) {
                "Overlay: Granted"
            } else {
                "Overlay: Denied"
            }

            // Register Shizuku listeners for state changes
            shizukuPermissionManager.registerShizukuListeners(
                onBinderReceived = { updateUIState() },
                onBinderDead = { updateUIState() }
            )
        }
    }

    /**
     * Updates UI state based on service and permission status.
     */
    private fun updateUIState() {
        isServiceRunning = FloatingBubbleService.isServiceRunning
        binding.masterToggle.isChecked = isServiceRunning
        binding.statusText.text = if (isServiceRunning) {
            getString(R.string.service_enabled)
        } else {
            getString(R.string.service_disabled)
        }
    }

    /**
     * Starts the FloatingBubbleService.
     * Verifies necessary permissions before starting.
     */
    private fun startFloatingBubbleService() {
        try {
            if (!systemPermissionManager.canDrawOverlays()) {
                Timber.w("Overlay permission not granted, opening settings")
                systemPermissionManager.openDisplaySettings()
                return
            }

            val intent = Intent(this, FloatingBubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            isServiceRunning = true
            Timber.d("FloatingBubbleService started")
        } catch (e: Exception) {
            Timber.e(e, "Error starting FloatingBubbleService")
        }
    }

    /**
     * Stops the FloatingBubbleService.
     */
    private fun stopFloatingBubbleService() {
        try {
            val intent = Intent(this, FloatingBubbleService::class.java)
            stopService(intent)

            isServiceRunning = false
            Timber.d("FloatingBubbleService stopped")
        } catch (e: Exception) {
            Timber.e(e, "Error stopping FloatingBubbleService")
        }
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
        checkPermissions()
    }
}
