package com.r2h.magican.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs
import kotlin.math.sqrt

interface RotationDetector {
    fun events(config: RotationConfig = RotationConfig()): Flow<RotationEvent>
}

@Singleton
class AndroidRotationDetector @Inject constructor(
    @ApplicationContext context: Context
) : RotationDetector {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override fun events(config: RotationConfig): Flow<RotationEvent> = callbackFlow {
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (gyroscope == null) {
            close()
            return@callbackFlow
        }

        var lastEmitMs = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x) + (y * y) + (z * z))
                val nowMs = event.timestamp / 1_000_000L

                if (!passesAxisThreshold(config, x, y, z, magnitude)) return
                if (lastEmitMs != 0L && nowMs - lastEmitMs < config.minEmitIntervalMs) return

                lastEmitMs = nowMs
                trySend(
                    RotationEvent(
                        timestampMs = nowMs,
                        xRadPerSec = x,
                        yRadPerSec = y,
                        zRadPerSec = z,
                        magnitudeRadPerSec = magnitude
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, gyroscope, config.sensorDelay)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private fun passesAxisThreshold(
        config: RotationConfig,
        x: Float,
        y: Float,
        z: Float,
        magnitude: Float
    ): Boolean {
        return when (config.axis) {
            RotationAxis.Any -> magnitude >= config.thresholdRadPerSec
            RotationAxis.X -> abs(x) >= config.thresholdRadPerSec
            RotationAxis.Y -> abs(y) >= config.thresholdRadPerSec
            RotationAxis.Z -> abs(z) >= config.thresholdRadPerSec
        }
    }
}
