package com.r2h.magican.core.sensors

import kotlin.math.abs

enum class RotationAxis {
    Any,
    X,
    Y,
    Z
}

data class ShakeConfig(
    val triggerGravity: Float = 2.2f,
    val minShakesInWindow: Int = 2,
    val windowMs: Long = 650L,
    val minGapMs: Long = 120L,
    val sensorDelay: Int = android.hardware.SensorManager.SENSOR_DELAY_GAME
) {
    init {
        require(triggerGravity > 0f) { "triggerGravity must be > 0" }
        require(minShakesInWindow >= 1) { "minShakesInWindow must be >= 1" }
        require(windowMs > 0) { "windowMs must be > 0" }
        require(minGapMs >= 0) { "minGapMs must be >= 0" }
    }
}

data class ShakeEvent(
    val timestampMs: Long,
    val gForce: Float,
    val shakeCount: Int
)

data class RotationConfig(
    val axis: RotationAxis = RotationAxis.Any,
    val thresholdRadPerSec: Float = 1.25f,
    val minEmitIntervalMs: Long = 80L,
    val sensorDelay: Int = android.hardware.SensorManager.SENSOR_DELAY_GAME
) {
    init {
        require(thresholdRadPerSec > 0f) { "thresholdRadPerSec must be > 0" }
        require(minEmitIntervalMs >= 0) { "minEmitIntervalMs must be >= 0" }
    }
}

data class RotationEvent(
    val timestampMs: Long,
    val xRadPerSec: Float,
    val yRadPerSec: Float,
    val zRadPerSec: Float,
    val magnitudeRadPerSec: Float
) {
    fun dominantAxis(): RotationAxis {
        val ax = abs(xRadPerSec)
        val ay = abs(yRadPerSec)
        val az = abs(zRadPerSec)
        return when {
            ax >= ay && ax >= az -> RotationAxis.X
            ay >= ax && ay >= az -> RotationAxis.Y
            else -> RotationAxis.Z
        }
    }
}
