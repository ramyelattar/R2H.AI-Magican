# Phase L4: UI Performance Optimization

## Implemented

### 1) Lifecycle-aware UI state collection
All Compose feature screens now use lifecycle-aware state collection so they stop collecting flows when not visible.

- Replaced `collectAsState()` with `collectAsStateWithLifecycle()` in all feature screens.
- Added `androidx.lifecycle:lifecycle-runtime-compose` to all feature modules with Compose screens.

### 2) Reduced continuous background rendering cost
`MysticScaffold` now defaults fog animation to static mode.

- Added `fogAnimated` parameter (default `false`) to `MysticScaffold`.
- Wired `FogOverlay(animated = fogAnimated)`.
- `FogOverlay` now creates infinite transition only when animation is enabled.

### 3) Reduced list item recomposition work
`LibraryScreen` document card now memoizes derived display strings.

- Memoized metadata line formatting.
- Memoized bookmark summary line generation.

## Why this improves performance

- Prevents off-screen flow collection and unnecessary recompositions.
- Avoids always-on full-screen animation work by default.
- Reduces per-item string allocation in lazy lists.

## Validation status

- Static error check: clean for changed files.
- Full Gradle compilation: blocked in current environment due missing JDK paths on this machine.
