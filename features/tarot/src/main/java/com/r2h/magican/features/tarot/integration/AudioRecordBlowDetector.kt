package com.r2h.magican.features.tarot.integration

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Singleton
class AudioRecordBlowDetector @Inject constructor(
    @ApplicationContext private val context: Context
) : BlowDetector {

    @SuppressLint("MissingPermission")
    override fun events(config: BlowConfig): Flow<BlowEvent> = callbackFlow {
        if (!hasRecordPermission()) {
            close(SecurityException("RECORD_AUDIO permission is required for BlowDetector"))
            return@callbackFlow
        }

        val minBuffer = AudioRecord.getMinBufferSize(
            config.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            close(IllegalStateException("AudioRecord min buffer is invalid"))
            return@callbackFlow
        }

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            config.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            (minBuffer * 2).coerceAtLeast(2048)
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            close(IllegalStateException("AudioRecord could not initialize"))
            return@callbackFlow
        }

        val job = launch(Dispatchers.Default) {
            val buffer = ShortArray(1024)
            var blowStartMs = 0L
            var lastEmitMs = 0L

            runCatching { audioRecord.startRecording() }
                .onFailure {
                    close(IllegalStateException("AudioRecord start failed"))
                    return@launch
                }
            if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                close(IllegalStateException("AudioRecord did not enter recording state"))
                return@launch
            }

            while (!isClosedForSend) {
                val read = audioRecord.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
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

                var sumSquares = 0.0
                for (i in 0 until read) {
                    val s = buffer[i].toDouble() / Short.MAX_VALUE
                    sumSquares += s * s
                }
                val rms = sqrt(sumSquares / read).toFloat()
                val now = SystemClock.elapsedRealtime()

                if (rms >= config.amplitudeThreshold) {
                    if (blowStartMs == 0L) blowStartMs = now
                    val hold = now - blowStartMs
                    if (hold >= config.minHoldMs && now - lastEmitMs >= config.cooldownMs) {
                        lastEmitMs = now
                        trySend(BlowEvent(timestampMs = now, intensity = rms.coerceIn(0f, 1f)))
                    }
                } else {
                    blowStartMs = 0L
                }
            }
        }

        awaitClose {
            job.cancel()
            runCatching { audioRecord.stop() }
            audioRecord.release()
        }
    }

    private fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
