package com.delexai.controller

import android.app.Application
import timber.log.Timber

/**
 * Global Application class for Delex AI Controller.
 * Initializes logging and global exception handling.
 */
class DelexAIApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Set up global uncaught exception handler
        setupGlobalExceptionHandler()
    }

    /**
     * Configures a custom Thread.UncaughtExceptionHandler to prevent crashes
     * from terminating the application. Background services are silently restarted.
     */
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                Timber.e(exception, "Uncaught exception in thread: ${thread.name}")

                // Attempt to restart the FloatingBubbleService
                try {
                    val intent = android.content.Intent(this, com.delexai.controller.service.FloatingBubbleService::class.java)
                    startService(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to restart FloatingBubbleService")
                }

                // Log the error without crashing
            } catch (e: Exception) {
                Timber.e(e, "Error in global exception handler")
            }

            // Call default handler to allow system to log the exception
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
}
