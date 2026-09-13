import { test, expect } from '@playwright/test'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { seedRoleUser, signInViaUi, resetFirestoreAndAuthEmulators } from './helpers'

// ---------------------------------------------------------------------------
// Workflow #4 Report submission, #5 image, #6 video, #7 audio,
// #8 Offline report, #9 Reconnection, #10 Media synchronization.
// Not executed in this sandbox — see AUTOMATED_QA_REPORT.md.
// ---------------------------------------------------------------------------
const FIXTURES = path.join(path.dirname(fileURLToPath(import.meta.url)), 'fixtures')

test.beforeEach(async ({ page }) => {
  await resetFirestoreAndAuthEmulators()
  await seedRoleUser('reporter@e2e.test', 'correct-horse-battery-staple', 'Test Reporter', 'CITIZEN')
  await signInViaUi(page, 'reporter@e2e.test', 'correct-horse-battery-staple')
})

async function fillReportBasics(page: import('@playwright/test').Page, title: string) {
  await page.goto('/reports/new')
  await page.getByLabel('Short Title').fill(title)
  await page.getByLabel('Detailed Description').fill('A detailed, real-workflow test description of the incident.')
  await page.getByLabel(/i declare this report is truthful/i).check()
}

test('#4 report submission — a plain text report reaches SYNCED status while online', async ({ page }) => {
  await fillReportBasics(page, 'E2E plain report')
  await page.getByRole('button', { name: /submit report/i }).click()
  await expect(page.getByText(/incident report submitted successfully/i)).toBeVisible()
  await expect(page).toHaveURL('/dashboard')
})

test('#5 report with image — an attached photo uploads and the report syncs', async ({ page }) => {
  await fillReportBasics(page, 'E2E report with image')
  await page.getByLabel(/attach evidence/i).setInputFiles(path.join(FIXTURES, 'sample.jpg'))
  await expect(page.getByText('sample.jpg')).toBeVisible()
  await page.getByRole('button', { name: /submit report/i }).click()
  await expect(page.getByText(/incident report submitted successfully/i)).toBeVisible()
})

test('#6 report with video — an attached video uploads and the report syncs', async ({ page }) => {
  await fillReportBasics(page, 'E2E report with video')
  await page.getByLabel(/attach evidence/i).setInputFiles(path.join(FIXTURES, 'sample.mp4'))
  await expect(page.getByText('sample.mp4')).toBeVisible()
  await page.getByRole('button', { name: /submit report/i }).click()
  await expect(page.getByText(/incident report submitted successfully/i)).toBeVisible()
})

test('#7 report with audio — an attached voice note uploads and the report syncs', async ({ page }) => {
  await fillReportBasics(page, 'E2E report with audio')
  await page.getByLabel(/attach evidence/i).setInputFiles(path.join(FIXTURES, 'sample.mp3'))
  await expect(page.getByText('sample.mp3')).toBeVisible()
  await page.getByRole('button', { name: /submit report/i }).click()
  await expect(page.getByText(/incident report submitted successfully/i)).toBeVisible()
})

test('#8 offline report — submitting with no network saves the report locally instead of failing', async ({ page, context }) => {
  await fillReportBasics(page, 'E2E offline report')
  await context.setOffline(true)
  await page.getByRole('button', { name: /submit report/i }).click()
  await expect(page.getByText(/you appear to be offline.*saved on this device/i)).toBeVisible()
  await expect(page).toHaveURL('/dashboard')
  // The pending report is visible locally even though nothing reached the server yet.
  await expect(page.getByText('E2E offline report')).toBeVisible()
  await context.setOffline(false)
})

test('#9 reconnection + #10 media synchronization — an offline report (with an attached photo) syncs automatically once back online', async ({ page, context }) => {
  await context.setOffline(true)
  await fillReportBasics(page, 'E2E reconnection report')
  await page.getByLabel(/attach evidence/i).setInputFiles(path.join(FIXTURES, 'sample.jpg'))
  await page.getByRole('button', { name: /submit report/i }).click()
  await expect(page.getByText(/saved on this device/i)).toBeVisible()

  await context.setOffline(false)
  // The app's own background sync (Dexie-queued retry, see OFFLINE_REPORTING_PRODUCTION.md)
  // should pick the draft up without any further user action.
  await page.goto('/dashboard')
  await expect(page.getByText('E2E reconnection report')).toBeVisible()
  await expect(page.getByText(/pending|uploading/i)).toHaveCount(0, { timeout: 20_000 })
})

test('#8/#9 concurrent double-submit while offline does not create duplicate reports once synced', async ({ page, context }) => {
  await context.setOffline(true)
  await fillReportBasics(page, 'E2E idempotency report')
  const submit = page.getByRole('button', { name: /submit report/i })
  await Promise.all([submit.click(), submit.click()])
  await context.setOffline(false)
  await page.goto('/dashboard')
  await expect(page.getByText('E2E idempotency report')).toHaveCount(1)
})
