package com.r2h.magican.features.tarot.integration.di

import com.r2h.magican.features.tarot.integration.AudioRecordBlowDetector
import com.r2h.magican.features.tarot.integration.BlowDetector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TarotIntegrationModule {

    @Binds
    @Singleton
    abstract fun bindBlowDetector(impl: AudioRecordBlowDetector): BlowDetector
}
