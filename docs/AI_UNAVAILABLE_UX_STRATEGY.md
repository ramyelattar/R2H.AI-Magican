# AI Unavailable UX Strategy

**Version**: 1.0  
**Status**: Production Guidance  
**Last Updated**: 2026-03-01

## Overview

This document defines the user experience strategy for handling AI runtime unavailability in R2H.AI-Magican. The app must maintain luxury-grade UX even when the AI inference engine is offline, degraded, or failed.

---

## Principles

1. **Never Silent Failure**: Users must always know when AI is unavailable
2. **Graceful Degradation**: Provide meaningful alternatives when possible
3. **Transparent Recovery**: Show clear recovery actions
4. **Luxury Consistency**: Maintain premium aesthetics and tone even in error states

---

## AI Capability States

| State | User Message | UI Treatment | Available Actions |
|-------|--------------|--------------|-------------------|
| **Unloaded** | "Preparing your mystical advisor..." | Shimmer loading | Wait (auto-advances) |
| **Loading** | "Awakening the oracle..." | Progress indicator | Wait (auto-advances) |
| **Ready** | (No message) | Normal operation | All features enabled |
| **Degraded** | "Limited insights available" | Warning banner | Continue with warning |
| **Failed** | "Unable to connect to mystical realm" | Error state | Retry / Contact support |

---

## Feature-Specific Fallback Strategies

### Tarot Reading
**Primary UX**: AI-generated interpretation  
**Fallback UX**: 
- Show card spreads with traditional meanings (static content)
- Display: _"Your cards have been drawn. Full mystical interpretation requires oracle connection. [Reconnect]"_
- User can save spread for later interpretation

### Voice Aura Analysis
**Primary UX**: AI analysis of voice recording  
**Fallback UX**:
- Recording still captured and saved
- Display: _"Your voice signature has been preserved. Analysis will complete when the oracle awakens. [Check Status]"_
- Queue for processing when runtime becomes available

### Palm Reading
**Primary UX**: AI analysis of palm photo  
**Fallback UX**:
- Photo captured and saved to library
- Display: _"Your palm has been captured. Interpretation pending oracle availability. [View Library]"_

### Dream Interpretation
**Primary UX**: AI-generated analysis  
**Fallback UX**:
- Save dream journal entry with timestamp
- Display: _"Your dream has been recorded. Mystical analysis will arrive when energies align. [Journal]"_

### Horoscope
**Primary UX**: Personalized AI horoscope  
**Fallback UX**:
- Show generic daily horoscope (static content bundled in app)
- Display: _"General cosmic guidance available. Personalized reading requires oracle connection. [Retry]"_

### Compatibility Analysis
**Primary UX**: AI-generated compatibility report  
**Fallback UX**:
- Show basic zodiac compatibility chart (static)
- Display: _"Traditional compatibility shown. Deep mystical insights require oracle connection. [Retry]"_

### Birth Chart
**Primary UX**: AI-interpreted chart  
**Fallback UX**:
- Generate and display chart visualization (algorithmic, no AI)
- Display: _"Your celestial map is ready. Oracle interpretation pending. [Save Chart]"_

### Library (PDF Vault)
**AI Dependency**: None (purely storage)  
**Behavior**: Always available

---

## Error Message Catalog

### Runtime Unavailable (Release-Required Mode)
```
Title: Oracle Connection Required
Message: This version requires an active mystical connection. Please update to the latest version or contact support.
Actions: [Update App] [Contact Support]
Tone: Apologetic, helpful
```

### Runtime Unavailable (Startup Initialization)
```
Title: Preparing Your Experience
Message: Awakening the mystical oracle... This may take a moment on first launch.
Actions: [Auto-advances when ready]
Tone: Reassuring, patient
```

### Self-Test Failed
```
Title: Oracle Calibration Issue
Message: We're experiencing difficulty connecting to the mystical realm. Your readings may be delayed.
Actions: [Retry Now] [Continue with Limited Features]
Tone: Transparent, professional
```

### Model Verification Failed
```
Title: Integrity Check Incomplete
Message: Unable to verify mystical oracle authenticity. Please reinstall or contact support.
Actions: [Contact Support] [Learn More]
Tone: Serious, security-focused
```

### Inference Timeout
```
Title: Oracle Processing Delay
Message: Your reading is taking longer than expected. The cosmos may be congested.
Actions: [Wait] [Cancel and Retry]
Tone: Light, mystical
```

### Schema Validation Failed
```
Title: Mystical Translation Error
Message: The oracle's message couldn't be properly interpreted. This has been logged for improvement.
Actions: [Try Again] [Report Issue]
Tone: Transparent, improvement-focused
```

### Safety Policy Blocked
```
Title: Request Declined
Message: This query contains patterns incompatible with our mystical guidance principles.
Actions: [Rephrase Question] [Learn About Guidelines]
Tone: Firm but respectful
```

---

## UI Components

### Availability Banner (Persistent)
**Location**: Top of feature screens when not Ready  
**Visual**: Subtle gradient banner matching feature theme  
**Content**: State-appropriate message + action button  
**Behavior**: Auto-dismisses when state becomes Ready

### Retry Button
**Primary**: "Reconnect to Oracle"  
**Secondary**: "Try Again"  
**Loading State**: "Connecting..."  
**Success State**: Auto-dismisses + subtle success animation

### Status Indicator (Settings)
**Location**: Settings > About > System Status  
**Content**:
```
AI Oracle Status
━━━━━━━━━━━━━━━━━━━━━━━
● Ready / ⚠ Degraded / ✕ Unavailable
Model: llama-2-7b-mystical-q4 v1.2
Last Update: 2 hours ago
[Run Diagnostics]
```

---

## Recovery Flows

### Automatic Recovery
1. Runtime becomes available → Background self-test runs
2. If successful → Silently enable features + update UI
3. If failed → Show error banner with manual retry

### Manual Recovery (User-Initiated)
1. User taps "Reconnect" or "Retry"
2. Show loading indicator: "Awakening oracle..."
3. Run startup self-test (15s timeout)
4. Success → Enable features + show subtle success toast
5. Failure → Show specific error message + support action

### Queued Processing
For features that queue work (Voice Aura, Palm):
1. When runtime becomes Ready → Process queue automatically
2. Show notification: "Your voice aura analysis is complete"
3. Tap notification → Open result screen

---

## Accessibility Considerations

- All error states must have descriptive content for screen readers
- Retry buttons must be large enough (min 48dp touch target)
- Loading states must announce progress changes
- Error messages must not rely solely on color

---

## Analytics Events to Track

```kotlin
TelemetryEvent.UserExperiencedUnavailability(
    feature: String,
    state: AiCapabilityState,
    userAction: "dismissed" | "retried" | "contacted_support"
)

TelemetryEvent.RecoveryAttempt(
    trigger: "auto" | "manual",
    success: Boolean,
    durationMs: Long
)
```

---

## Testing Checklist

- [ ] Force each capability state and verify UX matches spec
- [ ] Test auto-recovery from Failed → Ready transition
- [ ] Verify queued processing for Voice/Palm when recovery happens
- [ ] Test all error message catalogs in UI
- [ ] Validate accessibility with TalkBack enabled
- [ ] Verify retry button debouncing (prevent spam)
- [ ] Test startup cold-launch with model verification failure
- [ ] Validate Settings > System Status accuracy

---

## Future Enhancements (Post-L1)

1. **Offline Mode Indicator**: Persistent status bar icon
2. **Smart Retry**: Exponential backoff for automatic retries
3. **Background Sync**: Process queued items when app in background
4. **Network-Based Fallback**: Cloud inference if local runtime fails (privacy-respecting)

---

## Support Resources

**User Documentation**: `/docs/user/troubleshooting_ai.md`  
**Developer Guide**: `/docs/dev/ai_runtime_integration.md`  
**Support Portal**: https://support.r2h.ai/magican/oracle-status
