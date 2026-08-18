package com.delexai.controller.screen

import android.content.Context
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.view.Display
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import timber.log.Timber

/**
 * Manages screen capture using MediaProjection.
 * Provides real-time frame access for visual analysis.
 *
 * PHASE 6 IMPLEMENTATION
 */
class ScreenCaptureManager(private val context: Context) {

    private val mediaProjectionManager = context.getSystemService(
        Context.MEDIA_PROJECTION_SERVICE
    ) as MediaProjectionManager

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var isCapturing = false

    private var onFrameAvailable: ((Bitmap) -> Unit)? = null

    companion object {
        private const val PROJECTION_CODE = 42
    }

    /**
     * Initializes the screen capture manager.
     * Must be called before using capture functionality.
     *
     * @param mediaProjection The MediaProjection object from activity result
     * @return true if initialization successful
     */
    fun initialize(mediaProjection: MediaProjection): Boolean {
        return try {
            this.mediaProjection = mediaProjection
            setupCaptureThread()
            setupImageReader()
            Timber.d("ScreenCaptureManager initialized successfully")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error initializing ScreenCaptureManager")
            false
        }
    }

    /**
     * Sets up the background thread for frame capture.
     */
    private fun setupCaptureThread() {
        captureThread = HandlerThread("ScreenCaptureThread").apply {
            start()
            captureHandler = Handler(looper)
        }
    }

    /**
     * Creates the ImageReader for frame access.
     */
    private fun setupImageReader() {
        try {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

            if (defaultDisplay != null) {
                val width = defaultDisplay.width
                val height = defaultDisplay.height

                imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2).apply {
                    setOnImageAvailableListener({ reader ->
                        try {
                            val image = reader.acquireLatestImage()
                            if (image != null) {
                                val bitmap = imageToBitmap(image)
                                onFrameAvailable?.invoke(bitmap)
                                image.close()
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error processing frame")
                        }
                    }, captureHandler)
                }

                // Start virtual display for screen capture
                mediaProjection?.createVirtualDisplay(
                    "DelexScreenCapture",
                    width,
                    height,
                    defaultDisplay.refreshRate.toInt(),
                    android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    imageReader!!.surface,
                    null,
                    captureHandler
                )

                Timber.d("ImageReader created: $width x $height")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting up ImageReader")
        }
    }

    /**
     * Converts an Image to a Bitmap for processing.
     *
     * @param image The Image to convert
     * @return Bitmap representation of the image
     */
    private fun imageToBitmap(image: android.media.Image): Bitmap {
        val planes = image.planes
        val pixelStride = planes[0].pixelStride

        val buffer = planes[0].buffer.apply {
            rewind()
        }

        val pixels = IntArray(buffer.remaining() / pixelStride)
        buffer.asIntBuffer().get(pixels)

        return Bitmap.createBitmap(pixels, image.width, image.height, Bitmap.Config.ARGB_8888)
    }

    /**
     * Starts screen capture.
     *
     * @return true if capture started successfully
     */
    fun startCapture(): Boolean {
        return try {
            if (mediaProjection != null && imageReader != null) {
                isCapturing = true
                Timber.d("Screen capture started")
                true
            } else {
                Timber.w("MediaProjection or ImageReader not initialized")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error starting screen capture")
            false
        }
    }

    /**
     * Stops screen capture.
     */
    fun stopCapture() {
        try {
            isCapturing = false
            Timber.d("Screen capture stopped")
        } catch (e: Exception) {
            Timber.e(e, "Error stopping screen capture")
        }
    }

    /**
     * Registers callback for frame availability.
     *
     * @param callback Invoked with each captured frame
     */
    fun setOnFrameAvailable(callback: (Bitmap) -> Unit) {
        onFrameAvailable = callback
    }

    /**
     * Checks if capture is currently active.
     *
     * @return true if capturing
     */
    fun isCapturing(): Boolean = isCapturing

    /**
     * Cleans up resources.
     */
    fun destroy() {
        try {
            stopCapture()
            mediaProjection?.stop()
            imageReader?.close()
            captureThread?.quit()
            serviceScope.cancel()
            Timber.d("ScreenCaptureManager destroyed")
        } catch (e: Exception) {
            Timber.e(e, "Error destroying ScreenCaptureManager")
        }
    }
}
