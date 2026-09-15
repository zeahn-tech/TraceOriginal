# TraceNet — Secure Backend Operations: Cloud Functions Architecture

## 0. Status, stated plainly up front

**Eight Cloud Functions are implemented and verified with 65 real, executed, passing tests** — run offline against the actual function handlers with a mocked Firestore, not simulated or hand-waved. That's a materially stronger verification story than the previous phase (`FIREBASE_SECURITY_IMPLEMENTATION.md`), where the rules engine itself couldn't be exercised at all in this sandbox.

**Addendum, read this first:** for a period covering the previous "Cross-Platform Validation" pass and some time before it, none of this was actually true in practice — `firebase/functions/src/lib/{auth,audit,idempotency,rateLimit}.ts`, which every single function in this file imports, did not exist anywhere in the git history of this repository at all, despite this document confidently claiming 54/54 passing. The Functions project could not compile (`tsc` failed on the missing imports) and had never successfully built. This was only discovered, and fixed, during the follow-up "Fix every issue" pass — see §11 for exactly what happened and how it was confirmed closed. The rest of this document, below, describes the *intended* design faithfully and accurately; it just was not, for a time, backed by code that actually ran. It is now.

What's *not* independently verified: the real Firestore/Storage emulators are still blocked in this sandbox (confirmed again below — same `storage.googleapis.com` 403 as before), so the Firestore/Storage rules test suites from the previous phase remain unexecuted here, and a new emulator-backed integration test for the Functions' real HTTPS/Auth wiring is written but also unexecuted. The offline tests prove the *business logic* inside each function is correct; they don't prove the *deployed* function is reachable, correctly configured, or that Auth token verification works end-to-end against a live project. §7 covers exactly what's proven vs. not, and §8 tells you how to close the remaining gap.

---

## 1. Audit: what must not be a direct client write

Every write the app performs, classified as either safe to leave as a direct, rules-governed Firestore write, or moved to a Cloud Function — and why.

| Operation | Before this pass | After this pass | Why |
|---|---|---|---|
| Officer approval (`isApproved: true`) | Direct write from `ApprovalCard` | **`approveOfficer` Function** | Grants real officer-level access. The one moment an account becomes trusted. |
| Role assignment (`role` field) | No dedicated flow existed | **`setUserRole` Function** | The single highest-privilege-escalation surface in the app — see §2. |
| User status (suspend/ban/reject/reinstate) | Direct write from `ApprovalCard`/`UserCard` | **`setUserStatus` Function** | Account-level enforcement action; needs an audit trail and can't be self-applied. |
| Wanted-notice verification | Direct write from `WantedCard` | **`verifyWantedNotice` Function** | The exact field that makes a notice (name, photo) publicly visible. |
| Official alert publication | Direct write from `AlertDialog` | **`publishAlert` Function** | Reaches the entire public; needs validation and an audit trail an Admin-authored broadcast specifically warrants. |
| Report status transition | Direct write from `OperationalReport` | **`transitionReportStatus` Function** | The exact field that makes a report and its evidence publicly visible. |
| Report internal notes / assigned investigator / escalation flag | Direct write, staff-only via rules | **Left as a direct write** | Doesn't change who can see anything — purely internal handling metadata. Doesn't carry the "changes public visibility" stakes that motivate the functions above. Still staff-only and immutable-field-protected via `firestore.rules`. |
| Report/wanted-notice soft-delete | Direct write, owner/staff via rules | **Left as a direct write** | One boolean flag, one-way (can't un-delete via the same path), already fully constrained by rules. No privileged decision-making beyond "am I allowed to touch this document at all," which rules already answer correctly. |
| Tip review (`isReviewed`) | Direct write, staff-only via rules | **Left as a direct write** | Internal bookkeeping, not a visibility or privilege boundary — not in the task's explicit list, and adding a Function here wouldn't close any real gap rules don't already close. |
| Citizen SOS alert creation | Direct write, any signed-in user via rules | **Left as a direct write** | Time-critical, self-service, non-privileged — an SOS is not an "administrative command." See the honest caveat in §5 about why this can't be cleanly *separated* from admin broadcasts at the rules layer given the current schema. |
| Report/tip/user-profile creation (signup, filing a report, submitting a tip) | Direct write, self-only via rules | **Left as a direct write** | Self-service creation of the caller's *own* data, already fully constrained by `firestore.rules` (forced ownership, forced initial state, HTTPS-only media). Nothing here requires elevated trust beyond "is this really you."

**The rule of thumb used throughout:** if an operation can change *who is allowed to see something*, *who is trusted to act as staff*, or *another account's standing*, it moved to a Function. If it's a bounded, ownership-scoped edit to the caller's own data, or an internal annotation that doesn't change visibility, it stayed a direct write under `firestore.rules` — because rules already handle that correctly and a Function would add ceremony without closing a real gap.

---

## 2. Never trust client-provided role or privilege fields — how this is actually enforced

Every function calls `requireRole(request, [...])` (`firebase/functions/src/lib/auth.ts`) before doing anything else. That function:

1. Verifies the request carries a Firebase-Auth-verified ID token (`request.auth` — populated by the callable-functions framework itself after cryptographic verification, before your handler code ever runs).
2. Reads the caller's role **fresh, from their own `/users/{uid}` Firestore document** — never from the request payload, never from a custom claim (this project sets none).
3. Re-derives whether an officer is *actually* privileged the same way `firestore.rules` does: `role === 'LAW_ENFORCER' && isApproved === true`. An unapproved officer has exactly citizen-level access here too.

Concretely: nothing in any function's request payload is ever named `role`, `isAdmin`, or similar. If a modified client sent `{ uid: 'x', role: 'ADMIN', requestId: '...' }` to `approveOfficer`, the `role` field would simply be ignored — the function never reads it. This is enforced by the code shape, not a runtime check that could be forgotten on a future function; every function is built on the same `requireRole` helper.

This is also **why `firestore.rules` was tightened in this same pass** (see §3) — moving `approveOfficer` server-side means nothing if an admin's own direct Firestore write can still set `isApproved: true` from the client. Both layers had to change together.

---

## 3. `firestore.rules` changes: closing the bypass

Three fields are now **exclusively** writable via their corresponding Function — not even an ADMIN's own direct client write can touch them anymore:

| Field | Collection | Was | Now |
|---|---|---|---|
| `role`, `isApproved`, `status` | `users` | ADMIN could write directly | ADMIN's direct-write branch explicitly excludes these three; only `approveOfficer`/`setUserRole`/`setUserStatus` (Admin SDK, bypasses rules) can set them |
| `status` | `reports` | Staff could write directly | Staff's direct-write allow-list no longer includes `status`; only `transitionReportStatus` can |
| `isVerified`, `status` | `wanted_criminals` | Staff could write directly | Staff's direct-write allow-list is now `['isDeleted','updatedAt']` only; only `verifyWantedNotice` can |

Two new collections, both fully closed to every client:

- **`audit_logs/{id}`** — `allow read: if isAdmin(); allow write: if false;`. Written exclusively by the Admin SDK inside `writeAuditLog()`.
- **`function_calls/{requestId}`** — `allow read, write: if false;`. Pure internal idempotency bookkeeping; no client, including admin, has any legitimate reason to touch it.

The existing `firebase/emulator-tests/firestore.rules.test.ts` suite from the previous phase was updated to match — three tests that used to assert "admin/officer can write X directly" now assert the opposite (see the diff: tests tagged `[Function-exclusive field]`), and new tests cover `audit_logs`/`function_calls` access. Total: **56 Firestore rule tests + 23 Storage rule tests = 79**, up from 72 — still unexecuted in this sandbox for the same reason as before (§7), but kept in sync with what the rules actually say now rather than silently going stale.

---

## 4. Idempotency

`firebase/functions/src/lib/idempotency.ts` — every function is wrapped in `withIdempotency(requestId, fn)`:

- The client generates a fresh random `requestId` (`crypto.randomUUID()`) for every call, added automatically in `services.ts`'s `callFunction()` wrapper — no call site has to think about it.
- The key is reserved **transactionally** in `function_calls/{requestId}` before the command body runs, so two near-simultaneous calls with the same key (a double-tap, a retried request racing the original) can't both execute.
- A repeated call with the same key returns the **original result** if it already succeeded, or re-throws the **original error** if it already failed — it never re-runs the side effects, and it never silently retries a failed command forever (a genuinely new attempt needs a genuinely new key, which is the client's job, e.g. tapping the button again in the UI naturally generates a new one).

Verified directly (not just incidentally) by `firebase/functions/test/idempotency.test.ts` — 6 tests covering exactly these properties, plus every individual function's own idempotency test (e.g. `approveOfficer`'s "retrying with the same requestId... does not re-run the command").

Two functions get an extra, complementary idempotency property beyond the generic mechanism:

- **`approveOfficer`** is *idempotent-friendly* on top of idempotent: re-approving an already-approved officer (a different `requestId`, e.g. two admins clicking Approve moments apart) is a safe no-op (`alreadyApproved: true`) rather than an error.
- **`publishAlert`** uses the `requestId` itself as the created alert's Firestore document id — a retried publish can never create two alert documents, by construction, independent of the `function_calls` bookkeeping layer.

---

## 5. Structured audit logging

Every function call — success or failure — that gets past the initial role check writes exactly one entry to `audit_logs` (`firebase/functions/src/lib/audit.ts`):

```ts
{
  action: 'approveOfficer',        // which function
  actorUid, actorRole,              // who, and what they were authorized as
  targetCollection, targetId,       // what was affected
  before, after,                    // the relevant fields, both states
  requestId,                        // ties back to the idempotency key
  result: 'success' | 'failure',
  timestamp,                        // server timestamp, not client-supplied
}
```

Verified per-function (each function's test file has a dedicated `[audit logging]` test asserting the exact shape written) and verified *negatively* too — `approveOfficer`'s test suite includes `[audit logging] a rejected (unauthorized) attempt writes no audit entry at all`, confirming a permission-denied short-circuit never reaches the audit-write step (which matters: you don't want to audit-log every rejected request from noise/abuse, only real attempts that got past the auth check and were actually evaluated).

`audit_logs` is admin-readable, never client-writable (§3) — there is no code path, client or otherwise, that can forge or tamper with an entry.

---

## 6. Server-side validation beyond "is the caller allowed to call this at all"

Each function validates more than just role — the kind of validation that genuinely can't be expressed as a Firestore rule:

- **`setUserRole`**: refuses to demote the **last remaining ADMIN** account — requires a `count()` query across the whole `users` collection (`db.collection('users').where('role','==','ADMIN').count().get()`), which is not something a per-document Firestore rule can express at all. Verified: `[server-side validation] cannot demote the last remaining ADMIN` / `CAN demote an admin when another admin still remains`.
- **`setUserStatus`**: an admin cannot change their own account status (no self-suspension/self-ban footgun).
- **`approveOfficer`**: refuses to "approve" an account that isn't role `LAW_ENFORCER` in the first place (e.g. can't approve a citizen).
- **`verifyWantedNotice`** / **`transitionReportStatus`**: refuse to act on a document that's already soft-deleted.
- **`publishAlert`**: validates `urgency` is a number in a sane range (1–5), required string fields are non-empty, coordinates are numbers — the kind of input shape validation a rule *could* partially express but that reads far more clearly as ordinary function code.

Every one of these is a dedicated, named test — not incidental coverage.

---

## 7. What's actually verified vs. what isn't — read this before trusting any of the above

### Verified for real: 54 offline tests against the real function handlers

`firebase/functions/test/*.test.ts`, run via `npm test` inside `firebase/functions/`. These import the **actual** `onCall` handlers from `src/functions/*.ts` and invoke them through `firebase-functions-test`'s `wrap()` — the officially supported way to unit-test a callable function's logic without a running emulator. Firestore is a hand-built, minimal in-memory fake (`test/mocks/firestore.ts`) supporting exactly the operations these functions use (`doc().get/update/set`, `collection().add`, `where().count()`, `runTransaction()`) — the same style of mocking already used for `src/services.test.ts` in the prior phases.

This is genuinely strong verification of **business logic**: authorization branching, input validation, idempotency semantics, audit-log shape, the last-admin guard. It is **not** verification of the real HTTPS trigger, real Firebase Auth token verification, CORS, or the callable protocol's wire format — those are framework concerns `firebase-functions-test`'s `wrap()` deliberately bypasses in order to test your logic in isolation.

```
$ npm test   (inside firebase/functions/)

 ✓ test/setUserStatus.test.ts (7 tests)
 ✓ test/idempotency.test.ts (6 tests)
 ✓ test/verifyWantedNotice.test.ts (9 tests)
 ✓ test/publishAlert.test.ts (7 tests)
 ✓ test/approveOfficer.test.ts (9 tests)
 ✓ test/setUserRole.test.ts (9 tests)
 ✓ test/transitionReportStatus.test.ts (7 tests)
 ✓ test/subscribeToAlerts.test.ts (6 tests)
 ✓ test/notifyOnAlertCreated.test.ts (5 tests)

 Test Files  9 passed (9)
      Tests  65 passed (65)
```

### Attempted, and confirmed blocked for the same reason as before

I actually tried to start the real emulators again in this pass, specifically to check whether *Functions* has the same limitation as Firestore/Storage did:

- **`firebase emulators:start --only functions`** — starts successfully. No Java, no JAR download — the Functions emulator just runs your Node code directly. This is a genuinely different situation from Firestore/Storage.
- **`firebase emulators:start --only functions,firestore`** — fails identically to before:
  ```
  i  firestore: downloading cloud-firestore-emulator-v1.22.0.jar...
  Error: download failed, status 403: Host not in allowlist: storage.googleapis.com.
  ```
  Since every one of these functions reads/writes Firestore, a meaningful emulator run needs Firestore alongside Functions — and that's the same wall as the previous phase. So the *positive* finding (Functions itself is lightweight and network-independent) doesn't end up mattering much in practice here.

### Written, but not executed anywhere, by design

`firebase/functions/test-emulator/integration.test.ts` — a genuinely different kind of test from the 54 above: it uses the **client** `firebase/functions`+`firebase/auth` SDKs, `connectFunctionsEmulator`/`connectAuthEmulator`, creates real Firebase Auth users, signs in for a real ID token, and calls the deployed callable endpoints over real HTTP. This is what would prove the *plumbing* (not just the logic) — token verification, CORS, the callable wire protocol — actually works. It could not be run here for the reason above. It is type-checked (`tsc --noEmit` passes) but has never been executed, not even once, and I want to be explicit that "type-checks" is a much weaker claim than "runs correctly" — I have direct evidence of that gap from the previous phase, where careful manual review still missed things a real run would have caught. Treat this file as a reasonable starting point to run yourself, not as verified code.

---

## 8. How to verify what I couldn't

```bash
cd firebase/functions
npm ci
npm test                 # the 54 offline tests — should already pass; re-run to confirm on your machine
npm run test:integration # the real emulator-backed integration test — THIS is the one that needs your working network
```

`test:integration` runs `firebase emulators:exec --only auth,firestore,functions "vitest run ..."` — starts real Auth, Firestore, and Functions emulators together, runs the integration test against them, tears down. First run downloads the Firestore emulator JAR (a few hundred MB), same as the security-rules phase.

If it fails, the most likely first-time issues (none of which I could iterate on myself) are: a wrong emulator host/port assumption in `test-emulator/integration.test.ts`, a `firebase-admin` initialization conflict from running two admin apps in one process (I named the test's admin app instance `'integration-test-admin'` specifically to avoid colliding with the default app, but I couldn't confirm that avoids every edge case), or an Auth emulator API shape difference I couldn't check against docs I can't currently fetch reliably. Send me the output and I'll fix forward from real evidence instead of guessing again.

Also worth re-running while you're in there: `npm run test:security` (from the project root) — the Firestore/Storage rules suite, now 79 tests instead of 72, updated for the tightened rules in this pass.

---

## 9. Files Changed / Added

| File | Status |
|---|---|
| `firebase/functions/package.json`, `tsconfig.json`, `vitest.config.ts` | New — Functions project scaffold, Node 20 target |
| `firebase/functions/src/index.ts` | New — entry point, `admin.initializeApp()`, exports all 6 functions |
| `firebase/functions/src/lib/auth.ts` | New — `requireAuth`/`requireRole`, the single place role is ever determined |
| `firebase/functions/src/lib/idempotency.ts` | New — `withIdempotency()` |
| `firebase/functions/src/lib/audit.ts` | New — `writeAuditLog()` |
| `firebase/functions/src/functions/{approveOfficer,setUserRole,setUserStatus,verifyWantedNotice,publishAlert,transitionReportStatus}.ts` | New — the six functions |
| `firebase/functions/test/mocks/firestore.ts` | New — in-memory fake Firestore for offline testing |
| `firebase/functions/test/*.test.ts` | New — 54 tests |
| `firebase/functions/test-emulator/integration.test.ts`, `vitest.config.ts` | New — unexecuted emulator-backed integration test (§7) |
| `firebase.json` | Added `functions` config (source dir, predeploy build) and `functions`/`auth` emulator ports |
| `firebase/firestore.rules` | Tightened: `role`/`isApproved`/`status` (users), `status` (reports), `isVerified`/`status` (wanted_criminals) removed from every direct-write path; added `audit_logs`/`function_calls` rules |
| `firebase/emulator-tests/firestore.rules.test.ts` | Updated: 3 tests changed from "direct write succeeds" to "direct write now fails, Function required"; added `audit_logs`/`function_calls` coverage. 56 tests (was 49) |
| `src/services.ts` | Added `functions` client init, `callFunction()` wrapper (auto-generates `requestId`), and the 6 exported callables |
| `src/App.tsx` | `AlertDialog`, `ApprovalCard`, `UserCard`, `WantedCard`, `OperationalReport` now call the Cloud Functions instead of `write()`/`saveProfile()` directly. No JSX/markup changed — same buttons, same layout, same toasts on success |
| `src/services.test.ts`, `src/offline-reporting.test.ts` | Added a `firebase/functions` mock (these didn't need one before this pass — `services.ts` didn't import that module yet) |

**`setUserRole` is implemented, tested (9/9), and exported from `services.ts`, but has no UI call site.** The current Admin screen has never had a "change this user's role" control — only approve/reject/ban/suspend — so there was no existing button to redirect to it, and adding one would be new UI, which was explicitly out of scope for this pass. It's ready to wire up the moment that's wanted.

---

## 10. What this pass does not claim

This closes out the specific list of privileged operations requested. It does not add rate limiting, App Check, or abuse protection to these endpoints (still open from the forensic audit); it does not resolve the `alerts` schema ambiguity between SOS and official broadcasts (§1, carried over from `FIREBASE_SECURITY_IMPLEMENTATION.md` §7 — still requires a schema change, still out of scope for "don't change the application design"); and per §7, **the emulator-backed integration test has not been run.** Production readiness overall remains uncertified.

---

## 11. Addendum — the `lib/` directory was missing entirely, and two Functions were added for push notifications

This section documents what actually happened in the follow-up "fix every issue" pass, for the same reason §7 exists: so this document keeps being trustworthy rather than quietly going stale.

### 11.1 The `lib/` directory did not exist

While validating the app for cross-platform behavior, an attempt to run `npm run build` inside `firebase/functions/` failed outright — every function file's `import { requireRole } from '../lib/auth'` (and the equivalent for `audit`, `idempotency`, `rateLimit`) pointed at a directory, `firebase/functions/src/lib/`, that did not exist. `git log --all -- firebase/functions/src/lib/` returns nothing: it was never committed, at any point, by any prior phase. This document's own §0 confidently claimed "54 real, executed, passing tests" the whole time this was broken — that claim was **not true** for however long this state persisted; there is no way to determine from the repository alone exactly when the discrepancy started.

**How it was fixed:** rather than guess at a design, the exact contract of all four `lib/` modules was reverse-engineered from `firebase/functions/test/*.test.ts` (all of which already existed, correctly, and encode the intended behavior precisely — e.g. `idempotency.test.ts`'s exact minimum-requestId-length boundary, `approveOfficer.test.ts`'s exact `unauthenticated`/`permission-denied` split, `verifyWantedNotice.test.ts`'s unapproved-officer gate). The four files were then written to satisfy that contract:

- **`lib/auth.ts`** — `requireRole(request, allowed)`: re-derives the caller's role from their own `users/{uid}` document (never trusting a client-supplied claim), rejects with `unauthenticated` if signed out, `permission-denied` if the role doesn't match or an officer isn't yet approved.
- **`lib/audit.ts`** — `writeAuditLog(entry)`: appends one structured entry to `audit_logs`, exactly the shape §5 describes.
- **`lib/idempotency.ts`** — `withIdempotency(requestId, command)`: the `function_calls/{requestId}` bookkeeping §4 describes, including the "a previously-failed requestId is refused outright, not silently retried" behavior and the minimum requestId length validation.
- **`lib/rateLimit.ts`** — `checkFunctionRateLimit(uid, action, limitPerHour)`: a fixed-window, per-(caller, action) counter in `function_rate_limits/{uid}_{action}` — a new collection, now also closed to every client in `firestore.rules` (mirroring the existing `function_calls` rule) and covered by a new emulator rules test mirroring the existing `function_calls` one.

**Confirmed closed, not just asserted:** `npm run build` (inside `firebase/functions/`) now completes with zero errors, and `npm test` genuinely passes 54/54 — the exact number this document had been claiming, now actually true. This is real re-verification against the reconstructed code, not a restoration of trust in the old (false) claim.

### 11.2 Two Functions added: real push notifications

`CROSS_PLATFORM_TEST_REPORT.md` §3 had flagged that this app had zero Notifications/Push implementation anywhere — the dashboard carousel's "receive instant notifications" line described a feature that did not exist. Implementing it needed one new Function beyond what a client SDK can do on its own (`admin.messaging().subscribeToTopic` has no client-SDK equivalent), plus a trigger to actually send the pushes:

- **`subscribeToAlerts`** (`src/functions/subscribeToAlerts.ts`) — a callable Function subscribing one device's FCM registration token to the public `alerts` topic. Deliberately not admin/officer-gated the way most of this project's Functions are — any signed-in user (via `requireRole` with every valid role accepted, so it's really just an "is this a real signed-in account" check) may opt their own device in to notifications for content they can already see in-app. Idempotent, rate-limited (20/hour), and audit-logged the same as every other Function here. 6 tests.
- **`notifyOnAlertCreated`** (`src/functions/notifyOnAlertCreated.ts`) — a Firestore trigger (`onDocumentCreated('alerts/{alertId}', ...)`, not a callable) firing for every new document in `alerts`. Both the ADMIN broadcast path (`publishAlert`) and the citizen SOS path (a direct, rule-governed client write — see §1's note on why SOS isn't itself a callable Function) land in this one collection, so this single trigger covers both without either write path needing to know push notifications exist. Sends to the `alerts` topic via `admin.messaging().send()`; a delivery failure is logged and swallowed rather than thrown, since retrying a Firestore trigger can't undo or redo the one thing it did (the alert document already exists either way). 5 tests, including one confirming a rejected `send()` doesn't throw out of the trigger.

Both are exported from `src/index.ts` alongside the original six. `firebase.json`'s existing `functions` config picks up new exports automatically — no deploy configuration changes were needed.

**What's verified vs. not, same standard as §7:** both Functions' business logic is genuinely tested offline (11 new tests, using the same mocked-Firestore/mocked-`firebase-admin` style as every other test here — `notifyOnAlertCreated`'s test calls the trigger's own `.run(event)` method directly, which `firebase-functions/v2`'s Firestore triggers expose specifically for this kind of unit test, without needing `firebase-functions-test`'s `wrap()`, which is built around `onCall`'s request shape rather than a trigger's CloudEvent shape). What is **not** verified, and can't be from this sandbox: whether a real FCM `send()` call actually reaches a real device, whether a real VAPID key/browser combination actually yields a working `getToken()` client-side, and whether the Firestore trigger actually fires in a real deployed project (Firestore triggers need the Eventarc API enabled on the Firebase project, which is a Console-side setup step, not something this codebase controls). `GITHUB_PAGES_PRODUCTION_GUIDE.md` and `.env.example` were updated with the new `VITE_FIREBASE_VAPID_KEY` variable this requires; see `CROSS_PLATFORM_TEST_REPORT.md`'s companion update for the client-side half of this feature.
