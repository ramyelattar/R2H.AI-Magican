package com.r2h.magican.core.sensors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun rememberShakeEventState(
    detector: ShakeDetector,
    config: ShakeConfig = ShakeConfig()
): State<ShakeEvent?> {
    val state = remember { mutableStateOf<ShakeEvent?>(null) }

    LaunchedEffect(detector, config) {
        detector.events(config).collect { state.value = it }
    }
    return state
}

@Composable
fun rememberRotationEventState(
    detector: RotationDetector,
    config: RotationConfig = RotationConfig()
): State<RotationEvent?> {
    val state = remember { mutableStateOf<RotationEvent?>(null) }

    LaunchedEffect(detector, config) {
        detector.events(config).collect { state.value = it }
    }
    return state
}

@Composable
fun rememberTwoFingerHoldEventState(
    detector: TwoFingerHoldDetector
): State<TwoFingerHoldEvent> {
    val state = remember { mutableStateOf<TwoFingerHoldEvent>(TwoFingerHoldEvent.Idle) }

    LaunchedEffect(detector) {
        detector.events.collect { state.value = it }
    }
    return state
}
