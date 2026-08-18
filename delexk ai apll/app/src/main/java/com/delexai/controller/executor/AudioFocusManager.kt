package com.delexai.controller.executor

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import timber.log.Timber

/**
 * Manages microphone access and audio focus.
 * Handles conflicts with other apps playing audio or incoming calls.
 * Automatically resumes listening when audio focus is regained.
 *
 * PHASE 3+ ENHANCEMENT - Audio Lifecycle Management
 */
class AudioFocusManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isListening = false

    private var onAudioFocusLost: (() -> Unit)? = null
    private var onAudioFocusGained: (() -> Unit)? = null

    /**
     * Requests audio focus for microphone input.
     * This allows the app to listen while respecting other apps' audio.
     *
     * @return true if audio focus was granted
     */
    fun requestAudioFocus(): Boolean {
        return try {
            val audioAttributes = AudioAttributes.Builder().apply {
                setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            }.build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).apply {
                    setAudioAttributes(audioAttributes)
                    setOnAudioFocusChangeListener(FocusChangeListener())
                }.build()

                val result = audioManager.requestAudioFocus(audioFocusRequest!!)
                val success = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

                if (success) {
                    isListening = true
                    Timber.d("Audio focus granted for microphone")
                } else {
                    Timber.w("Audio focus denied")
                }
                success
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    FocusChangeListener(),
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
                val success = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                if (success) {
                    isListening = true
                }
                success
            }
        } catch (e: Exception) {
            Timber.e(e, "Error requesting audio focus")
            false
        }
    }

    /**
     * Abandons audio focus and releases microphone.
     *
     * @return true if focus was abandoned
     */
    fun abandonAudioFocus(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest != null) {
                    val result = audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
                    audioFocusRequest = null
                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        isListening = false
                        Timber.d("Audio focus abandoned")
                        return true
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.abandonAudioFocus(FocusChangeListener())
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    isListening = false
                }
            }
            false
        } catch (e: Exception) {
            Timber.e(e, "Error abandoning audio focus")
            false
        }
    }

    /**
     * Checks if the app currently has audio focus.
     *
     * @return true if app has focus
     */
    fun hasAudioFocus(): Boolean = isListening

    /**
     * Registers callback for audio focus loss.
     *
     * @param callback Invoked when audio focus is lost
     */
    fun setOnAudioFocusLost(callback: () -> Unit) {
        onAudioFocusLost = callback
    }

    /**
     * Registers callback for audio focus gained.
     *
     * @param callback Invoked when audio focus is regained
     */
    fun setOnAudioFocusGained(callback: () -> Unit) {
        onAudioFocusGained = callback
    }

    /**
     * Gets the current audio stream for voice calls.
     *
     * @return Current volume
     */
    fun getVoiceCallVolume(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
    }

    /**
     * Sets the volume for voice calls.
     *
     * @param volume Volume level
     */
    fun setVoiceCallVolume(volume: Int) {
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, volume, 0)
            Timber.d("Voice call volume set to: $volume")
        } catch (e: Exception) {
            Timber.e(e, "Error setting voice call volume")
        }
    }

    /**
     * Mutes microphone input.
     *
     * @return true if muted successfully
     */
    fun mute(): Boolean {
        return try {
            audioManager.setMicrophoneMute(true)
            Timber.d("Microphone muted")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error muting microphone")
            false
        }
    }

    /**
     * Unmutes microphone input.
     *
     * @return true if unmuted successfully
     */
    fun unmute(): Boolean {
        return try {
            audioManager.setMicrophoneMute(false)
            Timber.d("Microphone unmuted")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error unmuting microphone")
            false
        }
    }

    /**
     * Checks if microphone is currently muted.
     *
     * @return true if muted
     */
    fun isMicrophoneMuted(): Boolean {
        return try {
            audioManager.isMicrophoneMute
        } catch (e: Exception) {
            Timber.e(e, "Error checking microphone mute status")
            false
        }
    }

    /**
     * Inner class for handling audio focus changes.
     */
    private inner class FocusChangeListener : AudioManager.OnAudioFocusChangeListener {

        override fun onAudioFocusChange(focusChange: Int) {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    Timber.d("Audio focus gained")
                    isListening = true
                    onAudioFocusGained?.invoke()
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    Timber.d("Audio focus lost (permanent)")
                    isListening = false
                    onAudioFocusLost?.invoke()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    Timber.d("Audio focus lost (transient)")
                    isListening = false
                    onAudioFocusLost?.invoke()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    Timber.d("Audio focus lost (can duck)")
                    // Can continue at lower volume
                }

                else -> Timber.d("Unknown audio focus change: $focusChange")
            }
        }
    }
}
