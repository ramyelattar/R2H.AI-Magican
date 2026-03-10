package com.r2h.magican.core.sensors.di

import com.r2h.magican.core.sensors.AndroidRotationDetector
import com.r2h.magican.core.sensors.AndroidShakeDetector
import com.r2h.magican.core.sensors.RotationDetector
import com.r2h.magican.core.sensors.ShakeDetector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SensorsModule {

    @Binds
    @Singleton
    abstract fun bindShakeDetector(impl: AndroidShakeDetector): ShakeDetector

    @Binds
    @Singleton
    abstract fun bindRotationDetector(impl: AndroidRotationDetector): RotationDetector
}
