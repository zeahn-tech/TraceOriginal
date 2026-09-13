import { test, expect } from '@playwright/test'
import { createAuthUser, getLatestResetLink, seedRoleUser, signInViaUi, resetFirestoreAndAuthEmulators } from './helpers'

// ---------------------------------------------------------------------------
// Workflow #1 Sign up, #2 Sign in, #3 Password reset, #16 Unauthorized access
// See playwright.config.ts / AUTOMATED_QA_REPORT.md for why this file is
// written+type-checked but not executed in this sandbox.
// ---------------------------------------------------------------------------
test.beforeEach(async () => { await resetFirestoreAndAuthEmulators() })

test('#1 sign up — a new citizen can register and lands signed in on the dashboard', async ({ page }) => {
  await page.goto('/sign-up')
  await page.getByLabel('Full Name').fill('Ama Citizen')
  await page.getByLabel('Email Address').fill('ama@e2e.test')
  await page.getByLabel('Password', { exact: true }).fill('correct-horse-battery-staple')
  await page.getByRole('button', { name: /^sign up$/i }).click()
  await expect(page).toHaveURL('/')
  await expect(page.getByText(/account created successfully/i)).toBeVisible()
})

test('#1 sign up — a law-enforcement applicant without ID/badge/contact is blocked client-side, never reaches Firebase', async ({ page }) => {
  await page.goto('/sign-up')
  await page.getByLabel('Full Name').fill('Officer Incomplete')
  await page.getByLabel('Email Address').fill('officer-incomplete@e2e.test')
  await page.getByLabel('Password', { exact: true }).fill('correct-horse-battery-staple')
  await page.getByLabel('Access Level').selectOption('LAW_ENFORCER')
  await page.getByRole('button', { name: /^sign up$/i }).click()
  await expect(page.getByText(/complete all required registration details/i)).toBeVisible()
  await expect(page).toHaveURL('/sign-up')
})

test('#2 sign in — a seeded citizen can sign in and reach the dashboard', async ({ page }) => {
  await seedRoleUser('citizen@e2e.test', 'correct-horse-battery-staple', 'Seeded Citizen', 'CITIZEN')
  await signInViaUi(page, 'citizen@e2e.test', 'correct-horse-battery-staple')
  await expect(page).toHaveURL('/')
  await expect(page.getByText(/signed in successfully/i)).toBeVisible()
})

test('#2 sign in — wrong password shows an error and does not sign in', async ({ page }) => {
  await createAuthUser('wrongpass@e2e.test', 'the-real-password')
  await signInViaUi(page, 'wrongpass@e2e.test', 'not-the-real-password')
  await expect(page.getByText(/sign in failed/i)).toBeVisible()
  await expect(page).toHaveURL('/sign-in')
})

test('#3 password reset — request flow sends a reset email via the Auth emulator', async ({ page }) => {
  await createAuthUser('forgot@e2e.test', 'original-password')
  await page.goto('/sign-in')
  await page.getByLabel('Email Address').fill('forgot@e2e.test')
  await page.getByRole('button', { name: /forgot password/i }).click()
  await page.getByRole('button', { name: /send reset link/i }).click()
  await expect(page.getByText(/password reset link sent/i)).toBeVisible()
  const link = await getLatestResetLink('forgot@e2e.test')
  expect(link).toContain('mode=resetPassword')
})

test('#16 unauthorized access — a signed-out visitor hitting /admin directly is redirected to sign-in, never sees the console', async ({ page }) => {
  await page.goto('/admin')
  await expect(page).toHaveURL('/sign-in')
  await expect(page.getByRole('heading', { name: /admin/i })).toHaveCount(0)
})

test('#16 unauthorized access — a signed-in CITIZEN hitting /admin directly is bounced to /dashboard, not shown the console', async ({ page }) => {
  await seedRoleUser('nosyclerk@e2e.test', 'correct-horse-battery-staple', 'Nosy Clerk', 'CITIZEN')
  await signInViaUi(page, 'nosyclerk@e2e.test', 'correct-horse-battery-staple')
  await page.goto('/admin')
  await expect(page).toHaveURL('/dashboard')
  await expect(page.getByText(/user management|verification queue/i)).toHaveCount(0)
})

test('#16 unauthorized access — an unapproved LAW_ENFORCER hitting /operations is bounced despite matching the role', async ({ page }) => {
  await seedRoleUser('pending-officer@e2e.test', 'correct-horse-battery-staple', 'Pending Officer', 'LAW_ENFORCER', false)
  await signInViaUi(page, 'pending-officer@e2e.test', 'correct-horse-battery-staple')
  await page.goto('/operations')
  await expect(page).toHaveURL('/dashboard')
})
