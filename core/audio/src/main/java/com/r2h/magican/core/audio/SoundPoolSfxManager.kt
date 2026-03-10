package com.r2h.magican.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Singleton
class SoundPoolSfxManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val volumePreferences: AudioVolumePreferences,
    @SfxResMap private val sfxResMap: Map<SfxType, Int>
) : Sfx, DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val isReleased = AtomicBoolean(false)

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val sampleIds = ConcurrentHashMap<SfxType, Int>()
    private val streamIds = ConcurrentHashMap<SfxType, Int>()

    @Volatile
    private var currentVolume: Float = DEFAULT_SFX_VOLUME

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch {
            volumePreferences.volumes.collectLatest { volumes ->
                currentVolume = volumes.sfx
            }
        }
    }

    override fun play(type: SfxType) {
        if (isReleased.get()) return

        val sampleId = sampleIds[type] ?: loadSample(type) ?: return
        val volume = currentVolume.coerceIn(0f, 1f)
        if (volume <= 0f) return

        val streamId = soundPool.play(sampleId, volume, volume, 1, 0, 1f)
        if (streamId != 0) {
            streamIds[type] = streamId
        }
    }

    override fun stop(type: SfxType) {
        if (isReleased.get()) return
        streamIds.remove(type)?.let(soundPool::stop)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isReleased.get()) return
        soundPool.autoResume()
    }

    override fun onStop(owner: LifecycleOwner) {
        if (isReleased.get()) return
        soundPool.autoPause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
    }

    private fun loadSample(type: SfxType): Int? {
        val resId = sfxResMap[type] ?: return null
        if (resId <= 0) return null

        val sampleId = soundPool.load(context, resId, 1)
        if (sampleId == 0) return null

        sampleIds[type] = sampleId
        return sampleId
    }

    private fun release() {
        if (!isReleased.compareAndSet(false, true)) return
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        streamIds.clear()
        sampleIds.clear()
        soundPool.release()
        scope.cancel()
    }
}
