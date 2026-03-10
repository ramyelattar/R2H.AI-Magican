package com.r2h.magican.features.tarot.integration

import kotlinx.coroutines.flow.Flow

data class BlowConfig(
    val amplitudeThreshold: Float = 0.18f,
    val minHoldMs: Long = 120L,
    val cooldownMs: Long = 700L,
    val sampleRate: Int = 16_000
)

data class BlowEvent(
    val timestampMs: Long,
    val intensity: Float
)

interface BlowDetector {
    fun events(config: BlowConfig = BlowConfig()): Flow<BlowEvent>
}
