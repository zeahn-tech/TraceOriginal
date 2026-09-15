# Firebase Backend Setup — connecting `tracenet-23a64`

**Context:** the app's Authentication/Firestore/Storage/Functions/Messaging integration has been fully implemented in code since earlier phases (see `FIREBASE_SECURITY_IMPLEMENTATION.md`, `FIREBASE_FUNCTIONS_ARCHITECTURE.md`) but was never connected to a real Firebase project — every prior phase ran against either the local emulator suite or an intentionally "unconfigured" build (see `IMPLIMENTATION_STATUS.MD` Phase 12). This phase wires in the real project (`tracenet-23a64`) the user provided and audits the existing implementation against it. **This document is the exact, minimal action list for what's left — everything else is already done.**

## 0. What this sandbox could and couldn't do, and why

Exactly the same class of limitation as `LIGHTHOUSE_PRODUCTION_REPORT.md`'s §1, confirmed the same way — by actually trying, not assuming: this sandbox's network is allowlisted and every Firebase/Google endpoint returns `host_not_allowed`, checked individually:

```
identitytoolkit.googleapis.com                          403 host_not_allowed
securetoken.googleapis.com                               403 host_not_allowed
firestore.googleapis.com                                 403 host_not_allowed
firebasestorage.googleapis.com                           403 host_not_allowed
firebase.googleapis.com                                  403 host_not_allowed
cloudfunctions.googleapis.com                             403 host_not_allowed
www.googleapis.com                                        403 host_not_allowed
tracenet-23a64.firebaseapp.com                            403 host_not_allowed
tracenet-23a64-default-rtdb.europe-west1.firebasedatabase.app   403 host_not_allowed
```

This means, from this sandbox, it is **not possible** to: sign in/up against the real project to test end-to-end, run `firebase deploy` (rules, functions, or anything else), enable Authentication sign-in providers, add authorized domains, or download the Firebase Emulator Suite's JARs (a pre-existing, separately-documented blocker — see `FIREBASE_SECURITY_IMPLEMENTATION.md` §8). Also attempted and confirmed unavailable: the provided GitHub PAT does not have permission to manage this repository's Actions variables (`GET .../actions/variables` → `403 Resource not accessible by personal access token`), so those cannot be set via API either — despite `api.github.com` itself being reachable.

Everything below marked **[DONE — verified in this sandbox]** is real, executed, and confirmed. Everything marked **[ACTION NEEDED — requires your own machine or the Firebase Console]** cannot be done from here regardless of credentials, because it requires reaching a host this sandbox cannot reach.

## 1. What's done

**[DONE — verified]** `.env` created locally (git-ignored — never committed; the repo's established convention, see `.env.example`, is to supply these at build time via GitHub Actions repository *variables*, not a committed file) with the real config, and the following confirmed by actually building and testing, not assumed:

- `npm run build` with the real config produces a bundle where `firebaseReady` evaluates `true` (traced through the exact boolean logic in `src/services.ts`, and confirmed the real `apiKey`/`projectId` values land in the built `services-*.js` chunk — not the eager entry chunk, per the lazy-loading work from the Lighthouse phase — and nowhere else).
- `npm run typecheck`, `npm run lint`, `npm run test` (**60/60**), `npm run test:pwa` (**25/25**) all still pass with the real `.env` present. Specifically checked `src/services.observeAuth.test.ts`'s `firebaseReady is false when required config is missing` test still passes — it uses `vi.stubEnv(...)` to force empty values regardless of what's in `.env`, so a real `.env` file cannot leak into or break that assertion. Verified by actually re-running the suite, not by reading the code and assuming.

**[DONE — verified]** `.firebaserc`'s `default` project updated from the placeholder `tracenet-emulator-test` to the real `tracenet-23a64` — confirmed this only affects `firebase deploy`/`firebase use` targeting, and does not affect the emulator test suites (`firebase/emulator-tests/*.test.ts`, `firebase/functions/test-emulator/integration.test.ts`), which hardcode their own separate placeholder project id directly in each test file, independent of `.firebaserc`.

**[DONE — verified]** Full code-level audit of the existing Authentication/Firestore/Storage/Functions implementation against what a real project needs (see §3 for the audit findings) — confirmed complete, no gaps found, nothing added or changed in `src/services.ts`, `firebase/firestore.rules`, or `firebase/storage.rules` because none was needed.

## 2. What's left — your action required

None of this can be done from this sandbox (see §0). In the order that unblocks the most:

### 2.1 Firebase Console: enable sign-in providers
**Authentication → Sign-in method** — enable:
- **Email/Password** (used by `session.signIn`/`session.signUp` in `src/services.ts`)
- **Google** (used by `session.google` via `signInWithPopup`)

Both providers are called by existing, already-implemented code today; both are almost certainly still in their new-project default state (disabled) until you turn them on.

### 2.2 Firebase Console: authorized domains
**Authentication → Settings → Authorized domains → Add domain** — add `zeahn-tech.github.io` (hostname only, no path). Already documented generically in `GITHUB_PAGES_PRODUCTION_GUIDE.md` §1.5; this is the concrete value for this deployment. Without it, `session.google` (`signInWithPopup`) fails with `auth/unauthorized-domain` even though everything else works — email/password sign-in is unaffected by this particular setting.

### 2.3 Deploy Firestore rules, Storage rules, and Cloud Functions
From your own machine (or any environment that can reach `googleapis.com`) — this repo, not this sandbox:
```bash
npm install -g firebase-tools   # if not already installed
firebase login
firebase deploy --only firestore:rules,storage:rules

cd firebase/functions && npm ci && npm run build && cd ../..
firebase deploy --only functions
```
`.firebaserc` already points at `tracenet-23a64` (§1), so no `--project` flag is needed. **One Console-side prerequisite before the functions deploy will succeed:** `notifyOnAlertCreated` is a Firestore-triggered function (`onDocumentCreated`), which requires the **Eventarc API** enabled on the underlying Google Cloud project first (Google Cloud Console → APIs & Services → Library → enable "Eventarc API") — already flagged in `GITHUB_PAGES_PRODUCTION_GUIDE.md` §1.7; repeating here because it's the single most common first-deploy failure for this specific function and easy to miss.

Until this step runs, the 7 callable functions (`approveOfficer`, `setUserRole`, `setUserStatus`, `verifyWantedNotice`, `publishAlert`, `transitionReportStatus`, `subscribeToAlerts`) will fail when called from the deployed app with a "not found" style error, and Firestore/Storage will fall back to their **default-deny** state (no rules deployed yet means Firestore/Storage reject everything by default on a fresh project) — so this is the highest-priority remaining step; nothing backend-side works at all until rules are deployed at least once.

### 2.4 GitHub repository variables (Settings → Secrets and variables → Actions → Variables)
The procedure is already documented generically in `GITHUB_PAGES_PRODUCTION_GUIDE.md` §1.3; here are the **exact values for this project** to paste in (confirmed correct against the config you provided and against what `src/services.ts` actually reads):

| Variable | Value |
|---|---|
| `VITE_FIREBASE_API_KEY` | `AIzaSyDvxeRAW09v312sDr0N_Iky0YtI2L8v4dw` |
| `VITE_FIREBASE_AUTH_DOMAIN` | `tracenet-23a64.firebaseapp.com` |
| `VITE_FIREBASE_PROJECT_ID` | `tracenet-23a64` |
| `VITE_FIREBASE_STORAGE_BUCKET` | `tracenet-23a64.firebasestorage.app` |
| `VITE_FIREBASE_MESSAGING_SENDER_ID` | `563728002879` |
| `VITE_FIREBASE_APP_ID` | `1:563728002879:web:2962892c52ff766fe5ed8a` |

Two more the app supports are **not** in the config snippet you pasted (that snippet only covers `firebase/app` + `firebase/analytics` initialization) and need a separate trip to the Console if you want those specific features working on the live site:

| Variable | Where to get it | If left unset |
|---|---|---|
| `VITE_FIREBASE_VAPID_KEY` | Project Settings → Cloud Messaging → Web configuration → Web Push certificates | Push notifications show a clear "not available on this deployment" message (`src/push.ts`) — rest of the app unaffected |
| `VITE_RECAPTCHA_SITE_KEY` | App Check → Apps → web app → reCAPTCHA v3 | App Check simply doesn't initialize — Firestore/Storage/Functions still work normally as long as App Check *enforcement* isn't turned on Console-side (leave it off if you skip this) |

I could not set these as repository variables myself — confirmed by trying: the PAT you provided returned `403 Resource not accessible by personal access token` against `GET /repos/zeahn-tech/TraceOriginal/actions/variables`, even though `api.github.com` itself is reachable from here. They need to be pasted in manually via the GitHub UI.

### 2.5 What you supplied but this app doesn't use
Your config snippet included `databaseURL` (Realtime Database) and `measurementId` (Analytics). This app's data layer is entirely Firestore-based (confirmed: no `firebase/database` import anywhere in `src/`), and no Analytics SDK is wired in anywhere in the codebase. Neither is a gap — the app was never designed to use either — so neither was added to `.env` or the variables table above. Flagging explicitly rather than silently dropping them, in case either is something you actually want added as a new feature (out of scope for "wire in what exists," in scope for a future phase if you'd like it).

## 3. Code-level audit — what I checked and confirmed complete

Read in full and cross-referenced against real usage, not spot-checked:

- **`src/services.ts` (auth section):** `session.signIn`/`signUp`/`google`/`reset`/`logout`/`password` (change-password with reauthentication) — all present, all calling the correct Firebase Auth SDK functions, all with a clear rejection message when `firebaseReady` is false rather than throwing an opaque error. `authErrorMessage()` translates 8 distinct Firebase Auth error codes (including `auth/too-many-requests`, Firebase's own automatic brute-force protection) into user-facing text.
- **`firebase/firestore.rules` (327 lines) and `firebase/storage.rules` (128 lines):** read in full. Every collection/path the app actually reads or writes (`users`, `reports`, `wanted_criminals`, `tips`, `alerts`, `audit_logs`, `rate_limits`, `function_calls`, `function_rate_limits`, and the four Storage paths `reports/`, `criminals/`, `id_cards/`, `profile_images/`) has a corresponding rule block; role/approval checks are re-derived server-side from the caller's own Firestore document (never trusted from client input); privileged mutations (`role`, `isApproved`, `status`, `isVerified`) are locked out of direct client writes entirely, exclusively reachable through the Cloud Functions in §3 below.
- **`firebase/functions/src/index.ts`:** exports 8 functions. Cross-referenced every `httpsCallable(functions, name)` call site in `src/services.ts`'s `callFunction()` wrapper against these exports — all 7 client-callable names (`approveOfficer`, `setUserRole`, `setUserStatus`, `verifyWantedNotice`, `publishAlert`, `transitionReportStatus`, `subscribeToAlerts`) match exactly; the 8th (`notifyOnAlertCreated`) is a Firestore trigger, not client-callable by design, consistent with `FIREBASE_FUNCTIONS_ARCHITECTURE.md`.
- **`src/sw.ts`:** background push message handling (`firebase/messaging/sw`, `onBackgroundMessage`) already wired, gated on the same `VITE_FIREBASE_*` + `VITE_FIREBASE_MESSAGING_SENDER_ID` presence check as the rest of the app, with an explicit "never let a messaging bootstrap failure break the rest of the service worker" fallback.

No code changes were needed anywhere in this audit — the implementation was already complete; it was simply never connected to a real project before now.
