package com.delexai.controller.executor

import android.view.accessibility.AccessibilityNodeInfo
import timber.log.Timber

/**
 * Wrapper around AccessibilityActionService for convenient gesture automation.
 * Provides high-level convenience methods for common automation tasks.
 *
 * PHASE 5 - UI Automation Wrapper
 */
class AccessibilityExecutor {

    private var accessibilityService: com.delexai.controller.service.AccessibilityActionService? = null

    /**
     * Sets the accessibility service reference.
     * Must be called before using any automation methods.
     *
     * @param service The AccessibilityActionService instance
     */
    fun setAccessibilityService(service: com.delexai.controller.service.AccessibilityActionService) {
        this.accessibilityService = service
        Timber.d("AccessibilityExecutor initialized with service")
    }

    /**
     * Finds a UI node by text content.
     *
     * @param text The text to search for
     * @return The AccessibilityNodeInfo or null if not found
     */
    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        return try {
            val service = accessibilityService
            if (service != null) {
                val nodes = service.findNodesByText(text)
                if (nodes.isNotEmpty()) {
                    Timber.d("Found node with text: $text")
                    nodes[0]
                } else {
                    Timber.w("No node found with text: $text")
                    null
                }
            } else {
                Timber.e("AccessibilityService not initialized")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error finding node by text: $text")
            null
        }
    }

    /**
     * Finds a UI node by resource ID.
     *
     * @param resourceId The resource ID to search for
     * @return The AccessibilityNodeInfo or null if not found
     */
    fun findNodeById(resourceId: String): AccessibilityNodeInfo? {
        return try {
            val service = accessibilityService
            if (service != null) {
                val node = service.findNodeById(resourceId)
                if (node != null) {
                    Timber.d("Found node with ID: $resourceId")
                }
                node
            } else {
                Timber.e("AccessibilityService not initialized")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error finding node by ID: $resourceId")
            null
        }
    }

    /**
     * Clicks an accessibility node.
     *
     * @param node The node to click
     * @return true if click was successful
     */
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return try {
            val service = accessibilityService
            if (service != null) {
                service.clickNode(node)
            } else {
                Timber.e("AccessibilityService not initialized")
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
     * @return true if typing was successful
     */
    fun typeText(text: String): Boolean {
        return try {
            val service = accessibilityService
            if (service != null) {
                service.typeText(text)
            } else {
                Timber.e("AccessibilityService not initialized")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error typing text")
            false
        }
    }

    /**
     * Performs a swipe up gesture (for scrolling or navigation).
     *
     * @param duration Duration of swipe in milliseconds
     * @return true if swipe was successful
     */
    fun swipeUp(duration: Long = 500): Boolean {
        return try {
            val service = accessibilityService
            if (service != null) {
                service.swipeUp(duration = duration)
            } else {
                Timber.e("AccessibilityService not initialized")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error swiping up")
            false
        }
    }

    /**
     * Performs a swipe down gesture.
     *
     * @param duration Duration of swipe in milliseconds
     * @return true if swipe was successful
     */
    fun swipeDown(duration: Long = 500): Boolean {
        return try {
            val service = accessibilityService
            if (service != null) {
                service.swipeDown(duration = duration)
            } else {
                Timber.e("AccessibilityService not initialized")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error swiping down")
            false
        }
    }

    /**
     * Performs a swipe left gesture.
     *
     * @param duration Duration of swipe in milliseconds
     * @return true if swipe was successful
     */
    fun swipeLeft(duration: Long = 500): Boolean {
        return try {
            val service = accessibilityService
            if (service != null) {
                service.swipeLeft(duration = duration)
            } else {
                Timber.e("AccessibilityService not initialized")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error swiping left")
            false
        }
    }

    /**
     * Performs a swipe right gesture.
     *
     * @param duration Duration of swipe in milliseconds
     * @return true if swipe was successful
     */
    fun swipeRight(duration: Long = 500): Boolean {
        return try {
            val service = accessibilityService
            if (service != null) {
                service.swipeRight(duration = duration)
            } else {
                Timber.e("AccessibilityService not initialized")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error swiping right")
            false
        }
    }

    /**
     * Clicks at specific screen coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if click was successful
     */
    fun clickAtCoordinates(x: Float, y: Float): Boolean {
        return try {
            val service = accessibilityService
            if (service != null) {
                service.clickAtCoordinates(x, y)
            } else {
                Timber.e("AccessibilityService not initialized")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error clicking at coordinates")
            false
        }
    }

    /**
     * Presses the back button.
     *
     * @return true if back was pressed
     */
    fun pressBack(): Boolean {
        return try {
            val service = accessibilityService
            if (service != null) {
                service.pressBack()
            } else {
                Timber.e("AccessibilityService not initialized")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error pressing back")
            false
        }
    }

    /**
     * Presses the home button.
     *
     * @return true if home was pressed
     */
    fun pressHome(): Boolean {
        return try {
            val service = accessibilityService
            if (service != null) {
                service.pressHome()
            } else {
                Timber.e("AccessibilityService not initialized")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error pressing home")
            false
        }
    }

    /**
     * Opens the recent apps menu.
     *
     * @return true if recents were opened
     */
    fun openRecents(): Boolean {
        return try {
            val service = accessibilityService
            if (service != null) {
                service.openRecents()
            } else {
                Timber.e("AccessibilityService not initialized")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error opening recents")
            false
        }
    }
}
