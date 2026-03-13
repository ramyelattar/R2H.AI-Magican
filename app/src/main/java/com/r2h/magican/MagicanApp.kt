package com.r2h.magican

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.r2h.magican.ai.runtime.AiRuntimeInitializer
import com.r2h.magican.ai.orchestrator.OrchestratorInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@HiltAndroidApp
class MagicanApp : Application() {

    @Inject
    lateinit var aiRuntimeInitializer: AiRuntimeInitializer

    @Inject
    lateinit var orchestratorInitializer: OrchestratorInitializer

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

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            val runtimeReadiness = try {
                // Phase 1: Load and verify AI runtime.
                aiRuntimeInitializer.initializeDefaultModelIfConfigured()
            } catch (e: CancellationException) {
                throw e
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
            } catch (e: CancellationException) {
                throw e
            } catch (error: Throwable) {
                Log.e("MagicanApp", "Orchestrator initialization failed: ${error::class.java.simpleName}")
            }
        }
    }

}
