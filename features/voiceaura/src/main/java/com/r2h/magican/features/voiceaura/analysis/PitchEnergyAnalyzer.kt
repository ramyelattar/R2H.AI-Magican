package com.r2h.magican.features.voiceaura.analysis

import com.r2h.magican.features.voiceaura.domain.VoiceFeatures
import com.r2h.magican.features.voiceaura.domain.VoiceFrame
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class PitchEnergyAnalyzer @Inject constructor() {

    fun analyze(frame: VoiceFrame): VoiceFeatures {
        if (frame.samples.isEmpty()) return VoiceFeatures(energy = 0f, pitchHz = null, voicedProbability = 0f)

        val normalized = FloatArray(frame.samples.size)
        var sumSquares = 0.0
        for (i in frame.samples.indices) {
            val n = frame.samples[i] / Short.MAX_VALUE.toFloat()
            normalized[i] = n
            sumSquares += n * n
        }

        val rms = sqrt(sumSquares / frame.samples.size).toFloat().coerceIn(0f, 1f)
        val pitchResult = estimatePitch(normalized, frame.sampleRate, rms)

        return VoiceFeatures(
            energy = rms,
            pitchHz = pitchResult.first,
            voicedProbability = pitchResult.second
        )
    }

    private fun estimatePitch(
        signal: FloatArray,
        sampleRate: Int,
        energy: Float
    ): Pair<Float?, Float> {
        if (energy < 0.015f || signal.size < 256) return null to 0f

        val minHz = 70
        val maxHz = 420
        val minLag = (sampleRate / maxHz).coerceAtLeast(1)
        val maxLag = (sampleRate / minHz).coerceAtMost(signal.size / 2)

        var bestLag = -1
        var bestScore = 0f

        for (lag in minLag..maxLag) {
            var dot = 0f
            var normA = 0f
            var normB = 0f
            var i = 0
            val limit = signal.size - lag
            while (i < limit) {
                val a = signal[i]
                val b = signal[i + lag]
                dot += a * b
                normA += a * a
                normB += b * b
                i++
            }

            val score = if (normA > 0f && normB > 0f) {
                (dot / kotlin.math.sqrt(normA * normB))
            } else {
                0f
            }

            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }

        if (bestLag <= 0 || bestScore < 0.22f) return null to bestScore.coerceIn(0f, 1f)
        return (sampleRate.toFloat() / bestLag.toFloat()) to bestScore.coerceIn(0f, 1f)
    }
}
