package com.r2h.magican.features.voiceaura.analysis

import com.r2h.magican.features.voiceaura.domain.SessionAnalytics
import com.r2h.magican.features.voiceaura.domain.VoiceFeatures
import com.r2h.magican.features.voiceaura.domain.VoiceSessionMode
import kotlin.math.sqrt

class VoiceSessionAggregator {

    private var frames: Int = 0
    private var sumEnergy: Float = 0f
    private var peakEnergy: Float = 0f
    private var voicedFrames: Int = 0
    private val pitchValues = mutableListOf<Float>()

    private var envelope: Float = 0f
    private var inhaleState = false
    private var breathCycles = 0
    private var lastTransitionMs = 0L

    @Synchronized
    fun reset() {
        frames = 0
        sumEnergy = 0f
        peakEnergy = 0f
        voicedFrames = 0
        pitchValues.clear()
        envelope = 0f
        inhaleState = false
        breathCycles = 0
        lastTransitionMs = 0L
    }

    @Synchronized
    fun ingest(
        features: VoiceFeatures,
        timestampMs: Long,
        mode: VoiceSessionMode
    ) {
        frames += 1
        sumEnergy += features.energy
        peakEnergy = maxOf(peakEnergy, features.energy)

        if (features.pitchHz != null) {
            pitchValues += features.pitchHz
            if (features.voicedProbability >= 0.30f) voicedFrames += 1
        }

        if (mode == VoiceSessionMode.Breath) {
            envelope = (envelope * 0.86f) + (features.energy * 0.14f)
            val now = timestampMs
            val refractoryMs = 550L

            if (!inhaleState && envelope >= 0.12f && now - lastTransitionMs > refractoryMs) {
                inhaleState = true
                lastTransitionMs = now
            } else if (inhaleState && envelope <= 0.07f && now - lastTransitionMs > refractoryMs) {
                inhaleState = false
                breathCycles += 1
                lastTransitionMs = now
            }
        }
    }

    @Synchronized
    fun snapshot(
        mode: VoiceSessionMode,
        durationMs: Long
    ): SessionAnalytics {
        val safeFrames = frames.coerceAtLeast(1)
        val avgEnergy = sumEnergy / safeFrames

        val avgPitch = if (pitchValues.isNotEmpty()) {
            pitchValues.sum() / pitchValues.size
        } else {
            null
        }

        val pitchStability = if (pitchValues.size >= 2 && avgPitch != null) {
            val variance = pitchValues.fold(0f) { acc, p ->
                val d = p - avgPitch
                acc + (d * d)
            } / pitchValues.size
            val std = sqrt(variance)
            (1f - (std / avgPitch.coerceAtLeast(1f))).coerceIn(0f, 1f)
        } else {
            null
        }

        val breathsPerMin = if (mode == VoiceSessionMode.Breath && durationMs > 0L) {
            val minutes = durationMs / 60_000f
            if (minutes > 0f) breathCycles / minutes else null
        } else {
            null
        }

        return SessionAnalytics(
            mode = mode,
            durationMs = durationMs,
            avgEnergy = avgEnergy,
            peakEnergy = peakEnergy,
            avgPitchHz = avgPitch,
            pitchStability = pitchStability,
            breathsPerMinute = breathsPerMin,
            voicePresence = voicedFrames / safeFrames.toFloat()
        )
    }
}
