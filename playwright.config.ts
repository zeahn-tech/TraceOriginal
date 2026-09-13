import { defineConfig, devices } from '@playwright/test'

// ---------------------------------------------------------------------------
// Browser-automation layer for TraceNet's 16 critical user workflows.
//
// STATUS: written and type-checked in this sandbox; NEVER EXECUTED here.
// Two independent, verified blockers, both pre-existing conditions of this
// sandbox (see IMPLIMENTATION_STATUS.MD "Known, permanently-open items"),
// not something introduced or worked around by this suite:
//   1. `npx playwright install chromium` fails — cdn.playwright.dev is not
//      in this sandbox's network egress allowlist (403, confirmed by
//      actually running the install, not assumed).
//   2. `firebase emulators:start` fails for the same reason against
//      storage.googleapis.com (403, also confirmed by actually running it —
//      identical to the Phase 5/6 blocker already documented for the
//      Firestore/Storage rules and Functions integration tests).
// This suite requires BOTH a real browser binary and a real Firebase
// Emulator Suite to run, so it inherits both blockers simultaneously.
// See AUTOMATED_QA_REPORT.md for the full verification-level breakdown.
// ---------------------------------------------------------------------------
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false, // shares one emulator project's Auth/Firestore state across specs
  retries: 0,
  reporter: [['list'], ['html', { outputFolder: 'e2e/report', open: 'never' }]],
  timeout: 30_000,
  use: {
    baseURL: 'http://localhost:4173',
    trace: 'retain-on-failure',
    video: 'retain-on-failure',
  },
  webServer: [
    {
      // Firebase Auth + Firestore + Storage emulator, seeded with the
      // project's real security rules — the same combination
      // `npm run test:security` uses, just kept running for the whole
      // browser session instead of exec'd around a single vitest run.
      command: 'npx firebase-tools emulators:start --only auth,firestore,storage --project tracenet-e2e',
      url: 'http://127.0.0.1:8080',
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
    {
      // Real production build served statically — NOT `vite dev` — so the
      // service worker, manifest, and precache list under test are the
      // actual shipped artifact (see PWA_ARCHITECTURE.md), matching what
      // `npm run test:pwa` already validates at the build-output level.
      command: 'npm run build && npm run preview -- --port 4173',
      url: 'http://localhost:4173',
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
      env: {
        VITE_FIREBASE_API_KEY: 'demo-api-key',
        VITE_FIREBASE_AUTH_DOMAIN: 'localhost',
        VITE_FIREBASE_PROJECT_ID: 'tracenet-e2e',
        VITE_FIREBASE_STORAGE_BUCKET: 'tracenet-e2e.appspot.com',
        VITE_FIREBASE_APP_ID: '1:e2e:web:e2e',
        VITE_FIREBASE_MESSAGING_SENDER_ID: '000000000000',
        VITE_USE_FIREBASE_EMULATOR: 'true',
      },
    },
  ],
  projects: [
    { name: 'desktop-chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile-chromium-pwa', use: { ...devices['Pixel 7'] } },
  ],
})
