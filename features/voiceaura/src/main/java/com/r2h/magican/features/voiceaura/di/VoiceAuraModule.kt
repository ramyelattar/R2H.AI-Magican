package com.r2h.magican.features.voiceaura.di

import com.r2h.magican.features.voiceaura.audio.AndroidVoiceSessionEngine
import com.r2h.magican.features.voiceaura.audio.VoiceSessionEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceAuraModule {

    @Binds
    @Singleton
    abstract fun bindVoiceSessionEngine(impl: AndroidVoiceSessionEngine): VoiceSessionEngine
}
