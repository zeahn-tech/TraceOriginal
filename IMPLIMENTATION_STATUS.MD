# TraceNet — Implementation Status

**Last updated:** after Phase 15 (Lighthouse Production Audit), complete.
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
**Correction (see Phase 14):** the "54/54 tests, executed" claim below was NOT true for an unknown period after this phase — the `firebase/functions/src/lib/` directory every function imports from had never actually been committed, so the whole package couldn't even compile. Phase 14 rebuilt it from the test suite's own contract and re-verified for real. Read this entry as the intended design, accurately described, not as a true claim about the state of the repository at every point since.
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

### 9. GitHub Pages Deployment Production Readiness — ✅ Complete, executed & passing
**Deliverable:** `GITHUB_PAGES_PRODUCTION_GUIDE.md`, `.github/workflows/deploy-pages.yml`

Reviewed the full deployment chain end-to-end (`npm ci`, `npm run build`, the Actions workflow, `vite.config.ts`, `index.html`, `404.html`, the manifest, and the service worker) against the requirement that Firebase Authentication must work from the deployed domain. Phase 8's PWA/routing/manifest/service-worker work held up completely under re-verification — no regressions found there.

**Real, currently-live bug found and fixed:** the deployment workflow computed and injected `VITE_BASE_PATH` but never injected any `VITE_FIREBASE_*` (or `VITE_RECAPTCHA_SITE_KEY`) value into the `npm run build` step. Since Vite bakes `import.meta.env.VITE_*` values in at build time, every production deploy was compiling `firebaseReady` (in `src/services.ts`) to `false` — Authentication, Firestore, Storage, and Functions never initialized on the live site, silently. This was not previously caught because no phase had a live GitHub Pages deployment to test against; the local build/test suites all default to Firebase being unconfigured and treat that as an acceptable, tested code path (`firebaseReady === false`), which masked the fact that the *deployed* build was following that same disabled path unintentionally.

**Fixed:**
- `.github/workflows/deploy-pages.yml` now injects `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN`, `VITE_FIREBASE_PROJECT_ID`, `VITE_FIREBASE_STORAGE_BUCKET`, `VITE_FIREBASE_MESSAGING_SENDER_ID`, `VITE_FIREBASE_APP_ID`, and `VITE_RECAPTCHA_SITE_KEY` from repository *variables* (not secrets — this config is public-by-design; see the guide) into the `Build` step's environment.
- Added `VITE_BASE_PATH` repository-variable override for user/org-site and custom-domain deployments, falling back to the existing `/<repo-name>/` derivation for project sites.
- Added `typecheck` and `lint` as explicit CI steps (previously only implicit via `npm run build`'s `tsc -b`; lint was never run in CI at all).
- Wired Phase 8's `npm run test:pwa` (`src/pwa-build.test.ts`, 25 tests) into CI, so a future regression in base-path/manifest/404/service-worker output fails the workflow instead of only being catchable by manually remembering to run it locally.
- Added a post-build "Verify production build artifacts" step: confirms `index.html`/`404.html`/`manifest.webmanifest`/`sw.js` all exist in the *real* build (not the synthetic one from the test suite), confirms the real resolved base path is actually applied to asset/manifest references, and emits a `::warning::` (non-fatal, since a valid all-static deployment doesn't require Firebase) if the required Firebase variables are unset.
- Added a post-deploy smoke test: retries an HTTP request against the live Pages URL for up to a minute, warning rather than failing on a miss (first-ever Pages deployments can occasionally take longer than one minute to propagate).

**Verified, directly, in this sandbox (not assumed):**
- `npm ci` — clean install, 1202 packages, no errors.
- `npm run typecheck` and `npm run lint` — both clean (lint: 0 errors, 18 pre-existing warnings, all in `App.tsx`/unused-import/exhaustive-deps territory, none introduced by this phase).
- `npm run test:pwa` — 25/25 passing, confirming Phase 8's work is intact.
- A production build with `VITE_BASE_PATH=/TraceOriginal/` and dummy `VITE_FIREBASE_*`/`VITE_RECAPTCHA_SITE_KEY` values set as environment variables (mirroring exactly what the workflow now does) — then `grep`-confirmed the literal dummy `test-api-key` string and the dummy `authDomain` value actually appear inside the compiled `dist/assets/*.js` bundle, proving the injection genuinely reaches the shipped artifact rather than only appearing to work.
- The workflow YAML itself was parsed with `yaml.safe_load` to confirm it is syntactically valid.

**Not verified (requires a real GitHub Actions run and a live Firebase project this sandbox cannot provide):** the workflow has never actually executed inside GitHub Actions itself (only its constituent steps were each run manually in this sandbox), a real Firebase project's authorized-domains configuration was never exercised against a real deployed URL, and the post-deploy smoke test's retry logic was never exercised against real Pages propagation timing. `GITHUB_PAGES_PRODUCTION_GUIDE.md` documents exactly what a human needs to configure (repository variables, Firebase Console authorized domains, optional custom domain/CNAME) and a manual verification checklist to close this gap after the next live deploy.

**Security note, addressed directly in the guide:** the guide is explicit that `VITE_FIREBASE_*` values belong in repository *variables*, never *secrets*, and never as Firebase Admin/service-account credentials — since everything prefixed `VITE_` is compiled into public client-side JavaScript regardless of which GitHub mechanism holds it.

---

### 10. Production Architecture Refactor — ✅ Complete, executed & passing
**Deliverable:** `ARCHITECTURE_REFACTOR_REPORT.md`

Decomposed the single 52,893-byte `src/App.tsx` (every screen, dialog, card, and shared primitive in one file) into per-feature modules — `src/features/{auth,dashboard,reports,tips,sos,maps,contacts,analytics,profile,operations,admin,public}/`, plus `src/shared/` for cross-feature primitives and cards, `src/hooks/useAppData.ts`, and `src/routing/` for the route table. `App.tsx` is now a 9-line composition root. Every one of the 17 existing routes, every permission check in `Protected`, every class name, and all existing behavior were preserved exactly — components were relocated character-for-character, not rewritten.

Added `React.lazy()`-based route-level code splitting (`src/routing/lazyRoutes.ts`) for every screen reachable past the splash/welcome screen, and a new `RouteErrorBoundary` (`src/routing/ErrorBoundary.tsx`, keyed by pathname) — the app previously had zero error boundaries anywhere, so any render exception blanked the whole page; a screen crash now shows a recovery screen and any navigation away from it self-heals. No dependency was added (`React.lazy`/`Suspense`/`Component` are already part of the installed `react` version) and no CSS changed (the new fallback/error UI reuses existing `.empty`/`.spin`/`.notice`/`.page` classes).

**Verified:** `npx tsc -b` clean; `npm run build` succeeds with the main JS chunk down from 351.19 kB (113.61 kB gzip) to 317.24 kB (104.90 kB gzip) plus 20 new on-demand route chunks (41.7 kB raw / ~19.5 kB gzip combined, e.g. a CITIZEN who never opens `/admin` now never downloads `AdminPage`'s 5.05 kB chunk at all); `npm run lint` 0 errors (6 pre-existing-cause warnings, same root causes as before the refactor, now attributed to their new files — one incidental new warning was found and fixed during this phase); `npm run test` 23/23 passing (`services.test.ts`/`offline-reporting.test.ts`, unaffected since neither imports `App.tsx`); `npm run test:pwa` 25/25 passing, confirming the production build's manifest/service-worker/base-path/icon output is still correct with the new chunk count (46 precache entries vs. 26 before, expected).

**Not verified (same class of sandbox limitation as prior phases):** a live browser click-through of all 17 routes and a forced-render-error check of the new error boundary's fallback UI — this sandbox's background processes don't survive across tool calls, so a `vite preview` server couldn't be kept alive long enough for a `curl`/browser smoke test. The build artifacts (chunk manifest, precache list) were inspected directly instead. Recommended as a manual follow-up once deployed to a preview URL: confirm each route's chunk loads in the Network tab, and confirm the error boundary's fallback renders correctly for a forced error.

---

### 11. Automated QA Suite — ✅ Executable layers complete & passing; browser-automation layer written, loadable, NOT executed
**Deliverable:** `AUTOMATED_QA_REPORT.md`, `src/routing/routing.test.tsx`, `src/features/auth/auth.test.tsx`, `e2e/` (Playwright), `playwright.config.ts`

Full QA pass across every area requested: Authentication, Routing, Role authorization, Reports, Tips, Wanted notices, Alerts, Media, Firebase integration, Offline reports, Offline media, Synchronization, Conflict resolution, PWA behavior. Most of this (Reports/Tips/Wanted/Alerts/Media/Firebase rules-and-Functions/Offline/Sync/Conflict) was already covered by Phases 3–7's existing suites; this phase's actual additions are the two gaps those suites didn't reach, plus the requested browser-automation layer:

- **Routing + role authorization, executed against the real route table** (`src/routing/routing.test.tsx`, 16/16 passing): renders the genuine `AppRoutes`/`Protected` components (not stubs) via `MemoryRouter`, covering unauthenticated redirects, unknown-path/deep-link fallback, the CITIZEN/LAW_ENFORCER/ADMIN role matrix including the unapproved-officer gate and the ADMIN-bypasses-any-role-check behavior, and `Protected`'s guard contract in isolation.
- **Authentication UI workflows, executed** (`src/features/auth/auth.test.tsx`, 9/9 passing): sign in (success, wrong credentials, Google), sign up (CITIZEN success, LAW_ENFORCER client-side field validation before any Firebase call, duplicate-email failure), and password reset (dialog pre-fill, success, failure) — real component behavior with `firebase/auth` mocked at the network boundary only, same pattern as `services.test.ts`.
- **Full suite re-verified together:** `npm run test` 48/48 (23 pre-existing + 25 new), `npm run typecheck` clean, `npm run lint` clean (same 6 pre-existing warnings, zero new), `npm run test:pwa` 25/25 — nothing broke, and the UI itself was not modified anywhere to make a test pass (the one non-test source change, described next, is opt-in infrastructure with zero effect unless explicitly enabled).
- **Browser-automation layer for the 16 named critical workflows** (`e2e/` — `auth.spec.ts`, `reports.spec.ts`, `roles.spec.ts`, `pwa.spec.ts`, 52 tests across 4 role personas × 2 viewport projects): covers sign up, sign in, password reset, report submission, report+image, report+video, report+audio, offline report, reconnection, media synchronization, law-enforcement workflow, admin workflow, PWA installation, refresh/update-prompt, deep link, and unauthorized access. Written against Playwright, seeding Citizen/Law-Enforcer/Admin fixtures directly into a local Firebase Emulator via its REST API (`e2e/helpers.ts`) since the app's own sign-up flow can only ever create a CITIZEN or a pending officer by design.
  - **Verified without execution:** `npx tsc` type-checks the whole `e2e/` tree clean, and — a step beyond simple type-checking — `npx playwright test --list` successfully loads every spec file and enumerates all 52 tests by name, confirming the suite's structure, imports, and fixtures resolve correctly end-to-end at the tooling level.
  - **NOT executed, for two independent, verified (not assumed) reasons specific to this sandbox:** `npx playwright install chromium` fails with a 403 — `cdn.playwright.dev` is not in this sandbox's network egress allowlist. Separately, `firebase emulators:start` (needed for both this suite and `test:security`) fails with the identical pre-existing 403 against `storage.googleapis.com` documented since Phase 5. Either blocker alone would prevent execution; both are present. Neither is a defect in the suite — see `AUTOMATED_QA_REPORT.md` for the full per-area verification-level table.
- **One small, additive, opt-in change to non-test source** (`src/services.ts`): added `connectAuthEmulator`/`connectFirestoreEmulator`/`connectStorageEmulator` wiring, gated behind `import.meta.env.VITE_USE_FIREBASE_EMULATOR === 'true'`. This flag is never set in production, in the existing vitest suites (which mock `firebase/*` out entirely and never set it), or in any build in this session — confirmed by grepping a real production build's output bundle for `connectAuthEmulator` and finding zero occurrences (Rollup tree-shakes the entire gated block away when the flag is absent at build time). It exists solely so the `e2e/` suite has something to connect to once a future session has real network access.

**Action needed from the user:** run `npm run test:e2e` from a machine with normal internet access (after `npx playwright install chromium` and with the Firebase Emulator Suite reachable); report back the pass/fail output. This is the single remaining unexecuted piece of the QA suite requested.

---

### 12. Splash-Screen Deadlock Fix — ✅ Complete, executed & passing
**Deliverable:** fix in `src/services.ts`; regression tests `src/services.observeAuth.test.ts`, `src/features/auth/splash.test.tsx`

**The reported symptom:** the deployed app never got past the splash screen — it sat on it forever, with the rest of the app (Welcome, Public Portal, Sign In, About, and every other route) completely unreachable.

**Root cause, found by tracing the actual code path, not guessed:** `observeAuth` (`src/services.ts`) returned a permanent no-op — `auth ? onAuthStateChanged(auth,callback) : (()=>{})` — whenever Firebase wasn't configured or failed to initialize (`auth` undefined). `useAppData.ts` only clears its `loading` state *inside* that callback, and `Splash.tsx` only navigates away from the splash screen once `loading` is `false`. So an unconfigured/failed Firebase setup meant the callback simply never fired, `loading` never cleared, and the entire app deadlocked on the splash screen with no error shown at all — this is exactly the scenario the project's own `GITHUB_PAGES_PRODUCTION_GUIDE.md` already warned about ("the site will deploy successfully but Authentication/Firestore/Storage will not initialize" if the `VITE_FIREBASE_*` repository variables were never configured), except the actual failure mode was worse than "auth doesn't work" — it was "nothing works, ever, including public unauthenticated pages that don't need Firebase at all."

**The fix:** `observeAuth` now always resolves its callback — with the real Firebase user when configured, or with `null` (asynchronously, matching Firebase's own always-async contract) when not — instead of hanging forever. This treats "not configured" the same as "signed out": the splash screen's existing 2.5s timer then does what it always does for a signed-out visitor and navigates to `/welcome`, and every public route (Welcome, Public Portal, Sign In, Sign Up, About, Map, Contacts, Analytics) becomes reachable regardless of Firebase's configuration state. Only the actions that genuinely require Firebase (`session.signIn`, `session.signUp`, etc. — already wired to reject with a clear "Firebase configuration is required..." message) fail; navigation itself no longer does.

**Verified:**
- `src/services.observeAuth.test.ts` (4/4 passing): proves `observeAuth` resolves `callback(null)` instead of hanging when Firebase is unconfigured, that the returned unsubscribe is safe to call, and that the configured-Firebase path is completely unaffected (still delegates straight to the real `onAuthStateChanged`).
- `src/features/auth/splash.test.tsx` (1/1 passing) — an end-to-end regression test that reproduces the exact reported scenario inside jsdom: real `useAppData` + real `Splash` rendered together with `VITE_FIREBASE_*` stubbed blank (the same env state as an unconfigured live deploy), asserting the splash screen actually navigates to `/welcome` instead of timing out. This closes the loop from the isolated `services.ts` fix up through the actual user-visible symptom.
- A real production build (`npm run build`) with zero `VITE_FIREBASE_*` environment variables set — reproducing the exact broken-deploy condition — still succeeds and produces a normal `dist/` output.
- Full suite re-run together: `npm run test` 53/53 (48 prior + 5 new across the two regression files), `npm run typecheck` clean, `npm run lint` clean (same 6 pre-existing warnings, 0 new), `npm run test:pwa` 25/25.
- No UI was changed. This is a one-line logic fix (plus its regression tests) in `services.ts`'s data layer.

**Separately, and outside what a code fix can address:** if the live deploy's actual problem is that `VITE_FIREBASE_*` repository variables were never set, sign-in/sign-up/reports/etc. will still not *function* even though the app is no longer stuck — only `GITHUB_PAGES_PRODUCTION_GUIDE.md` §1's one-time GitHub/Firebase configuration can fix that half. This phase guarantees the app is navigable either way; it cannot itself supply Firebase credentials that were never configured.

---

### 13. Cross-Platform Validation — ✅ Complete, code-level analysis + existing suite re-verified; live device testing NOT executed
**Deliverable:** `CROSS_PLATFORM_TEST_REPORT.md`

Requested scope: validate the production PWA (install, auth, navigation, reports, camera, microphone, image/video/audio upload, offline mode, reconnection, media playback, notifications, geolocation, responsive behavior) across Android Chrome, iPhone Safari, iPad Safari, Windows Chrome, Windows Edge, macOS Safari, macOS Chrome, and Linux Chrome/Firefox.

**What was actually done, and the epistemic line drawn explicitly in the report itself:** this sandbox has no real devices or browser binaries (same `cdn.playwright.dev` 403 blocker documented since Phase 11). Every platform-specific claim in the report is tagged as one of: code-verified (read directly from source, or a real `npm run build`/`npm run test`/`npm run test:pwa` run in this sandbox), documented platform behavior (a publicly-verifiable vendor fact, e.g. "desktop Firefox never fires `beforeinstallprompt`"), or requires manual device verification (listed explicitly, never assumed to pass). No platform-behavior claim is phrased as something observed on a device in this session, because none was available to observe.

**Real bugs found, platform-independent (affect every platform equally, not a Safari-vs-Chrome issue):**
- **HIGH:** `MediaThumb` (`src/components/MediaAsset.tsx`) renders attached videos `muted`, with no `controls` and no click handler — there is currently no way, on any browser, to actually watch a video attachment. Audio attachments are unaffected (`<audio controls>`). Not fixed in this phase — this task's scope was validation, not a UI redesign, and fixing it (add `controls`, drop the forced `muted`, and/or a full-size viewer) is a real product decision, not a compatibility patch.
- **MEDIUM:** `captureMedia()` (forces the native camera via `input.capture='environment'`) and `recordMedia()` (a full `getUserMedia`/`MediaRecorder` in-page recorder with proper Safari-aware codec fallback) both already exist in `src/services.ts` but are never called from any component — grepped, confirmed zero call sites outside `services.ts` itself. The shipped `ReportForm`/`WantedForm` UI uses a plain `<input type="file" accept="image/*,video/*,audio/*">` with no `capture` attribute, which is exactly the config most likely to produce inconsistent camera/mic access on iOS Safari (see below). Not wired up in this phase, same reasoning as above.
- **LOW:** no persistent "you're offline" indicator anywhere in the UI — offline state is only ever surfaced reactively, via a one-time post-submit toast. The actual offline-first pipeline (Phases 3/4) is unaffected and still correct; this is a UX gap adjacent to it.
- **Scope-clarifying, not a bug:** grepped the entire `src/` tree for `Notification`/`PushManager`/`push` — there is no Notifications API or web-push implementation anywhere. The "receive instant notifications" line in the dashboard carousel is marketing copy only. The report states this plainly rather than marking "Notifications" as passing on any platform.

**Named, vendor-documented platform limitations (not bugs in this app):** no `beforeinstallprompt` on iOS/iPadOS Safari (the existing manual "Add to Home Screen" banner in `InstallPrompt.tsx` already handles this correctly, with one caveat below); no PWA install support at all in desktop Firefox; iOS Safari's file-input action sheet behaving less predictably than Android's for a combined `image/*,video/*,audio/*` accept string; the Background Sync API being Chromium-only (already correctly documented in `src/sw.ts` since Phase 8, with the universal `online`-event fallback doing the real work everywhere); and no dedicated tablet breakpoint (`styles.css` has one `min-width:760px` query, so an iPad in portrait gets the same 2-column treatment as a narrow desktop window rather than a tablet-tuned layout — not broken, just undifferentiated).

**Verified in this phase (executed, not assumed):** full re-run of the existing suite confirms zero regressions from Phase 12's baseline — `npm run typecheck` clean, `npm run lint` 0 errors / same 6 pre-existing warnings, `npm run test` 53/53, `npm run test:pwa` 25/25, and a real `npm run build` inspected directly (manifest icons/display/orientation, 46 precache entries, all match Phase 8/9's prior fixes with no drift).

**Not verified, and named explicitly rather than assumed (requires a human with physical devices):** actual iOS/iPadOS action-sheet behavior for the camera/mic file input; whether the iOS install banner's user-agent sniff fires on a stock iPad (which defaults to a desktop-class UA string unless "Request Mobile Website" is toggled); real orientation-lock behavior for an installed standalone iOS PWA; live reproduction of the video-playback gap on an actual touchscreen; and confirmation that desktop Firefox shows no install affordance at all. All five are listed as a concrete checklist in `CROSS_PLATFORM_TEST_REPORT.md` §7.

**Action needed from the user:** work through `CROSS_PLATFORM_TEST_REPORT.md` §7 on real hardware and report back results; decide whether the §2.1 (video playback) and §2.2 (camera/mic wiring) findings should become a follow-up phase, since fixing them is a product/UI decision outside this validation pass's scope.

---

### 14. Fix Every Issue Found — Cloud Functions restoration, PWA bug fixes, push notifications — ✅ Complete, executed & passing
**Deliverable:** fixes across `firebase/functions/`, `src/`, updated `CROSS_PLATFORM_TEST_REPORT.md` and `FIREBASE_FUNCTIONS_ARCHITECTURE.md`

Requested scope: fix every issue Phase 13 found (and anything else discovered along the way) to a production-grade standard, not just document it. Four things were fixed, one of which was far more serious than anything Phase 13 had actually flagged.

**Found and fixed, most serious first:**

1. **The entire Cloud Functions codebase could not compile.** Every function in `firebase/functions/src/functions/` imports from `../lib/{auth,audit,idempotency,rateLimit}` — and that directory did not exist anywhere in this repository's git history, at any point (`git log --all -- firebase/functions/src/lib/` returns nothing). This means Phase 6/7's "54/54 tests, executed" claim was not actually true for however long this state persisted — there is no way to determine from the repo alone when the discrepancy started. Fixed by reverse-engineering the exact contract of all four modules from `firebase/functions/test/*.test.ts` (which already existed and correctly encoded the intended behavior) and writing them to satisfy it exactly — including the precise `unauthenticated`/`permission-denied` split, the unapproved-officer gate, and the exact minimum-requestId-length boundary the idempotency tests require. `npm run build` inside `firebase/functions/` now completes with zero errors, and `npm test` genuinely passes 54/54 — the same number that had been claimed, now actually true. Full account in `FIREBASE_FUNCTIONS_ARCHITECTURE.md` §11.1.
2. **Video playback (Phase 13 §2.1) — fixed.** `MediaThumb` (`src/components/MediaAsset.tsx`) is now clickable/focusable and opens a full-size, unmuted `<video controls>` in a dialog (reusing the existing `Dialog` component), rather than being permanently silent and non-interactive. The exact CSS-selector structure the component relied on (`.thumbs img`, `.wanted-card>img`) was preserved — no stylesheet changes were needed for this.
3. **Camera/mic wiring (Phase 13 §2.2) — fixed.** A new `CaptureButtons` component (`src/shared/CaptureButtons.tsx`) wires up `captureMedia` (now also forcing the native camera for video, not just images) and a new `startRecording` — an improved, stoppable version of the formerly-dead-code `recordMedia`, with a live in-dialog preview and a Stop button rather than a fixed 60-second blind recording — into `ReportForm`, `WantedForm`, and `SignUpPage`. This also gives desktop browsers a real camera/mic capture path for the first time, since desktops have no native camera app to hand a file-input `capture` attribute off to.
4. **No persistent offline indicator (Phase 13 §2.3) — fixed.** A new `OfflineBanner` component, mounted at the top of `App.tsx`, shows a persistent, safe-area-aware banner for as long as `navigator.onLine` is false, using the same `online`/`offline` events the sync engine already relied on.
5. **No Notifications/Push implementation at all (Phase 13 §3) — implemented for real.** Firebase Cloud Messaging is now wired end-to-end: a "Enable Push Notifications" control in Profile settings (`src/push.ts`), a new `subscribeToAlerts` Cloud Function (any signed-in user may subscribe their own device to the public `alerts` topic — deliberately not admin/officer-gated, since there's nothing privileged about opting in to content already visible in-app), a new `notifyOnAlertCreated` Firestore trigger that sends a real push for every new alert document (covering both the admin-broadcast and citizen-SOS paths, since both land in the same collection), and background-push handling added to `src/sw.ts` (confirmed, not assumed, that `import.meta.env.VITE_FIREBASE_*` correctly resolves at build time inside the service-worker bundle, by building once with real-looking dummy values and inspecting the output directly). A new `VITE_FIREBASE_VAPID_KEY` variable was added to `.env.example` and the deploy workflow for this. Full account in `FIREBASE_FUNCTIONS_ARCHITECTURE.md` §11.2.

**Verified in this phase (executed, not assumed), at every step along the way, not just at the end:**
- Client: `npm run typecheck` clean, `npm run lint` 0 errors / same 6 pre-existing warnings, `npm run build` (inspected `dist/sw.js` directly for both the unconfigured and dummy-configured Firebase cases), `npm run test` 60/60 (53 prior + 7 new for `src/push.test.ts`), `npm run test:pwa` 25/25.
- Functions: `npm run build` clean, `npm test` 65/65 (54 prior + 6 new for `subscribeToAlerts` + 5 new for `notifyOnAlertCreated`).
- A new `function_rate_limits` Firestore rule was added (mirroring the existing `function_calls` one — denied to every client, written only by the Admin SDK) and a matching emulator rules test was added to `firebase/emulator-tests/firestore.rules.test.ts` — **written, type-consistent with the existing suite, but NOT executed**, same emulator-network blocker as every other emulator-dependent test in this project (see "Known, permanently-open items" below).

**Not verified, and named explicitly rather than assumed:** everything Phase 13 already listed as requiring a real device still does (this phase didn't have device access either); additionally, whether a real FCM `send()` call reaches a real device, whether a real VAPID key/browser combination actually yields a working `getToken()` client-side, and whether the Firestore trigger actually fires in a real deployed project (Firestore triggers require the Eventarc API to be enabled on the Firebase project — a Console-side step, not something this codebase controls). `GITHUB_PAGES_PRODUCTION_GUIDE.md` §1.7 now documents this deploy prerequisite explicitly.

**Action needed from the user:** deploy the updated Cloud Functions (`firebase deploy --only functions`, after enabling the Eventarc API if not already enabled) and set `VITE_FIREBASE_VAPID_KEY`; then work through the updated `CROSS_PLATFORM_TEST_REPORT.md` §7 (item 7 specifically covers confirming a real push arrives) on real hardware.

---

### 15. Lighthouse Production Audit — ✅ Complete, executed & passing, real headless-browser workaround found
**Deliverable:** `LIGHTHOUSE_PRODUCTION_REPORT.md`; fixes across `src/`, `index.html`

**Environment breakthrough, relevant to every prior phase's "no browser access" blocker:** this sandbox's network allowlist blocks `*.github.io` (the live deploy) and standard Chrome/Playwright downloads (`cdn.playwright.dev`, `storage.googleapis.com`) alike — but `@sparticuz/chromium` (a headless Chromium build shipped *inside* its npm package, downloadable from the already-allowlisted `registry.npmjs.org`) runs correctly here under `puppeteer-core`. This let a **real** Lighthouse audit run against a real production build in this sandbox for the first time — not a static analysis. This likely also unblocks Phase 11's `e2e/` Playwright suite (52 tests, previously blocked by the identical `cdn.playwright.dev` 403) and `firebase emulators:start` remains a separate, still-open blocker (`storage.googleapis.com`) unaffected by this. Worth a dedicated attempt in a future phase.

**What was actually audited:** since the live URL itself was unreachable from here, this phase combines two runs — this sandbox's audit of a local production build (`VITE_BASE_PATH=/TraceOriginal/`, matching the real deploy's derived base path) via Lighthouse 13.4.1, and a live run the user performed directly against `https://zeahn-tech.github.io/TraceOriginal/` via Chrome DevTools, whose JSON/HTML the user supplied. The two agree closely (Performance 57 vs. 59, Accessibility 89 vs. 87, SEO 82 vs. 82), cross-validating both. Lighthouse 13.4.1 has dropped the PWA category entirely (Google deprecated its automated installability heuristics); Lighthouse 9.6.8 (last version with a scored PWA category) was run supplementally against the same build for that one score, documented explicitly as a different tool version rather than silently substituted.

**Baseline → final scores** (this sandbox's build, before → after fixes): Performance 59→**95**, Accessibility 87→**100**, Best Practices 100→**100**, SEO 82→**100**, PWA 100→**100** (all five meet or exceed the requested targets: 90+/90+/95+/90+/100).

**Real issues found and fixed, verified safe against the full existing test suite at every step (`npm run typecheck` clean, `npm run lint` 0 errors/same 6 pre-existing warnings throughout, `npm run test` 60/60, `npm run test:pwa` 25/25 — before AND after every change, no test modified):**
1. **Firebase SDK (~622 KB) blocking initial paint on every route, including fully public/unauthenticated ones.** Three files pulled the whole SDK eagerly into the entry bundle merely to reach one or two functions from `services.ts`: `src/hooks/useAppData.ts`, `src/push.ts`, `src/main.tsx`. Converted all three from static to dynamic `import()`, additionally deferred via a new `requestIdleCallback`-based helper (`src/shared/idle.ts`) so the fetch starts once the browser is idle after first paint rather than competing with it. Safe with minimal blast radius because `db`/`storage`/`functions` are only ever touched inside `services.ts` itself (confirmed by grep first) — none of that file's 771 lines needed to change. Result: `dist/index.html`'s eager `modulepreload` list dropped from `[icons.js, firebase.js]` to `[icons.js]`; total `/welcome` page weight 1,636 KiB→998 KiB; FCP 6.3s→2.3s; LCP 9.8s→2.5s.
2. **Three oversized source images** (`img_tracenet_logo_*`, `img_liberia_police_banner_*`, `img_hero_banner_*` — up to 1024×1024/899 KB) displayed at a maximum of 200×200 or inside a 300×180 box. **Scope catch:** these exact files are shared with the native Android app (`app/src/main/res/drawable/`, referenced by `SplashScreen.kt`/`WelcomeScreen.kt`/etc. and the adaptive launcher icon) — resizing them in place would have silently degraded Android assets this task was never asked to touch. Created web-specific optimized copies in a new `src/assets/images/` instead, pointed `src/shared/assets.ts` at those; the Android drawables are untouched. Combined weight 2.27 MB→157 KB.
3. **Missing `alt` attributes** on 5 `<img>` elements (logo on Welcome/AuthPage/SignUpPage/AboutPage, profile photo on ProfilePage).
4. **Missing `<meta name="description">`** in `index.html`.
5. **A real WCAG contrast failure**, caught by the user's live run and not by this sandbox's local build (the value of running against the actual live deploy): white text at 14px bold on `#1e88e5` measured 3.67:1, below the required 4.5:1. Fixed by darkening only the *background-behind-white-text* usages (6 CSS rules: `.outline-button`'s effective background, `.primary-button`/`.small-primary`, `.notice`, `.dash-header`, `.chips button.on`, `.update`'s blue variant) to `#1776c9` — same hue/saturation, ~7% darker, 4.7:1 contrast — while leaving every other use of the original blue (borders, icons, links-on-white, all of which already passed) untouched.

**Found and explicitly NOT changed, with reasoning (not silently omitted):**
- **GitHub Pages' 404-then-recover deep-link flow** costs a real, unavoidable ~1.1s (confirmed via the live report's own `redirects` audit) plus one console `404` (why Best Practices scored 96 instead of 100 on the live run) whenever a route other than `/` is requested directly. This is the standard, already-correctly-implemented workaround for SPA routing on a host with zero server-side rewrite capability (unlike Netlify/Vercel) — GitHub Pages will always serve a real HTTP 404 first for any non-literal-file path, by design of the platform, not this app's code. Cannot be eliminated without changing hosting platforms, which is outside this deployment target's own audit scope.
- **`unused-javascript` still flags the idle-deferred Firebase chunk** on pages that don't end up needing it in a given session — confirmed this audit carries **zero weight** in Lighthouse's performance score, so it's a legitimate diagnostic, not a scoring gap. Eliminating it fully would mean not loading Firebase until an actual sign-in attempt, changing how the app detects an already-signed-in returning visitor — a real product-behavior tradeoff, scoped out of a fix-don't-redesign pass.
- **Performance capped at 95, not 100** — FCP/LCP sub-metric scores of 0.76/0.90 reflect normal variance for a client-rendered SPA with no server-side rendering/prerendering under Lighthouse's simulated mobile throttling. Closing this last gap needs prerendering/edge rendering, a real architecture change outside this pass's scope; 95 exceeds the requested 90+ target.

**Action needed from the user:** deploy this branch so the live site picks up these fixes, then re-run Lighthouse against the real URL to confirm the numbers transfer (expected to, given how closely the two baselines already agreed). Separately, consider in a future phase whether `@sparticuz/chromium` genuinely unblocks Phase 11's e2e suite — worth a dedicated attempt now that a working headless-browser path exists in this sandbox.

---

## Known, permanently-open items (not phase-specific, unlikely to close without infrastructure this sandbox can't provide)

- **A previously-undetected gap of unknown duration existed where `firebase/functions/` could not compile at all**, despite this document confidently claiming otherwise (see Phase 6's correction note and Phase 14). This particular item is now closed, but it's recorded here as a standing reminder: this document's own "verified" claims are only as good as whoever last actually ran the command, and a stale claim can persist for an unknown number of phases before anyone notices. Re-running `npm run build`/`npm test` in `firebase/functions/` costs a few seconds and catches this class of problem immediately — do it, don't trust the last claim on faith.
- **No real device has ever received an actual push notification from this app**, and can't be confirmed from this sandbox — Phase 14 implemented the full FCM pipeline and unit-tested the business logic on both ends, but "did a real push actually arrive" needs a real deployed Firebase project, a real VAPID key, and a real device or browser. See `CROSS_PLATFORM_TEST_REPORT.md` §7 item 7.

- **The Firestore/Storage emulator cannot be downloaded in this sandbox.** Every phase from 5 onward has unexecuted emulator-dependent tests as a result. This is an environment limitation, not a quality gap in the tests themselves — they're written to the same standard as everything else, just unconfirmed.
- **Playwright's own browser binaries still cannot be downloaded in this sandbox** (`cdn.playwright.dev` — confirmed by actually running `npx playwright install chromium` and getting a 403), so Phase 11's `e2e/` suite as written (which drives Playwright's own browser context) is still blocked by that plus the separate emulator blocker below. **Update (Phase 15):** a working headless-Chromium path *does* now exist in this sandbox — `@sparticuz/chromium` (ships its own Chromium binary inside the npm package, downloadable from the already-allowlisted `registry.npmjs.org`) runs correctly under `puppeteer-core`, and was used to execute a real Lighthouse audit here for the first time (see `LIGHTHOUSE_PRODUCTION_REPORT.md`). This doesn't unblock Playwright's `test()` runner directly (it's hardcoded to its own downloaded browsers), but Playwright can be pointed at an already-running external browser via `chromium.connect()`/`connectOverCDP()` against a `@sparticuz/chromium` instance launched separately — untried here, out of this phase's scope, but a concrete, worth-attempting path for a future phase before assuming this blocker is unconditionally permanent.
- **`alerts` schema cannot distinguish an admin's official broadcast from a citizen's SOS signal** — both go through the same collection with no `type`/`source` field. Documented, not fixed, since fixing it is a schema change and every phase has been instructed not to change the application design.
- **No server-side orphan-media cleanup** — if a Storage upload succeeds but the Firestore write never completes (device never reconnects), the file has no scheduled cleanup. Would need a Cloud Scheduler + Function, not yet built.
- **No real Android/iOS device or desktop browser has ever been exercised against this app.** Phase 13's cross-platform report is a code-level analysis plus vendor-documented platform facts, not live device testing — this sandbox has no physical devices or browser binaries any more than it has emulator or Playwright-browser network access. `CROSS_PLATFORM_TEST_REPORT.md` §7 is the checklist for closing this gap.
- **The GitHub Actions workflow has never executed in real GitHub Actions, and no real Firebase project's authorized-domain configuration has been exercised against a live deployed URL.** Every constituent command was run and verified manually in this sandbox (see Phase 9), but a live end-to-end run — real Actions runner, real Pages deploy, real Firebase Console configuration — has not happened yet. `GITHUB_PAGES_PRODUCTION_GUIDE.md` §3 is the checklist for closing this gap after the next push to `main`.

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

# Browser-automation E2E suite (Phase 11) — requires BOTH a downloaded
# Playwright browser (`npx playwright install chromium`) AND the Firebase
# Emulator Suite reachable at the same time
cd ..  # back to repo root
npm run test:e2e
```

Additionally, once deployed to a live GitHub Pages URL (Phase 8's PWA work cannot be fully verified without this):
- Visit the deep-link URL directly (e.g. `https://<user>.github.io/tracenet-pwa/reports/new`) without navigating in-app first — confirm it lands on the actual page, not a GitHub 404.
- Install the app on an Android and an iOS device; confirm the home-screen icon and standalone (no browser chrome) rendering both look correct.
- Deploy a second time while the app is open in a browser tab from the first deployment; confirm the "Update available" prompt appears and its Refresh button actually picks up the new version.

Before that first live deploy, complete the one-time GitHub/Firebase configuration in `GITHUB_PAGES_PRODUCTION_GUIDE.md` §1 (Pages source set to GitHub Actions, `VITE_FIREBASE_*`/`VITE_RECAPTCHA_SITE_KEY` repository variables, Firebase Console authorized domains) — without it, the site will deploy successfully but Authentication/Firestore/Storage will not initialize (Phase 9's fix only forwards the variables if they exist; it cannot supply values that were never configured). After deploying:
- Confirm sign-in works from the live deployed URL (this is the concrete test that authorized domains + injected Firebase config are both correct together).
- Check the "Verify production build artifacts" step's log in the Actions run for a `::warning::` about missing Firebase variables — its absence confirms all three required variables reached the build.

