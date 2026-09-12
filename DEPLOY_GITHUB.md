# GitHub Pages Deployment

## Architecture

TraceNet is built as a Vite single-page application and deployed as a static artifact through GitHub Actions. GitHub Pages serves the compiled `dist` directory; Firebase remains the backend for Authentication, Firestore, Storage, and Functions.

The workflow derives `VITE_BASE_PATH` from the repository name. A project site uses `/<repository>/`; an organization site or custom domain should set `VITE_BASE_PATH=/` in the workflow or repository variable. The same base is consumed by Vite assets, React Router's `basename`, the generated manifest, and the service worker.

## One-time setup

1. Push the repository to GitHub with the default branch named `main`.
2. In **Settings > Pages**, set **Source** to **GitHub Actions**.
3. Add the public Firebase web configuration as repository variables named `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN`, `VITE_FIREBASE_PROJECT_ID`, `VITE_FIREBASE_STORAGE_BUCKET`, `VITE_FIREBASE_MESSAGING_SENDER_ID`, and `VITE_FIREBASE_APP_ID`.
4. Add the deployed Pages hostname to Firebase Authentication authorized domains and configure the Google provider redirect/origin settings.
5. Confirm Firebase Rules, indexes, Storage Rules, and Functions are deployed by their protected Firebase workflow. They are not deployed by the Pages workflow.

## Deployment

Push to `main` or run **Actions > Deploy TraceNet PWA > Run workflow**. The workflow runs `npm ci`, sets the repository base path, builds Vite, uploads `dist`, and deploys it with the Pages OIDC token.

The build must succeed before deployment. Keep `package-lock.json` committed so `npm ci` is reproducible. Do not put Firebase Admin credentials, private keys, or other secrets in `VITE_*` variables; Vite embeds `VITE_*` values into public JavaScript.

## Routing and deep links

`BrowserRouter` uses `import.meta.env.BASE_URL`. On a direct request to `/repository/dashboard`, GitHub Pages returns `404.html`; the fallback stores the requested URL, loads the app shell, and `src/main.tsx` restores the URL before route matching. This keeps clean browser URLs without requiring hash routing.

## PWA behavior

`vite-plugin-pwa` uses the custom `src/sw.ts` worker with `injectManifest`. Vite fingerprints compiled assets. Workbox precaches the generated shell and calls `cleanupOutdatedCaches()` so old asset caches are invalidated after release. Navigation falls back to the precached shell when offline, while Firebase data uses Firestore's IndexedDB persistence.

The manifest provides standalone display, shortcuts, theme/background colors, and an `any maskable` icon. Android, Windows, macOS, and Linux Chromium-based browsers can install through the browser prompt. iPhone Safari does not support `beforeinstallprompt`; users install through **Share > Add to Home Screen**, which the app explains in its install prompt.

## Verification checklist

- Open the deployed URL and confirm JavaScript, CSS, icons, manifest, and `sw.js` load under the repository path.
- Visit a deep route directly, refresh it, and confirm it remains on that route.
- Install from Chrome/Edge and confirm the standalone launch URL and shortcuts.
- Turn off the network after the first successful load and confirm the shell and previously cached Firestore records open.
- Publish a new commit and confirm the update notification appears without discarding active work.
- Inspect DevTools Application > Cache Storage and confirm obsolete Workbox caches are removed after activation.
- Test the manual iPhone Safari installation flow separately; iOS does not expose the Chromium install prompt API.