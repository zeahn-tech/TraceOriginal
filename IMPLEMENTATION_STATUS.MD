# TraceNet — Implementation Status

**Last updated:** during Phase 7 (Abuse Protection), mid-implementation.
**Purpose:** single source of truth for what's been done, what's verified vs. not, and exactly what to pick up next. Read this before starting any new work on this project.

---

## How to read "verified"

Three distinct levels appear throughout this document, and they are not interchangeable:

- **Executed & passing** — a real test suite was actually run in this environment and passed. Strongest claim.
- **Written, type-checked, not executed** — the code/tests compile and load correctly, but could not be run against a real Firebase emulator because this sandbox cannot reach `storage.googleapis.com` (confirmed by actually attempting it — 403 Forbidden, not assumed).
- **Manually reviewed only** — reasoned through carefully, sometimes catching real bugs this way, but with no automated confirmation at all.

Every phase below states which level applies to which piece. Don't upgrade a "written, not executed" claim to "verified" without actually running it.

---

## Phase-by-phase status

### 1. Forensic Audit — ✅ Complete
**Deliverable:** `FINAL_FORENSIC_AUDIT.md`
Full line-by-line audit of the original codebase. Found the build was completely broken, a runtime-crashing `Select` component bug, no Firestore/Storage rules, offline reporting silently broken for any report with attached media, and ~15 other issues classified Critical/High/Medium/Low with exact file/line evidence.

### 2. Build Verification — ✅ Complete, executed & passing
**Deliverable:** `BUILD_VERIFICATION_REPORT.md`
Fixed the `Select` crash, missing `vite-env.d.ts`, `sw.ts` DOM/WebWorker lib conflict, npm peer-dependency conflict (removed unused `@vite-pwa/assets-generator`), added ESLint. `npm ci` / `npm run build` / `npm run lint` / `npm run typecheck` all pass clean from a genuinely clean environment.

### 3. Media Production Pipeline — ✅ Complete, executed & passing
**Deliverable:** `MEDIA_PRODUCTION_REPORT.md`
Full rewrite of `src/services.ts`'s media pipeline: durable `LOCAL_PENDING → UPLOADING → UPLOADED → REPORT_PENDING → SYNCED/FAILED/CONFLICT` state machine, deterministic ids (duplicate prevention), correct MIME/metadata, retry with backoff, large-file limits, lazy loading + missing-media fallback (`components/MediaAsset.tsx`). 19/19 tests in `src/services.test.ts`, run against mocked Firebase with a real IndexedDB (`fake-indexeddb`).

### 4. True Offline-First Reporting — ✅ Complete, executed & passing
**Deliverable:** `OFFLINE_REPORTING_PRODUCTION.md`
Verified (most infrastructure already existed from Phase 3) and added an explicit, literal 15-step demonstration test (`src/offline-reporting.test.ts`) walking through open-offline → compose → submit → simulated browser restart → reconnect → sync, plus a concurrent double-submit idempotency test. 21/21 total app tests passing at this point.

### 5. Firebase Security Rules — ⚠️ Written & reasoned through carefully, NOT emulator-executed
**Deliverable:** `FIREBASE_SECURITY_IMPLEMENTATION.md`, `firebase/firestore.rules`, `firebase/storage.rules`, `firebase/emulator-tests/`
Full role-based (CITIZEN/LAW_ENFORCER/ADMIN, approval-aware) authorization for every collection and every storage path. Required real client-side changes too — Firestore rejects unfiltered `list` queries that aren't provably safe, so `services.ts`/`App.tsx` had to be changed to issue role-scoped queries (`myReports` vs `allReports` vs `publicReports`, etc.) instead of one universal unfiltered listener.
**Verification gap:** the actual Firestore/Storage emulators cannot be downloaded in this sandbox (`storage.googleapis.com` is not in the network allowlist — confirmed by actually running `firebase emulators:start` and getting a 403). 72 emulator tests were written (later grown to 79, see Phase 7) but never executed here. Manual review caught two real bugs before delivery (an unsupported `.toSet()/.difference()` construct, and unsafe dot-access on genuinely-absent optional fields) — this is exactly the class of bug an emulator run is supposed to catch mechanically, which is why this remains explicitly uncertified.
**Action needed from the user:** run `npm run test:security` from a machine with normal internet access; report back the pass/fail output.

### 6. Firebase Cloud Functions — ✅ Business logic executed & passing; integration layer written, not executed
**Deliverable:** `FIREBASE_FUNCTIONS_ARCHITECTURE.md`, `firebase/functions/`
Six Cloud Functions (`approveOfficer`, `setUserRole`, `setUserStatus`, `verifyWantedNotice`, `publishAlert`, `transitionReportStatus`), each re-deriving the caller's role server-side from Firestore (never trusting client-supplied claims), wrapped in a shared idempotency mechanism, writing structured `audit_logs` entries. `firestore.rules` was tightened in the same pass so `role`/`isApproved`/`status`/report-`status`/wanted-`isVerified` are exclusively Function-managed — not even an admin's own direct write can touch them anymore.
**Verified:** 54/54 tests, executed offline via `firebase-functions-test`'s `wrap()` against a hand-built in-memory Firestore mock (`firebase/functions/test/`) — this genuinely runs the real handler code, not a simulation. Caught real bugs in the test harness itself before delivery.
**Not verified:** `firebase/functions/test-emulator/integration.test.ts` (real Auth token → real HTTPS call → real Firestore) is written and type-checks but has never been executed — same emulator-network blocker as Phase 5.

### 7. Abuse Protection — ✅ Complete
**Deliverable:** `ABUSE_PROTECTION_REPORT.md`

**Completed so far, this phase (all re-verified passing just before this status doc was written):**
- **Firebase App Check** — `initializeAppCheck` wired in `services.ts` (dynamic import, only loads if `VITE_RECAPTCHA_SITE_KEY` is configured — a public reCAPTCHA v3 *site* key, not a secret). `enforceAppCheck: true` added to all 6 Cloud Functions.
- **Per-user daily submission quotas** (reports: 10/day, tips: 15/day, alerts/SOS: 5/day) — implemented as an atomically-enforced mechanism: `services.ts#writeWithQuota()` performs a Firestore transaction that creates the document AND increments a `rate_limits/{uid}_{kind}` counter together; `firestore.rules` independently re-derives and re-checks the same constraint via `getAfter()`, so a client cannot bypass this by skipping the transaction. Wired into report creation (`processDraft`), tip creation (`TipForm`), and SOS alert creation (`SOS` component — which also needed `user` threaded into its route props, a pre-existing gap).
- **Field-length and media-count validation** in `firestore.rules` (title ≤200 chars, description/content ≤5000 chars, reports ≤6 total attached media, wanted notices ≤4 photos) — cost-abuse protection.
- **Cloud Functions rate limiting** — new `firebase/functions/src/lib/rateLimit.ts`, a separate mechanism from the client-facing quotas above, protecting the *privileged Functions themselves* against a compromised/scripted admin account (approveOfficer/setUserRole/setUserStatus: 30/hr, verifyWantedNotice: 60/hr, publishAlert: 10/hr, transitionReportStatus: 120/hr).
- **Retry attempt ceiling** — `MAX_RETRY_ATTEMPTS = 5` now actually enforced (the `attempts` counter existed since Phase 3 but was never checked against a limit). `processDraft` refuses to auto-retry, `retryDraft` refuses to reset, and `PendingDrafts` in `App.tsx` only shows the Retry button while attempts remain, Discard-only once exhausted.
- **New/updated tests, all executed & passing:**
  - `src/services.test.ts`: 21/21 (added quota-exceeded and retry-ceiling tests)
  - `src/offline-reporting.test.ts`: 2/2 (unaffected)
  - `firebase/functions/test/`: 54/54 (all 6 functions re-verified after adding App Check + rate limiting; two real bugs found and fixed along the way — see below)
  - `firebase/emulator-tests/firestore.rules.test.ts`: grown to 65 tests (added `rate_limits` collection rules + quota-mechanism tests + field-length tests), type-checks and loads correctly (confirmed via the same "fails only on ECONNREFUSED" check used throughout this project), but — like the rest of Phase 5 — not executed against a real emulator

**Real bugs found and fixed during this phase:** see `ABUSE_PROTECTION_REPORT.md` §11 for the full list (three, all caught by actually running something — `tsc`, the offline test suite, or a file-load check — not by reasoning alone).

**Authentication abuse protection:** added `authErrorMessage()` to `services.ts`, translating Firebase Auth error codes into clear, actionable messages — most importantly `auth/too-many-requests` (Firebase's own automatic, Google-managed lockout after repeated failed attempts; not something this app configures, only something it must surface well instead of a raw `"Firebase: Error (auth/too-many-requests)."` string). Wired into all four Auth call sites: `SignIn`, `SignUp`, `ResetDialog`, change-password. Also documents (in the report) the two Console-level settings that can't be set from code: App Check enforcement toggle per-service, and Auth password policy.

**Final full-gauntlet re-verification, all green:**
- Main app: `npm ci` / `build` / `lint` / `typecheck` / `test` → 23/23 tests passing (`services.test.ts` 21, `offline-reporting.test.ts` 2)
- Cloud Functions: `tsc --noEmit` clean, 54/54 offline tests passing
- Emulator test suites (Phase 5 + this phase's additions): 88 tests total (65 Firestore + 23 Storage) confirmed structurally sound (load and fail only on the expected `ECONNREFUSED`), consistent with every prior phase — still not executed against a real emulator
- Verified directly (not assumed) that App Check's code is dead-code-eliminated from the production bundle entirely when `VITE_RECAPTCHA_SITE_KEY` is unset, and loads as its own ~22 KB async chunk when it is set — built both ways and inspected the actual output

**Phase complete.** Next work on this project should start a new phase (not yet scoped) or close out the permanently-open items below if real emulator/network access becomes available.

---

### 8. Production PWA Hardening — ✅ Complete, executed & passing
**Deliverable:** `PWA_PRODUCTION_VALIDATION.md`
Full audit of the manifest, service worker, Workbox config, and GitHub Pages deployment plumbing. Found and fixed three real, currently-live bugs: (1) `404.html` — the file GitHub Pages deep-link recovery depends on — lived in the project root instead of `public/`, so it never actually reached the deployed site at all; (2) `sw.ts` called `self.skipWaiting()` unconditionally, completely bypassing the configured `registerType:'prompt'` update-consent flow and risking mid-session version skew, compounded by `InstallPrompt`'s Refresh button never actually invoking the real update handshake; (3) the runtime image-caching route had no origin restriction, meaning it could (and likely did) cache private Firebase Storage evidence photos past their authorization window, since the Cache API has no concept of `storage.rules`' per-request access control. Also replaced the SVG-only icon set with a production PNG set (including a purpose-built maskable variant, verified via simulated circle/squircle mask rendering — not just reasoned about geometrically), fixed the broken iOS `apple-touch-icon` reference, added missing favicon links, and split the manifest's incorrectly-combined `"any maskable"` icon entry into proper separate entries.

**Verified:** 25/25 tests in `src/pwa-build.test.ts` (`npm run test:pwa`), which runs a real production build (GitHub-Pages-style base path included) and inspects the actual generated manifest/HTML/service-worker/icon output — no mocking, no emulator dependency. Two real false-positive/false-negative bugs were caught and fixed in the test suite itself along the way (assertions built on identifiers that production minification renames away) — documented in the report as a methodology note, consistent with this project's running theme that untested assumptions about generated output are exactly the kind of thing that quietly breaks.

**Also fixed, incidentally:** `vitest` was pinned to `^4.1.11`, which triggers a reproducible `npm`/Arborist crash on a fully clean install (confirmed with a cleared cache, not a fluke). Reverted to `^3.2.4` — the version used reliably across every prior phase — after confirming no test code needed anything v4-specific. `npm ci` re-confirmed clean afterward.

**Not verified (requires a real browser/device/live deployment this sandbox cannot provide):** actual install-banner behavior, real iOS/Android home-screen rendering, a live GitHub Pages deep-link request, and real update timing across two sequential live deployments. Each has a specific manual verification step listed in the report's §11 rather than being assumed to work.

---

## Known, permanently-open items (not phase-specific, unlikely to close without infrastructure this sandbox can't provide)

- **The Firestore/Storage emulator cannot be downloaded in this sandbox.** Every phase from 5 onward has unexecuted emulator-dependent tests as a result. This is an environment limitation, not a quality gap in the tests themselves — they're written to the same standard as everything else, just unconfirmed.
- **`alerts` schema cannot distinguish an admin's official broadcast from a citizen's SOS signal** — both go through the same collection with no `type`/`source` field. Documented, not fixed, since fixing it is a schema change and every phase has been instructed not to change the application design.
- **No server-side orphan-media cleanup** — if a Storage upload succeeds but the Firestore write never completes (device never reconnects), the file has no scheduled cleanup. Would need a Cloud Scheduler + Function, not yet built.

---

## Verification checklist for a future session with real network access

Run these, in this order, and report back full output:

```bash
# Main app
cd tracenet-pwa
npm ci && npm run build && npm run lint && npm run typecheck && npm test && npm run test:pwa

# Firestore/Storage rules (Phase 5 + 7's additions)
npm run test:security

# Cloud Functions
cd firebase/functions
npm ci && npm test                # offline suite — should already pass; re-confirm
npm run test:integration          # emulator-backed integration test — never yet run
```

Additionally, once deployed to a live GitHub Pages URL (Phase 8's PWA work cannot be fully verified without this):
- Visit the deep-link URL directly (e.g. `https://<user>.github.io/tracenet-pwa/reports/new`) without navigating in-app first — confirm it lands on the actual page, not a GitHub 404.
- Install the app on an Android and an iOS device; confirm the home-screen icon and standalone (no browser chrome) rendering both look correct.
- Deploy a second time while the app is open in a browser tab from the first deployment; confirm the "Update available" prompt appears and its Refresh button actually picks up the new version.
