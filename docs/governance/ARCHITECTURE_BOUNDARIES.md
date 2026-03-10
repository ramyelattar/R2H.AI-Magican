# Architecture Boundaries

## Module Contract
- `:app` owns bootstrap only: navigation shell, top-level theming, permission entry points, feature routing.
- `:core:*` exposes reusable primitives only; no feature-specific business logic.
- `:ai:runtime` owns model lifecycle + inference engine only.
- `:ai:orchestrator` owns prompt construction, guardrails, response normalization; it must not access Android UI.
- `:features:*` own user journeys and presentation state. They can depend on `:core:*` and `:ai:*` APIs, never on other feature internals.

## Forbidden Dependencies
- `:features:* -> :features:*` (cross-feature direct dependency) is forbidden.
- `:features:* -> implementation classes in :ai:runtime` is forbidden; use `AiRuntime` interface only.
- `:app -> feature internal data classes` is forbidden; app consumes route entry composables only.
- `:core:* -> :features:*` is forbidden.

## Layering Rules Inside Feature Modules
- `presentation` depends on `domain` contracts.
- `data` depends on `domain` and external systems.
- `presentation` must not parse raw JSON directly from runtime output; use shared parser/use case.

## Startup Rules
- App startup must not instantiate all feature screens.
- Sensors, mic, audio recorders, and model loading are lazy and user-triggered only.

## Ownership
- Platform team: `:app`, `:core:*`, build tooling.
- AI runtime team: `:ai:*`.
- Feature team: `:features:*`.
- Security team required reviewer for `:ai:runtime`, `:core:security` (when introduced), and release policy files.

## Enforcement
- CI blocks forbidden project dependencies.
- CI blocks direct imports from forbidden packages via static grep checks.
- Branch protection must require all Phase-1 quality checks before merge.
