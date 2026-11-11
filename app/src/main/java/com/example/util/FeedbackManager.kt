package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class FeedbackManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var musicJob: Job? = null

    @Volatile
    private var isMusicEnabled: Boolean = false

    @Volatile
    private var isSfxEnabled: Boolean = true

    @Volatile
    private var isVibrationEnabled: Boolean = true

    fun updateSettings(music: Boolean, sfx: Boolean, vibration: Boolean) {
        val wasMusicEnabled = isMusicEnabled
        isMusicEnabled = music
        isSfxEnabled = sfx
        isVibrationEnabled = vibration

        if (music && !wasMusicEnabled) {
            startAmbientMusic()
        } else if (!music && wasMusicEnabled) {
            stopAmbientMusic()
        }
    }

    fun startAmbientMusic() {
        if (!isMusicEnabled || musicJob?.isActive == true) return
        musicJob = scope.launch {
            val notesHz = doubleArrayOf(261.63, 329.63, 392.00, 493.88, 523.25, 392.00, 329.63, 293.66)
            var idx = 0
            while (isActive && isMusicEnabled) {
                val freq = notesHz[idx % notesHz.size]
                playSoftTone(freqHz = freq, durationMs = 650, amplitude = 0.08)
                idx++
                delay(750L)
            }
        }
    }

    fun stopAmbientMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    /**
     * Haptic vibration + chime when a player correctly identifies a difference.
     */
    fun playSpotFoundFeedback() {
        if (isVibrationEnabled) {
            vibratePattern(
                timings = longArrayOf(0L, 45L, 45L, 85L),
                amplitudes = intArrayOf(0, 200, 0, 255)
            )
        }
        if (isSfxEnabled) {
            scope.launch {
                playSoftTone(587.33, 110, 0.22) // D5
                delay(90)
                playSoftTone(880.00, 180, 0.25) // A5
            }
        }
    }

    /**
     * Haptic vibration + alert tone when a player runs out of time.
     */
    fun playTimeOutFeedback() {
        if (isVibrationEnabled) {
            vibratePattern(
                timings = longArrayOf(0L, 180L, 80L, 180L, 80L, 320L),
                amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            )
        }
        if (isSfxEnabled) {
            scope.launch {
                val descending = doubleArrayOf(392.00, 349.23, 311.13, 261.63)
                for (freq in descending) {
                    playSoftTone(freq, 170, 0.26)
                    delay(140)
                }
            }
        }
    }

    /**
     * Short haptic buzz on an incorrect tap.
     */
    fun playMissPenaltyFeedback() {
        if (isVibrationEnabled) {
            vibratePattern(
                timings = longArrayOf(0L, 95L),
                amplitudes = intArrayOf(0, 220)
            )
        }
        if (isSfxEnabled) {
            scope.launch {
                playSoftTone(196.00, 150, 0.24)
                delay(100)
                playSoftTone(164.81, 180, 0.24)
            }
        }
    }

    /**
     * Celebratory haptic pattern when all differences are found.
     */
    fun playVictoryFeedback() {
        if (isVibrationEnabled) {
            vibratePattern(
                timings = longArrayOf(0L, 50L, 50L, 50L, 50L, 140L),
                amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
            )
        }
        if (isSfxEnabled) {
            scope.launch {
                val arpeggio = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
                for (freq in arpeggio) {
                    playSoftTone(freq, 130, 0.24)
                    delay(110)
                }
            }
        }
    }

    fun playHintFeedback() {
        if (isVibrationEnabled) {
            vibratePattern(
                timings = longArrayOf(0L, 28L, 40L, 28L),
                amplitudes = intArrayOf(0, 110, 0, 150)
            )
        }
        if (isSfxEnabled) {
            scope.launch {
                playSoftTone(659.25, 95, 0.16) // E5 soft radar ping
                delay(75)
                playSoftTone(783.99, 130, 0.18) // G5
            }
        }
    }

    fun playClickFeedback() {
        if (isVibrationEnabled) {
            vibratePattern(
                timings = longArrayOf(0L, 18L),
                amplitudes = intArrayOf(0, 120)
            )
        }
        if (isSfxEnabled) {
            scope.launch {
                playSoftTone(740.0, 45, 0.14)
            }
        }
    }

    private fun playSoftTone(freqHz: Double, durationMs: Int, amplitude: Double) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = sin(PI * i / numSamples) * exp(-2.2 * i / numSamples)
                val wave = sin(2.0 * PI * freqHz * t) + 0.3 * sin(4.0 * PI * freqHz * t)
                val value = (wave * envelope * amplitude * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                samples[i] = value.toShort()
            }

            val bufferSize = samples.size * 2
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize.coerceAtLeast(1024))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            Thread.sleep(durationMs.toLong() + 20L)
            audioTrack.stop()
            audioTrack.release()
        } catch (ignored: Exception) {
        }
    }

    private fun vibratePattern(timings: LongArray, amplitudes: IntArray) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(timings, -1)
                }
            }
        } catch (ignored: Exception) {
        }
    }
}
