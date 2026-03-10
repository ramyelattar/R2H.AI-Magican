package com.r2h.magican.ai.runtime.di

import com.r2h.magican.ai.runtime.AiRuntime
import com.r2h.magican.ai.runtime.LlamaCppRuntime
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiRuntimeModule {

    @Binds
    @Singleton
    abstract fun bindAiRuntime(impl: LlamaCppRuntime): AiRuntime
}
