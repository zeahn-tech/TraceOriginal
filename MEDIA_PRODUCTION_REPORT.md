# TraceNet — Production Media Pipeline Report

**Scope:** rebuild the report/wanted-notice media pipeline (select → validate → local persistence → upload → Storage → download URL → Firestore association → cross-device playback) as a durable, testable state machine. No UI redesign — every visual element that previously rendered correctly still renders identically; the changes are almost entirely in `src/services.ts` (new), with minimal, appearance-preserving wiring changes in `src/App.tsx` and two small new components.

---

## 1. Architecture

### 1.1 The state machine

Two linked state machines, both persisted in IndexedDB (Dexie) so they survive refreshes, crashes, and closed tabs — nothing lives only in JavaScript memory.

**Per-file (`MediaJob.status`):**
```
LOCAL_PENDING -> UPLOADING -> UPLOADED
                     \-> FAILED (retryable back to LOCAL_PENDING)
```

**Per-submission (`ReportDraft.status`):**
```
LOCAL_PENDING -> UPLOADING -> UPLOADED -> REPORT_PENDING -> SYNCED
     ^                \-> FAILED <-/           \-> CONFLICT
     \-------------------- FAILED / CONFLICT retried back to LOCAL_PENDING
```

| State | Meaning |
|---|---|
| `LOCAL_PENDING` | Saved to IndexedDB; no network activity attempted yet (typically: offline) |
| `UPLOADING` | One or more attached files are actively uploading to Firebase Storage |
| `UPLOADED` | Every attached file has a confirmed HTTPS Firebase Storage download URL |
| `REPORT_PENDING` | Media is fully uploaded; the Firestore document write is in flight or queued |
| `SYNCED` | Firestore document confirmed written — terminal state; local copies are pruned |
| `FAILED` | Unrecoverable error this cycle; safe to retry |
| `CONFLICT` | A different document already exists at this id — never silently overwritten |

### 1.2 The core invariant

**A Firestore document is written in exactly one place** (`processDraft()` in `src/services.ts`), and only after every attached file has reached `UPLOADED` with a confirmed download URL. There is no code path — online or offline — where a report or wanted notice can be created without either (a) having zero attached media, or (b) having every attached file's real HTTPS URL already in hand. This is what makes "a successful queued media upload must update the corresponding pending report" true by construction rather than by convention: the URLs are assembled directly from the completed `MediaJob` rows and written into the *same* payload as the rest of the document, in the same function, in the same pass.

### 1.3 Stable IDs (duplicate prevention)

- **Draft id:** `${collection}:${reportId}` — deterministic.
- **Media job id:** `${draftId}:${fieldKey}:${file.name}:${file.size}:${file.lastModified}` — deterministic from the file's own identity.
- **Report id:** generated once via `useState(() => uuid())` at form-mount time in `ReportForm`/`WantedForm` (previously generated fresh inside `submit()` on every click). This is what makes a retry idempotent: resubmitting the same compose session reuses the same draft, and Dexie's `.put()` (upsert) on both tables means re-queueing identical files never creates a second row. Verified in `services.test.ts` — submitting the same file twice for the same report produces exactly one `mediaJobs` row and, once synced, exactly one Firestore write.

### 1.4 The non-negotiable HTTPS-only rule

`assertRemoteMedia()` (kept from the prior pass, now exercised at one additional point) is the single enforcement gate every write path in `services.ts` routes through — `write()`, `create()`, `syncOutbox()`, `resolveConflict()`, and `processDraft()`'s final `setDoc`. It rejects any `imageUrls`/`videoUrls`/`audioUrls`/`*Url` value that isn't `https://firebasestorage.googleapis.com/...`. Structurally, this rule is now also *unreachable to bypass by construction* for the new pipeline: `processDraft()` builds the final payload exclusively from `job.downloadUrl`, which is only ever set to the return value of `getDownloadURL()` (checked against the same HTTPS pattern immediately after upload, before it's even written to the local job row) — there is no code path that puts a `blob:`, `file://`, or `content://` value into that field in the first place. `assertRemoteMedia` is the defense-in-depth backstop, not the only line of defense. Tested directly in `services.test.ts`.

---

## 2. Requirements → Implementation

| # | Requirement | Where | Notes |
|---|---|---|---|
| — | Images / Videos / Audio | `services.ts` (`MediaKind`, `fieldKeyFor`), `MediaAsset.tsx` | One pipeline, three kinds; kind is derived from MIME type at selection time. |
| 1 | Reliable Firebase Storage upload | `uploadJobFile()` | `uploadBytesResumable` + 3-attempt exponential backoff (500ms·2ⁿ + jitter). |
| 2 | Correct MIME type | `uploadJobFile()` | `contentType: job.contentType` (from `file.type`, preserved through compression). Asserted in `services.test.ts`. |
| 3 | Correct metadata | `uploadJobFile()` | `customMetadata: { mediaId, draftId, kind, fieldKey, originalFileName, uploadedAt }`. Asserted in `services.test.ts`. |
| 4 | Download URL retrieval | `uploadJobFile()` | `getDownloadURL()`, validated against the Firebase Storage HTTPS pattern before being trusted. |
| 5 | Upload retry | `uploadJobFile()` (in-attempt) + `retryDraft()` (user-triggered, cross-session) | Two layers: automatic retry within a single upload attempt, and explicit retry for a `FAILED` draft found later (including after a full app restart). |
| 6 | Failed-upload state | `MediaStatus.FAILED` / `DraftStatus.FAILED`, `lastError` field | Never silently dropped — surfaced via `getPendingReportDrafts()`. |
| 7 | Persistent upload queue | Dexie tables `reportDrafts` + `mediaJobs` (schema v3) | Survives refresh/crash/close — see §4. |
| 8 | Media/report association | `processDraft()`'s final payload assembly | The *only* place a document is written; see §1.2. |
| 9 | Duplicate prevention | Deterministic ids, §1.3 | Tested directly. |
| 10 | Media deletion lifecycle | `discardDraft()` → `deleteJobFromStorage()` | User-initiated discard of a not-yet-synced draft also deletes any already-uploaded Storage objects — no orphan left behind. See §5 for the boundary of what this can and can't cover. |
| 11 | Missing-media handling | `MediaThumb` (`components/MediaAsset.tsx`) | Renders a same-sized fallback tile instead of a broken-image icon when a URL is absent. |
| 12 | Playback error handling | `MediaThumb`'s `onError` handlers | A load/playback failure on an `<img>`/`<video>`/`<audio>` swaps to the same fallback tile. |
| 13 | Image lazy loading | `MediaThumb` | Native `loading="lazy" decoding="async"`. |
| 14 | Video lazy loading | `MediaThumb` | `preload="none"` — the standards-based equivalent, since `<video>` has no `loading` attribute. |
| 15 | Audio lazy loading | `MediaThumb` | `preload="none"`, same rationale. |
| 16 | Resource cleanup | `pruneSyncedDraft()` | Deletes local blob copies and draft bookkeeping the moment Firestore is confirmed as the source of truth. |
| 17 | Object URL cleanup | `createMediaPreview`/`revokeMediaPreview` (`services.ts`), consumed by `MediaPreview.tsx` | Implemented correctly (effect-scoped create/revoke) — see the honest caveat in §6 about where this is and isn't currently exercised in the live UI. |
| 18 | Large-file protection | `validateMediaFile()` | 15 MiB images / 50 MiB other media, enforced *before* anything is queued — an oversized file never creates a job, so it can never become an orphan. Checked at selection time (`ReportForm`'s `choose`) and again defense-in-depth inside `submitReportWithMedia()`. |
| 19 | Browser compatibility | `capabilities` object (`services.ts`) | Feature-detects IndexedDB, Background Sync, MediaRecorder, `createImageBitmap`; every consumer checks the flag instead of assuming support. `localDb` itself is `undefined` (not a crash) in a browser without IndexedDB, and every function that needs it throws a clear, catchable error instead of a `TypeError` deep in Dexie. |

---

## 3. Files Changed

| File | Change |
|---|---|
| `src/services.ts` | Full rewrite of the media/report pipeline (~520 lines). Kept: Firebase bootstrap, auth/session helpers, `write`/`create`/`syncOutbox`/`getPendingSync`/`resolveConflict` (unchanged — still correct for edits/simple writes that carry no pending local media), `assertRemoteMedia`, `captureMedia`/`recordMedia`/`createMediaPreview`/`revokeMediaPreview`. New: `MediaStatus`, `DraftStatus`, `MediaJob`, `ReportDraft` types; Dexie schema v3 (`reportDrafts` + redesigned `mediaJobs`, replacing the old untyped `drafts` table); `submitReportWithMedia`, `processDraft`, `processReportQueue`, `getPendingReportDrafts`, `retryDraft`, `discardDraft`, `uploadSingle`, `validateMediaFile`, `capabilities`. Removed: the old single-file `upload()`, `queueMedia`, `MediaUploadQueuedError`, and the never-wired `queueOfflineReport` — all replaced by the draft pipeline. |
| `src/components/MediaAsset.tsx` | **New.** `MediaThumb` component: lazy loading + missing/broken-media fallback for images/video/audio, rendering a single root element so it drops into the existing `.thumbs img` / `.wanted-card>img` CSS selectors without any visual change to the working case. |
| `src/App.tsx` | `ReportForm`: report id now generated once at mount (not per-submit); `choose()` validates files immediately on selection; `submit()` now calls `submitReportWithMedia` instead of the old upload-then-write flow, with toast wording that distinguishes "submitted", "saved offline — will sync automatically", and "failed — retry from your dashboard". `WantedForm`: same pipeline, added a `busy`/loading state (previously had none). `SignUp`: id-card upload now uses `uploadSingle`. `ReportCard`/`WantedCard`: image rendering switched to `MediaThumb`; `ReportCard` now also renders video/audio evidence, which previously had **zero UI** despite `services.ts` already supporting those uploads. New `PendingDrafts` component surfaces not-yet-synced reports/notices on the Citizen dashboard and Operations' Wanted tab, reusing the existing `.record-card`/`.status`/`.card-actions` classes verbatim — no new visual language introduced. Several `write('reports'/'wanted_criminals', ...)` call sites now bump `updatedAt`. |
| `src/domain.ts` | Added `updatedAt: number` to `Report` and `Wanted` — needed for the conflict-detection comparison and for `syncOutbox`'s pre-existing (previously inert, since neither type had this field) conflict check to actually mean something. |
| `src/services.test.ts` | **New.** 19 tests exercising the real pipeline logic against mocked `firebase/firestore`/`firebase/storage` and a real IndexedDB implementation (`fake-indexeddb`) — see §7. |
| `src/test/setup.ts` | **New.** Vitest setup: loads `fake-indexeddb`, stubs Firebase env vars so `firebaseReady` is true in tests. |
| `vitest.config.ts` | **New.** jsdom environment, points at the setup file. |
| `package.json` | Added `"test": "vitest run"` script and `vitest`, `fake-indexeddb`, `jsdom` as devDependencies. |

No file under `app/` (Android reference), no CSS rule, and no existing component's rendered markup for the *working* case (valid, loaded media) was changed in appearance.

---

## 4. How each numbered test scenario was verified

All of the following are real, passing automated tests in `src/services.test.ts` (`npm test` — 19/19 passing), run against the actual `services.ts` module with `firebase/storage`/`firebase/firestore` mocked and a genuine IndexedDB (`fake-indexeddb`) backing Dexie — not a hand-simulated approximation.

| Scenario | Test | Result |
|---|---|---|
| Online image | `online image: uploads, writes an HTTPS-only Firestore doc, and prunes local copies` | ✅ `SYNCED`, HTTPS URL written, local rows pruned |
| Online video | `online video: correct kind/field and HTTPS URL` | ✅ `SYNCED`, URL in `videoUrls` |
| Online audio | `online audio: correct kind/field and HTTPS URL` | ✅ `SYNCED`, URL in `audioUrls` |
| Offline image | `offline image/video/audio: draft and every file persist locally as LOCAL_PENDING...` | ✅ (all three kinds in one test) `LOCAL_PENDING`, zero Firestore writes attempted, 3/3 jobs persisted |
| Offline video | (same test) | ✅ |
| Offline audio | (same test) | ✅ |
| Reconnection | `reconnection: a draft saved while offline completes automatically once back online` | ✅ flips `navigator.onLine`, re-runs `processDraft`, reaches `SYNCED` |
| Retry | `retry: a failed upload can be retried and completes once the transient error clears` | ✅ `FAILED` → `retryDraft()` → `SYNCED` |
| Failed upload | `failed upload: stays visible via getPendingReportDrafts rather than disappearing silently` | ✅ permanent failure still enumerable, not lost |
| Browser refresh during upload | `browser refresh during upload: a draft stuck at UPLOADING ... resumes correctly` | ✅ seeds a Dexie row exactly as a torn-down session would leave it, confirms `processDraft` recovers using the already-persisted blob (see the honest limitation noted in §6 about *in-flight* uploads specifically) |
| Browser closed / reopened | `browser closed and reopened: a draft that was never processed before close is picked up by processReportQueue` | ✅ no in-memory state relied upon — only what `processReportQueue()` discovers from Dexie |
| Second device viewing media | `second device viewing media: the synced document contains only plain HTTPS URLs a second client can read` | ✅ reads back the Firestore-side document only (never the originating device's IndexedDB) and asserts every URL is a plain HTTPS Firebase Storage URL |
| *(bonus, not explicitly requested but directly relevant)* | `duplicate prevention`, `idempotent re-sync`, `conflict`, `discard`, `non-negotiable rule`, `MIME type + metadata`, size/type validation | ✅ all pass — see §7 for the full list |

---

## 5. "The system must not create orphan media" — what's actually guaranteed vs. what remains a known exposure

**Guaranteed by this implementation:**
- A file is never queued at all if it fails validation (§2, #18) — an invalid file can't become an orphan because it never gets a Storage path.
- A Firestore document can never reference a URL that doesn't exist yet (§1.2) — you cannot end up with a `SYNCED` report pointing at a file that was never actually uploaded.
- `discardDraft()` deletes any already-uploaded Storage object for a draft the user explicitly abandons before it synced (§2, #10) — tested.
- Once `SYNCED`, local copies are pruned (§2, #16) — no redundant local storage growth over time.

**Known, honestly-scoped remaining exposure:** if a draft's media finishes uploading to Storage but the Firestore write then fails *permanently* (e.g. the device goes offline forever, or the user uninstalls the app / clears site data without ever reconnecting), the uploaded Storage objects are never referenced by any document and are never cleaned up. This is an inherent limitation of a **client-only** architecture — there is no server-side process watching for this. Closing it properly requires a scheduled Firebase Function that reconciles Storage objects against Firestore documents and deletes unreferenced ones past some age threshold. That's backend infrastructure work (Firebase Functions don't exist in this project yet, per the forensic audit's finding 2.5) and is out of scope for this client-side media-pipeline pass — flagging it here so it isn't lost.

---

## 6. Honest limitations

- **"Browser refresh during upload" resumes the file from scratch, not byte-for-byte.** There is no service-worker-hosted upload engine in this project (adding one means importing the Firebase Storage SDK into `sw.ts`, a substantial, separate piece of work). What *is* guaranteed: the file's blob is durably in IndexedDB before any upload starts, so a refresh mid-upload never loses the file — it just restarts that file's upload from 0% instead of resuming from wherever it was. For the typical evidence-photo/short-clip sizes this app handles (capped at 15/50 MiB), this is a reasonable, honest trade-off; it would matter more for very large files on very slow connections.
- **Object URL cleanup (#17) is implemented but not currently exercised by the live UI.** `createMediaPreview`/`revokeMediaPreview` and their correct effect-scoped lifecycle live in `components/MediaPreview.tsx`, but the file-picker's "attached files" list intentionally still shows only an icon + filename (no live thumbnail) — exactly as it did before this pass — specifically to honor "preserve existing media-related visual design." Adding live blob-URL thumbnails to the pre-submit file list is a small, low-risk follow-up if wanted, but it does change what that list looks like, so it wasn't done here without explicit direction.
- **CONFLICT is real but, by design, rare for report *creation*.** Since report/wanted ids are freshly generated per compose session, a genuine identity collision is only plausible if something else legitimately writes to the same id — which the test suite verifies is handled correctly (flagged, never silently overwritten) — but this is different from the more common "two officers editing the same existing report" conflict scenario, which is already handled by the separate, pre-existing `write()`/`syncOutbox()` path (unchanged in this pass).
- **Video/audio evidence now has a UI** (§3, `ReportCard`) where it previously had none at all — this is new visible content, not a redesign of anything that existed, and it only appears on reports that actually have video/audio attached.

---

## 7. Full Test Results

```
$ npm test

 ✓ src/services.test.ts (19 tests) 4177ms
   ✓ rejects unsupported file types before anything is queued
   ✓ rejects images over the 15 MiB limit and other media over the 50 MiB limit
   ✓ accepts a valid image/video/audio file under the size limit
   ✓ sends the correct MIME type and Storage metadata for each upload
   ✓ online image: uploads, writes an HTTPS-only Firestore doc, and prunes local copies (no orphan)
   ✓ online video: correct kind/field and HTTPS URL
   ✓ online audio: correct kind/field and HTTPS URL
   ✓ offline image/video/audio: draft and every file persist locally as LOCAL_PENDING; nothing is written to Firestore; nothing is lost
   ✓ reconnection: a draft saved while offline completes automatically once back online
   ✓ retry: a failed upload can be retried and completes once the transient error clears
   ✓ failed upload: stays visible via getPendingReportDrafts rather than disappearing silently
   ✓ browser refresh during upload: a draft stuck at UPLOADING resumes correctly on next processDraft call
   ✓ browser closed and reopened: a draft that was never processed before close is picked up by processReportQueue
   ✓ second device viewing media: the synced document contains only plain HTTPS URLs
   ✓ duplicate prevention: submitting the same file twice never creates a second job or a second Firestore write
   ✓ idempotent re-sync: calling processDraft again after a draft already synced is a safe no-op
   ✓ conflict: a different document already occupying the same id is flagged CONFLICT instead of being silently overwritten
   ✓ discard: removes an already-uploaded (but not yet synced) file from Storage and clears local bookkeeping
   ✓ non-negotiable rule: assertRemoteMedia rejects blob:/file:/content: URLs

 Test Files  1 passed (1)
      Tests  19 passed (19)
```

---

## 8. Build/Lint/Typecheck (re-verified after this pass, from a clean environment)

```
npm ci            → PASS (580 packages)
npm run build     → PASS (tsc -b + vite build + injectManifest service worker)
npm run lint      → PASS (0 errors, 16 pre-existing warnings, unrelated to this pass)
npm run typecheck → PASS
npm test          → PASS (19/19)
```

All five commands were run from a genuinely clean state (`node_modules`/`package-lock.json`/`dist` deleted, then `npm install` → `npm ci`).

---

## 9. What this pass does *not* claim

This is a media-pipeline correctness pass. It does not implement, and does not claim to have implemented, Firestore/Storage security rules, Firebase Functions, the general edit-conflict UI for non-media fields, or any of the other outstanding items from `FINAL_FORENSIC_AUDIT.md`. **Production readiness is not certified.**
