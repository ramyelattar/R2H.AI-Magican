package com.r2h.magican.core.audio

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.audioDataStore: DataStore<Preferences> by preferencesDataStore(name = "audio_prefs")

@Singleton
class AudioVolumePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val Sfx = floatPreferencesKey("sfx_volume")
        val Ambience = floatPreferencesKey("ambience_volume")
    }

    val volumes: Flow<AudioVolumes> = context.audioDataStore.data
        .map { prefs ->
            AudioVolumes(
                sfx = (prefs[Keys.Sfx] ?: DEFAULT_SFX_VOLUME).coerceIn(0f, 1f),
                ambience = (prefs[Keys.Ambience] ?: DEFAULT_AMBIENCE_VOLUME).coerceIn(0f, 1f)
            )
        }
        .distinctUntilChanged()

    suspend fun setSfxVolume(value: Float) {
        context.audioDataStore.edit { prefs ->
            prefs[Keys.Sfx] = value.coerceIn(0f, 1f)
        }
    }

    suspend fun setAmbienceVolume(value: Float) {
        context.audioDataStore.edit { prefs ->
            prefs[Keys.Ambience] = value.coerceIn(0f, 1f)
        }
    }
}
