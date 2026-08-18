package com.delexai.controller.executor

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.delexai.controller.nlp.IntentParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Routes parsed commands to appropriate executors (Accessibility, Shizuku, etc.)
 * Implements the core orchestration logic between NLP and system automation.
 *
 * PHASE 5 FINAL - Command Orchestration
 */
class CommandRouter(
    private val context: Context,
    private val systemCommandExecutor: SystemCommandExecutor,
    private val accessibilityExecutor: AccessibilityExecutor
) {

    private val intentParser = IntentParser(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Routes a recognized command to appropriate executor.
     *
     * @param command The recognized voice command or text input
     */
    fun routeCommand(command: String) {
        scope.launch {
            try {
                Timber.d("Routing command: $command")

                val intent = intentParser.parseCommand(command)
                Timber.d("Parsed intent - Action: ${intent.action}, Target: ${intent.target}")

                when (intent.action) {
                    "launch_app" -> handleAppLaunch(intent)
                    "toggle_setting" -> handleSettingToggle(intent)
                    "ui_interaction" -> handleUIInteraction(intent)
                    "type_text" -> handleTextInput(intent)
                    "navigate" -> handleNavigation(intent)
                    else -> Timber.w("Unknown action: ${intent.action}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error routing command: $command")
            }
        }
    }

    /**
     * Handles app launch commands.
     *
     * @param intent The parsed intent
     */
    private suspend fun handleAppLaunch(intent: IntentParser.ParsedIntent) {
        try {
            val packageName = intent.target
            Timber.d("Launching app: $packageName")

            val success = intentParser.launchApp(packageName)
            if (!success) {
                // Try to launch by app name if package name didn't work
                val resolvedPackage = intentParser.resolveAppPackage(intent.target)
                if (resolvedPackage != null) {
                    intentParser.launchApp(resolvedPackage)
                } else {
                    Timber.w("Could not resolve app package for: ${intent.target}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error launching app")
        }
    }

    /**
     * Handles system setting toggle commands (Wi-Fi, Bluetooth, etc.)
     *
     * @param intent The parsed intent
     */
    private suspend fun handleSettingToggle(intent: IntentParser.ParsedIntent) {
        try {
            val setting = intent.target
            val enable = intent.parameters["enable"]?.toBoolean() ?: true

            Timber.d("Toggling setting: $setting (enable=$enable)")

            when (setting) {
                "wifi" -> systemCommandExecutor.toggleWiFi(enable)
                "bluetooth" -> systemCommandExecutor.toggleBluetooth(enable)
                "gps", "location" -> systemCommandExecutor.toggleGPS(enable)
                "nfc" -> systemCommandExecutor.toggleNFC(enable)
                "brightness" -> {
                    val brightness = intent.parameters["value"]?.toIntOrNull() ?: 128
                    systemCommandExecutor.setBrightness(brightness)
                }
                "auto_rotate" -> systemCommandExecutor.toggleAutoRotate(enable)
                "flashlight" -> systemCommandExecutor.toggleFlashlight(enable)
                else -> Timber.w("Unknown setting: $setting")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error toggling setting")
        }
    }

    /**
     * Handles UI interaction commands (click, tap, etc.)
     *
     * @param intent The parsed intent
     */
    private suspend fun handleUIInteraction(intent: IntentParser.ParsedIntent) {
        try {
            val target = intent.target
            Timber.d("Handling UI interaction: $target")

            // Find and click the target element
            val nodeInfo = accessibilityExecutor.findNodeByText(target)
            if (nodeInfo != null) {
                accessibilityExecutor.clickNode(nodeInfo)
                Timber.d("Clicked node: $target")
            } else {
                Timber.w("Could not find UI element: $target")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error handling UI interaction")
        }
    }

    /**
     * Handles text input commands.
     *
     * @param intent The parsed intent
     */
    private suspend fun handleTextInput(intent: IntentParser.ParsedIntent) {
        try {
            val textToType = intent.target
            Timber.d("Typing text: $textToType")

            accessibilityExecutor.typeText(textToType)
        } catch (e: Exception) {
            Timber.e(e, "Error typing text")
        }
    }

    /**
     * Handles navigation commands (open maps, search location, etc.)
     *
     * @param intent The parsed intent
     */
    private suspend fun handleNavigation(intent: IntentParser.ParsedIntent) {
        try {
            val location = intent.target
            val app = intent.parameters["app"] ?: "maps"

            Timber.d("Navigating to: $location (app=$app)")

            when (app) {
                "maps" -> {
                    val uri = Uri.parse("geo:0,0?q=$location")
                    val mapsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                    if (mapsIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(mapsIntent)
                    } else {
                        Timber.w("Google Maps not available")
                    }
                }
                else -> Timber.w("Unknown navigation app: $app")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error handling navigation")
        }
    }

    /**
     * Destroys the router and releases resources.
     */
    fun destroy() {
        try {
            scope.cancel()
            Timber.d("CommandRouter destroyed")
        } catch (e: Exception) {
            Timber.e(e, "Error destroying CommandRouter")
        }
    }
}
