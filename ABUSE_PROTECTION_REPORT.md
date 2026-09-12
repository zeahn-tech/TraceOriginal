# TraceNet — Production Abuse Protection Report

## 0. Status, stated plainly up front

Every item on the requested checklist is implemented, and the parts that can be verified in this sandbox (business logic, the whole client app) are — 23/23 app tests and 54/54 Cloud Functions tests, all actually executed and passing. The parts that require a real Firebase emulator (the `firestore.rules` quota/limit enforcement) are written, carefully reasoned through, and confirmed to load/type-check correctly, but **not executed** — this sandbox cannot reach `storage.googleapis.com` to download the emulator, the same limitation documented in every prior security-related phase of this project (see `IMPLEMENTATION_STATUS.MD`). This is stated once here and not re-litigated per section below; assume it applies to every `firestore.rules` claim unless a section says otherwise.

---

## 1. Audit: abuse surfaces and what protects each one

| Surface | Risk | Protection | Layer |
|---|---|---|---|
| Non-app clients hitting Firestore/Storage/Functions directly (bots, scrapers, scripted abuse) | Cost abuse, data scraping, spam at scale | **Firebase App Check** | Client init + Functions (`enforceAppCheck: true`) |
| Repeated failed sign-in attempts | Credential stuffing, brute force | **Firebase Auth's own automatic abuse protection** (Google-managed, not app code) + graceful client handling | Firebase-managed + `authErrorMessage()` |
| A citizen submitting unlimited incident reports | Storage/Firestore cost abuse, spam, moderation overload | **Per-user daily quota (10/day)** | `firestore.rules` + `services.ts#writeWithQuota` |
| A citizen submitting unlimited tips | Same as above | **Per-user daily quota (15/day)** | Same mechanism |
| A citizen firing unlimited SOS alerts | Public alert spam, responder fatigue/alert fatigue, cost | **Per-user daily quota (5/day)** | Same mechanism |
| Oversized or wrong-type file uploads | Storage cost abuse, unusable data | **Client + server file type/size limits** | `services.ts#validateMediaFile` + `storage.rules` |
| Excessive media attached to a single report/notice | Storage cost abuse | **Per-document media-count caps** (reports ≤6 total, wanted notices ≤4) | `firestore.rules` |
| Oversized text fields (title/description/content) | Firestore document-size/cost abuse, unusable UI | **Field-length caps** | `firestore.rules` |
| A compromised/scripted admin or officer account hammering privileged Cloud Functions | Cost abuse, mass privilege-escalation actions, service disruption | **Per-function, per-caller rate limits** | `firebase/functions/src/lib/rateLimit.ts` |
| A client retrying a failed upload/sync forever | Storage/Firestore cost abuse, wasted bandwidth on a device that will never succeed | **Retry attempt ceiling (5 attempts)** | `services.ts` (`MAX_RETRY_ATTEMPTS`) |
| A double-tap or flaky-network retry executing a privileged command twice | Duplicate side effects, inconsistent audit trail | **Idempotency keys** (already covered in `FIREBASE_FUNCTIONS_ARCHITECTURE.md`; unaffected by this phase) | Cloud Functions |
| Secrets or Admin SDK credentials shipped to the browser | Full backend compromise if leaked | **Audited: none present** — see §6 | N/A |

---

## 2. Firebase App Check

**What it does:** proves a request genuinely comes from this app running in a real browser, not a script, bot, or someone calling your Firestore/Storage/Functions endpoints directly with a stolen API key. It does **not** replace authentication or authorization — a request can be a "real app instance" and still be an unauthorized user; App Check and `firestore.rules`/Cloud Functions role checks are complementary layers, not substitutes for each other.

**Implementation:**
- `src/services.ts`: `initializeAppCheck()` with `ReCaptchaV3Provider`, gated behind a `VITE_RECAPTCHA_SITE_KEY` environment variable.
- All 6 Cloud Functions: `onCall<T>({ enforceAppCheck: true }, handler)` — a request without a valid App Check token is rejected by the framework before the handler code even runs.

**A verified, specific technical detail worth being precise about** (checked by actually building twice, once with the key set and once without — not assumed):
- **Without `VITE_RECAPTCHA_SITE_KEY` set** (e.g. local development), Vite's static `import.meta.env` replacement plus Rollup's dead-code elimination remove the entire App Check code path from the shipped bundle — confirmed by grepping the built output for `ReCaptchaV3`/`app-check`/`recaptcha` and finding zero matches, and no extra chunk file.
- **With the key set**, App Check code loads as its own separate ~22 KB chunk (`index.esm-*.js` in the build output), fetched lazily and only when the app actually initializes App Check — it never blocks or bloats the main bundle.

**Not a secret, and here's why:** a reCAPTCHA v3 *site* key is explicitly designed by Google to be embedded in client-side code — it identifies which reCAPTCHA configuration to challenge against, the same way a Firebase `apiKey` identifies which Firebase project to talk to. The *secret* key that actually verifies challenge responses lives entirely on Google's servers, configured via the Firebase Console, and this app never sees it, holds it, or transmits it. See §6 for the fuller audit of what is and isn't safe to expose.

**Required operational step, not expressible in code:** App Check enforcement toward Firestore and Storage (as opposed to Functions, which is enforced in code above) is a **Firebase Console setting**, not something `firestore.rules`/`storage.rules` syntax can express. This must be turned on manually per-service in the Console once a real App Check provider is configured — documented here as a required deployment step, since it genuinely cannot be done from this codebase.

---

## 3. Authentication abuse protection

**What Firebase already does automatically, with no app code involved:** Firebase Authentication (Identity Platform under the hood) applies its own automatic, Google-managed abuse protection — repeated failed sign-in attempts against an account or from a given client trigger a temporary lockout, surfaced to the app as an `auth/too-many-requests` error. This is not configurable from application code and was already active before this phase; what this phase adds is *handling it well* rather than showing the person a raw, technical error string.

**What this phase added:** `src/services.ts#authErrorMessage()` — translates Firebase Auth error codes into clear, actionable messages, wired into all four Auth call sites (`SignIn`, `SignUp`, `ResetDialog`, change-password):

| Code | Message shown |
|---|---|
| `auth/too-many-requests` | "Too many attempts. For your account's security, sign-in has been temporarily locked — please wait a few minutes and try again." |
| `auth/invalid-credential`, `auth/wrong-password`, `auth/user-not-found` | "Incorrect email or password." (deliberately identical for all three — see note below) |
| `auth/email-already-in-use` | "An account with this email already exists." |
| `auth/weak-password` | "Choose a stronger password (at least 6 characters)." |
| `auth/network-request-failed` | "Network error. Check your connection and try again." |
| anything else | falls back to the raw Firebase message, still surfaced, never silently swallowed |

**Why "incorrect email or password" for three different codes:** this is intentional, and is itself a form of abuse protection — giving a different message for "wrong password" versus "no such user" lets an attacker enumerate which emails have accounts (email enumeration). Using one identical message for all three closes that off. (Firebase also offers a project-level "Email Enumeration Protection" setting in the Console that changes what the SDK itself returns; this app's message mapping produces the same practical outcome regardless of whether that setting is toggled on, so this holds either way.)

**Required operational step, not expressible in code:** password policy (minimum length/complexity) is a **Firebase Console / Identity Platform setting**, not application code — documented here as a required deployment step. Recommended: minimum 8 characters, require at least one number, enable the Console's email enumeration protection setting as defense-in-depth alongside the client-side message mapping above.

---

## 4. Upload limits, media upload limits, per-document quotas

Carried forward from the media-pipeline phase (`MEDIA_PRODUCTION_REPORT.md`) and enforced identically client- and server-side — restated here because this report is the canonical "document all limits" deliverable:

| Limit | Value | Enforced in |
|---|---|---|
| Image file size | ≤ 15 MiB | `services.ts#validateMediaFile` (client) + `storage.rules#withinSizeLimit` (server) |
| Video/audio file size | ≤ 50 MiB | Same |
| Allowed image types | jpeg, png, webp, gif | Same |
| Allowed video types | mp4, webm, quicktime | Same |
| Allowed audio types | mpeg, mp4, wav, webm, ogg | Same |

**New in this phase** — per-document media-count and text-length caps, added to `firestore.rules`:

| Limit | Value | Rationale |
|---|---|---|
| Total attached media per report (images+videos+audio combined) | ≤ 6 | Matches `ReportForm`'s existing UI cap — now enforced server-side too, not just as a client convenience |
| Photos per wanted notice | ≤ 4 | Matches `WantedForm`'s existing UI cap |
| Report/wanted-notice title or name | ≤ 200 characters | Prevents pathological documents; keeps the UI usable |
| Report description / tip content / alert content | ≤ 5,000 characters (alert content: 2,000) | Same rationale, tuned per field's actual use |

---

## 5. Per-user daily quotas — reports, tips, alerts (the core of "report submission limits")

This is the most architecturally significant addition in this phase, so it gets its own detailed section.

### The problem

Nothing previously limited how many reports, tips, or SOS alerts a single account could create. A spammy or malicious citizen — or simply a buggy client stuck in a loop — could create unbounded documents, each potentially carrying attached media, at real Storage/Firestore cost with no ceiling.

### Why this couldn't just be "a Cloud Function"

The obvious-seeming fix — route report/tip/alert creation through a Cloud Function that checks a counter first — was deliberately **not** done, because report creation is architected to work fully offline (the entire point of `MEDIA_PRODUCTION_REPORT.md`/`OFFLINE_REPORTING_PRODUCTION.md`). Cloud Functions require a live network connection; routing creation through one would silently break offline reporting, undoing three prior phases of work. This constraint shaped the actual design below.

### The design: atomic client transaction + independent server-side re-check

`services.ts#writeWithQuota()` performs a single Firestore **transaction** that does two things together, atomically:
1. Creates the report/tip/alert document.
2. Increments the caller's own rolling 24-hour counter at `rate_limits/{uid}_{kind}`.

If the counter is already at the daily limit, the transaction throws *before creating anything* — no partial state, no orphaned document.

Critically, this is **not just a client-side courtesy**. `firestore.rules` independently re-derives and re-checks the exact same constraint using `getAfter()` — a Firestore rules feature that lets a rule inspect the state of a *different* document as it will exist after the current transaction commits. The create rule for `reports`/`tips`/`alerts` requires: the matching counter document must show a count that is exactly one more than it was before this transaction, and that resulting count must not exceed the limit. A modified client that tried to skip the transaction and write the report directly — bypassing the counter increment entirely — is rejected, because `getAfter()` would show the counter unchanged.

The counter document itself has its own independent rules (defense in depth, a third layer): a citizen can only read/write their *own* counter, and any write to it must be a genuine `+1` from the prior value or a legitimate 24-hour-window reset using the server's own `request.time` (never a client-supplied timestamp, which could be forged) — so even attempting to directly overwrite the counter to a fake low value is rejected on its own terms, independent of whatever document creation it was trying to enable.

**How this stays compatible with offline reporting:** the transaction only ever runs at the moment the app actually talks to Firestore — whether that's immediately (online submission) or later, when a queued offline draft is finally synced on reconnect (`processDraft`/`syncOutbox`, unchanged from prior phases). The offline queueing mechanism itself needed no changes; only what happens at the moment of the real network write changed.

### Limits

| Kind | Limit | Window |
|---|---|---|
| Reports | 10 / day | Rolling 24h from first submission of the day |
| Tips | 15 / day | Same |
| SOS Alerts | 5 / day | Same |

These are defined once, in `services.ts#DAILY_LIMITS`, and duplicated (necessarily — rules can't import application constants) in `firestore.rules`' `quotaOk()` calls. **If these numbers are ever changed, both places must be updated together** — flagged explicitly here since it's the one place in this mechanism that isn't self-enforcing.

**Scope note:** `wanted_criminals` creation is intentionally *not* quota-limited — it's staff-only (officer/admin, already gated by the approval workflow), a materially lower abuse-risk population than open citizen self-service, matching the same risk-based reasoning already used throughout this project's rules design.

**Known limitation, stated plainly:** Admin's official alert broadcasts (`publishAlert`, a Cloud Function using the Admin SDK) bypass `firestore.rules` entirely, by design — that path is rate-limited separately, at the Function layer (§7), not by this mechanism. The two rate limits (client-facing quota vs. Function-layer rate limit) protect different things: one bounds a citizen's self-service volume, the other bounds what a privileged account can do even if compromised.

---

## 6. No secrets in the frontend, no Admin credentials in the PWA — audit

Checked directly, not assumed:

- **`firebase-admin` does not appear anywhere in the main app.** `grep -rn "firebase-admin" package.json src/` returns nothing. It is a dependency of `firebase/functions/package.json` only — a completely separate Node project that never ships to the browser.
- **Main app's runtime dependencies are exactly:** `dexie`, `firebase` (client SDK), `lucide-react`, `react`, `react-dom`, `react-router-dom`. No server-only or credential-bearing packages.
- **`.env` is gitignored** (confirmed in `.gitignore`), so local secrets never get committed — though as covered above, everything currently read from `import.meta.env` in this app (`VITE_FIREBASE_*`, `VITE_RECAPTCHA_SITE_KEY`) is public-by-design, not actually sensitive.
- **The production build contains no admin-SDK or test-tooling code** — checked by grepping the actual built `dist/` output for `firebase-admin`/`rules-unit-testing`/`firebase-tools` and finding nothing, not inferred from the dependency list alone.
- **Every privileged operation goes through a Cloud Function**, which is the only place `firebase-admin` (and therefore real elevated privilege) exists at all — see `FIREBASE_FUNCTIONS_ARCHITECTURE.md` for the complete list and the authorization model. Nothing in the browser ever holds a credential capable of bypassing `firestore.rules`/`storage.rules`.

---

## 7. Cloud Functions rate limiting (protecting privileged endpoints from privileged accounts)

New: `firebase/functions/src/lib/rateLimit.ts`, applied to all 6 functions. This is deliberately a **separate mechanism** from the per-user daily quotas in §5 — those protect against a spammy *citizen*; this protects against a compromised, buggy, or scripted *admin/officer* account calling an already-privileged endpoint far faster than any legitimate human workflow would.

Enforced with the Admin SDK against an internal-only `function_rate_limits` collection (never readable or writable by any client — same closed-by-default treatment as `function_calls`, the idempotency bookkeeping table).

| Function | Limit | Rationale |
|---|---|---|
| `approveOfficer` | 30 / hour | Generous for legitimate bulk approval sessions, bounded against a runaway script |
| `setUserRole` | 30 / hour | Same reasoning; also the single highest-privilege-escalation surface in the app |
| `setUserStatus` | 30 / hour | Same |
| `verifyWantedNotice` | 60 / hour | Officers may process many notices in a session |
| `publishAlert` | 10 / hour | Real emergency broadcasts are, and should be, rare |
| `transitionReportStatus` | 120 / hour | Officers actively triaging many reports in a shift |

**Design note, verified deliberately rather than assumed:** the rate-limit check runs on *every* invocation, including idempotent retries of the same `requestId` — not just on invocations that actually execute new side effects. This is intentional: a client stuck retrying the same call rapidly (a bug, or a deliberately abusive script) still costs a real transaction/read even if the underlying command is a safe no-op, and this ensures that cost is bounded too. Confirmed correct — and confirmed *not* to break idempotency itself — by the full 54-test offline suite, including the dedicated idempotency tests, all still passing after this was added.

---

## 8. Retry limits

The `attempts` counter has existed on `MediaJob`/`ReportDraft` since the media-pipeline phase, but was previously tracked without ever being checked against a ceiling — an unbounded retry loop (a stuck client, or a user tapping "Retry" indefinitely on a draft that will never succeed) could hammer Storage/Firestore forever.

**This phase:** `MAX_RETRY_ATTEMPTS = 5` in `services.ts`, now actually enforced:
- `processDraft()` refuses to attempt anything further once a `FAILED` draft has reached the cap — no network activity at all, not even a wasted attempt.
- `retryDraft()` (the function the UI's Retry button calls) refuses to reset a capped-out draft back to a retryable state.
- `PendingDrafts` in `App.tsx` only renders the Retry button while attempts remain; once exhausted, only Discard is offered, with status text explaining why ("Could not sync after 5 attempts. Discard and resubmit as a new report.").

A capped-out draft is not silently lost — it remains visible, labeled clearly, and the person can always discard and submit a genuinely new attempt (a new draft, new id, fresh attempt counter) if the underlying problem (e.g. a since-fixed network issue) has resolved.

---

## 9. Full "document all limits" table

Every numeric limit in the system, in one place:

| Limit | Value | File |
|---|---|---|
| Image file size | 15 MiB | `services.ts`, `storage.rules` |
| Video/audio file size | 50 MiB | `services.ts`, `storage.rules` |
| Media items per report | 6 total | `App.tsx` (`ReportForm`), `firestore.rules` |
| Photos per wanted notice | 4 | `App.tsx` (`WantedForm`), `firestore.rules` |
| Report/wanted-notice title length | 200 characters | `firestore.rules` |
| Report description / tip content length | 5,000 characters | `firestore.rules` |
| Alert content length | 2,000 characters | `firestore.rules` |
| Reports per user per day | 10 | `services.ts` (`DAILY_LIMITS`), `firestore.rules` |
| Tips per user per day | 15 | Same |
| SOS alerts per user per day | 5 | Same |
| `approveOfficer` / `setUserRole` / `setUserStatus` calls per admin per hour | 30 | `firebase/functions/src/lib/rateLimit.ts` |
| `verifyWantedNotice` calls per staff member per hour | 60 | Same |
| `publishAlert` calls per admin per hour | 10 | Same |
| `transitionReportStatus` calls per staff member per hour | 120 | Same |
| Media upload retry attempts (in-flight, per attempt) | 3, with exponential backoff | `services.ts` (`uploadJobFile`) |
| Draft sync retry ceiling (across attempts, user-visible) | 5 | `services.ts` (`MAX_RETRY_ATTEMPTS`) |
| Firebase Auth failed sign-in lockout | Google-managed, not app-configurable | Firebase Auth (automatic) |

---

## 10. Files Changed

| File | Change |
|---|---|
| `src/services.ts` | Added App Check initialization; `RateLimitConfig`/`DAILY_LIMITS`/`writeWithQuota()`; extended `write()`/`saveOutbox()`/`syncOutbox()`/`resolveConflict()` to carry and correctly replay rate-limit config through offline queuing; added `MAX_RETRY_ATTEMPTS` enforcement in `processDraft()`/`retryDraft()`; added `authErrorMessage()` |
| `src/App.tsx` | `TipForm`, `SOS` now pass a rate-limit config on genuine creation (not on updates); `SOS` now receives `user` (a pre-existing gap — it was login-gated but never actually given the user object); `PendingDrafts`/`draftStatusDetail` updated for the retry ceiling; all four Auth call sites use `authErrorMessage()` |
| `firebase/firestore.rules` | Added `rate_limits/{uid}_{kind}` collection rules (self-owned, `getAfter()`-verified increments using server `request.time`, forgery-resistant window logic); added `quotaOk()` checks to `reports`/`tips`/`alerts` create rules; added field-length and media-count caps to `reports`/`wanted_criminals`/`tips`/`alerts` create rules |
| `firebase/emulator-tests/firestore.rules.test.ts` | Added a `createWithQuota()` test helper mirroring the real client transaction shape; updated every report/tip/alert "authorized create" test to use it (bare `setDoc` for these three collections is now correctly expected to fail); added a dedicated quota-mechanism test suite (11 tests: bypass-attempt denial, hitting the 11th report, independent per-user quotas, counter-forgery denial, ownership isolation, tips/alerts quotas) and a field-length/media-count test suite (3 tests). Total grown from 56 to 65 tests |
| `firebase/functions/src/lib/rateLimit.ts` | New — `checkFunctionRateLimit()` |
| `firebase/functions/src/functions/*.ts` (all 6) | Added `enforceAppCheck: true` and a `checkFunctionRateLimit()` call at the top of each handler |
| `firebase/functions/test/mocks/firestore.ts` | Fixed fake `FieldValue.serverTimestamp()` to return an object with a working `.toMillis()` (needed once rate-limit logic started reading its own previously-stored timestamp back within a test) |
| `.env.example` | Rewritten — previously listed unrelated placeholder keys (`GEMINI_API_KEY`, `MAPS_API_KEY`) that this app doesn't use, and didn't list the Firebase config it actually reads. Now accurate, with commentary on why every listed value is safe to be public |

---

## 11. Real bugs found and fixed during this phase

Documented because it's directly relevant to how much confidence to place in the unexecuted parts of this report:

1. `rateLimit.ts` originally called `snap.exists()` — the Admin SDK's `DocumentSnapshot.exists` is a **property**, not a method (the client SDK, confusingly, does use `.exists()`). Caught by `tsc`, not by manual review — a reminder that even careful reasoning misses things a type-checker or test run catches immediately.
2. The offline Functions test suite's fake Firestore mock returned a placeholder string for `FieldValue.serverTimestamp()`. This was invisible for months of prior tests because nothing had ever read a previously-stored timestamp back and called a method on it — the moment rate-limiting logic did exactly that (checking whether a window had expired), it broke, and the fix (return an object with a working `.toMillis()`) was straightforward once the failure pointed directly at it.
3. A `describe(...)` block opening line was accidentally deleted while inserting new tests into `firestore.rules.test.ts` — caught immediately by attempting to load the file (the same "does it at least parse and fail only on `ECONNREFUSED`" check used throughout this project), not by anything more sophisticated.

None of these were caught by reasoning alone; all three were caught by actually running something — the type checker, the offline test suite, or the file-load check. This is the same lesson every prior phase's report has already stated: unexecuted rules code should be trusted less than executed code, by construction, not just in principle.

---

## 12. What this phase does not claim

- The `firestore.rules` additions in this phase (the entire quota mechanism) have not been executed against a real emulator — see §0 and `IMPLEMENTATION_STATUS.MD` for exactly what to run and report back.
- App Check enforcement toward Firestore/Storage requires a manual Firebase Console step this codebase cannot perform.
- Password policy configuration requires a manual Firebase Console/Identity Platform step this codebase cannot perform.
- The `alerts` schema still cannot distinguish an admin broadcast from a citizen SOS at the data-model level (documented since `FIREBASE_SECURITY_IMPLEMENTATION.md`, unchanged here — still a schema change, still out of scope for "don't change the application design").
- No server-side orphan-media cleanup exists yet (would need a scheduled Cloud Function, not built).

**Production readiness overall remains uncertified**, consistent with every prior phase of this project.
