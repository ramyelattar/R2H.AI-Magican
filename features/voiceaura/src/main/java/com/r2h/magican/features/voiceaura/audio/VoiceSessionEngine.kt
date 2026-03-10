package com.r2h.magican.features.voiceaura.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.r2h.magican.features.voiceaura.domain.VoiceFrame
import com.r2h.magican.features.voiceaura.domain.VoiceSessionMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface VoiceSessionEngine {
    fun frames(
        mode: VoiceSessionMode,
        sampleRate: Int = 16_000,
        frameSize: Int = 1024
    ): Flow<VoiceFrame>
}

@Singleton
class AndroidVoiceSessionEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : VoiceSessionEngine {

    @SuppressLint("MissingPermission")
    override fun frames(
        mode: VoiceSessionMode,
        sampleRate: Int,
        frameSize: Int
    ): Flow<VoiceFrame> = callbackFlow {
        if (!hasRecordAudioPermission()) {
            close(SecurityException("RECORD_AUDIO permission is required"))
            return@callbackFlow
        }

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBuffer <= 0) {
            close(IllegalStateException("Invalid AudioRecord min buffer"))
            return@callbackFlow
        }

        val source = when (mode) {
            VoiceSessionMode.Hum -> MediaRecorder.AudioSource.VOICE_RECOGNITION
            VoiceSessionMode.Breath -> MediaRecorder.AudioSource.MIC
        }

        val recorder = AudioRecord(
            source,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            (minBuffer * 2).coerceAtLeast(frameSize * 4)
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            close(IllegalStateException("AudioRecord initialization failed"))
            return@callbackFlow
        }

        val job = launch(Dispatchers.Default) {
            val buffer = ShortArray(frameSize)
            runCatching { recorder.startRecording() }
                .onFailure {
                    close(IllegalStateException("AudioRecord start failed"))
                    return@launch
                }
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                close(IllegalStateException("AudioRecord did not enter recording state"))
                return@launch
            }

            while (isActive && !isClosedForSend) {
                val read = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (read <= 0) {
                    if (read == AudioRecord.ERROR_DEAD_OBJECT ||
                        read == AudioRecord.ERROR_INVALID_OPERATION ||
                        read == AudioRecord.ERROR_BAD_VALUE
                    ) {
                        close(IllegalStateException("AudioRecord read failed with code=$read"))
                        break
                    }
                    continue
                }

                trySend(
                    VoiceFrame(
                        samples = if (read == buffer.size) buffer.copyOf() else buffer.copyOf(read),
                        sampleRate = sampleRate,
                        timestampMs = SystemClock.elapsedRealtime()
                    )
                )
            }
        }

        awaitClose {
            job.cancel()
            runCatching { recorder.stop() }
            recorder.release()
        }
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
