# Branch Protection Required Checks

Configure repository branch protection/rulesets so the following checks are required on `main` and `develop`:

- `build-quality-security`
- `verify-release-artifacts` (required for release tags or protected release branches)

Required review rules:
- Minimum 1 reviewer for normal modules.
- Minimum 2 reviewers for `:ai:runtime`, build tooling, and security policy files.
- Security reviewer mandatory for files under `docs/security`, `ai/runtime`, and CI workflows.

Direct pushes to protected branches must be disabled.
