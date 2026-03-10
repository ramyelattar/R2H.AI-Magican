package com.r2h.magican.core.audio

data class AudioVolumes(
    val sfx: Float = DEFAULT_SFX_VOLUME,
    val ambience: Float = DEFAULT_AMBIENCE_VOLUME
)

enum class SfxType {
    Tap,
    Swipe,
    Success,
    Error,
    MysticChime
}

enum class AmbienceTrack {
    Nebula,
    RainTemple,
    DeepFocus
}

interface Sfx {
    fun play(type: SfxType)
    fun stop(type: SfxType)
}

interface Ambience {
    fun play(track: AmbienceTrack)
    fun stop()
}

const val DEFAULT_SFX_VOLUME: Float = 0.85f
const val DEFAULT_AMBIENCE_VOLUME: Float = 0.65f
