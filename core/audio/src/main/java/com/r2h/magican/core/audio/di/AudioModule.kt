package com.r2h.magican.core.audio.di

import com.r2h.magican.core.audio.Ambience
import com.r2h.magican.core.audio.AmbienceTrack
import com.r2h.magican.core.audio.AmbienceUriMap
import com.r2h.magican.core.audio.ExoPlayerAmbienceManager
import com.r2h.magican.core.audio.Sfx
import com.r2h.magican.core.audio.SfxResMap
import com.r2h.magican.core.audio.SfxType
import com.r2h.magican.core.audio.SoundPoolSfxManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindSfx(impl: SoundPoolSfxManager): Sfx

    @Binds
    @Singleton
    abstract fun bindAmbience(impl: ExoPlayerAmbienceManager): Ambience
}

@Module
@InstallIn(SingletonComponent::class)
object AudioConfigModule {

    @Provides
    @Singleton
    @SfxResMap
    fun provideSfxResMap(): Map<SfxType, Int> = emptyMap()

    @Provides
    @Singleton
    @AmbienceUriMap
    fun provideAmbienceUriMap(): Map<AmbienceTrack, String> = emptyMap()
}
