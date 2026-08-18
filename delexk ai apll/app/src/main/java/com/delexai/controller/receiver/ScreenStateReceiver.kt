package com.delexai.controller.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Broadcast receiver for detecting screen state changes.
 * Manages the floating bubble service based on screen on/off events.
 */
class ScreenStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            if (context == null || intent == null) return

            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Timber.d("Screen turned on")
                    onScreenOn(context)
                }

                Intent.ACTION_SCREEN_OFF -> {
                    Timber.d("Screen turned off")
                    onScreenOff(context)
                }

                else -> Timber.d("Unknown action: ${intent.action}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in ScreenStateReceiver")
        }
    }

    /**
     * Called when the screen is turned on.
     * Restarts listening or other background tasks if needed.
     *
     * @param context Application context
     */
    private fun onScreenOn(context: Context) {
        try {
            // Resume voice listening if bubble service is active
            Timber.d("Resuming services on screen on")
        } catch (e: Exception) {
            Timber.e(e, "Error on screen on event")
        }
    }

    /**
     * Called when the screen is turned off.
     * May pause heavy operations like screen monitoring.
     *
     * @param context Application context
     */
    private fun onScreenOff(context: Context) {
        try {
            // Pause screen monitoring to save battery
            Timber.d("Pausing screen monitoring on screen off")
        } catch (e: Exception) {
            Timber.e(e, "Error on screen off event")
        }
    }
}
