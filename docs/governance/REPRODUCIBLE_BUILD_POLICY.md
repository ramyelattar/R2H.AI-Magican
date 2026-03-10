# Reproducible Build Policy

## Build Determinism Requirements
- Gradle wrapper version pinned and checked in.
- Version catalog is single source of dependency versions.
- Dependency locking files are required and reviewed in PRs.
- JDK/toolchain version pinned in CI.

## Verification Process
1. Build release artifact twice from same commit in clean environments.
2. Compare SHA-256 hashes.
3. Hash mismatch is a release blocker.

## Allowed Variance
- No variance permitted for signed release artifacts.

## Enforcement
- CI release pipeline runs reproducibility check before promotion to beta/production.
