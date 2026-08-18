package com.delexai.controller.nlp

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Parses natural language commands and routes them to appropriate executors.
 * Provides dynamic command matching and intent extraction.
 *
 * PHASE 4 CONTINUATION - Intent Router
 */
class IntentParser(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Data class representing a parsed command.
     */
    data class ParsedIntent(
        val action: String,
        val target: String = "",
        val parameters: Map<String, String> = emptyMap(),
        val confidence: Float = 1.0f
    )

    /**
     * Parses a command string and extracts actionable intent.
     *
     * @param command The spoken or written command
     * @return ParsedIntent with action, target, and parameters
     */
    fun parseCommand(command: String): ParsedIntent {
        return try {
            val lowerCommand = command.lowercase().trim()

            // App launching patterns
            if (lowerCommand.contains("open") || lowerCommand.contains("launch")) {
                return parseAppLaunch(command)
            }

            // Settings control patterns
            if (lowerCommand.contains("turn") || lowerCommand.contains("toggle")) {
                return parseSettingsControl(command)
            }

            // UI interaction patterns
            if (lowerCommand.contains("click") || lowerCommand.contains("tap")) {
                return parseUIInteraction(command)
            }

            // Text input patterns
            if (lowerCommand.contains("type") || lowerCommand.contains("send") || lowerCommand.contains("message")) {
                return parseTextInput(command)
            }

            // Navigate patterns
            if (lowerCommand.contains("navigate") || lowerCommand.contains("go to")) {
                return parseNavigation(command)
            }

            // Default fallback
            ParsedIntent(
                action = "unknown",
                target = command,
                confidence = 0.5f
            )
        } catch (e: Exception) {
            Timber.e(e, "Error parsing command: $command")
            ParsedIntent(action = "error", target = command, confidence = 0.0f)
        }
    }

    /**
     * Parses app launch commands (e.g., "Open WhatsApp").
     *
     * @param command The command string
     * @return ParsedIntent with action and target app
     */
    private fun parseAppLaunch(command: String): ParsedIntent {
        val appNames = listOf(
            "whatsapp" to "com.whatsapp",
            "facebook" to "com.facebook.katana",
            "instagram" to "com.instagram.android",
            "tiktok" to "com.zhiliaoapp.musically",
            "youtube" to "com.google.android.youtube",
            "gmail" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "telegram" to "org.telegram.messenger",
            "discord" to "com.discord"
        )

        val lowerCommand = command.lowercase()
        for ((name, packageName) in appNames) {
            if (lowerCommand.contains(name)) {
                return ParsedIntent(
                    action = "launch_app",
                    target = packageName,
                    parameters = mapOf("app_name" to name),
                    confidence = 0.95f
                )
            }
        }

        // Generic app launch
        return ParsedIntent(
            action = "launch_app",
            target = command.replace("open", "").replace("launch", "").trim(),
            confidence = 0.7f
        )
    }

    /**
     * Parses settings control commands (e.g., "Turn on Wi-Fi").
     *
     * @param command The command string
     * @return ParsedIntent with action and setting type
     */
    private fun parseSettingsControl(command: String): ParsedIntent {
        val lowerCommand = command.lowercase()

        val settings = mapOf(
            "wifi" to "wifi",
            "bluetooth" to "bluetooth",
            "gps" to "gps",
            "location" to "gps",
            "airplane" to "airplane_mode",
            "nfc" to "nfc",
            "brightness" to "brightness",
            "volume" to "volume",
            "auto-rotate" to "auto_rotate",
            "flashlight" to "flashlight",
            "hotspot" to "hotspot"
        )

        val enable = lowerCommand.contains("on") || lowerCommand.contains("turn on") ||
                     lowerCommand.contains("enable") || lowerCommand.contains("start")

        for ((keyword, setting) in settings) {
            if (lowerCommand.contains(keyword)) {
                return ParsedIntent(
                    action = "toggle_setting",
                    target = setting,
                    parameters = mapOf("enable" to enable.toString()),
                    confidence = 0.9f
                )
            }
        }

        return ParsedIntent(action = "unknown", confidence = 0.3f)
    }

    /**
     * Parses UI interaction commands (e.g., "Click the send button").
     *
     * @param command The command string
     * @return ParsedIntent with action and target element
     */
    private fun parseUIInteraction(command: String): ParsedIntent {
        return ParsedIntent(
            action = "ui_interaction",
            target = command.replace("click", "").replace("tap", "").trim(),
            confidence = 0.8f
        )
    }

    /**
     * Parses text input commands (e.g., "Type hello world").
     *
     * @param command The command string
     * @return ParsedIntent with action and text to type
     */
    private fun parseTextInput(command: String): ParsedIntent {
        val textToType = command
            .replace("type", "", ignoreCase = true)
            .replace("send", "", ignoreCase = true)
            .replace("message", "", ignoreCase = true)
            .replace("say", "", ignoreCase = true)
            .trim()

        return ParsedIntent(
            action = "type_text",
            target = textToType,
            confidence = 0.85f
        )
    }

    /**
     * Parses navigation commands (e.g., "Navigate to restaurant").
     *
     * @param command The command string
     * @return ParsedIntent with action and location
     */
    private fun parseNavigation(command: String): ParsedIntent {
        val location = command
            .replace("navigate to", "", ignoreCase = true)
            .replace("go to", "", ignoreCase = true)
            .trim()

        return ParsedIntent(
            action = "navigate",
            target = location,
            parameters = mapOf("app" to "maps"),
            confidence = 0.85f
        )
    }

    /**
     * Extracts app package name from an app name string.
     * Uses fuzzy matching and common app database.
     *
     * @param appName The app name to resolve
     * @return Package name or null if not found
     */
    fun resolveAppPackage(appName: String): String? {
        return try {
            val appDatabase = mapOf(
                "whatsapp" to "com.whatsapp",
                "facebook" to "com.facebook.katana",
                "instagram" to "com.instagram.android",
                "tiktok" to "com.zhiliaoapp.musically",
                "youtube" to "com.google.android.youtube",
                "gmail" to "com.google.android.gm",
                "google maps" to "com.google.android.apps.maps",
                "maps" to "com.google.android.apps.maps",
                "chrome" to "com.android.chrome",
                "telegram" to "org.telegram.messenger",
                "discord" to "com.discord",
                "twitter" to "com.twitter.android",
                "snapchat" to "com.snapchat.android",
                "spotify" to "com.spotify.music",
                "camera" to "com.android.camera2"
            )

            val lowerName = appName.lowercase().trim()
            appDatabase[lowerName] ?: appDatabase.values.find {
                appName.lowercase().contains(it.substringAfterLast("."))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error resolving app package for: $appName")
            null
        }
    }

    /**
     * Launches an app by package name.
     *
     * @param packageName The package name to launch
     * @return true if launch was successful
     */
    fun launchApp(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                Timber.d("App launched: $packageName")
                true
            } else {
                Timber.w("Launch intent not found for package: $packageName")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error launching app: $packageName")
            false
        }
    }
}
