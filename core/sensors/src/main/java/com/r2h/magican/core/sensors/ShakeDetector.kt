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
import kotlin.math.sqrt

interface ShakeDetector {
    fun events(config: ShakeConfig = ShakeConfig()): Flow<ShakeEvent>
}

@Singleton
class AndroidShakeDetector @Inject constructor(
    @ApplicationContext context: Context
) : ShakeDetector {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override fun events(config: ShakeConfig): Flow<ShakeEvent> = callbackFlow {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            close()
            return@callbackFlow
        }

        var shakeCount = 0
        var windowStartMs = 0L
        var lastShakeMs = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

                val gx = event.values[0] / SensorManager.GRAVITY_EARTH
                val gy = event.values[1] / SensorManager.GRAVITY_EARTH
                val gz = event.values[2] / SensorManager.GRAVITY_EARTH

                val gForce = sqrt((gx * gx) + (gy * gy) + (gz * gz))
                val nowMs = event.timestamp / 1_000_000L

                if (gForce < config.triggerGravity) return
                if (lastShakeMs != 0L && nowMs - lastShakeMs < config.minGapMs) return

                if (windowStartMs == 0L || nowMs - windowStartMs > config.windowMs) {
                    windowStartMs = nowMs
                    shakeCount = 0
                }

                shakeCount += 1
                lastShakeMs = nowMs

                if (shakeCount >= config.minShakesInWindow) {
                    trySend(
                        ShakeEvent(
                            timestampMs = nowMs,
                            gForce = gForce,
                            shakeCount = shakeCount
                        )
                    )
                    windowStartMs = nowMs
                    shakeCount = 0
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, accelerometer, config.sensorDelay)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
