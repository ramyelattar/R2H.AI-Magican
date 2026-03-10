package com.r2h.magican.ai.orchestrator.usecase

import com.r2h.magican.ai.orchestrator.MagicanOrchestrator
import com.r2h.magican.ai.orchestrator.PalmInput
import com.r2h.magican.ai.orchestrator.domain.ResponseMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PalmReadingUseCase @Inject constructor(
    orchestrator: MagicanOrchestrator,
    mapper: ResponseMapper
) : SimpleAiUseCase<PalmInput>(orchestrator, mapper)
