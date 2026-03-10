package com.r2h.magican.ai.orchestrator.usecase

import com.r2h.magican.ai.orchestrator.FeatureInput
import com.r2h.magican.ai.orchestrator.MagicanOrchestrator
import com.r2h.magican.ai.orchestrator.domain.AiResponse
import com.r2h.magican.ai.orchestrator.domain.ResponseContent
import com.r2h.magican.ai.orchestrator.domain.ResponseMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base use-case for AI-powered features.
 * Encapsulates orchestration + response mapping, removing JSON handling from ViewModels.
 * 
 * Subclasses provide feature-specific input construction and optional typed transformations.
 */
abstract class BaseAiUseCase<TInput : FeatureInput, TTyped>(
    private val orchestrator: MagicanOrchestrator,
    private val mapper: ResponseMapper
) {

    /**
     * Executes AI orchestration with automatic response parsing.
     * Returns typed AiResponse with success/blocked/error handling.
     */
    suspend fun execute(input: TInput): AiResponse<TTyped?> {
        return withContext(Dispatchers.Default) {
            val raw = orchestrator.orchestrate(input)
            mapper.map(raw, contentTransformer = ::transformContent)
        }
    }

    /**
     * Optional: Transform generic ResponseContent into feature-specific typed data.
     * Default implementation returns null (no additional typing).
     */
    protected open fun transformContent(content: ResponseContent): TTyped? = null
}

/**
 * Simple use-case that doesn't require additional typing beyond ResponseContent.
 */
abstract class SimpleAiUseCase<TInput : FeatureInput>(
    orchestrator: MagicanOrchestrator,
    mapper: ResponseMapper
) : BaseAiUseCase<TInput, Nothing>(orchestrator, mapper)
