# Contributing

## Required status checks

For protected branches, configure GitHub branch protection to require the orchestrator gate check before merge:

- Workflow: `Orchestrator Gate`
- Job/check: `Orchestrator Gate (ubuntu-latest)`
- Job/check: `Orchestrator Gate (windows-latest)`

This check is defined in `.github/workflows/orchestrator-gate.yml` and runs:

- `./gradlew --no-daemon orchestratorGate`

Local equivalent:

- `./gradlew orchestratorGate`
- `.\gradlew.bat orchestratorGate`
