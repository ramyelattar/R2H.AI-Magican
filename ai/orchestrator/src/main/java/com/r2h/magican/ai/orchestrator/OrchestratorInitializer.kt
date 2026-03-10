package com.r2h.magican.ai.orchestrator

import android.util.Log
import com.r2h.magican.ai.runtime.AiRuntimeInitializer
import com.r2h.magican.ai.runtime.RuntimeReadinessReport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orchestrator initialization that runs after AI runtime is loaded.
 * Performs self-test verification and prepares orchestration layer.
 */
@Singleton
class OrchestratorInitializer @Inject constructor(
    private val startupSelfTest: StartupSelfTest,
    private val aiRuntimeInitializer: AiRuntimeInitializer
) {

    private val initMutex = Mutex()
    private var hasRun = false
    var selfTestEnabled = true  // Can be disabled for testing

    suspend fun initialize(runtimeReadiness: RuntimeReadinessReport?) {
        initMutex.withLock {
            if (hasRun) {
                Log.w("OrchestratorInit", "Initialization already run, skipping")
                return
            }

            Log.i("OrchestratorInit", "Starting orchestrator initialization...")
            val readiness = runtimeReadiness ?: aiRuntimeInitializer.currentReadiness()
            if (!readiness.isReadyForInference) {
                Log.w(
                    "OrchestratorInit",
                    "Runtime not ready for inference: ${readiness.availability} - ${readiness.reason}"
                )
            }

            if (selfTestEnabled) {
                // Let self-test decide whether to run or skip based on readiness.
                val result = startupSelfTest.run(readiness)

                when (result) {
                    is StartupSelfTestResult.Passed -> {
                        Log.i("OrchestratorInit", "Self-test passed in ${result.durationMs}ms")
                    }
                    is StartupSelfTestResult.Failed -> {
                        Log.e("OrchestratorInit", "Self-test failed: ${result.reason}")
                        // Note: Hard failures are handled at runtime layer with AI_RUNTIME_REQUIRED
                        // Orchestrator just logs and reports via telemetry
                    }
                    is StartupSelfTestResult.Skipped -> {
                        Log.w("OrchestratorInit", "Self-test skipped: ${result.reason}")
                    }
                }
            }

            hasRun = true
            Log.i("OrchestratorInit", "Orchestrator initialization complete")
        }
    }
}
