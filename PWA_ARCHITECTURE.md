# TraceNet PWA Architecture

**Status:** Architecture specification only. This document defines the target web architecture; it does not require implementation changes in the current React or Android sources.

**Scope:** React, TypeScript, Vite, Firebase Authentication, Firestore, Firebase Storage, `vite-plugin-pwa`, IndexedDB, and browser Background Sync.

## Architectural decision

TraceNet will be a static React single-page PWA deployed to GitHub Pages, with Firebase as its managed backend. The app must preserve the Android functional specification and visual system while replacing Android Room/session/geofence assumptions with web-native persistence, service-worker caching, explicit queues, and server-enforced authorization.

The client is deliberately **not** trusted to approve officers, publish notices, alter roles, calculate reputation, or write audit records. Privileged transitions use Firebase Cloud Functions/Admin SDK (part of the Firebase backend boundary) with Auth custom claims and Firestore/Storage Rules. GitHub Pages hosts only immutable client assets; it never contains secrets or privileged code.

Firestore’s web persistent local cache supplies cached document/query access and reconciliation when connectivity returns; its documented conflict behavior is last-write-wins, so mutable workflows use server-side transaction/version validation for protected state changes. [Firebase Firestore offline persistence](https://firebase.google.com/docs/firestore/manage-data/enable-offline)

## Technology stack

| Layer | Choice | Responsibility |
| --- | --- | --- |
| UI | React 18+ / TypeScript / Vite | Route-driven app shell, accessible components, Android-faithful rendering |
| Client state | Zustand stores + React Context | Session, UI/navigation, transient form state, connectivity, sync status; Context only for Firebase and feature services |
| Server state | Firebase Web SDK + Firestore listeners | Authenticated remote data, optimistic local reads/writes, Firestore cache |
| Local application persistence | IndexedDB via Dexie | Durable outbox, upload metadata, drafts, local settings, asset/media references—not a competing entity database |
| PWA | `vite-plugin-pwa` with Workbox `injectManifest` | Manifest, precache, custom service worker, runtime cache, offline app shell, Background Sync registration |
| Identity | Firebase Authentication | Email/password, Google, password reset and reauthentication |
| Files | Firebase Storage | Evidence, profile image and verified officer ID uploads, resumable transfer |
| Privileged backend | Firebase Cloud Functions/Admin SDK | Claims, approval/moderation workflows, immutable audit records, trusted fan-out/notifications and outbox endpoint |
| Hosting/CI | GitHub Pages + GitHub Actions | Build and publish static `dist` artifact |

`injectManifest` is selected over generated service-worker mode because TraceNet needs explicit app-shell fallbacks, route behavior, media cache limits and custom sync messaging. Vite PWA documents `strategies: 'injectManifest'` for a custom Workbox worker. [vite-plugin-pwa guide](https://vite-pwa-org.netlify.app/guide/inject-manifest)

### Repository alignment

The current repository already contains a Vite React entry point, Firebase/Dexie/PWA dependencies, and a custom service-worker entry configured through `vite-plugin-pwa`. The target implementation should converge on the structure below incrementally; this document intentionally does not prescribe a code migration or modify the existing application. The production base path must be made environment-driven before deployment so project Pages (`/<repository>/`) and root/custom-domain Pages (`/`) use the same router, manifest, service-worker, and asset configuration.

## Repository and folder structure

```text
tracenet-pwa/
├─ public/
│  ├─ icons/                         # copied/derived PWA icons only
│  ├─ brand/                         # Android source logo and fixed artwork
│  ├─ offline.html
│  └─ _redirects                     # omitted for GitHub Pages; SPA fallback is SW based
├─ src/
│  ├─ app/
│  │  ├─ App.tsx
│  │  ├─ providers.tsx               # Firebase, auth, error boundary, router, theme
│  │  ├─ router.tsx
│  │  └─ routes.ts                   # route IDs, loaders/guards and links
│  ├─ assets/                        # imported Android artwork/icons
│  ├─ components/
│  │  ├─ design-system/              # faithful primitives: card, form, dialog, nav, chart
│  │  ├─ media/
│  │  └─ feedback/
│  ├─ features/
│  │  ├─ auth/
│  │  ├─ public-portal/
│  │  ├─ citizen/
│  │  ├─ reports/
│  │  ├─ tips/
│  │  ├─ alerts/
│  │  ├─ sos/
│  │  ├─ wanted/
│  │  ├─ law-enforcement/
│  │  ├─ administration/
│  │  ├─ contacts/
│  │  ├─ analytics/
│  │  ├─ profile/
│  │  └─ map/
│  ├─ services/
│  │  ├─ firebase/                   # initialized SDK, typed repositories, converters
│  │  ├─ auth/
│  │  ├─ firestore/
│  │  ├─ storage/
│  │  ├─ sync/                       # outbox processor and conflict resolver
│  │  ├─ notifications/
│  │  └─ location/
│  ├─ db/
│  │  ├─ indexedDb.ts                # Dexie schema/migrations
│  │  ├─ outbox.ts
│  │  ├─ drafts.ts
│  │  └─ mediaCache.ts
│  ├─ stores/                        # Zustand state only; never duplicate Firestore records
│  ├─ hooks/
│  ├─ domain/
│  │  ├─ models.ts
│  │  ├─ schemas.ts                  # runtime validation/mappers
│  │  ├─ permissions.ts
│  │  └─ constants.ts
│  ├─ styles/                        # CSS tokens derived solely from DESIGN_SYSTEM.md
│  ├─ sw.ts                          # Workbox custom service worker
│  ├─ registerSW.ts
│  ├─ main.tsx
│  └─ vite-env.d.ts
├─ functions/                        # Firebase Functions/Admin SDK; separate deploy target
│  └─ src/
├─ firestore.rules
├─ firestore.indexes.json
├─ storage.rules
├─ firebase.json
├─ vite.config.ts
├─ package.json
├─ .env.example                      # only VITE_ public Firebase web configuration
├─ .github/workflows/deploy-pages.yml
└─ docs/                             # generated architecture/specification documents
```

Feature folders own their route screens, view models/hooks, validation, repository calls and tests. Shared design primitives must not import feature code. The codebase must use path aliases (`@/app`, `@/features`, `@/services`, `@/db`, `@/domain`, `@/components`) and lazy-load feature route bundles.

## Routing structure

React Router data/browser routing is used with a basename derived from Vite `BASE_URL`, so the same code works at a custom domain, `username.github.io`, or a project Pages URL. Browser route navigation is handled by the SPA shell fallback in the service worker; users who request a deep link before first service-worker install need GitHub Pages’ `404.html` redirect shim or should use hash routing. The recommended architecture uses BrowserRouter plus a generated `404.html` SPA redirect shim so public URLs remain clean.

```text
/                                 splash → welcome/session destination
/welcome                          welcome
/sign-in                          sign in + reset-password dialog flow
/sign-up                          registration
/public                           public viewing portal
/dashboard                        citizen dashboard                 [citizen+]
/reports/new                     incident report                   [citizen+]
/reports/:reportId               own report/details                [owner/enforcer/admin]
/tips/new                        anonymous tip                     [citizen+]
/sos                             SOS active screen                 [citizen+]
/map                             report map
/map?lat=&lng=                   notification/SOS target map
/contacts                         emergency directory
/analytics                        analytics
/profile                          profile/security/help
/about                            static about
/operations                       law-enforcer dashboard           [approved enforcer/admin]
/operations/wanted/new           post wanted notice                [approved enforcer/admin]
/admin                           administrator dashboard           [admin claim]
*                                 route-aware Not Found / app-shell fallback
```

Route guards wait for Firebase Auth hydration and Firestore profile/claim resolution; they render a loading shell rather than a protected route. A claim/profile change forces token refresh and redirects to the correct role home. The client guard is UX only; Firestore Rules and Functions are authoritative.

## State management

### Sources of truth

| State type | Store | Notes |
| --- | --- | --- |
| Auth identity/token/claims | Firebase Auth observer + `authStore` | Never persist raw passwords/tokens manually. |
| User profile / collections | Typed Firestore repositories and `onSnapshot` hooks | Firestore persistent cache is entity/query authority. |
| UI state | `uiStore` | active tab, dialog, toast/snackbar, installation prompt, theme compatibility behavior. |
| Connectivity/service worker | `networkStore`, `syncStore` | online/offline, SW version, outbox counts, retry/blocked errors. |
| Form state | Local component state / React Hook Form | Persist recoverable draft only to IndexedDB; clear after confirmed submit. |
| Outbox/media jobs | IndexedDB + `syncStore` projection | Durable across tab close/reload. |
| Preferences | IndexedDB `settings` table | non-security local preference only; security policies come from Firestore/Functions. |

Do not mirror Firestore collections in Zustand. Repositories expose query hooks and normalized converters; components subscribe only to the minimal query. All data received from Firestore or IndexedDB is runtime-validated before display/use.

## Firebase architecture

### Firebase app initialization

One modular Firebase app is initialized from `VITE_FIREBASE_*` public config. It creates Auth, Storage and Firestore once. Firestore uses `persistentLocalCache` with `persistentMultipleTabManager`, allowing browser IndexedDB cache across tabs. Persistence must be offered only after a “This is a trusted device” acknowledgement because cached Firestore data remains on the device; Firebase notes web persistence is disabled by default and is persistent between sessions. [Firebase documentation](https://firebase.google.com/docs/firestore/manage-data/enable-offline)

Firestore automatic local indexes are enabled where supported. Cache logout policy is explicit: sign out removes app IndexedDB drafts/outbox/media references and clears Firestore persistence only after all listeners terminate; this prevents the next user on a shared device reading a prior user’s cached records.

### Collections and trust boundary

Client-readable public projections are separated from private operational records:

```text
users/{uid}                       private profile; role mirrored for UI only
reports/{reportId}                private/full report and evidence metadata
tips/{tipId}                      private investigation tip
alerts/{alertId}                  authorized alert record
wanted_criminals/{id}            operational notice/moderation state
public_reports/{id}               server-published safe projection
public_wanted/{id}                server-published verified projection
public_alerts/{id}                server-published alert projection
audit_logs/{id}                   server-created immutable logs
help_messages/{id}                sender/admin-only access
system_settings/current           admin-managed server policy
```

The public portal queries only `public_*` collections. Functions atomically generate/update/remove projections when an approved moderator changes a source record. This prevents unverified records, contact data, internal notes, IDs and full evidence metadata leaking via client filters.

### Authorization and Rules

Authentication supports email/password, Google, reset email, reauthentication and password change. A Function assigns/changes custom claims (`citizen`, `law_enforcer`, `admin`, `approved`) after controlled review. Firestore Rules use `request.auth`, claim checks, strict document field allowlists, owner checks, immutable server fields, and transition restrictions. Storage Rules derive identity from path and claim:

```text
profiles/{uid}/{object}
id_cards/{uid}/{object}                 owner upload/read; approved admin reviewer read
reports/{reportId}/{object}             reporting owner upload; investigator/admin limited read
wanted/{noticeId}/{object}              approved enforcer/admin write; public only server-approved derivative
```

Functions validate MIME, bytes, media dimensions/duration where appropriate, create server timestamps, scan/moderate before publication, apply reputation/status updates transactionally, write audit logs, and never accept role/status/approval/audit fields from arbitrary client payloads. Firebase App Check is enabled for web and enforced for protected APIs.

### Repository API

Each feature accesses Firebase through typed repositories rather than SDK calls in components. Public reads use cache-aware listeners. Private workflow commands call Functions (e.g. `submitReport`, `reviewTip`, `approveOfficer`, `publishWanted`, `createAlert`, `transitionReport`, `deleteAccount`) and return a command receipt. Simple owner drafts may be direct writes only where Rules permit. All commands include idempotency IDs from the outbox.

## IndexedDB and offline architecture

### Database schema

```text
tracenet-client
├─ outbox                 id, command, payload, state, attempts, createdAt, nextAttemptAt, idempotencyKey
├─ uploads                id, outboxId, fileMeta, blobKey, storagePath, uploadSession, state, progress
├─ blobs                  id, Blob, mimeType, size, sha256, createdAt, expiresAt
├─ drafts                 id, feature, ownerUid, payload, updatedAt
├─ settings               key, value, ownerUid?, updatedAt
├─ notifications          id, event/ref, readAt, createdAt
└─ migration_meta         schemaVersion, completedAt
```

Never persist passwords, OAuth credentials, Firebase ID tokens, private keys or permanent download URLs in this database. Blob records are encrypted at-rest only if a WebCrypto key can be safely scoped to an authenticated user; otherwise evidence remains only in the browser’s origin storage and UI explains device risk.

### Cache model

* **Precache:** versioned Vite assets, icons, fonts if any, shell `index.html`, `offline.html`, and small static Android brand assets.
* **Navigation:** network-first with a short timeout, then cached `index.html` for application routes; preserve 404/API exclusions.
* **Static images:** stale-while-revalidate, versioned cache name, bounded entry/age limits.
* **Firebase/Google Maps/network APIs:** network-only; do not cache authenticated Firestore responses, ID-card/evidence URLs or map tiles indiscriminately in Cache Storage.
* **Firestore content:** Firebase’s persistent local cache, not Workbox’s HTTP cache.
* **Sensitive logout:** remove Cache Storage namespaces containing user-specific media previews; delete drafts/outbox/blobs for that user after confirmed discard or completed sync.

### Offline UX contract

An always-visible, Android-faithful connectivity/sync indicator distinguishes: online/synced, offline/saved locally, syncing, retry scheduled, and action-required/conflict. Offline public browsing works only for the app shell and records previously cached on that device. The app must never promise that uncached Firestore records, maps, real-time alerts or emergency dispatch are available offline.

Reports/tips can be saved as drafts offline. On submit, the app creates a durable outbox command and retains the media blob subject to size/quota checks. UI gives a pending receipt/queue position, lets the user retry or discard, and preserves a draft on a failed validation/upload. SOS offline behavior must be explicit: show “cannot transmit without a connection,” retain a local emergency draft only if user opts in, and prominently offer the emergency contacts/call path; never display it as sent.

## Media architecture

1. A user selects image/video/audio with browser `<input>`/Media Capture where supported. Validate MIME, extension, checksum, count, file size (images ≤15 MiB; video/audio ≤50 MiB) and client-preview safely.
2. Create a report/tip/wanted draft with media in `uploads`/`blobs`; preview from object URLs only and revoke URLs when the view exits.
3. When online, request a server-authorized storage path/metadata and upload with Firebase Storage `uploadBytesResumable`, persist progress/session metadata, then retrieve the resulting secure reference—not a public URL—as the entity attachment. Firebase documents the resumable web upload API. [Firebase Storage upload docs](https://firebase.google.com/docs/storage/web/upload-files)
4. Submit a command only after required uploads have completed. The Function confirms ownership and commits the Firestore record atomically/idempotently.
5. Render download media through authorized Storage references/short-lived signed/authorized URLs; show the Android-equivalent loading, unavailable and retry UI. Never precache evidence; cache only a small, user-controlled thumbnail preview in IndexedDB if appropriate.
6. Deletion invokes a Function that removes document references and schedules/executes Storage deletion according to retention/legal-hold policy.

## Synchronization and Background Sync architecture

### Command outbox state machine

```text
DRAFT → QUEUED → VALIDATING → UPLOADING_MEDIA → READY_TO_COMMIT
      → SENDING → ACKNOWLEDGED → COMPLETE
                     ↘ RETRY_WAIT ↗
      → BLOCKED_AUTH | BLOCKED_CONFLICT | FAILED_PERMANENT | DISCARDED
```

Every outbox item receives a UUID idempotency key. Connectivity restoration, app foreground, explicit Retry, token refresh and service-worker sync all trigger a single-leader processor. `navigator.locks` (with an IndexedDB lease fallback) prevents two tabs from uploading or committing the same job. Exponential backoff with jitter retries transient errors; authorization/validation errors become action-required and never loop forever.

### Reconciliation strategy

* Direct Firestore local mutations reconcile through the Firebase SDK’s persistent cache/listeners.
* Privileged command responses and `onSnapshot` updates mark outbox commands acknowledged by idempotency key/version.
* Each mutable record carries `revision`, `updatedAt`, `updatedBy`; Functions reject stale protected transitions. UI offers reload/keep-draft/manual-resolution rather than silently overwriting critical notes/status.
* For a compound report/evidence upload, the final server command is the commit point. Orphan uploads have server lifecycle cleanup.

### Background Sync boundary

The custom Workbox worker registers a Background Sync tag (for example `tracenet-command-sync`) and uses Workbox `BackgroundSyncPlugin` only for same-origin, idempotent command endpoint requests. The endpoint is a Firebase HTTPS Function that validates the Firebase token and idempotency key. On `sync`, the worker retries minimal JSON command envelopes and signals open clients via `postMessage`.

Do **not** attempt to execute Firebase Auth, Firestore listeners, or arbitrary Storage uploads autonomously in the service worker. Browser Background Sync is best-effort, lacks universal support and cannot guarantee Firebase auth/session availability or safely retain large evidence files. It supplements—not replaces—the foreground outbox processor. Media jobs resume when an authenticated client is open; the SW can notify that user action is required. This distinction is necessary for reliable, auditable evidence handling.

## Service worker and PWA behavior

`src/sw.ts` precaches the injected Vite manifest (`self.__WB_MANIFEST`), claims clients after update, uses versioned cache names, and broadcasts update availability. `vite-plugin-pwa` supplies manifest generation, service-worker registration and the injected precache list. The web manifest uses `name: "TraceNet"`, `short_name: "TraceNet"`, `display: "standalone"`, `theme_color: "#1E88E5"`, `background_color: "#F5F9FF"`, safe icons/maskable icons derived from the Android artwork, and base-aware `start_url`/`scope`.

The client uses the generated registration hook to show an Android-faithful “update available” prompt; it must not force reload while an upload/outbox action is active. Push notifications are a later Firebase Cloud Messaging/Web Push capability and require explicit browser permission; they are architecturally separate from local in-app alert listeners.

## Deployment architecture: GitHub Pages

### Build configuration

`vite.config.ts` derives `base` from `VITE_BASE_PATH`:

* User/org Pages (`https://<owner>.github.io/`) or custom domain: `base: '/'`.
* Project Pages (`https://<owner>.github.io/<repository>/`): `base: '/<repository>/'`.

This is required so Vite rewrites generated asset paths for a nested public path. [Vite static deployment guide](https://vite.dev/guide/static-deploy.html)

All router links, manifest start URL/scope, service-worker scope, asset URLs and `404.html` redirect logic consume the same base variable. Firebase Authentication’s Authorized Domains must include the Pages domain/custom domain, and Google OAuth redirect/authorized origins must include it. Firebase public web configuration is injected as GitHub repository Variables at build time; it is not secret. Functions service credentials, private keys and App Check administration credentials remain only in Firebase/GitHub protected secrets and never in the static build.

### GitHub Actions flow

```text
push/PR → lint + typecheck + unit/component tests + PWA build check
main push → build Vite dist with production variables
          → upload-pages-artifact(dist)
          → deploy-pages
```

The workflow uses pinned `actions/checkout`, `actions/setup-node`, `actions/configure-pages`, `actions/upload-pages-artifact`, and `actions/deploy-pages`, with `pages: write`, `id-token: write`, and least-privilege `contents: read`. GitHub Pages should be configured with **Source: GitHub Actions**; GitHub documents this artifact-based deployment model. [GitHub Pages documentation](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site)

Firebase Rules, indexes, and Functions deploy through a separate protected CI workflow/service account after emulator/rules tests; never deploy them from the Pages job. Production deployment additionally runs `firebase deploy --only firestore:rules,firestore:indexes,storage,functions` from a protected environment with approval.

## Environment configuration

```text
VITE_BASE_PATH=/tracenet/
VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_AUTH_DOMAIN=...
VITE_FIREBASE_PROJECT_ID=...
VITE_FIREBASE_STORAGE_BUCKET=...
VITE_FIREBASE_MESSAGING_SENDER_ID=...
VITE_FIREBASE_APP_ID=...
VITE_FIREBASE_MEASUREMENT_ID=...          # optional
VITE_FUNCTIONS_REGION=...
```

No Android Maps key is copied as a browser secret. A web maps provider key, if map parity is required, must be browser-restricted to approved Pages/custom-domain origins and route usage/billing restrictions. All validation and access control remains server-side regardless of key restrictions.

## Acceptance criteria before implementation

* A fresh install works offline after first successful shell load and faithfully renders the documented cached states.
* Public routes cannot query private source collections; unapproved officers cannot access operational data even with modified client code.
* Sign-out removes all user-specific local outbox, blobs, drafts and cached private records.
* An interrupted media upload survives reload, resumes only with valid user auth, never duplicates the final report, and reports a clear queued/blocked state.
* Multi-tab mutation/upload races produce one final server command and visible conflict handling.
* Project Pages deployment functions both at the repository subpath and via a configured custom domain, including deep links, PWA install and service-worker asset scope.
* Rules/Function/emulator tests cover every role, ownership edge, status transition and Storage path; visual tests compare implementation to `DESIGN_SYSTEM.md`.

