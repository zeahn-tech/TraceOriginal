# TraceNet — Production Architecture Refactor Report

**Scope:** Decompose `src/App.tsx` into feature modules with route-level lazy loading. No UI change, no behavior change, no visual redesign, no new routes, no dependency additions.

---

## 1. Before

`src/App.tsx` was 141 lines / **52,893 bytes** — every screen, dialog, card, form, and shared UI primitive in the app lived in one file, most of it as single-line dense function bodies. It exported nothing but the default `App` component, so nothing in it could be imported individually, tree-shaken separately, or lazy-loaded. The production build shipped it as one **351.19 kB (113.61 kB gzip)** JS chunk that every visitor downloaded in full before seeing anything past the splash screen — Admin's command center, Operations' incident triage, and the Citizen dashboard's report form all loaded together regardless of which role opened the app or which single screen they used.

## 2. What changed

### 2.1 Directory structure

`App.tsx` is now a 9-line composition root: it owns the `useAppData()` call, the toast/PWA-update state, and renders `<AppRoutes>`. Everything else moved out, organized by the areas listed in the brief:

```
src/
  App.tsx                     — composition root only
  hooks/useAppData.ts         — auth/data subscription hook
  routing/
    AppRoutes.tsx             — the route table (every path/element preserved exactly)
    lazyRoutes.ts             — one React.lazy() per feature screen
    RouteFallback.tsx         — Suspense fallback (reuses existing .empty/.spin CSS)
    ErrorBoundary.tsx         — new: route-level error boundary (see §3.3)
  shared/
    ui.tsx                    — Page, Dashboard, NavIcon, Brand, Input, TextArea,
                                 Select, FileInput, Notice, Dialog, Tabs, Stat, Empty
    Protected.tsx             — route guard
    types.ts                  — Toast type
    assets.ts                 — logo/banner/hero URLs, county list
    cards/                    — ReportCard, AlertCard, TipCard, WantedCard
                                 (shared across dashboard/public/operations/admin)
  features/
    auth/          — Welcome, Splash, AuthPage, SignUpPage, ResetDialog,
                      PasswordDialog, routeFor
    dashboard/     — CitizenDashboard, Carousel
    reports/       — ReportForm, OperationalReport, PendingDrafts, draftStatus
    tips/          — TipForm
    sos/           — SOSPage
    maps/          — MapPage
    contacts/      — ContactsPage, ContactDialog
    analytics/     — AnalyticsPage
    profile/       — ProfilePage
    operations/    — OperationsPage, WantedForm
    admin/         — AdminPage, ApprovalCard, UserCard, SettingsPanel, AlertDialog
    public/        — PublicPortal, AboutPage
```

Every one of the 12 requested areas (Authentication, Dashboards, Reports, Tips, SOS, Maps, Emergency contacts, Analytics, Profile, Law-enforcement operations, Wanted notices, Admin) plus Shared components now has its own home.

**How the extraction was done:** every component body was moved character-for-character out of `App.tsx` into its new file — same JSX, same class names, same logic, same inline styles, same comments. Nothing was reformatted, renamed, or rewritten. The only changes to the code itself were the `import`/`export` lines each file needed and three trivial de-duplications forced by the split (see §3.4).

### 2.2 Route-level lazy loading

`routing/lazyRoutes.ts` wraps every feature screen in `React.lazy()`, and `routing/AppRoutes.tsx` wraps the existing `<Routes>` tree in a single `<Suspense>` boundary. `Welcome`, `Splash`, and `Protected` stay in the eager bundle since they're needed for the very first paint regardless of role; everything reachable only after sign-in or navigation is chunked separately.

**Every existing path is preserved exactly** — same 17 routes, same order, same `<Protected>` wrapping, same role checks, same catch-all redirect to `/`. This was a mechanical swap of `<Auth>` for `<AuthPage>` (etc.) in the JSX; no route logic changed.

**Measured build output**, `npm run build`, before vs. after:

| | Before | After |
|---|---|---|
| Main JS chunk | 351.19 kB (113.61 kB gzip) | **317.24 kB (104.90 kB gzip)** |
| Route chunks | 0 (all inline) | **20 chunks, 41.7 kB raw / ~19.5 kB gzip combined**, fetched on demand |
| `firebase` / `icons` vendor chunks | unchanged | unchanged (not part of this refactor) |

Concretely: a CITIZEN who only ever visits `/dashboard`, `/reports/new`, `/sos`, `/map`, `/contacts`, and `/profile` now never downloads `AdminPage` (5.05 kB), `OperationsPage` (1.46 kB), or `WantedForm` (1.97 kB) at all — and an ADMIN or LAW_ENFORCER who never opens the public portal never downloads `PublicPortal`. Previously every one of these shipped in the single bundle to every visitor.

### 2.3 Error boundaries

`routing/ErrorBoundary.tsx` (`RouteErrorBoundary`) is new — the original app had **zero** error boundaries anywhere, so any render exception in any screen unmounted the entire React tree to a blank white page. `AppRoutes` now wraps the route `<Suspense>` in one `RouteErrorBoundary`, **keyed by `location.pathname`**: if a screen throws, the user sees a recovery screen (styled with the existing `.notice`/`.page` classes, no new visual language) instead of a blank page, and navigating to any other route automatically remounts a clean boundary — no manual reset needed, no risk of a stale crash screen following the user around the app. This also catches lazy-chunk fetch failures (e.g. a stale cached `index.html` requesting a chunk removed by a later deploy), which is a real, previously-unhandled failure mode of adding code-splitting at all.

### 2.4 Everything intentionally left alone

- **No dependency was added.** `React.lazy`/`Suspense`/`Component` are all built into the `react` version already in `package.json`.
- **No CSS changed.** `styles.css` is untouched; the only new UI (`RouteFallback`, the error boundary's fallback screen) reuses existing classes (`.empty`, `.spin`, `.notice`, `.page`, `.card-actions`, `.primary-button`).
- **No route, redirect, or permission check changed.** `Protected`'s logic is byte-for-byte what it was.
- **No manual-chunk / vite.config.ts change.** The existing `firebase`/`icons` manual chunks are untouched; route splitting happens automatically from the `import()` boundaries in `lazyRoutes.ts`.
- **Media/lifecycle code already reviewed for cancellation/cleanup was not touched.** `components/MediaAsset.tsx` and `components/MediaPreview.tsx` (object-URL lifecycle, `onError` fallback tiles) and `PendingDrafts`' polling effect (interval + `online` listener cleanup, `cancelled` flag) already followed the cancellation/cleanup patterns this task asked to preserve — they moved file location only, with their code and comments intact.

### 2.5 The three unavoidable de-duplications

Splitting one file into forty-eight forces a few pieces of code to be imported by more than one consumer instead of just being in scope. Three shared pieces were extracted verbatim into `shared/`:

1. `ReportCard`, `AlertCard`, `TipCard`, `WantedCard` → `shared/cards/*` (each was already used from 2–4 different screens in the original file; they now have one canonical definition each instead of relying on same-file scope).
2. `Page`, `Dashboard`, `NavIcon`, `Brand`, and the form primitives (`Input`, `TextArea`, `Select`, `FileInput`, `Notice`, `Dialog`, `Tabs`, `Stat`, `Empty`) → `shared/ui.tsx`.
3. `logo`/`banner`/`hero`/`counties` → `shared/assets.ts` (the `import.meta.url`-relative asset paths were re-based for the new file depth and verified by inspecting the built `dist/assets/*.jpg` output — same three images, same hashes' worth of content, just referenced from a deeper path).

No behavior depends on where these definitions physically live; this is purely making the existing implicit sharing (same-file scope) explicit (`import`).

---

## 3. Verification

All of the following were actually executed in this sandbox against the refactored code, not just reasoned through:

| Check | Result |
|---|---|
| `npx tsc -b` | **Clean**, no errors |
| `npm run build` | **Succeeds** — see bundle table above; PWA precache step also succeeded (46 entries after split vs. 26 before, expected since each chunk is now its own precache entry) |
| `npm run lint` | **0 errors**, 6 warnings — same pre-existing warning *causes* as the original file (two `exhaustive-deps` hints and two intentionally-unused props that existed in the monolithic `App.tsx` before this refactor, plus one unrelated pre-existing warning in `firebase/functions/`), now just attributed to their new file locations. No new warning categories were introduced; one incidental new warning (`react-refresh/only-export-components` on `shared/ui.tsx`, caused by `labelRole` being a non-component export next to components) was found and fixed by inlining `labelRole` as an unexported local helper. |
| `npm run test` (`vitest run`) | **23/23 passing** — `services.test.ts` (21) + `offline-reporting.test.ts` (2), unaffected since neither test file imports anything from `App.tsx` or its extracted pieces |
| `npm run test:pwa` | **25/25 passing** — confirms the production build's manifest, service worker, base-path handling, and icon set are all still correct after the chunk-count change |

**Not verified (sandbox limitation, consistent with this project's existing verification-level convention in `IMPLIMENTATION_STATUS.MD`):** an actual browser click-through of the lazy-loaded routes and the error boundary's fallback UI. This sandbox's background processes don't persist across tool invocations, so a `vite preview` server could not stay up long enough for a live `curl`/browser smoke test. Everything or the build artifacts themselves were inspected directly instead (chunk manifest, precache list, `tsc`/lint/test exit codes). A manual pass — open each of the 17 routes, confirm the on-demand chunk loads in the Network tab, and force a render error to confirm the boundary's fallback renders — is the recommended follow-up once this is deployed to a preview URL.

---

## 4. Net result

- Same UI, same behavior, same routes, same design, same dependencies.
- `App.tsx`: 52,893 bytes → 9 lines.
- Initial JS payload: 113.61 kB gzip → 104.90 kB gzip, with a further ~19.5 kB gzip now deferred into 20 on-demand chunks instead of being mandatory for every visitor.
- Zero error boundaries → one, catching both render errors and lazy-chunk load failures, without changing what a working screen looks like.
- Every feature area is independently readable, testable, and importable for the first time.
