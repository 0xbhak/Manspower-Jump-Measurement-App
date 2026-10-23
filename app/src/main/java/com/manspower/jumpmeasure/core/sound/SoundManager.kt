package com.manspower.jumpmeasure.core.sound

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * SoundManager provides audio cues for countdown beeps, start tones, and landing confirmation.
 * Uses native ToneGenerator for zero-latency, reliable audio feedback without needing external sound files.
 */
object SoundManager {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to initialize ToneGenerator", e)
        }
    }

    /**
     * Beep for countdown seconds (3, 2, 1) - distinct 120ms beep
     */
    fun playCountdownBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Exception) {
            Log.w("SoundManager", "Error playing countdown beep", e)
        }
    }

    /**
     * Start / GO tone - higher pitch start whistle tone
     */
    fun playStartTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 320)
        } catch (e: Exception) {
            Log.w("SoundManager", "Error playing start tone", e)
        }
    }

    /**
     * Landing / Measurement Success tone - confirmation chime
     */
    fun playLandingSuccessTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 260)
        } catch (e: Exception) {
            Log.w("SoundManager", "Error playing landing tone", e)
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
