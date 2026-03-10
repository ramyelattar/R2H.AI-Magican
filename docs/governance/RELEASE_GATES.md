# Release Gates

## Required Technical Gates
- Build: `assembleDebug` and release candidate build succeed.
- Quality: `lintDebug`, `detekt`, `ktlintCheck` succeed.
- Security: SBOM generated; vulnerability scan has zero High/Critical unresolved.
- Dependency: duplicate class check passes.
- Performance: macrobenchmark gates pass (startup and key journeys).
- Reliability: crash/ANR budgets in threshold on beta rollout.

## KPI Threshold Gates
- Cold start p50 < 900 ms
- Cold start p95 < 1800 ms
- Crash-free sessions > 99.7%
- ANR < 0.2%
- Model load success > 99.5%
- Integrity failures < 0.1%

## Promotion Path
- Dev -> Internal -> Beta (5% -> 20% -> 50%) -> Production

## Blockers (Hard No-Go)
- Any P0 unresolved bug.
- Any open High/Critical dependency vulnerability without approved exception.
- Missing signing verification.
- Reproducibility mismatch for release artifact.
- Missing permission UX validation for mic-sensitive features.
