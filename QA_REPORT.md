# TraceNet QA Report

**Review date:** 2026-08-24  
**Scope:** Web PWA screens, routes, Firebase operations, reports, media, offline/online behavior, synchronization, authentication, installation, and desktop/mobile browser compatibility.

## Release Status

**Status: BLOCKED / NOT RELEASE-READY**

Static source checks were completed, but end-to-end QA could not be certified because the repository has no configured Firebase environment, seeded test accounts/data, deployable Firestore/Storage Rules, or automated browser/device test suite. The production build is also not currently reproducible locally because dependency installation is incomplete/stalled.

No claim of Lighthouse, device, Firebase, or offline runtime pass is made without executable evidence.

## Static QA Findings

### Route and screen inventory

| Route/screen | Source path | Static result | Runtime result |
| --- | --- | --- | --- |
| Splash and session redirect `/` | `src/App.tsx` | Present | Not executed |
| Welcome `/welcome` | `src/App.tsx` | Present | Not executed |
| Sign in `/sign-in` | `src/App.tsx` | Present | Not executed |
| Sign up `/sign-up` | `src/App.tsx` | Present | Not executed |
| Public portal `/public` | `src/App.tsx` | Present | Not executed |
| Citizen dashboard `/dashboard` | `src/App.tsx` | Protected route present | Not executed |
| New report `/reports/new` | `src/App.tsx` | Protected route present | Not executed |
| New tip `/tips/new` | `src/App.tsx` | Protected route present | Not executed |
| SOS `/sos` | `src/App.tsx` | Protected route present | Not executed |
| Incident map `/map` | `src/App.tsx` | Present | Not executed |
| Emergency contacts `/contacts` | `src/App.tsx` | Present | Not executed |
| Analytics `/analytics` | `src/App.tsx` | Present | Not executed |
| Profile `/profile` | `src/App.tsx` | Protected route present | Not executed |
| About `/about` | `src/App.tsx` | Present | Not executed |
| Law-enforcement operations `/operations` | `src/App.tsx` | Role and approval guard present | Not executed |
| New wanted notice `/operations/wanted/new` | `src/App.tsx` | Role guard present | Not executed |
| Admin `/admin` | `src/App.tsx` | Admin guard present | Not executed |
| Unknown route `*` | `src/App.tsx` | Redirect present | Not executed |

Shared flows requiring separate tests include password reset, password change/reauthentication, Google sign-in, sign-out, account deletion, dialogs, file selection, image preview, video/audio playback, install prompt, update prompt, and offline queue status.

### Firebase operation inventory

| Operation | Implementation | QA status |
| --- | --- | --- |
| Firebase app initialization | `src/services.ts` | Static only; no environment |
| Email/password sign-in | `src/services.ts` | Static only |
| Email/password signup | `src/services.ts` | Static only |
| Google popup sign-in | `src/services.ts` | Static only; OAuth domain unverified |
| Password reset | `src/services.ts` | Static only |
| Reauthentication/password update | `src/services.ts` | Static only |
| Sign-out | `src/services.ts` | Static only |
| Profile read/write | `src/services.ts` | Static only; Rules absent |
| Firestore listeners | `src/services.ts` | Static only; Rules/indexes absent |
| Firestore writes/adds | `src/services.ts` | Static only; Rules absent |
| Firestore persistent cache | `src/services.ts` | Static only; browser not exercised |
| Firebase Storage resumable upload | `src/services.ts` | Static only; Storage Rules absent |
| Download URL validation | `src/services.ts` | Static inspection passed |
| IndexedDB outbox/media jobs | `src/services.ts` | Static inspection passed |
| Background Sync bridge | `src/sw.ts` | Static inspection passed; browser not exercised |
| PWA registration/update | `src/main.tsx` | Static inspection passed; build unavailable |

## Media QA Matrix

The shared media layer contains validation, image compression, resumable upload retry, Blob-backed IndexedDB queueing, object-URL preview cleanup, and video/audio recording helpers. Each item still requires execution with Firebase Storage and real browser permissions.

| Media case | Expected behavior | Status |
| --- | --- | --- |
| JPEG/PNG/WebP image selection | Validate, preview, compress when applicable, upload | Not executed |
| Oversized image | Reject above 15 MiB | Static implementation present |
| MP4/WebM video selection | Validate, preview, playback, upload | Not executed |
| Oversized video | Reject above 50 MiB | Static implementation present |
| Audio selection/recording | Validate, preview, playback, upload | Not executed |
| Camera capture | Browser file capture where supported | Not executed |
| Microphone/camera recording | Permission, recording, stop, Blob output | Not executed |
| Upload progress | Report progress to caller | Static implementation present |
| Transient upload failure | Retry with backoff, retain failed job | Static implementation present |
| Offline upload | Store Blob in IndexedDB, never local URI | Static inspection passed |
| Reconnect upload | Retry queued jobs after `online`/service-worker message | Static implementation present |
| Download/playback | Use HTTPS Firebase Storage URL and native controls | Not executed |
| Unsupported MIME | Reject before upload | Static implementation present |
| Object URL cleanup | Revoke preview URL on component cleanup | Static implementation present |

## Offline and Synchronization QA

| Scenario | Expected result | Status |
| --- | --- | --- |
| Open app after first online load while offline | Precached shell opens | Static service-worker fallback present; not executed |
| Read cached reports offline | Firestore persistent local cache serves prior data | Not executed; requires seeded Firebase data |
| Create report offline | Full report and media persist durably | Incomplete end-to-end wiring: report form must call `queueOfflineReport()` |
| Close/reopen during offline capture | Draft and Blob jobs remain available | Static API present; UI recovery not verified |
| Reconnect | Outbox and media jobs replay | Static processor present; not executed |
| Duplicate replay | One idempotent record, no duplicate report | Not certified; current direct replay needs backend idempotency tests |
| Remote update while local item is pending | Mark conflict, preserve local payload | Static conflict API present; not executed |
| Keep local/discard local | Explicit user resolution without silent loss | API present; no visible conflict UI verified |
| Failed permanent upload | Retain job and actionable error | Static job retention present; UI not verified |
| Multi-tab sync | Single leader / no duplicate uploads | Not certified; no navigator-lock leader is currently verified |
| Storage quota exhaustion | Preserve recoverable draft and explain failure | Not executed |

## Authentication and Authorization QA

| Case | Expected result | Status |
| --- | --- | --- |
| Anonymous public browsing | Only public-safe data visible | Not certifiable: Firestore Rules missing |
| Invalid credentials | Clear failure, no session | Not executed |
| Citizen route access | Citizen routes available, admin/operations denied | Client guard present; backend authorization unverified |
| Pending law-enforcer | Citizen experience until approval | Client guard present; backend claims unverified |
| Approved law-enforcer | Operations available | Client guard present; backend claims unverified |
| Admin | Admin route available | Client guard present; backend claims unverified |
| Modified client / direct SDK call | Privileged write rejected | **Fail/blocker:** Rules and Functions absent |
| Sign-out on shared device | User-specific local data cleared | Not verified end-to-end |
| Google OAuth | Authorized domain and redirect succeed | Blocked until Firebase configuration |
| Password reset/change | Auth email and reauthentication succeed | Blocked until Firebase configuration |

## PWA Installation and Platform QA

| Platform/browser | Required checks | Status |
| --- | --- | --- |
| Android Chrome | Manifest, install prompt, standalone launch, shortcuts, offline shell, update | Not executed |
| iPhone Safari | Apple metadata, Share > Add to Home Screen, standalone launch, offline shell | Not executed |
| Windows Edge/Chrome | Installability, shortcuts, update, offline shell | Not executed |
| macOS Safari/Chrome/Edge | Installability where supported, standalone behavior, update | Not executed |
| Linux Chromium/Firefox | Installability where supported, service-worker scope, offline shell | Not executed |
| Desktop responsive layouts | No overflow, keyboard navigation, dialogs, media controls | Not executed |
| Tablet responsive layouts | Touch targets, orientation, media capture, route navigation | Not executed |

## Build and Tooling Checks

- Source diagnostics passed for the recently edited PWA/media files.
- The earlier malformed JSX handlers in `src/App.tsx` were corrected during this work.
- `npm run build` was attempted but could not complete because the local dependency tree is empty/incomplete and npm installation stalled.
- A committed `package-lock.json` is still required for the GitHub Actions `npm ci` workflow.
- No Lighthouse run was possible, so target scores of Performance 95+, PWA 100, Accessibility 95+, and Best Practices 100 are unverified.

## Blocking Defects Before Release

1. Add and deploy tested `firestore.rules` and `storage.rules`.
2. Add Firebase Functions for privileged commands, idempotency, audit logging, and server-side validation.
3. Wire the report form to the atomic offline report draft API and build visible pending/conflict recovery UI.
4. Add a reproducible lockfile and restore dependencies so the production build succeeds.
5. Run browser automation against Firebase Emulator Suite with seeded accounts for every role and route.
6. Run Lighthouse on the deployed HTTPS build from a clean profile and record the four category scores.
7. Execute physical-device tests for Android Chrome and iPhone Safari, plus desktop/tablet browser tests.
8. Verify deployed headers, manifest, service-worker scope, cache invalidation, deep links, Firebase OAuth domains, and Storage download playback.

## Final Recommendation

Do not mark TraceNet production-ready from static inspection alone. The architecture and client-side test surfaces are present, but backend security, runtime integration, offline report recovery, reproducible build dependencies, and cross-platform execution evidence remain release gates.
