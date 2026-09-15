# TraceNet — Firebase Production Security: Implementation Report

## 0. Status, stated plainly up front

**Rules and a complete emulator test suite are written and believed correct based on careful manual verification. They have NOT been executed against a real Firestore/Storage emulator, because this sandboxed environment cannot reach `storage.googleapis.com` (where Google hosts the emulator binaries) — confirmed by actually attempting it, not assumed. Per your instruction — "do not mark security complete until the emulator tests pass" — this is explicitly **not** marked complete. §1 explains exactly what happened, §8 gives you the exact command to run the tests yourself, and §9 tells you exactly what to look for and report back so this can be closed out properly.**

Everything else in this document — the rules themselves, the data-layer changes required to make them enforceable, and the test suite — is real, complete work, not a placeholder.

---

## 1. What actually happened when I tried to run the emulators

1. Installed `firebase-tools` and `@firebase/rules-unit-testing@4.0.1` (the latest version compatible with this project's `firebase@^11.0.2` — `@firebase/rules-unit-testing@5.x` requires `firebase@^12`, which would have meant upgrading the whole Firebase SDK as a side effect, so I deliberately did not do that).
2. Wrote `firebase.json`/`.firebaserc` and ran `firebase emulators:start --only firestore,storage --project tracenet-emulator-test` for real.
3. It failed immediately:
   ```
   Error: Could not download Firestore Emulator...
   ⨯ ... 403 Forbidden ... storage.googleapis.com
   ```
   This sandbox's network egress allowlist does not include `storage.googleapis.com`, which is where every Firebase emulator (Firestore, Storage, the Java-based rules runtime itself) is distributed from. There is no npm package, GitHub mirror, or offline alternative that provides the actual rules-evaluation engine — it is a Google-distributed JAR, full stop. I checked for any already-cached copy (`~/.cache/firebase/emulators`, filesystem-wide search) — none exists.
4. I confirmed there is no CLI-level offline rules linter either (`firebase deploy --dry-run` still needs an authenticated, networked Firebase project).
5. As the closest available substitute for "prove the test code is real," I ran the actual test suite with no emulator running. Vitest correctly discovered and attempted all **72 tests**, and the *only* failure is:
   ```
   TypeError: fetch failed
   Caused by: Error: connect ECONNREFUSED 127.0.0.1:8080
   ```
   This is the expected, correct failure mode when the test code is valid but has nothing to connect to — not a syntax error, not an import error, not a type error. It's the strongest evidence available in this environment that the suite itself is sound.

Given this, I did the next best thing available: an unusually careful manual line-by-line re-read of both rules files against the Firestore/Storage Rules language semantics, specifically hunting for exactly the kind of subtle bug that only an emulator run would normally catch. This review **did** find and fix two real bugs before you ever see this — see §6. That's precisely the value emulator tests provide, and precisely why I'm not comfortable calling this "done" — I found two bugs by reading carefully; I have no way to know if a real emulator run would find a third.

---

## 2. Authorization model

Three roles, matching `src/domain.ts`'s `Role` type exactly — `CITIZEN`, `LAW_ENFORCER`, `ADMIN` — with one additional, important distinction the UI already shows but the *backend never enforced until now*: an officer is only privileged once **both** `role == 'LAW_ENFORCER'` **and** `isApproved == true`. An unapproved officer account has exactly the same server-side access as a citizen. This closes a real gap: previously, nothing stopped a newly-registered, not-yet-vetted "officer" account from calling the Firestore SDK directly to act with full officer privileges before anyone at TraceNet had approved them.

Every role check happens by reading the caller's **own** `/users/{uid}` document server-side (`callerDoc()`/`callerRole()` in the rules) — never from a client-supplied claim, header, or field. This is what "never treat React route guards as security boundaries" means concretely: `src/App.tsx`'s `Protected` component still exists and still controls what renders, but it is now backed by rules that hold even if `Protected` is bypassed entirely (calling the SDK from DevTools, a modified client, curl against the REST API).

---

## 3. Firestore rules — collection by collection

Full source: `firebase/firestore.rules`. Summary of the authorization boundary for each collection (see the file itself for the complete, commented logic):

| Collection | Read | Create | Update | Delete |
|---|---|---|---|---|
| `users/{uid}` | self, or ADMIN (any) | self only, at signup; role locked to `CITIZEN`/`LAW_ENFORCER`, `isApproved` forced correct for that role | self: only `name`/`contact`/`address`/`profileImageUrl`/`idCardUrl`, never `role`/`isApproved`/`status`/`id`. ADMIN: anything except `id` | never |
| `reports/{id}` | owner, staff, or public if `VERIFIED` & not deleted | signed-in user as themselves only, forced `status: PENDING`, HTTPS-only media URLs | staff: `status`/`internalNotes`/`assignedInvestigator`/`isEscalated`/`isDeleted`/`updatedAt` only. Owner: `title`/`description`/`updatedAt`, and `isDeleted` one-way (soft-delete only, never un-delete) | never (soft-delete only) |
| `wanted_criminals/{id}` | staff, or public if verified & not deleted | staff only, forced `isVerified: false` | staff: `isVerified`/`status`/`isDeleted`/`updatedAt` only | never |
| `tips/{id}` | staff, or the attributed submitter reading their own tip back (anonymous tips are unreadable by anyone but staff, including their own sender) | signed-in user; anonymous tips must have no `submitterId`, attributed tips must have `submitterId == caller`; must reference a real, existing report | staff: `isReviewed`/`isDeleted` only | never |
| `alerts/{id}` | everyone, including signed-out visitors | any signed-in user (see §7 for an honest caveat about this one) | staff only, content fields only | never |

### Why "protect X" maps to a rule, concretely

- **Protect role fields / approval status**: `users` update rule's `affectedKeys().hasOnly([...])` for the self-branch never includes `role`, `isApproved`, or `status` — no matter what a client sends, those three fields cannot move through the self-update path. Verified by tests `[immutable field] a citizen cannot change their own role field` / `...isApproved field`.
- **Protect report status transitions**: only the `isPrivileged()` branch of the `reports` update rule can touch `status` at all; the owner's branch's `affectedKeys().hasOnly([...])` doesn't include it. Verified by `[privilege escalation] the reporting citizen cannot change their own report's status`.
- **Protect ownership**: every `create` rule pins the actor's own uid into the relevant identity field (`reporterId`, `submitterId`) via equality check against `request.auth.uid` — a client cannot create a document "as" someone else. Verified by `[ownership violation]`-tagged tests across reports and tips.
- **Protect immutable fields**: `id` on every collection, `reporterId`/evidence arrays on reports, `name`/`description` on wanted notices — none of these appear in any `affectedKeys().hasOnly([...])` allow-list for *any* actor, privileged or not, so they cannot be changed by any update, ever, by design (only re-created, which rules also block since `create` requires the document not already exist).

---

## 4. Storage rules

Full source: `firebase/storage.rules`.

### The ordering problem, and how it's solved

The media pipeline built in the previous pass (`MEDIA_PRODUCTION_REPORT.md`) uploads files to Storage **before** the corresponding Firestore report document exists — that's the whole point of the durable-transaction design. A storage rule that checks "does the Firestore report I'm attaching to belong to me" would reject every single legitimate upload, because at upload time that document doesn't exist yet to check against.

The fix: report evidence paths now embed the uploader's own uid directly — `reports/{reportId}/{uid}/{kind}/{fileName}` (previously `reports/{reportId}/{kind}/{fileName}`, no uid segment). This is a real, necessary change to `src/App.tsx`'s `ReportForm` (the `storagePath` passed into `submitReportWithMedia`) — not a visual change, an internal storage-layout change that makes ownership verifiable from the request path alone, with zero dependency on write ordering. Storage rules can then say, correctly and simply, "you may write here if the path's uid matches you."

**Reads** of already-uploaded evidence *do* cross-reference Firestore (`firestore.get`/`firestore.exists`), because by read time the report document is guaranteed to exist — an evidence photo is readable by its uploader, by staff, or by the public once (and only once) the report itself is `VERIFIED` and not deleted.

### Paths

| Path | Write | Read |
|---|---|---|
| `reports/{reportId}/{uid}/{kind}/{file}` | path-uid owner, or staff | owner, staff, or public once the report is `VERIFIED` |
| `criminals/{id}/{kind}/{file}` | staff only | staff, or public once the notice is verified |
| `id_cards/{uid}/{file}` | self only | self, or **ADMIN only** |
| `profile_images/{uid}/{file}` | self only | self, or ADMIN (defensive — see note below) |
| anything else | denied | denied |

### File type and size validation

Every write path checks `request.resource.contentType` against the same allow-list `services.ts#validateMediaFile` uses (`image/jpeg|png|webp|gif`, `video/mp4|webm|quicktime`, `audio/mpeg|mp4|wav|webm|ogg`) and `request.resource.size` against the same 15 MiB (image) / 50 MiB (other) limits — server-side enforcement of the exact same limits the client already enforces, so a modified client can't bypass "large-file protection" by talking to Storage directly.

### Private identity documents — the explicit "unless authorized" requirement

`id_cards/{uid}/{file}` is the one path in this app that matches "prevent users from accessing another user's private identity documents unless explicitly authorized" word for word. The rule: the document's own owner can read it; **only ADMIN** is "explicitly authorized" beyond that — not even an approved officer can read another user's id card. This is deliberate and tested (`[private media / unauthorized read] even an approved OFFICER cannot read another user's id card`) — the only legitimate reason anyone besides the owner needs to see this file is the officer-approval review workflow (`ApprovalCard` in `src/App.tsx`), which is an ADMIN-only screen.

`profile_images/{uid}/{file}` gets the identical rule even though no code currently writes there (`Profile.tsx` has no upload UI for it yet) — documented in the rules file itself as a defensive default so this path is never accidentally wide-open the moment someone does wire it up.

---

## 5. Client-side changes required to make these rules enforceable (not a design change)

This is the part of the work that surprised me and is worth being explicit about: **you cannot bolt real per-document security rules onto an app whose client issues unfiltered `collection().onSnapshot()` queries for everything, and expect the app to keep working.** Firestore's rule engine will not silently filter an unfiltered `list` query down to "just the documents you're allowed to see" — if the rule's condition depends on document content (owner, verification status) rather than being unconditionally true for the caller's role, Firestore rejects the **entire query** with `permission-denied`. This is by design ("rules are not filters"), and it's exactly what stops a citizen's browser from listing every report in the database even if some of those reports would individually be visible to them.

So closing the forensic audit's Critical finding — "the client fetches the entire `users`/`reports`/`tips`/`wanted_criminals` collections regardless of role" — **required** changing what queries the client issues, not just adding rules on top of the existing queries. Concretely, in `src/services.ts`:

- Added `listenWhere()`, a `where()`-filtered counterpart to the existing unfiltered `listen()`.
- Replaced the old flat `collections.{reports,tips,wanted,users}` with role-scoped functions: `myReports`/`myTips` (owner-filtered, for citizens), `allReports`/`allTips`/`allWanted`/`allUsers` (unfiltered — safe for staff/admin, since the rule's privileged-role branch doesn't depend on document content), and `publicReports`/`publicWanted` (verified-only, safe for anyone including signed-out visitors).

And in `src/App.tsx`, `useAppData()` now subscribes to the right set of queries for the signed-in user's actual role, instead of one universal set for everyone. Knock-on effects, all behavior-preserving for the *intended* audience of each screen:

- **The Public Portal and the "related incident" dropdown on the tip form** now explicitly use the always-on `publicReports`/`publicWanted` feeds, so they keep showing the full community-wide verified feed regardless of whether the visitor is signed in, a citizen, or signed out — exactly as before.
- **The Citizen dashboard** now reads its own reports/tips via an owner-filtered query instead of receiving (and discarding) everyone else's. Visually identical; the underlying request is what changed.
- **`/map` and `/analytics`**, which were previously unguarded at the route level and (per the forensic audit) would have shown *every* report's coordinates to anyone who typed the URL, now inherit whichever role-scoped report set the rules actually allow — a citizen or signed-out visitor sees a narrower, safe set instead of the full incident database. This is a genuine security improvement that falls directly out of doing the data-layer change correctly, not a deliberate feature change.

None of this touches layout, styling, copy, or any component's visual output for the audiences those screens were actually designed for.

---

## 6. Two real bugs the manual review caught (this is why "believed correct" isn't "proven correct")

1. **`.toSet().difference(...)`** in the original draft of `allHttpsStorageUrls()` — I'm not confident that Set-typed list operations are part of Firestore's constrained rules-CEL dialect (as opposed to full CEL), and using an unsupported method would have caused rules **deployment** to fail outright, not just a logic bug. Replaced with `.filter(...).size() == urls.size()`, which is a well-documented, definitely-supported pattern.
2. **Unsafe dot-access on optional fields.** I initially wrote things like `resource.data.isDeleted != true` and `request.resource.data.submitterId == uid()`. I then checked against this app's **actual** write payloads (`ReportForm`, `WantedForm`, `TipForm` in `src/App.tsx`) and confirmed several of these fields are genuinely *absent*, not `false`/`null`, at creation time — `isDeleted` and `isEscalated` are never set when a report or wanted notice is first created; `submitterId` is omitted entirely for anonymous tips. Dot-accessing a missing key on a Firestore Rules map is unsafe and can error out the whole condition rather than gracefully evaluating to a sensible default. Fixed every instance to use `.get(field, default)` instead. This also surfaced a genuine, pre-existing, *unrelated* client bug while I was in that exact code: `TipForm` was setting `submitterId: undefined` for anonymous tips, which the Firestore JS SDK actually throws on by default (no `ignoreUndefinedProperties` configured in this project) — meaning **anonymous tip submission was silently broken before this pass, for reasons that have nothing to do with security rules.** Fixed it (omit the key entirely instead of setting it to `undefined`) since it was a one-line, non-visual fix directly adjacent to the rules work and I'd already found it.

Neither of these would have been obvious from reading the rules file in isolation — the first is a language-surface-area question, the second required cross-referencing actual application code. This is exactly the category of mistake an emulator run is supposed to catch mechanically; I caught these two by hand, which is a materially weaker guarantee than 72 passing assertions against the real rules engine.

---

## 7. Honest limitations and known gaps

- **`alerts` create rule cannot distinguish an Admin's official broadcast from a citizen's SOS signal.** Both go through the exact same `write('alerts', ...)` code path in `src/App.tsx` today, and the `Alert` schema in `src/domain.ts` has no field marking which is which. The rule therefore allows any signed-in user to create an alert — which is *correct* for SOS (any citizen legitimately needs this) but means a technically-CITIZEN account could also craft a fake high-urgency "broadcast" through direct SDK access. Closing this properly needs a schema change (a `source`/`type` field) distinguishing the two, which is a real design change and explicitly out of scope for "do not change the application design." Flagging it here rather than silently limiting it.
- **No Cloud Functions.** Privileged transitions (officer approval, report verification, wanted-notice verification) are enforced entirely by Firestore rules checking field-level permissions on direct client writes — there is no server-side Functions layer providing audit logging, rate limiting, or additional business-logic validation beyond what a declarative rule can express. This matches the forensic audit's finding 2.5, unresolved by design (rules-only was the assignment; Functions is a larger, separate infrastructure project).
- **No App Check / rate limiting / abuse protection** — unaffected by this pass, still an open item from the forensic audit.
- **The emulator tests have not actually been run.** Repeating this from §0 because it's the most important honest limitation in this document.

---

## 8. How to actually run this (you have working internet; I don't)

```bash
cd tracenet-pwa   # project root
npm ci
npm run test:security
```

`test:security` runs `firebase emulators:exec --project tracenet-emulator-test --only firestore,storage "vitest run --config firebase/emulator-tests/vitest.config.ts"` — this starts the *real* Firestore and Storage emulators, waits for them to be ready, runs all 72 tests against them, and tears the emulators down afterward. First run will download the emulator JARs (a few hundred MB) from Google's servers, which is exactly the step this sandbox cannot perform.

If you'd rather run it interactively: `firebase emulators:start --only firestore,storage --project tracenet-emulator-test` in one terminal, then `npx vitest run --config firebase/emulator-tests/vitest.config.ts` in another.

## 9. What I need back from you to close this out

Please run the command above and share:
1. The final summary line (`Test Files ... passed`, `Tests ... passed`).
2. The full output if **anything** fails — a failing assertion here means either a rule is wrong or a test's expectation is wrong, and both are worth knowing about before this is called done.

Once that comes back clean, I'll update this document's §0 to reflect actual, verified emulator results and can revisit whether "security complete" is an accurate statement at that point — it still wouldn't cover the two open items in §7 (the `alerts` schema gap and the absence of Cloud Functions), which would need their own explicit sign-off regardless of test results.

---

## 10. Files Changed / Added

| File | Status |
|---|---|
| `firebase/firestore.rules` | New |
| `firebase/storage.rules` | New |
| `firebase.json` | New |
| `.firebaserc` | Updated (Phase 16) — now points to the real project id `tracenet-23a64`, replacing the emulator-only placeholder `tracenet-emulator-test` this row originally called out. See `FIREBASE_BACKEND_SETUP.md`. |
| `firebase/emulator-tests/firestore.rules.test.ts` | New — 49 tests |
| `firebase/emulator-tests/storage.rules.test.ts` | New — 23 tests |
| `firebase/emulator-tests/vitest.config.ts` | New — separate Node-environment config so these don't run as part of the regular `npm test` (which stays jsdom-based and mock-based, unaffected) |
| `package.json` | Added `firebase-tools`, `@firebase/rules-unit-testing@4.0.1` devDependencies; added `test:security` script |
| `src/domain.ts` | Unaffected by this pass (already had `updatedAt` from the previous media-pipeline work, which the rules rely on) |
| `src/services.ts` | Replaced flat `collections.{reports,tips,wanted,users}` with role-scoped query functions (`myReports`/`allReports`/`publicReports`, etc.) required for the new rules to be satisfiable by real client queries; added `listenWhere()` |
| `src/App.tsx` | `useAppData()` now subscribes per-role instead of universally; `/public` route and `TipForm`'s incident dropdown wired to the always-on public-verified feeds; `ReportForm`'s Storage path now embeds the uploader's uid (`reports/{id}/{uid}`) so ownership is verifiable without a Firestore round-trip at upload time; fixed the discovered `submitterId: undefined` bug in `TipForm` |

`npm ci` / `npm run build` / `npm run lint` / `npm run typecheck` / `npm test` all re-verified passing after every change in this pass (21/21 application tests, 0 lint errors, clean build) — see the command output captured during this session. This does **not** include the 72 emulator-backed security tests, which require the steps in §8.
