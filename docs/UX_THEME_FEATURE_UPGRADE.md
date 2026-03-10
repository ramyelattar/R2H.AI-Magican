# UX, Theme, and Product Upgrade Pass

## Delivered

### Theme upgrades
- `MysticTheme` now defaults to system dark mode instead of forcing dark mode.
- Added dynamic Material You color support on Android 12+ (`dynamicColor = true`).
- Preserved existing neon/glass token system for visual brand identity.

### User experience upgrades
- Home screen redesigned as a discovery hub:
  - Feature search field
  - Rich feature cards with descriptions
  - Continue-last-feature shortcut
  - Theme mode controls (System / Dark / Light)
- Feature screens now include an explicit “Back to Home” action for faster navigation.

### Shared component upgrade
- `NeonButton` now supports loading state (`isLoading`) with inline progress indicator and automatic disable while working.

### App-level improvements
- Navigation remembers last opened feature within app session/state restoration.
- Home copy and structure now guide users toward actions with less friction.

## Validation
- Built successfully with `:app:assembleDebug` after changes.

## Files changed
- `core/design/theme/MysticTheme.kt`
- `core/design/components/NeonButton.kt`
- `app/MainActivity.kt`
