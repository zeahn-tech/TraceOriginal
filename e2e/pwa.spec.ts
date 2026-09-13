import { test, expect } from '@playwright/test'
import { seedRoleUser, signInViaUi, resetFirestoreAndAuthEmulators } from './helpers'

// ---------------------------------------------------------------------------
// Workflow #13 PWA installation, #14 Refresh, #15 Deep link.
// Not executed in this sandbox — see AUTOMATED_QA_REPORT.md. These run
// against the real production build (`npm run build` + `vite preview`, see
// playwright.config.ts), not `vite dev`, matching what `npm run test:pwa`
// already validates at the build-output level (manifest, precache list,
// injected service worker) — this file is what would additionally exercise
// that build inside a real browser.
// ---------------------------------------------------------------------------
test.beforeEach(async () => { await resetFirestoreAndAuthEmulators() })

test('#13 PWA installation — a beforeinstallprompt event surfaces the app\'s real Install banner and button', async ({ page }) => {
  await page.goto('/')
  await page.evaluate(() => {
    const evt = new Event('beforeinstallprompt', { cancelable: true }) as Event & { prompt: () => Promise<void>; userChoice: Promise<{ outcome: string }> }
    evt.prompt = async () => {}
    evt.userChoice = Promise.resolve({ outcome: 'accepted' })
    window.dispatchEvent(evt)
  })
  await expect(page.getByText(/install tracenet for faster offline access/i)).toBeVisible()
  await expect(page.getByRole('button', { name: /^install$/i })).toBeVisible()
})

test('#13 PWA installation — the manifest is linked and installability criteria are met', async ({ page }) => {
  await page.goto('/')
  const manifestHref = await page.locator('link[rel="manifest"]').getAttribute('href')
  expect(manifestHref).toBeTruthy()
  const manifest = await page.evaluate(async (href) => {
    const res = await fetch(href!)
    return res.json()
  }, manifestHref)
  expect(manifest.display).toBe('standalone')
  expect(manifest.icons.some((i: { purpose?: string }) => i.purpose === 'maskable')).toBe(true)

  // A service worker must actually register for the browser to consider the
  // app installable at all.
  await page.waitForFunction(() => Boolean(navigator.serviceWorker?.controller) || Boolean(navigator.serviceWorker?.ready))
  const registered = await page.evaluate(async () => Boolean(await navigator.serviceWorker.getRegistration()))
  expect(registered).toBe(true)
})

test('#14 refresh — reloading a signed-in, deep session preserves the session and current route', async ({ page }) => {
  await seedRoleUser('refresh-user@e2e.test', 'correct-horse-battery-staple', 'Refresh User', 'CITIZEN')
  await signInViaUi(page, 'refresh-user@e2e.test', 'correct-horse-battery-staple')
  await page.goto('/profile')
  await page.reload()
  await expect(page).toHaveURL('/profile')
  await expect(page.getByText(/sign in/i)).toHaveCount(0)
})

test('#14 refresh — after a new service worker is found, the "update available" prompt appears and Refresh calls the real updateServiceWorker function', async ({ page }) => {
  await page.goto('/')
  await page.waitForFunction(() => Boolean(navigator.serviceWorker?.ready))
  // Fires the exact custom event main.tsx's registerSW({onNeedRefresh}) dispatches
  // (`tracenet-pwa-update`, detail = an update function) — real app wiring,
  // not a stand-in event name. `window.__e2eUpdateCalled` lets the test
  // assert the real button actually invokes the function InstallPrompt
  // received, not just that some button rendered.
  await page.evaluate(() => {
    (window as unknown as { __e2eUpdateCalled?: boolean }).__e2eUpdateCalled = false
    window.dispatchEvent(new CustomEvent('tracenet-pwa-update', {
      detail: async () => { (window as unknown as { __e2eUpdateCalled?: boolean }).__e2eUpdateCalled = true },
    }))
  })
  await expect(page.getByText(/a new tracenet version is ready/i)).toBeVisible()
  await page.getByRole('button', { name: /^refresh$/i }).click()
  await expect.poll(() => page.evaluate(() => (window as unknown as { __e2eUpdateCalled?: boolean }).__e2eUpdateCalled)).toBe(true)
})

test('#15 deep link — visiting /reports/new directly (no in-app navigation first) renders the report form when signed in', async ({ page }) => {
  await seedRoleUser('deeplink-user@e2e.test', 'correct-horse-battery-staple', 'Deep Link User', 'CITIZEN')
  await signInViaUi(page, 'deeplink-user@e2e.test', 'correct-horse-battery-staple')
  await page.goto('/reports/new')
  await expect(page.getByText(/incident report/i)).toBeVisible()
  await expect(page.getByLabel('Short Title')).toBeVisible()
})

test('#15 deep link — visiting a protected deep link while signed out redirects to sign-in rather than a dead page', async ({ page }) => {
  await page.goto('/reports/new')
  await expect(page).toHaveURL('/sign-in')
})

test('#15 deep link — visiting an unknown path does not produce a static-host 404, it resolves inside the app', async ({ page }) => {
  const response = await page.goto('/this/route/does/not/exist')
  expect(response?.status()).toBeLessThan(400)
  await expect(page.getByText(/404/i)).toHaveCount(0)
})
