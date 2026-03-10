package com.r2h.magican.core.haptics

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidHaptics @Inject constructor(
    @ApplicationContext private val context: Context
) : Haptics {

    private val vibrator: Vibrator? by lazy { resolveVibrator(context) }

    override fun tick() {
        vibrate(
            predefined = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            } else {
                VibrationEffect.createOneShot(12L, 90)
            },
            fallback = VibrationEffect.createOneShot(12L, 90)
        )
    }

    override fun confirm() {
        vibrate(
            predefined = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            } else {
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 20L, 26L, 32L),
                    intArrayOf(0, 130, 0, 200),
                    -1
                )
            },
            fallback = VibrationEffect.createWaveform(
                longArrayOf(0L, 20L, 26L, 32L),
                intArrayOf(0, 130, 0, 200),
                -1
            )
        )
    }

    override fun pattern(type: HapticPatternType) {
        val effect = when (type) {
            HapticPatternType.Selection -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                } else {
                    VibrationEffect.createOneShot(12L, 90)
                }
            }

            HapticPatternType.Success -> {
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 16L, 22L, 34L),
                    intArrayOf(0, 120, 0, 210),
                    -1
                )
            }

            HapticPatternType.Warning -> {
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 28L, 36L, 28L),
                    intArrayOf(0, 210, 0, 150),
                    -1
                )
            }

            HapticPatternType.Error -> {
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 30L, 20L, 30L, 20L, 44L),
                    intArrayOf(0, 230, 0, 180, 0, 255),
                    -1
                )
            }

            HapticPatternType.MysticPulse -> {
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 18L, 24L, 18L, 24L, 36L),
                    intArrayOf(0, 110, 0, 170, 0, 220),
                    -1
                )
            }
        }

        vibrate(predefined = effect, fallback = effect)
    }

    @SuppressLint("MissingPermission")
    private fun vibrate(predefined: VibrationEffect, fallback: VibrationEffect) {
        if (!isSystemHapticsEnabled()) return
        val engine = vibrator ?: return
        if (!engine.hasVibrator()) return

        runCatching { engine.vibrate(predefined) }
            .onFailure { runCatching { engine.vibrate(fallback) } }
    }

    private fun isSystemHapticsEnabled(): Boolean {
        return runCatching {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            ) == 1
        }.getOrDefault(true)
    }

    private fun resolveVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
