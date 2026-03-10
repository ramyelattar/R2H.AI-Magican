# Security Baseline

## Data Handling
- No raw microphone frame persistence.
- No model prompt/response logging without redaction.
- Encrypted local documents must remain in app-private storage.

## Integrity Controls
- Strict checksum verification for model artifacts.
- Signature validation is mandatory for metadata/artifacts.
- No unsigned artifact activation in any release channel.

## Network Controls
- Certificate pinning required for model/metadata endpoints.
- Pinset requires primary + backup pin with documented rotation.
- Cleartext traffic is disabled unless explicitly justified.

## Permission Controls
- Runtime permission request required before any mic capture path.
- Permission rationale UI required for initial deny and permanent deny.

## Logging Governance
- Structured logs only.
- Redact PII, prompts, document content, model paths containing user identifiers.
- Security events exported with immutable severity and code.

## Dependency Security
- SBOM must be generated for release candidates.
- Vulnerability scan must fail on open High/Critical issues unless exception approved.

## Signing and Release Security
- Release signing config must be present and validated in release pipeline.
- Artifact hash (SHA-256) must be generated and verified at promotion points.

## Threat Modeling
- Threat model required for:
  - mic capture flows
  - local model runtime
  - encrypted vault storage
- Security review signoff required before production promotion.
