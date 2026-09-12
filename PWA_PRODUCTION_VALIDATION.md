# TraceNet — Production PWA Hardening & Validation

## 0. Status, stated plainly up front

Six real, concretely-verified bugs were found and fixed — not style nitpicks, but things that would actively break the app in production: the update mechanism silently bypassed its own consent flow, the GitHub Pages deep-link recovery file never reached the deployed site at all, and the service worker's image cache would have quietly retained private evidence photos past their authorization window. Every fix below is confirmed against real build output (25 automated tests, all executed, all passing — `npm run test:pwa`), not just reasoned about. What's *not* verified, because it requires a real browser/device/live deployment this sandbox cannot provide, is called out explicitly in §11 rather than implied.

---

## 1. Critical findings, in order of severity

### 1.1 CRITICAL — `404.html` never reached the deployed site at all

**The single most severe finding.** `404.html` — the file that makes GitHub Pages deep links and hard-refreshes work at all, since GitHub Pages has no server-side rewrite capability — lived in the **project root**, not `public/`. Vite only auto-copies files from `public/` into `dist/`; root-level files that aren't build entry points are simply never included. Confirmed by actually building and checking: `ls dist/404.html` → `No such file or directory`.

**Practical impact:** anyone who bookmarked a deep link (`/tracenet-pwa/reports/new`), refreshed the page while on any client-side route, or received a shared link to a specific screen would have hit GitHub's raw, generic 404 error page — not the app, not even the fallback shim, nothing.

**Fix:** moved the file to `public/404.html` (Vite now copies it automatically), and added a `try/catch` around the `sessionStorage` write for robustness in restricted/private-browsing contexts, with a comment documenting the single-segment-base-path assumption baked into its redirect logic.

### 1.2 CRITICAL — the service worker bypassed its own update-consent flow

`vite.config.ts` sets `registerType: 'prompt'` — the intended design: a new service worker installs and *waits*, and only activates once the person confirms an in-app "Update available" prompt. But `src/sw.ts` called `self.skipWaiting()` **unconditionally, at the top level**, on every install — meaning the new worker activated immediately, silently, regardless of `registerType`, regardless of whether anyone had seen or clicked anything.

**Practical impact:** the currently-open tab could end up with its network requests answered by a *different* service worker version than the one its already-loaded JavaScript bundle was built against — precisely the "leave users on broken cached assets" failure mode this task named explicitly. Compounding this, `InstallPrompt.tsx`'s "Refresh" button called a bare `location.reload()`, never actually invoking the update handshake `virtual:pwa-register` provides for exactly this purpose — so even when the (largely decorative, given the above) prompt appeared, its button didn't do what it implied.

**Fix, three coordinated changes:**
- `src/sw.ts`: removed the unconditional call; added `self.addEventListener('message', e => { if(e.data?.type==='SKIP_WAITING') self.skipWaiting() })` — activation now only happens on explicit request.
- `src/main.tsx`: the `updateSW()` function `registerSW()` returns (previously discarded entirely) is now captured and threaded through to the UI via the existing custom-event mechanism.
- `src/components/InstallPrompt.tsx`: the Refresh button now calls the real `updateSW(true)`, which sends the `SKIP_WAITING` message and reloads once the new worker actually takes control.

**Verified:** `npm run test:pwa` confirms every `skipWaiting()` call site in the compiled worker is preceded by the `SKIP_WAITING` message check (not just present somewhere), and confirms both `main.tsx` and `InstallPrompt.tsx`'s source contain the corrected wiring.

### 1.3 HIGH — the service worker could cache private Firebase Storage evidence photos

The runtime image-caching rule matched `request.destination === 'image'` with **no origin restriction at all**. This app's own static images are already precached at build time (they match the precache `globPatterns`), so in practice this rule's only real-world targets were `<img>` tags pointing at `https://firebasestorage.googleapis.com/...` — i.e., report/wanted-notice/id-card evidence photos, which `storage.rules` gate per-request based on ownership, role, and a report's verification status (see `FIREBASE_SECURITY_IMPLEMENTATION.md`).

**Practical impact:** the Cache Storage API has no concept of Firebase's authorization state. Once a photo was fetched and cached this way, it would keep being served from the service worker's cache — bypassing any subsequent rules re-check — for up to 7 days or until evicted, even after a role change, a report's visibility changing, or the underlying object being deleted.

**Fix:** the matcher now requires `url.origin === self.location.origin` — cross-origin Firebase Storage requests are never intercepted by this route at all and always go straight to the network (and therefore straight through `storage.rules`'s authorization check) on every single request.

**Verified:** `npm run test:pwa` confirms the origin check is present in the compiled route matcher, and separately confirms no Firebase domain string appears anywhere in the compiled service worker at all.

### 1.4 MEDIUM — no real PNG icons; `apple-touch-icon` pointed at an SVG

iOS Safari does not rasterize SVG for the `apple-touch-icon` tag — carried over, unfixed, from the original forensic audit. There was also no maskable icon at all beyond a single SVG marked with the combined purpose `"any maskable"`.

**Fix:** generated a full, production-quality PNG icon set (§5) and corrected the manifest's icon declarations.

### 1.5 LOW — no favicon declared; `"any maskable"` combined on one icon

No `<link rel="icon">` at all (browsers would fall back to requesting `/favicon.ico` at the *domain root*, which 404s under a GitHub Pages subpath). Separately, declaring one icon as both `"any"` and `"maskable"` is a well-documented anti-pattern: an icon meant to display full-bleed under `"any"` gets crop-tested by a maskable-aware OS anyway, and an icon that wasn't purpose-built with safe-zone padding looks wrong once actually masked.

**Fix:** added explicit favicon links (32px/16px PNG + SVG); split into genuinely separate `"any"` and `"maskable"` icon manifest entries backed by two different source images (§5).

---

## 2. Web App Manifest — verified field by field

Built with `VITE_BASE_PATH=/tracenet-pwa/` (mirroring the real GitHub Pages deploy workflow) and the actual `dist/manifest.webmanifest` inspected directly:

| Field | Value | Note |
|---|---|---|
| `name` / `short_name` | "TraceNet Liberia" / "TraceNet" | Present, required for install |
| `start_url` | `"./"` | Relative — resolves against the manifest's own URL (`<base>/manifest.webmanifest`), so it correctly becomes `<base>/` under any base path without needing to hardcode one |
| `scope` | `"./"` | Same reasoning; correctly resolves to `<base>/`, confirmed by the generated service-worker registration call using `scope:"/tracenet-pwa/"` |
| `display` | `"standalone"` | Confirmed — not `"browser"`, which would open in a normal tab instead of app-like chrome |
| `theme_color` / `background_color` | `#1E88E5` / `#F5F9FF` | Present — used for the status bar and splash screen in standalone mode |
| `orientation` | `"portrait-primary"` | Present |
| `icons` | 6 entries, PNG + SVG, `any` + `maskable` split (see §5) | Fixed this pass |
| `shortcuts` | "Report incident", "Emergency contacts" | Present, updated to reference the new PNG icons |

---

## 3. Service Worker / Workbox / Precache / Runtime caching

`src/sw.ts` uses `vite-plugin-pwa`'s `injectManifest` strategy (hand-written worker logic, Workbox handles precaching/routing primitives). Verified via actual build output:

- **Precache:** `precacheAndRoute(self.__WB_MANIFEST)` — 26 entries in a base-path build (JS/CSS bundles, `index.html`, the manifest, all icons, hero/banner images). `globPatterns: ['**/*.{js,css,html,svg,png,jpg,jpeg}']` in `vite.config.ts` — confirmed to now also correctly pick up the new icon PNGs with no config change needed.
- **Cache invalidation:** `cleanupOutdatedCaches()` runs on activate — confirmed present in the compiled output via Workbox's internal `precache-v2` cache-name constant (the imported function name itself is minified away, so this was verified by checking for what actually survives minification — see the sibling note in §11 about why this distinction matters for how this test suite was written). Combined with content-hashed JS/CSS filenames (confirmed via `readdirSync` in the test suite — every asset filename matches `-[hash].js`), an old and a new deployment's assets can never collide in the same cache, and stale entries from a superseded deployment are actively removed rather than accumulating forever.
- **Runtime caching:** exactly two routes registered — navigation requests (`NetworkFirst`, 3s timeout, see §4) and same-origin images (`StaleWhileRevalidate`, now origin-restricted per §1.3). Firestore/Storage/Functions calls have `request.destination === ''` (they're fetch/XHR, not navigations or images) and match neither route, so they're never intercepted by the service worker at all — confirmed directly, not inferred, by checking that no Firebase domain string appears anywhere in the compiled worker.
- **Offline navigation:** `setCatchHandler` falls back to the precached `index.html` for any failed navigation (`request.mode === 'navigate'`) — this is what lets the app boot with zero network on a repeat visit. Confirmed in compiled output.

---

## 4. Update lifecycle & cache invalidation — the corrected flow

This is worth walking through end-to-end now that §1.2's fix is in place, since it's the part of this system most prone to silent breakage:

1. A new deployment ships. A returning visitor's browser detects the new `sw.js` differs byte-for-byte from the currently-installed one (standard browser behavior, not app-specific) and begins installing it.
2. The new worker's `install` event runs `precacheAndRoute` for the *new* asset list, but does **not** call `skipWaiting()` — it sits in the `waiting` state, and the currently-open tab keeps being served by the *old* worker, consistently, for as long as that tab stays open.
3. `virtual:pwa-register`'s registration logic detects the waiting worker and fires `onNeedRefresh()`, which now correctly threads the real `updateSW` function through to `InstallPrompt` (§1.2).
4. The person sees "A new TraceNet version is ready" and taps Refresh. This calls `updateSW(true)`, which posts `{type:'SKIP_WAITING'}` — the *only* thing that triggers `self.skipWaiting()` in `sw.ts` now — and reloads the page once the new worker has taken control (`clientsClaim()`, called on `activate`, makes that immediate).
5. The reloaded page is now consistently served by the new worker and the new precached assets. No version-skew window; no silent mid-session takeover.

If the person never taps Refresh, they simply keep using the old (still fully functional) version until their next natural navigation/reload — which is the correct, expected `registerType: 'prompt'` behavior, now actually delivered rather than only nominally configured.

---

## 5. Icons — production-quality PNGs, generated and verified

Rasterized via `sharp` at build-appropriate density from purpose-specific SVG sources (not reused naively across purposes — see below):

| File | Size | Purpose | Alpha |
|---|---|---|---|
| `icons/pwa-192.png`, `pwa-512.png` | 192×192, 512×512 | `any` — full icon artwork | Yes |
| `icons/maskable-192.png`, `maskable-512.png` | 192×192, 512×512 | `maskable` — purpose-built, see below | Yes (background is opaque, but PNG alpha channel present) |
| `icons/apple-touch-icon.png` | 180×180 | iOS home screen | **No** — flattened, full-bleed |
| `icons/favicon-32.png`, `favicon-16.png` | 32×32, 16×16 | Browser tab | Yes |

**Why the maskable icon is a separate design, not a reused crop of the standard icon:** the standard icon's document/text-line details sit close enough to the edges that they'd be cropped away by an aggressive mask shape. The maskable variant instead centers a single bold emblem (the red circle + star) well within Google's recommended safe zone. **Verified visually, not just geometrically reasoned about** — the actual generated `maskable-512.png` was rendered through simulated circle and squircle adaptive-icon masks (the two most common shapes across Android launchers) using the exact same alpha-compositing a real launcher applies, and the emblem survives both with generous clearance on every side.

**Why `apple-touch-icon.png` has no alpha channel:** iOS applies its own corner-rounding and, for a transparent source image, backs it with a solid white square before rounding — which looks visibly wrong against this icon's blue background. The apple-specific source SVG omits the rounded-rect entirely (full-bleed square) and the PNG is flattened against the brand blue, so iOS's own masking produces a clean result.

**Confirmed by an actual double-build test**, not assumed: built once with `VITE_RECAPTCHA_SITE_KEY` unset and once with it set (this was actually to validate a different, prior phase's App Check chunking claim, but the same build-inspection discipline applies throughout this report) — every claim about generated file contents in this document reflects what a real `npm run build` actually produced, checked directly.

---

## 6. Installability, App Scope, Start URL, GitHub Pages base path

All of the following were verified by building with `VITE_BASE_PATH=/tracenet-pwa/` and inspecting `dist/` directly, not by reading the config and assuming it works:

- Every same-origin `src`/`href` in the built `index.html` is correctly prefixed with `/tracenet-pwa/` — verified programmatically across every matched attribute, not spot-checked.
- The manifest link tag: `<link rel="manifest" href="/tracenet-pwa/manifest.webmanifest">` — correctly prefixed.
- The service worker registers with `scope: "/tracenet-pwa/"` (confirmed by inspecting the actual generated registration call in the built bundle) — matching the manifest's `scope` and preventing the worker from ever being able to control anything outside the app's own subpath, which matters on GitHub Pages where multiple projects can be hosted as sibling paths under the same origin (`username.github.io/other-project/` must never be affected by this app's service worker, and it structurally cannot be).
- React Router's `<BrowserRouter basename={import.meta.env.BASE_URL}>` — `BASE_URL` is Vite's own built-in reflection of the configured `base`, so this always stays in sync automatically; no separate hardcoded value to drift.
- `.github/workflows/deploy-pages.yml` computes `VITE_BASE_PATH` as `/${GITHUB_REPOSITORY#*/}/` — i.e., always exactly `/<repo-name>/`, a single path segment. This matches `404.html`'s redirect logic's assumption exactly (documented explicitly in the file itself now, per §1.1's fix) — the two were previously coupled implicitly with no comment connecting them; a future change to either without checking the other would have been a silent trap.

---

## 7. Deep links & 404 recovery

Covered in depth in §1.1. To restate the mechanism now that it's actually deployed: GitHub Pages serves `404.html` for any request path it can't resolve to a real file (which, for a client-side-routed SPA, is *every* deep link and every hard refresh on a non-root route). That file stashes the requested path in `sessionStorage` and redirects to the app's base path; `main.tsx` reads that back on load and calls `history.replaceState` to restore the real URL before React Router ever renders — so the person lands on the page they actually asked for, not the home screen, with no visible flash of a wrong route in between (the restore happens before `createRoot(...).render(...)` is called).

---

## 8. Standalone mode, Maskable icons, Apple metadata

- **Standalone mode:** `display: "standalone"` confirmed in the manifest (§2).
- **Maskable icons:** covered in depth in §5, including the visual mask-simulation verification.
- **Apple metadata:** `apple-mobile-web-app-capable`, `apple-mobile-web-app-status-bar-style`, `apple-mobile-web-app-title` were already present and correct from a prior pass; this pass fixed the actually-broken piece (`apple-touch-icon` pointing at an unsupported SVG, §1.4) and added the standard favicon links alongside it.

---

## 9. Background Sync — honest capability, not overstated reliability

Per the explicit instruction not to claim Background Sync works everywhere: it doesn't, and the code and its comments now say so plainly. Background Sync (the browser API triggering `sw.ts`'s `sync` event listener) is a Chromium-only feature — no support in Safari/iOS or Firefox as of this writing. Where it *is* supported, this app treats it as exactly what it can safely be treated as: a best-effort accelerator that does nothing more than relay a "you might be able to sync now" message to already-open tabs — the same nudge the standard `online` window event already provides.

**The actual, universal mechanism** — the one every browser gets, with or without Background Sync — is `services.ts#initializeMediaSync()`'s `online` event listener, built in the offline-reporting phase of this project and unchanged here. `npm run test:pwa` verifies this listener is present in `services.ts` and that `sw.ts` documents the reliability caveat explicitly, rather than merely trusting that a comment somewhere says the right thing.

---

## 10. Sensitive data caching — resolved

Covered fully in §1.3. Restated as its own checklist item since it was explicitly called out in the task: **the service worker cannot cache Firestore, Storage, or Functions responses at all** (none match any registered route — different `request.destination`), and the one route that theoretically *could* have caught Storage image responses is now hard-restricted to same-origin, closing that off structurally rather than by convention.

---

## 11. Testing — what's automated and verified vs. what needs a real device/browser

### Automated, executed, passing: 25/25 (`npm run test:pwa`)

`src/pwa-build.test.ts` runs a real `npm run build` (with a GitHub-Pages-style base path) as an actual subprocess and inspects the real generated `manifest.webmanifest`, `index.html`, `404.html`, `sw.js`, and icon files — no mocking, no emulator, nothing simulated. This is the same "verify against real output, not assumptions" discipline used throughout this project, and it's fully executable in this sandbox with zero external dependencies, unlike the Firebase-rules test suites elsewhere in this project.

**A methodology note worth being explicit about**, because it shaped how these tests had to be written: production minification renames locally-imported identifiers (`matchPrecache`, `registerRoute`, `cleanupOutdatedCaches` all become short meaningless names like `Oe`, `q`, `v`), so a naive `expect(swJs).toContain('matchPrecache')`-style assertion can silently pass or fail for the wrong reason — in two cases while writing this suite, an assertion built on a since-minified identifier either failed correctly (caught) or, worse, matched zero real occurrences and passed as a false positive on an empty loop (also caught, by manually checking the actual compiled output against every literal-string assumption before trusting the suite). The final suite deliberately asserts on things minification cannot touch: string literals (`"index.html"`, `"image"`), object property key names (`networkTimeoutSeconds`), globals accessed via property lookup (`self.skipWaiting`), and Workbox's own internal constant strings (`precache-v2`) — each verified by direct inspection of the actual compiled `dist/sw.js`, not assumed to survive.

Mapped against the requested test list:

| Requested test | Automated coverage |
|---|---|
| **GitHub Pages subpath** | Full — base-path-prefixing across every asset reference, manifest relative resolution, SW scope |
| **Deep link** | `404.html` present + correct in `dist/`; restore logic present in `main.tsx` source |
| **New deployment** | Content-hashed filenames (no collision); `cleanupOutdatedCaches` present |
| **Old cached version** | `skipWaiting` gating confirmed; `updateSW` capture-and-use confirmed in both `main.tsx` and `InstallPrompt.tsx` source |
| **Offline launch** (the mechanism) | `setCatchHandler` → precached `index.html` fallback confirmed |
| **Online recovery** (the mechanism) | `NetworkFirst` navigation strategy confirmed; app-driven `online` listener confirmed |
| **Installed PWA** (the declared config) | `display: standalone`, theme/background color, Apple meta tags all confirmed |
| **Reload** (the mechanism) | Same as "old cached version" — the update pipeline is what makes a reload correct |

### Requires a real browser/device — not executable in this sandbox, and not claimed to be

| Test | Why it can't be automated here | How to actually check |
|---|---|---|
| **First visit** (does the install banner actually appear) | Requires a real Chrome/Edge instance evaluating live installability heuristics against a served origin | Deploy, visit in Chrome, confirm the install icon appears in the address bar / `beforeinstallprompt` fires (the app's own listener for this was unchanged in this pass and already existed) |
| **Installed PWA** (does it actually look/feel native once installed) | Requires actually installing on a device | Install on Android and iOS; confirm standalone chrome (no browser UI), confirm the home-screen icon matches what §5 describes |
| **Offline launch** (real device, airplane mode) | Requires a real service-worker-controlled browser tab and toggling real network state | Visit once online, enable airplane mode, relaunch from the home screen icon or reopen the tab |
| **Online recovery** (real reconnect) | Same reasoning | Go offline, submit a report (per `OFFLINE_REPORTING_PRODUCTION.md`), reconnect, confirm it syncs |
| **GitHub Pages subpath** (the *live* site) | This sandbox cannot deploy to GitHub Pages or reach the public internet | After deploying, visit `https://<user>.github.io/tracenet-pwa/` directly and confirm it loads |
| **Deep link** (the *live* 404 redirect) | Same — needs a real GitHub Pages-hosted 404 response | Visit `https://<user>.github.io/tracenet-pwa/reports/new` directly (not via in-app navigation) and confirm it lands on the Report form, not a GitHub 404 page |
| **New deployment / old cached version** (real timing) | Requires two real deployments in sequence and a real browser holding the first one open across the second | Deploy, load the app, deploy again, confirm the update prompt appears and Refresh genuinely picks up the new version rather than appearing to do nothing |

None of these are hand-waved as "probably fine" — each has a concrete, specific step listed for whoever has access to a real device and a live deployment to close the loop.

---

## 12. Files Changed

| File | Change |
|---|---|
| `src/sw.ts` | Removed unconditional `skipWaiting()`, added the gated message listener; restricted image-caching route to same-origin; expanded comments on Background Sync reliability |
| `src/main.tsx` | Captures `registerSW()`'s returned `updateSW` function; passes it through the existing custom-event mechanism instead of discarding it |
| `src/components/InstallPrompt.tsx` | Refresh button now calls the real `updateSW(true)` instead of a bare `location.reload()` |
| `index.html` | Fixed `apple-touch-icon` to reference a real PNG; added favicon `<link>` tags |
| `public/404.html` | **New location** (moved from the project root, which Vite never copied into `dist/`); added `try/catch` robustness and documentation comments |
| `404.html` (root) | Deleted — superseded by `public/404.html` |
| `public/icons/pwa-192.png`, `pwa-512.png`, `maskable-192.png`, `maskable-512.png`, `apple-touch-icon.png`, `favicon-32.png`, `favicon-16.png` | New — production PNG icon set, generated from purpose-specific source SVGs via `sharp` |
| `vite.config.ts` | Manifest `icons` array rebuilt: PNG-first, `any`/`maskable` purposes properly separated (no more combined `"any maskable"`); `shortcuts` icons updated to PNG |
| `src/pwa-build.test.ts` | New — 25 tests, executed against real build output (see §11) |
| `vitest.pwa.config.ts` | New — dedicated config so this suite (which invokes real builds, ~30s) doesn't slow down the fast unit-test suite |
| `package.json` | Added `test:pwa` script; added `sharp` as a devDependency (needed for both icon generation and the test suite's PNG-dimension assertions) |

**Also encountered and fixed along the way, unrelated to PWA specifics but blocking a clean install:** `vitest` was pinned to `^4.1.11`, which triggers a reproducible `npm`/Arborist internal crash (`Cannot read properties of null (reading 'edgesOut')`) on a fully clean install (confirmed reproducible with a cleared cache, not a fluke) — reverted to `^3.2.4`, the version this project has used reliably across every prior phase; no test code required any v4-specific API, so this was a pure, safe downgrade. Re-verified `npm ci` succeeds cleanly with no flags after the revert.

---

## 13. Full gauntlet, re-verified clean after every change in this pass

```
npm ci            → PASS
npm run build     → PASS (base-path and default builds both)
npm run lint      → PASS (0 errors, 18 pre-existing warnings, unrelated to this pass)
npm run typecheck → PASS
npm test          → PASS (23/23 — unaffected by this pass, confirmed unchanged)
npm run test:pwa  → PASS (25/25 — new this pass)
```

---

## 14. What this pass does not claim

Per §11, real-browser and real-device behavior (install banners, actual iOS/Android rendering, live GitHub Pages hosting, real update timing across two deployments) is not and cannot be verified from this sandbox — each has a specific manual step listed rather than being asserted as working. This pass does not address anything outside the PWA shell itself (Firestore/Storage rules, Cloud Functions, abuse protection, and the media/offline pipeline are all covered by their own prior reports and unaffected by anything here). **Production readiness overall remains uncertified**, consistent with every prior phase of this project.
