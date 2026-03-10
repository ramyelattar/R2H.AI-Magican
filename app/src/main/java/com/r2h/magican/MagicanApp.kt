package com.r2h.magican

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.r2h.magican.ai.runtime.AiRuntimeInitializer
import com.r2h.magican.ai.orchestrator.OrchestratorInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@HiltAndroidApp
class MagicanApp : Application() {

    @Inject
    lateinit var aiRuntimeInitializer: AiRuntimeInitializer

    @Inject
    lateinit var orchestratorInitializer: OrchestratorInitializer

    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build()
            )
        }

        startupScope.launch {
            val runtimeReadiness = try {
                // Phase 1: Load and verify AI runtime.
                aiRuntimeInitializer.initializeDefaultModelIfConfigured()
            } catch (_: CancellationException) {
                throw
            } catch (error: Throwable) {
                Log.e("MagicanApp", "AI runtime initialization failed: ${error::class.java.simpleName}")
                aiRuntimeInitializer.latestReadiness()
            }

            if (runtimeReadiness != null) {
                Log.i("MagicanApp", "Runtime availability: ${runtimeReadiness.availability} - ${runtimeReadiness.reason}")
            }

            try {
                // Phase 2: Run orchestration self-test and prepare orchestrator.
                orchestratorInitializer.initialize(runtimeReadiness)
            } catch (_: CancellationException) {
                throw
            } catch (error: Throwable) {
                Log.e("MagicanApp", "Orchestrator initialization failed: ${error::class.java.simpleName}")
            }
        }
    }

    override fun onTerminate() {
        startupScope.cancel()
        super.onTerminate()
    }
}
