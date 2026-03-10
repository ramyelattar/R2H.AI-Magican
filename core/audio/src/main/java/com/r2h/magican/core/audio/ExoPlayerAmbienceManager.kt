package com.r2h.magican.core.audio

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
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
class ExoPlayerAmbienceManager @Inject constructor(
    @ApplicationContext context: Context,
    private val volumePreferences: AudioVolumePreferences,
    @AmbienceUriMap private val ambienceUriMap: Map<AmbienceTrack, String>
) : Ambience, DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val isReleased = AtomicBoolean(false)

    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .build()
        .apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
        }

    private var currentTrack: AmbienceTrack? = null
    private var resumeOnForeground: Boolean = false

    @Volatile
    private var currentVolume: Float = DEFAULT_AMBIENCE_VOLUME

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch {
            volumePreferences.volumes.collectLatest { volumes ->
                currentVolume = volumes.ambience
                player.volume = volumes.ambience.coerceIn(0f, 1f)
            }
        }
    }

    override fun play(track: AmbienceTrack) {
        if (isReleased.get()) return
        val uri = ambienceUriMap[track] ?: return

        scope.launch {
            if (currentTrack != track) {
                currentTrack = track
                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
            }
            player.volume = currentVolume.coerceIn(0f, 1f)
            player.playWhenReady = true
            player.play()
        }
    }

    override fun stop() {
        if (isReleased.get()) return
        scope.launch {
            resumeOnForeground = false
            player.stop()
            currentTrack = null
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (isReleased.get()) return
        scope.launch {
            resumeOnForeground = player.isPlaying || player.playWhenReady
            player.pause()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isReleased.get()) return
        if (!resumeOnForeground) return
        scope.launch {
            if (currentTrack != null) {
                player.playWhenReady = true
                player.play()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
    }

    private fun release() {
        if (!isReleased.compareAndSet(false, true)) return
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        player.release()
        scope.cancel()
    }
}
