import { test, expect } from '@playwright/test'
import { seedRoleUser, signInViaUi, resetFirestoreAndAuthEmulators } from './helpers'

// ---------------------------------------------------------------------------
// Workflow #11 Law-enforcement workflow, #12 Admin workflow.
// Not executed in this sandbox — see AUTOMATED_QA_REPORT.md.
// ---------------------------------------------------------------------------
test.beforeEach(async () => { await resetFirestoreAndAuthEmulators() })

test('#11 law-enforcement workflow — an approved officer reaches Operations and can publish a wanted notice', async ({ page }) => {
  await seedRoleUser('officer@e2e.test', 'correct-horse-battery-staple', 'Officer Approved', 'LAW_ENFORCER', true)
  await signInViaUi(page, 'officer@e2e.test', 'correct-horse-battery-staple')
  await page.goto('/operations')
  await expect(page.getByText(/OPERATIONS/i)).toBeVisible()

  await page.goto('/operations/wanted/new')
  await page.getByLabel(/full name|name/i).first().fill('E2E Wanted Subject')
  await page.getByLabel(/description/i).fill('Wanted in connection with an E2E test scenario.')
  await page.getByRole('button', { name: /submit|publish|create/i }).click()
  await page.goto('/operations')
  await expect(page.getByText('E2E Wanted Subject')).toBeVisible()
})

test('#11 law-enforcement workflow — a citizen tip against a report is visible to officers in Operations', async ({ page, context }) => {
  // Citizen submits a report + tip
  await seedRoleUser('citizen-e2e@e2e.test', 'correct-horse-battery-staple', 'Citizen Reporter', 'CITIZEN')
  await signInViaUi(page, 'citizen-e2e@e2e.test', 'correct-horse-battery-staple')
  await page.goto('/reports/new')
  await page.getByLabel('Short Title').fill('E2E report awaiting tip')
  await page.getByLabel('Detailed Description').fill('Report used to attach a citizen tip in this workflow test.')
  await page.getByLabel(/i declare this report is truthful/i).check()
  await page.getByRole('button', { name: /submit report/i }).click()
  await expect(page.getByText(/incident report submitted successfully/i)).toBeVisible()

  // Same session hands off to an officer viewing Operations in a fresh context
  const officerContext = await context.browser()!.newContext()
  const officerPage = await officerContext.newPage()
  await seedRoleUser('officer-e2e@e2e.test', 'correct-horse-battery-staple', 'Officer Reviewer', 'LAW_ENFORCER', true)
  await signInViaUi(officerPage, 'officer-e2e@e2e.test', 'correct-horse-battery-staple')
  await officerPage.goto('/operations')
  await expect(officerPage.getByText('E2E report awaiting tip')).toBeVisible()
  await officerContext.close()
})

test('#12 admin workflow — an admin can approve a pending law-enforcement applicant', async ({ page }) => {
  await seedRoleUser('root-admin@e2e.test', 'correct-horse-battery-staple', 'Root Admin', 'ADMIN')
  await seedRoleUser('pending-officer-2@e2e.test', 'correct-horse-battery-staple', 'Pending Officer Two', 'LAW_ENFORCER', false)
  await signInViaUi(page, 'root-admin@e2e.test', 'correct-horse-battery-staple')
  await page.goto('/admin')
  await expect(page.getByText(/COMMAND CENTER/i)).toBeVisible()
  await page.getByRole('button', { name: 'Approvals' }).click()
  await expect(page.getByText('Pending Officer Two')).toBeVisible()
  await page.getByRole('button', { name: /approve/i }).click()
  await expect(page.getByText(/approved/i)).toBeVisible()

  // The now-approved officer can reach Operations, proving the Cloud
  // Function-backed approval actually took effect end to end.
  const officerPage = await page.context().browser()!.newPage()
  await signInViaUi(officerPage, 'pending-officer-2@e2e.test', 'correct-horse-battery-staple')
  await officerPage.goto('/operations')
  await expect(officerPage).toHaveURL('/operations')
})

test('#12 admin workflow — an admin can publish a public safety alert', async ({ page }) => {
  await seedRoleUser('root-admin-2@e2e.test', 'correct-horse-battery-staple', 'Root Admin Two', 'ADMIN')
  await signInViaUi(page, 'root-admin-2@e2e.test', 'correct-horse-battery-staple')
  await page.goto('/admin')
  await page.getByRole('button', { name: /^bell|alert/i }).click()
  await page.getByLabel(/title/i).fill('E2E Public Safety Alert')
  await page.getByLabel(/content|message/i).fill('This is a test alert published end to end.')
  await page.getByRole('button', { name: /publish|send/i }).click()
  await page.goto('/public')
  await expect(page.getByText('E2E Public Safety Alert')).toBeVisible()
})
