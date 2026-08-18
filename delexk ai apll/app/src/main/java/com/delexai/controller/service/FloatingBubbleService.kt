package com.delexai.controller.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.delexai.controller.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import timber.log.Timber

/**
 * Foreground Service that manages the floating bubble UI overlay.
 * Keeps the service alive using a persistent notification.
 * Handles touch events and dragging of the bubble.
 */
class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: FrameLayout
    private lateinit var bubbleImageView: ImageView
    private var bubbleParams: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var isListening = false
    private var isGlowing = false

    // Touch tracking for dragging
    private var lastX = 0f
    private var lastY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    companion object {
        var isServiceRunning = false
            private set

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "delex_ai_channel"
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("FloatingBubbleService created")
        isServiceRunning = true

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            createBubbleUI()
            registerServiceListeners()
            Timber.d("FloatingBubbleService started with bubble UI")
        } catch (e: Exception) {
            Timber.e(e, "Error starting FloatingBubbleService")
            stopSelf()
        }
        return START_STICKY
    }

    /**
     * Creates the floating bubble UI and adds it to the window manager.
     */
    private fun createBubbleUI() {
        try {
            // Create container
            bubbleView = FrameLayout(this).apply {
                setBackgroundColor(0)
                tag = "bubble_container"
            }

            // Create bubble image
            bubbleImageView = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_dialog_info) // Placeholder - replace with actual bubble icon
                setBackgroundResource(android.R.drawable.ic_input_delete) // Will be styled with color
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setColorFilter(resources.getColor(R.color.bubble_blue, null))
            }

            // Add image to container
            bubbleView.addView(
                bubbleImageView,
                FrameLayout.LayoutParams(
                    100, 100,
                    Gravity.CENTER
                )
            )

            // Create layout parameters for overlay window
            bubbleParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                format = PixelFormat.TRANSLUCENT
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_RECEIVE_TOUCH_EVENTS

                width = 120
                height = 120
                x = 0
                y = 0
                gravity = Gravity.TOP or Gravity.START
            }

            // Add touch listener for dragging
            bubbleView.setOnTouchListener { _, event ->
                handleBubbleTouch(event)
            }

            // Add to window manager
            windowManager.addView(bubbleView, bubbleParams)
            Timber.d("Bubble UI created and added to window manager")

        } catch (e: Exception) {
            Timber.e(e, "Error creating bubble UI")
            throw e
        }
    }

    /**
     * Handles touch events on the bubble for dragging and interaction.
     *
     * @param event The MotionEvent
     * @return true if event was handled
     */
    private fun handleBubbleTouch(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = bubbleParams?.x?.toFloat() ?: 0f
                lastY = bubbleParams?.y?.toFloat() ?: 0f
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                Timber.d("Bubble touch down at (${event.rawX}, ${event.rawY})")
                true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.rawX - lastTouchX).toInt()
                val deltaY = (event.rawY - lastTouchY).toInt()

                bubbleParams?.apply {
                    x = (lastX + deltaX).toInt()
                    y = (lastY + deltaY).toInt()

                    // Clamp to screen bounds
                    val displayMetrics = resources.displayMetrics
                    x = x.coerceIn(0, displayMetrics.widthPixels - width)
                    y = y.coerceIn(0, displayMetrics.heightPixels - height)
                }

                try {
                    windowManager.updateViewLayout(bubbleView, bubbleParams)
                } catch (e: Exception) {
                    Timber.e(e, "Error updating bubble position")
                }
                true
            }

            MotionEvent.ACTION_UP -> {
                // Toggle listening state on tap
                toggleListeningState()
                Timber.d("Bubble tapped - toggling listening state")
                true
            }

            else -> false
        }
    }

    /**
     * Toggles the listening/active state of the bubble.
     * Updates the visual appearance (glow effect).
     */
    private fun toggleListeningState() {
        isListening = !isListening
        updateBubbleAppearance()

        if (isListening) {
            Timber.d("Bubble now listening")
            startListening()
        } else {
            Timber.d("Bubble listening disabled")
            stopListening()
        }
    }

    /**
     * Updates the bubble's visual appearance based on listening state.
     */
    private fun updateBubbleAppearance() {
        try {
            if (isListening) {
                bubbleImageView.setColorFilter(
                    resources.getColor(R.color.bubble_glow, null)
                )
                isGlowing = true
            } else {
                bubbleImageView.setColorFilter(
                    resources.getColor(R.color.bubble_blue, null)
                )
                isGlowing = false
            }
            Timber.d("Bubble appearance updated - glowing: $isGlowing")
        } catch (e: Exception) {
            Timber.e(e, "Error updating bubble appearance")
        }
    }

    /**
     * Starts the voice listening / wake-word detection.
     * (Implementation will be added in Phase 4)
     */
    private fun startListening() {
        try {
            Timber.d("Starting voice listening")
            // This will be implemented in Phase 4 with SpeechRecognizer integration
        } catch (e: Exception) {
            Timber.e(e, "Error starting listening")
        }
    }

    /**
     * Stops voice listening.
     */
    private fun stopListening() {
        try {
            Timber.d("Stopping voice listening")
            // This will be implemented in Phase 4
        } catch (e: Exception) {
            Timber.e(e, "Error stopping listening")
        }
    }

    /**
     * Registers listeners for system events (screen on/off, etc.)
     */
    private fun registerServiceListeners() {
        try {
            // Screen state listener will be added via receiver
            Timber.d("Service listeners registered")
        } catch (e: Exception) {
            Timber.e(e, "Error registering service listeners")
        }
    }

    /**
     * Creates the persistent notification for the foreground service.
     *
     * @return Notification for foreground service
     */
    private fun createNotification(): Notification {
        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel(CHANNEL_ID, "Delex AI Controller", importance)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Delex AI Controller")
            .setContentText("Floating bubble active - Click to listen")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * Creates notification channel for foreground service.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Delex AI Controller",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Timber.d("Notification channel created")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("FloatingBubbleService destroyed")
        isServiceRunning = false

        try {
            if (::bubbleView.isInitialized) {
                windowManager.removeView(bubbleView)
            }
            stopListening()
            serviceScope.cancel()
        } catch (e: Exception) {
            Timber.e(e, "Error during service cleanup")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
