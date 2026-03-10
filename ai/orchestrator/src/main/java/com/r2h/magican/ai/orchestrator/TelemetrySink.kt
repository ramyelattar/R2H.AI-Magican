package com.r2h.magican.ai.orchestrator

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Centralized telemetry sink for AI orchestration events.
 * Logs to Android logcat and exposes event stream for monitoring.
 */
@Singleton
class TelemetrySink @Inject constructor() {

    private val _events = MutableSharedFlow<TelemetryEvent>(
        replay = 50,  // Keep last 50 events for debugging
        extraBufferCapacity = 100
    )
    val events: SharedFlow<TelemetryEvent> = _events.asSharedFlow()

    fun emit(event: TelemetryEvent) {
        // Log to Android logcat
        val tag = "MagicanTelemetry"
        val message = "[${event.eventId.take(8)}] ${event.describe()}"

        when (event.severity) {
            EventSeverity.Debug -> Log.d(tag, message)
            EventSeverity.Info -> Log.i(tag, message)
            EventSeverity.Warning -> {
                Log.w(tag, message)
                if (event is TelemetryEvent.SafetyBlocked) {
                    Log.w(tag, "  Classifications: ${event.classifications.joinToString { "${it.category}:${it.level}" }}")
                }
            }
            EventSeverity.Error -> {
                Log.e(tag, message)
            }
            EventSeverity.Critical -> {
                Log.wtf(tag, message)  // "What a Terrible Failure"
            }
        }

        // Emit to shared flow (non-blocking)
        _events.tryEmit(event)
    }
}
