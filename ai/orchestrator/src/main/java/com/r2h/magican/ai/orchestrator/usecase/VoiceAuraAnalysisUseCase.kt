package com.r2h.magican.ai.orchestrator.usecase

import com.r2h.magican.ai.orchestrator.MagicanOrchestrator
import com.r2h.magican.ai.orchestrator.VoiceAuraInput
import com.r2h.magican.ai.orchestrator.domain.ResponseMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceAuraAnalysisUseCase @Inject constructor(
    orchestrator: MagicanOrchestrator,
    mapper: ResponseMapper
) : SimpleAiUseCase<VoiceAuraInput>(orchestrator, mapper)
