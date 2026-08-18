package com.delexai.controller.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import timber.log.Timber

/**
 * Accessibility Service for UI automation and gesture control.
 * Provides methods for finding UI nodes, simulating touches, swipes, and typing.
 * Integrates with the system accessibility framework to interact with any app.
 */
class AccessibilityActionService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Timber.d("AccessibilityActionService created")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return

            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    Timber.d("Window state changed: ${event.className}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error handling accessibility event")
        }
    }

    override fun onInterrupt() {
        Timber.d("Accessibility service interrupted")
    }

    /**
     * Finds AccessibilityNodeInfo objects by text content.
     *
     * @param text The text to search for
     * @return List of matching AccessibilityNodeInfo objects
     */
    fun findNodesByText(text: String): List<AccessibilityNodeInfo> {
        return try {
            val nodes = mutableListOf<AccessibilityNodeInfo>()
            val rootNode = rootInActiveWindow

            if (rootNode != null) {
                findNodesByTextRecursive(rootNode, text, nodes)
                Timber.d("Found ${nodes.size} nodes with text: $text")
            } else {
                Timber.w("Root node is null")
            }

            nodes
        } catch (e: Exception) {
            Timber.e(e, "Error finding nodes by text: $text")
            emptyList()
        }
    }

    /**
     * Recursively searches for nodes matching text.
     *
     * @param node The current node to search
     * @param text The text to match
     * @param results List to accumulate results
     */
    private fun findNodesByTextRecursive(
        node: AccessibilityNodeInfo,
        text: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        try {
            if (node.text?.contains(text, ignoreCase = true) == true) {
                results.add(node)
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    findNodesByTextRecursive(child, text, results)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in recursive node search")
        }
    }

    /**
     * Finds a node by its resource ID.
     *
     * @param resourceId The resource ID to search for
     * @return The matching AccessibilityNodeInfo or null
     */
    fun findNodeById(resourceId: String): AccessibilityNodeInfo? {
        return try {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                findNodeByIdRecursive(rootNode, resourceId)
            } else {
                Timber.w("Root node is null for ID search")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error finding node by ID: $resourceId")
            null
        }
    }

    /**
     * Recursively searches for a node by resource ID.
     *
     * @param node The current node
     * @param resourceId The resource ID to match
     * @return The matching node or null
     */
    private fun findNodeByIdRecursive(node: AccessibilityNodeInfo, resourceId: String): AccessibilityNodeInfo? {
        try {
            if (node.viewIdResourceName == resourceId) {
                return node
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    val result = findNodeByIdRecursive(child, resourceId)
                    if (result != null) {
                        return result
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in recursive ID search")
        }

        return null
    }

    /**
     * Clicks an AccessibilityNodeInfo.
     *
     * @param node The node to click
     * @return true if click was performed
     */
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return try {
            if (node.isClickable) {
                val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (result) {
                    Timber.d("Node clicked successfully: ${node.text}")
                } else {
                    Timber.w("Failed to click node: ${node.text}")
                }
                result
            } else {
                Timber.w("Node is not clickable: ${node.text}")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error clicking node")
            false
        }
    }

    /**
     * Types text into a focused input field.
     *
     * @param text The text to type
     * @return true if typing was performed
     */
    fun typeText(text: String): Boolean {
        return try {
            val bundle = android.os.Bundle()
            bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val result = rootInActiveWindow?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle) ?: false
            if (result) {
                Timber.d("Text typed successfully: $text")
            } else {
                Timber.w("Failed to type text: $text")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error typing text")
            false
        }
    }

    /**
     * Performs a swipe gesture from one point to another.
     *
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param endX Ending X coordinate
     * @param endY Ending Y coordinate
     * @param duration Duration of swipe in milliseconds
     * @return true if swipe was performed
     */
    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 500): Boolean {
        return try {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }

            val gesture = GestureDescription.Builder().apply {
                addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            }.build()

            val result = dispatchGesture(gesture, null, null)
            if (result) {
                Timber.d("Swipe performed: ($startX,$startY) -> ($endX,$endY)")
            } else {
                Timber.w("Failed to perform swipe")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error performing swipe")
            false
        }
    }

    /**
     * Performs a swipe up gesture (useful for TikTok).
     *
     * @param x X coordinate to swipe at
     * @param duration Duration of swipe in milliseconds
     * @return true if swipe was performed
     */
    fun swipeUp(x: Float = 500f, duration: Long = 500): Boolean {
        val displayMetrics = resources.displayMetrics
        return swipe(x, displayMetrics.heightPixels - 100f, x, 100f, duration)
    }

    /**
     * Performs a swipe down gesture.
     *
     * @param x X coordinate to swipe at
     * @param duration Duration of swipe in milliseconds
     * @return true if swipe was performed
     */
    fun swipeDown(x: Float = 500f, duration: Long = 500): Boolean {
        val displayMetrics = resources.displayMetrics
        return swipe(x, 100f, x, displayMetrics.heightPixels - 100f, duration)
    }

    /**
     * Performs a swipe left gesture.
     *
     * @param y Y coordinate to swipe at
     * @param duration Duration of swipe in milliseconds
     * @return true if swipe was performed
     */
    fun swipeLeft(y: Float = 500f, duration: Long = 500): Boolean {
        val displayMetrics = resources.displayMetrics
        return swipe(displayMetrics.widthPixels - 100f, y, 100f, y, duration)
    }

    /**
     * Performs a swipe right gesture.
     *
     * @param y Y coordinate to swipe at
     * @param duration Duration of swipe in milliseconds
     * @return true if swipe was performed
     */
    fun swipeRight(y: Float = 500f, duration: Long = 500): Boolean {
        val displayMetrics = resources.displayMetrics
        return swipe(100f, y, displayMetrics.widthPixels - 100f, y, duration)
    }

    /**
     * Simulates a click at specific coordinates using gestures.
     *
     * @param x X coordinate to click
     * @param y Y coordinate to click
     * @return true if click was performed
     */
    fun clickAtCoordinates(x: Float, y: Float): Boolean {
        return try {
            val path = Path().apply {
                moveTo(x, y)
            }

            val gesture = GestureDescription.Builder().apply {
                addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            }.build()

            val result = dispatchGesture(gesture, null, null)
            if (result) {
                Timber.d("Click performed at ($x, $y)")
            } else {
                Timber.w("Failed to click at ($x, $y)")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error clicking at coordinates")
            false
        }
    }

    /**
     * Presses the back button.
     *
     * @return true if action was performed
     */
    fun pressBack(): Boolean {
        return try {
            val result = performGlobalAction(GLOBAL_ACTION_BACK)
            if (result) {
                Timber.d("Back button pressed")
            } else {
                Timber.w("Failed to press back button")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error pressing back button")
            false
        }
    }

    /**
     * Presses the home button.
     *
     * @return true if action was performed
     */
    fun pressHome(): Boolean {
        return try {
            val result = performGlobalAction(GLOBAL_ACTION_HOME)
            if (result) {
                Timber.d("Home button pressed")
            } else {
                Timber.w("Failed to press home button")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error pressing home button")
            false
        }
    }

    /**
     * Opens the recent apps menu.
     *
     * @return true if action was performed
     */
    fun openRecents(): Boolean {
        return try {
            val result = performGlobalAction(GLOBAL_ACTION_RECENTS)
            if (result) {
                Timber.d("Recents opened")
            } else {
                Timber.w("Failed to open recents")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error opening recents")
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("AccessibilityActionService destroyed")
        serviceScope.cancel()
    }
}
