package com.r2h.magican.core.haptics.di

import com.r2h.magican.core.haptics.AndroidHaptics
import com.r2h.magican.core.haptics.Haptics
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HapticsModule {

    @Binds
    @Singleton
    abstract fun bindHaptics(impl: AndroidHaptics): Haptics
}
