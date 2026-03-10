package com.r2h.magican.features.library.di

import com.r2h.magican.features.library.data.AiRuntimePdfSummarizer
import com.r2h.magican.features.library.data.AndroidPdfMetadataExtractor
import com.r2h.magican.features.library.data.HeuristicPdfTextExtractor
import com.r2h.magican.features.library.data.LocalPdfSummarizer
import com.r2h.magican.features.library.data.PdfMetadataExtractor
import com.r2h.magican.features.library.data.PdfTextExtractor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryModule {

    @Binds
    @Singleton
    abstract fun bindMetadataExtractor(impl: AndroidPdfMetadataExtractor): PdfMetadataExtractor

    @Binds
    @Singleton
    abstract fun bindTextExtractor(impl: HeuristicPdfTextExtractor): PdfTextExtractor

    @Binds
    @Singleton
    abstract fun bindSummarizer(impl: AiRuntimePdfSummarizer): LocalPdfSummarizer
}
