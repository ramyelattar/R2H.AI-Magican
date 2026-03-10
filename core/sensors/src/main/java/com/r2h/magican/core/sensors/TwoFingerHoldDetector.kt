package com.r2h.magican.core.sensors

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class TwoFingerHoldConfig(
    val holdDurationMillis: Long = 600L,
    val maxDriftPx: Float = 28f
) {
    init {
        require(holdDurationMillis > 0L) { "holdDurationMillis must be > 0" }
        require(maxDriftPx >= 0f) { "maxDriftPx must be >= 0" }
    }
}

sealed interface TwoFingerHoldEvent {
    data object Idle : TwoFingerHoldEvent
    data class Started(val timestampMs: Long) : TwoFingerHoldEvent
    data class Triggered(val timestampMs: Long, val heldForMs: Long) : TwoFingerHoldEvent
    data class Finished(val timestampMs: Long) : TwoFingerHoldEvent
    data class Cancelled(val reason: String) : TwoFingerHoldEvent
}

@Stable
class TwoFingerHoldDetector internal constructor() {
    private val _events = MutableSharedFlow<TwoFingerHoldEvent>(
        replay = 1,
        extraBufferCapacity = 4
    )

    val events: SharedFlow<TwoFingerHoldEvent> = _events.asSharedFlow()

    init {
        _events.tryEmit(TwoFingerHoldEvent.Idle)
    }

    internal fun dispatch(event: TwoFingerHoldEvent) {
        _events.tryEmit(event)
    }
}

@Composable
fun rememberTwoFingerHoldDetector(): TwoFingerHoldDetector = remember { TwoFingerHoldDetector() }

fun Modifier.twoFingerHold(
    detector: TwoFingerHoldDetector,
    config: TwoFingerHoldConfig = TwoFingerHoldConfig(),
    enabled: Boolean = true,
    onTriggered: () -> Unit = {}
): Modifier {
    if (!enabled) return this

    return pointerInput(detector, config) {
        awaitEachGesture {
            var firstId: PointerId? = null
            var firstOrigin: Offset? = null
            var secondId: PointerId? = null
            var secondOrigin: Offset? = null
            var startMs = 0L
            var triggered = false
            var terminated = false

            while (!terminated) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }

                if (firstId == null) {
                    val first = pressed.firstOrNull()
                    if (first != null) {
                        firstId = first.id
                        firstOrigin = first.position
                    }
                    continue
                }

                if (secondId == null) {
                    val second = pressed.firstOrNull { it.id != firstId }
                    if (second != null) {
                        secondId = second.id
                        secondOrigin = second.position
                        startMs = SystemClock.uptimeMillis()
                        detector.dispatch(TwoFingerHoldEvent.Started(startMs))
                    }
                    continue
                }

                if (secondId != null) {
                    val first = pressed.firstOrNull { it.id == firstId }
                    val second = pressed.firstOrNull { it.id == secondId }

                    if (first == null || second == null) {
                        detector.dispatch(
                            if (triggered) {
                                TwoFingerHoldEvent.Finished(SystemClock.uptimeMillis())
                            } else {
                                TwoFingerHoldEvent.Cancelled("finger_lifted")
                            }
                        )
                        terminated = true
                        continue
                    }

                    val localFirstOrigin = firstOrigin ?: first.position
                    val secondStart = secondOrigin ?: second.position
                    val driftExceeded =
                        (first.position - localFirstOrigin).getDistance() > config.maxDriftPx ||
                            (second.position - secondStart).getDistance() > config.maxDriftPx

                    if (driftExceeded) {
                        detector.dispatch(TwoFingerHoldEvent.Cancelled("drift_exceeded"))
                        terminated = true
                        continue
                    }

                    val nowMs = SystemClock.uptimeMillis()
                    val heldFor = nowMs - startMs
                    if (!triggered && heldFor >= config.holdDurationMillis) {
                        triggered = true
                        detector.dispatch(TwoFingerHoldEvent.Triggered(nowMs, heldFor))
                        onTriggered()
                    }
                }

                if (pressed.isEmpty()) {
                    if (secondId != null) {
                        detector.dispatch(
                            if (triggered) {
                                TwoFingerHoldEvent.Finished(SystemClock.uptimeMillis())
                            } else {
                                TwoFingerHoldEvent.Cancelled("released")
                            }
                        )
                    }
                    terminated = true
                }
            }
        }
    }
}
