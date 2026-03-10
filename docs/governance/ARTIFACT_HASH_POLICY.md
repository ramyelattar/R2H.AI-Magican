# Artifact Hash Policy

## Required Artifacts
- AAB/APK release artifacts
- Model metadata manifest
- Model binary artifacts

## Algorithm
- SHA-256 (hex lowercase)

## Verification Points
- Post-build artifact generation
- Pre-promotion validation
- Post-download verification on client for model artifacts

## Storage
- Hash manifests stored as immutable CI artifacts and release attachments.

## Failure Policy
- Hash mismatch immediately blocks promotion and triggers security incident workflow.
