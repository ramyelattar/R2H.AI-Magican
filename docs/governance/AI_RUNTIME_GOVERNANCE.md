# AI Runtime Governance

## Runtime Responsibility
- `:ai:runtime` is the only authority for model installation, validation, activation, and inference execution.
- `:ai:orchestrator` is stateless with respect to model artifact lifecycle.

## Runtime State Model
- `NotInstalled`
- `Downloading`
- `Verifying`
- `Installed`
- `Loading`
- `Ready`
- `Degraded`
- `Rollback`

## Transition Policy
- Inference allowed only in `Ready`.
- Any integrity failure transitions to `Degraded` then `Rollback` if previous verified artifact exists.
- `Rollback` can transition to `Ready` or `NotInstalled`.

## ModelArtifactResolver Contract
- Resolver input:
  - model id
  - device ABIs
  - app version
  - runtime schema version
- Resolver output:
  - exact artifact descriptor + signed metadata
- Hard checks:
  - version compatibility
  - abi compatibility
  - sha256 checksum
  - signature verification

## Operational SLOs
- Model load success rate > 99.5%
- Warm load p95 < 1500 ms
- Cold load p95 < 5000 ms
- Integrity failures < 0.1%

## Fallback Policy
- Production builds must not use permissive integrity fallback.
- If model is unavailable, features enter explicit gated mode (`AI_UNAVAILABLE`) with safe UX fallback.

## Telemetry
- Required event families:
  - `AI-DL-*` download
  - `AI-INT-*` integrity
  - `AI-LD-*` loading
  - `AI-INF-*` inference
  - `AI-RB-*` rollback

## Rollback Policy
- Keep active + previous verified artifacts.
- Auto rollback after repeated load failures (3 failures in 10 minutes).
- Block re-activation of failed hash until metadata revision changes.
