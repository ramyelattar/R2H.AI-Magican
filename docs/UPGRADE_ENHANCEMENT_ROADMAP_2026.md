# Upgrade and Enhancement Roadmap (2026)

## Phase 1 (0-2 weeks): Reliability and Contract Hardening
- Goal: Make orchestration, startup, and degraded-mode behavior predictable and test-backed.
- Scope:
- Add contract-resilience tests for encoder/mapper malformed output handling.
- Expand safety regression suite for tokenizer/classifier normalization edge cases.
- Keep cancellation-safe coroutine handling across startup and feature view models.
- Ensure module-level test execution stability on Windows test workers.
- Exit criteria:
- `:ai:orchestrator:testDebugUnitTest` green with new resilience tests.
- Target modules compile cleanly in debug (`runtime`, `orchestrator`, `library`, `tarot`).

### Phase 1 status
- In progress.
- Implemented:
- Orchestrator contract-resilience tests (`OrchestratorContractResilienceTest`).
- Safety harmful phrase normalization fix (`build bomb` path).
- Windows test PATH normalization for orchestrator test workers.

## Phase 2 (2-6 weeks): Product and UX Upgrades
- Goal: Improve user-perceived quality and trust for AI features.
- Scope:
- Streaming AI response UI (token-by-token) with cancel/retry.
- Degraded/offline UX badges and actionable recovery hints in feature screens.
- Tone-style presets (`direct`, `gentle`, `actionable`) wired into prompt templates.
- Telemetry quality improvements (token estimates, error taxonomy dashboards).
- Exit criteria:
- Streaming enabled for at least Tarot + Library summarize.
- Degraded-mode UX consistency in all AI-backed features.

## Phase 3 (6-12 weeks): Platform and Security Modernization
- Goal: Raise security and maintainability baseline for long-term scale.
- Scope:
- Migrate deprecated Android crypto APIs to supported replacements.
- Add signed model manifest/update pipeline with rollback.
- Introduce encrypted local feature memory and optional Library-grounded responses.
- Evaluate `kapt` to `ksp` migration path per module.
- Exit criteria:
- Crypto deprecation backlog closed.
- Model delivery flow supports integrity, versioning, and rollback.

## Immediate next implementation items
1. Add multilingual/red-team safety regression cases (`Arabic`, obfuscated English patterns).
2. Add schema fuzz tests for `JsonStyleEncoder` and `ResponseMapper`.
3. Add app-level assembly validation gate to CI (`assembleDebug` + orchestrator unit tests).
