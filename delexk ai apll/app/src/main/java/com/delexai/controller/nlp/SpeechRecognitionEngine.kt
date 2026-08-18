package com.delexai.controller.nlp

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import timber.log.Timber

/**
 * Handles speech recognition and wake-word detection.
 * Uses Android's native SpeechRecognizer for English language processing.
 * Transitions from background wake-word listening to active intent parsing.
 *
 * PHASE 4 IMPLEMENTATION
 */
class SpeechRecognitionEngine(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private var onWakeWordDetected: ((String) -> Unit)? = null
    private var onCommandRecognized: ((String) -> Unit)? = null
    private var onError: ((Int, String) -> Unit)? = null

    companion object {
        // Universal wake words - flexible pattern matching
        private val WAKE_WORDS = listOf(
            "hey", "hello", "hi", "yo",
            "what's up", "how are you", "wake up", "come out"
        )

        // Dismissal words
        private val DISMISSAL_WORDS = listOf(
            "go away", "get out", "go", "stop", "bye", "dismiss", "close"
        )
    }

    /**
     * Initializes the speech recognizer.
     */
    fun initialize() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(SpeechRecognitionListener())
                Timber.d("SpeechRecognizer initialized")
            } else {
                Timber.w("Speech recognition not available on this device")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error initializing SpeechRecognizer")
        }
    }

    /**
     * Starts continuous listening for voice input.
     */
    fun startListening() {
        try {
            if (speechRecognizer == null) {
                initialize()
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // Use offline mode if available
            }

            speechRecognizer?.startListening(intent)
            Timber.d("Started listening for speech")
        } catch (e: Exception) {
            Timber.e(e, "Error starting listening")
        }
    }

    /**
     * Stops listening.
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            Timber.d("Stopped listening")
        } catch (e: Exception) {
            Timber.e(e, "Error stopping listening")
        }
    }

    /**
     * Registers callback for wake-word detection.
     *
     * @param callback Invoked when a wake word is detected
     */
    fun setOnWakeWordDetected(callback: (String) -> Unit) {
        onWakeWordDetected = callback
    }

    /**
     * Registers callback for command recognition.
     *
     * @param callback Invoked when a full command is recognized
     */
    fun setOnCommandRecognized(callback: (String) -> Unit) {
        onCommandRecognized = callback
    }

    /**
     * Registers callback for errors.
     *
     * @param callback Invoked on recognition error
     */
    fun setOnError(callback: (Int, String) -> Unit) {
        onError = callback
    }

    /**
     * Checks if a phrase contains a wake word.
     *
     * @param phrase The recognized phrase
     * @return true if any wake word is detected
     */
    private fun isWakeWord(phrase: String): Boolean {
        val lowerPhrase = phrase.lowercase()
        return WAKE_WORDS.any { lowerPhrase.contains(it) }
    }

    /**
     * Checks if a phrase contains a dismissal word.
     *
     * @param phrase The recognized phrase
     * @return true if any dismissal word is detected
     */
    fun isDismissalWord(phrase: String): Boolean {
        val lowerPhrase = phrase.lowercase()
        return DISMISSAL_WORDS.any { lowerPhrase.contains(it) }
    }

    /**
     * Releases resources.
     */
    fun destroy() {
        try {
            speechRecognizer?.destroy()
            serviceScope.cancel()
            Timber.d("SpeechRecognitionEngine destroyed")
        } catch (e: Exception) {
            Timber.e(e, "Error destroying SpeechRecognitionEngine")
        }
    }

    /**
     * Inner class implementing RecognitionListener for speech recognition events.
     */
    private inner class SpeechRecognitionListener : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            Timber.d("Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Timber.d("Beginning of speech detected")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // RMS level changed - can be used for visual feedback
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Buffer received
        }

        override fun onEndOfSpeech() {
            Timber.d("End of speech")
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                else -> "Unknown error"
            }
            Timber.e("Speech recognition error: $errorMessage")
            onError?.invoke(error, errorMessage)

            // Restart listening after error
            startListening()
        }

        override fun onResults(results: Bundle?) {
            try {
                results?.let {
                    val matches = it.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val topResult = matches[0]
                        Timber.d("Speech recognition result: $topResult")

                        // Check for dismissal words
                        if (isDismissalWord(topResult)) {
                            Timber.d("Dismissal word detected: $topResult")
                            return
                        }

                        // Check for wake words
                        if (isWakeWord(topResult)) {
                            Timber.d("Wake word detected: $topResult")
                            onWakeWordDetected?.invoke(topResult)
                        } else {
                            // Process as command
                            Timber.d("Command recognized: $topResult")
                            onCommandRecognized?.invoke(topResult)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing speech results")
            }

            // Restart listening for continuous operation
            startListening()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Partial results available - can be used for real-time feedback
            try {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Timber.d("Partial result: ${matches[0]}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing partial results")
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Timber.d("Speech recognition event: $eventType")
        }
    }
}
