# GitHub Pages Production Deployment Guide

This is the authoritative, step-by-step reference for deploying TraceNet to
GitHub Pages. It documents the **exact repository configuration required**,
what `.github/workflows/deploy-pages.yml` does at each step, and how to
verify a deployment is actually working end-to-end (not just that the build
succeeded).

For the architectural overview (why the app is structured this way), see
`DEPLOY_GITHUB.md`. This guide focuses on the concrete configuration steps
and verification procedure.

---

## 1. Required GitHub repository configuration

### 1.1 Pages source

**Settings → Pages → Build and deployment → Source: `GitHub Actions`**

Do not select "Deploy from a branch" — the workflow publishes the built
`dist/` artifact via `actions/deploy-pages`, not a branch's raw contents.

### 1.2 Default branch

The workflow triggers on `push` to `main`. If the repository's default
branch is named differently, update the `branches:` list in
`.github/workflows/deploy-pages.yml` to match, or push to `main` directly.

### 1.3 Repository variables (Settings → Secrets and variables → Actions → Variables tab)

These are **variables**, not secrets — the Firebase web config is meant to
be public (it identifies the project to Google's servers; it is not a
credential that grants privileged access — access control is enforced by
Firestore/Storage Rules and Firebase Auth, not by hiding this config). Using
repository *variables* rather than *secrets* also means their values are
visible in workflow logs when useful for debugging, which secrets
deliberately redact.

| Variable | Required | Source |
| --- | --- | --- |
| `VITE_FIREBASE_API_KEY` | Yes, for Auth/Firestore/Storage to work | Firebase Console → Project settings → General → Your apps → Web app → SDK setup and configuration |
| `VITE_FIREBASE_AUTH_DOMAIN` | Yes | same location |
| `VITE_FIREBASE_PROJECT_ID` | Yes | same location |
| `VITE_FIREBASE_STORAGE_BUCKET` | Yes, if the app reads/writes Storage | same location |
| `VITE_FIREBASE_MESSAGING_SENDER_ID` | Yes, now that push notifications use Cloud Messaging (see below) | same location |
| `VITE_FIREBASE_APP_ID` | Yes | same location |
| `VITE_FIREBASE_VAPID_KEY` | Only if push notifications should work | Firebase Console → Project settings → Cloud Messaging → Web configuration → Web Push certificates |
| `VITE_RECAPTCHA_SITE_KEY` | Only if Firebase App Check is enabled | Firebase Console → App Check → your web app → reCAPTCHA v3 site key |
| `VITE_BASE_PATH` | Optional override | See §1.4 |

If `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_PROJECT_ID`, or
`VITE_FIREBASE_APP_ID` are left unset, the site will still build and deploy,
but `firebaseReady` in `src/services.ts` evaluates to `false` and
Authentication/Firestore/Storage/Functions never initialize on the live
site — the workflow's "Verify production build artifacts" step emits a
`::warning::` in this case so it isn't a silent failure.

If `VITE_FIREBASE_VAPID_KEY` specifically is left unset, everything else
in the app works normally — `enablePushNotifications()` (`src/push.ts`)
simply fails with a clear "not configured for this deployment" message
when someone taps "Enable Push Notifications" in Profile settings,
rather than the feature being broken or the button being confusingly
absent. There is no build-time warning for this one, unlike the three
above, because push notifications are an enhancement, not something the
rest of the app depends on functioning.

**Never put the Firebase Admin SDK service account key, any private key, or
any server-side secret in a `VITE_*` variable.** Everything prefixed
`VITE_` is compiled into the public JavaScript bundle and is visible to
anyone who opens the deployed site.

### 1.4 Base path (`VITE_BASE_PATH`)

The workflow derives this automatically:

```
VITE_BASE_PATH=/<repository-name>/
```

which is correct for a standard **project site** (e.g.
`https://<owner>.github.io/<repo>/`). Override it with a `VITE_BASE_PATH`
repository variable only if:

- Deploying to a **user/organization site** (`<owner>.github.io` with no
  repo suffix) — set `VITE_BASE_PATH=/`.
- Deploying behind a **custom domain** at the root — set
  `VITE_BASE_PATH=/`.
- Deploying a custom domain under a subpath — set it to that subpath.

This same value drives Vite's asset URLs, React Router's `basename`, the
web app manifest, and the service worker registration — there is only one
place to change it.

### 1.5 Firebase Console: authorized domains

**Firebase Console → Authentication → Settings → Authorized domains → Add domain**

Add the exact hostname the site will be served from:

- Project site: `<owner>.github.io` (the hostname only — do not include the
  `/<repo>/` path; authorized domains match on origin, not path).
- Custom domain: the custom domain itself, e.g. `app.example.org`.

Without this, `signInWithPopup`/`signInWithRedirect` (Google sign-in) will
fail with `auth/unauthorized-domain` even though the Firebase web config is
correctly injected into the build.

### 1.6 Custom domain (optional)

If using a custom domain: add a `public/CNAME` file containing the domain,
configure the DNS records GitHub Pages documents for apex or `www` domains,
and set `VITE_BASE_PATH=/` (§1.4) since the app is then served from the
domain root, not a repo subpath. Also re-check §1.5.

### 1.7 Cloud Functions (deployed separately from GitHub Pages)

The GitHub Pages workflow deploys only the static frontend — it never runs
`firebase deploy`. The eight Cloud Functions in `firebase/functions/` (see
`FIREBASE_FUNCTIONS_ARCHITECTURE.md`) need their own deploy, run manually
(or from a separate workflow this project doesn't currently have):

```bash
cd firebase/functions && npm ci && npm run build
firebase deploy --only functions
```

`notifyOnAlertCreated` is a **Firestore trigger** (`onDocumentCreated`),
not a callable function like the other seven — this specifically requires
the **Eventarc API** to be enabled on the Firebase project (Google Cloud
Console → APIs & Services → enable "Eventarc API"), which is a one-time
Console-side setup step `firebase deploy` does not do for you. If it's not
enabled, the deploy itself will fail with a clear error naming Eventarc,
not silently skip the function.

Push notifications also require the `VITE_FIREBASE_VAPID_KEY` repository
variable (§1.3) to be set for the *frontend* build — the Function-side
deploy above and the frontend env var are two independent prerequisites,
and either one being missing means push notifications don't work even
though everything else in the app does.

---

## 2. What the workflow does

`.github/workflows/deploy-pages.yml` runs two jobs, `build` then `deploy`:

### `build`

1. **Checkout** the repository.
2. **Install dependencies** with `npm ci` — this requires `package-lock.json`
   to be committed and in sync with `package.json`; `npm ci` fails (by
   design) if they've drifted, which is the correct behavior for
   reproducible CI builds.
3. **Type-check** (`tsc -b`) and **lint** (`eslint .`) — lint warnings do not
   fail the build; lint *errors* do.
4. **Validate PWA build output** — runs `npm run test:pwa`
   (`src/pwa-build.test.ts`), which performs a full production build against
   a synthetic base path and inspects the real `dist/` output: every asset
   reference is correctly prefixed, the manifest has installable fields and
   correctly-sized icons, `404.html` contains the deep-link redirect logic,
   and the service worker never calls `skipWaiting()` outside the
   `SKIP_WAITING` message handler. This step runs its own build/rebuild
   internally and leaves a plain, non-prefixed `dist/` behind afterward — it
   does not affect the real production build in the next step.
5. **Resolve the base path** — see §1.4.
6. **Build** — `npm run build` (`tsc -b && vite build`), with the Firebase
   `VITE_*` variables and the resolved `VITE_BASE_PATH` injected as env vars.
7. **Verify production build artifacts** — a second, real-build check: confirms
   `index.html`, `404.html`, `manifest.webmanifest`, and `sw.js` all exist,
   that `404.html` still contains the redirect logic, and that
   `index.html`'s asset and manifest references are actually prefixed with
   the *real* resolved base path (not the synthetic one from step 4). Warns
   (without failing) if required Firebase variables are missing.
8. **Configure Pages** and **upload the `dist/` artifact**.

### `deploy`

1. **Deploy** the uploaded artifact via `actions/deploy-pages`, using the
   `id-token: write` permission for OIDC (no long-lived deployment secret
   needed).
2. **Smoke-test** — requests the deployed URL and retries for up to a
   minute, warning (not failing) if it doesn't see an HTTP 200 in that
   window, since first-ever Pages deployments can occasionally take a few
   minutes to propagate.

If any step in `build` fails, `deploy` never runs — a broken build is never
published.

---

## 3. Manual verification checklist (after a deploy)

The workflow's automated checks cover build correctness; these require a
real browser against the live URL:

- [ ] Open `https://<owner>.github.io/<repo>/` — page loads with no console
      404s for JS/CSS/icons/manifest.
- [ ] Open DevTools → Application → Manifest — installable, icons render,
      no errors.
- [ ] Open DevTools → Application → Service Workers — `sw.js` registered
      and activated, scoped to `/<repo>/`.
- [ ] Navigate to a deep route (e.g. `/<repo>/reports/new`), then hard-refresh
      the browser — should land back on that same route, not a GitHub 404
      page or the home route.
- [ ] Sign in (if Firebase Auth is configured) — confirms
      `VITE_FIREBASE_*` variables reached the build and the deployed domain
      is an authorized domain (§1.5).
- [ ] Install the app (Chrome/Edge install icon, or Android "Add to Home
      screen") and confirm it opens in standalone mode at the correct
      start URL.
- [ ] Go offline (DevTools → Network → Offline) and reload — the app shell
      should still load from the service worker cache.
- [ ] Push a follow-up commit and reload the already-open tab — the in-app
      "Update available" prompt should appear rather than silently serving
      stale assets forever.
- [ ] If Cloud Functions were deployed (§1.7) and `VITE_FIREBASE_VAPID_KEY`
      is set: Profile → "Enable Push Notifications" → accept the browser
      permission prompt → confirm no error toast appears. Have an admin
      publish a test alert (or trigger SOS) and confirm a real push
      notification arrives, including with the tab in the background.

---

## 4. Troubleshooting

| Symptom | Likely cause |
| --- | --- |
| Blank page, console 404s for `/assets/...` | `VITE_BASE_PATH` mismatch — confirm it matches the actual deployed subpath (§1.4). |
| Deep link / refresh on a route shows GitHub's raw 404 page | `public/404.html` didn't reach the deploy, or Pages source isn't set to GitHub Actions (§1.1) — a branch-based Pages deploy would serve a different file layout. |
| Sign-in fails with `auth/unauthorized-domain` | The deployed hostname isn't in Firebase Authentication's authorized domains (§1.5). |
| App loads but no data / silent Firebase failures | One or more `VITE_FIREBASE_*` repository variables are unset — check the `::warning::` in the "Verify production build artifacts" step's log. |
| `npm ci` fails in CI | `package-lock.json` is missing or out of sync with `package.json` — regenerate it locally with `npm install` and commit it. |
| Workflow fails at "Validate PWA build output" | A real regression in build output — read the specific failing assertion in `src/pwa-build.test.ts`; it's written to name exactly what broke. |
| Custom domain shows GitHub's default 404 or doesn't resolve | DNS not yet propagated, or `public/CNAME` missing/incorrect (§1.6). |
| "Enable Push Notifications" always fails with "not configured for this deployment" | `VITE_FIREBASE_VAPID_KEY` isn't set for the frontend build (§1.3) — this is separate from the other `VITE_FIREBASE_*` variables and easy to miss since the rest of the app works fine without it. |
| Push permission is granted but no notification ever arrives | `subscribeToAlerts`/`notifyOnAlertCreated` weren't deployed (§1.7), or the Eventarc API isn't enabled on the Firebase project — check the Cloud Functions deploy output/logs, not the frontend. |
