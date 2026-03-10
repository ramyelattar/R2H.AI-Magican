# Threat Model Template (Mic + Local AI + Storage)

## 1. System Context
- Feature:
- Data classes involved:
- Entry points:
- Trust boundaries:

## 2. Assets
- User audio data
- Prompt/context payloads
- Model artifacts + metadata
- Encrypted document files + index

## 3. Data Flow
- Source -> processor -> storage -> output
- Include all temporary files, in-memory buffers, and telemetry sinks

## 4. Threat Scenarios
- Artifact tampering (metadata/model)
- Prompt injection and unsafe output shaping
- Microphone permission misuse
- Data exfiltration from encrypted vault
- Logging leakage of sensitive content
- Rollback abuse / downgrade attack

## 5. Controls
- Integrity checks (checksum + signature)
- Certificate pinning
- Permission gating + rationale
- Redacted logging policy
- Encrypted at-rest storage
- Runtime feature gating by model state

## 6. Residual Risk Assessment
- Threat:
- Likelihood:
- Impact:
- Residual score:
- Mitigation owner:

## 7. Verification
- Unit tests:
- Integration tests:
- Security tests:
- Observability alerts:

## 8. Signoff
- Feature owner:
- Security reviewer:
- Date:
