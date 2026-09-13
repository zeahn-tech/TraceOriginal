import type { Page } from '@playwright/test'

// ---------------------------------------------------------------------------
// Seeding helpers against the local Firebase Emulator Suite (Auth REST +
// Firestore REST), used to put ADMIN and approved-LAW_ENFORCER accounts in
// place directly — the app's own sign-up flow can only ever create a
// CITIZEN or a pending (unapproved) LAW_ENFORCER by design (see
// SignUpPage.tsx: `role==='ADMIN'?'CITIZEN':role`), so higher-privilege
// fixtures have to be written directly, exactly the way a real deployment's
// first admin would be (a one-time manual Firestore edit — see
// FIREBASE_FUNCTIONS_ARCHITECTURE.md's note on there being no
// "self-promote to admin" Cloud Function by design).
// ---------------------------------------------------------------------------
const PROJECT_ID = 'tracenet-e2e'
const AUTH_BASE = 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1'
const FIRESTORE_BASE = `http://127.0.0.1:8080/v1/projects/${PROJECT_ID}/databases/(default)/documents`

export type SeedRole = 'CITIZEN' | 'LAW_ENFORCER' | 'ADMIN'

/** Creates (or reuses) an emulator Auth user and returns its uid + idToken. */
export async function createAuthUser(email: string, password: string) {
  const res = await fetch(`${AUTH_BASE}/accounts:signUp?key=demo-api-key`, {
    method: 'POST',
    body: JSON.stringify({ email, password, returnSecureToken: true }),
  })
  const body = await res.json()
  if (!res.ok && body.error?.message !== 'EMAIL_EXISTS') throw new Error(`Auth seed failed: ${JSON.stringify(body)}`)
  if (body.error?.message === 'EMAIL_EXISTS') {
    const signIn = await fetch(`${AUTH_BASE}/accounts:signInWithPassword?key=demo-api-key`, {
      method: 'POST', body: JSON.stringify({ email, password, returnSecureToken: true }),
    })
    return (await signIn.json()) as { localId: string; idToken: string }
  }
  return body as { localId: string; idToken: string }
}

/** Directly writes a `users/{uid}` Firestore document via the emulator's REST API, bypassing security rules (emulator REST calls with no Firebase Auth header are treated as admin-equivalent for seeding). */
export async function seedUserProfile(uid: string, fields: { name: string; email: string; role: SeedRole; isApproved: boolean }) {
  const res = await fetch(`${FIRESTORE_BASE}/users/${uid}`, {
    method: 'PATCH',
    body: JSON.stringify({
      fields: {
        id: { stringValue: uid }, name: { stringValue: fields.name }, email: { stringValue: fields.email },
        role: { stringValue: fields.role }, isApproved: { booleanValue: fields.isApproved },
      },
    }),
  })
  if (!res.ok) throw new Error(`Firestore seed failed: ${await res.text()}`)
}

export async function seedRoleUser(email: string, password: string, name: string, role: SeedRole, isApproved = true) {
  const { localId } = await createAuthUser(email, password)
  await seedUserProfile(localId, { name, email, role, isApproved })
  return localId
}

/** Signs in through the real UI (not a token shortcut) — this is itself part of what several specs are verifying. */
export async function signInViaUi(page: Page, email: string, password: string) {
  await page.goto('/sign-in')
  await page.getByLabel('Email Address').fill(email)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByRole('button', { name: /^sign in$/i }).click()
}

/** Reads the most recent password-reset OOB link the Auth emulator captured for an email, instead of a real inbox. */
export async function getLatestResetLink(email: string): Promise<string> {
  const res = await fetch(`http://127.0.0.1:9099/emulator/v1/projects/${PROJECT_ID}/oobCodes`)
  const { oobCodes } = (await res.json()) as { oobCodes: { email: string; oobLink: string; requestType: string }[] }
  const match = [...oobCodes].reverse().find(c => c.email === email && c.requestType === 'PASSWORD_RESET')
  if (!match) throw new Error(`No PASSWORD_RESET oob code found for ${email}`)
  return match.oobLink
}

export async function resetFirestoreAndAuthEmulators() {
  await fetch(`http://127.0.0.1:8080/emulator/v1/projects/${PROJECT_ID}/databases/(default)/documents`, { method: 'DELETE' })
  await fetch(`http://127.0.0.1:9099/emulator/v1/projects/${PROJECT_ID}/accounts`, { method: 'DELETE' })
}
