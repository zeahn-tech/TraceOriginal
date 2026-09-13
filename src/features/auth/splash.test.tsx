import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

// ---------------------------------------------------------------------------
// End-to-end (within jsdom) proof that the observeAuth fix actually resolves
// the reported symptom: with Firebase unconfigured, the real `useAppData`
// hook's `loading` flag must clear, and the real `Splash` screen must
// navigate away instead of sitting there forever.
//
// `services.ts` reads `import.meta.env.VITE_FIREBASE_*` in top-level module
// code, which only runs once per module instance — so this test must
// `vi.resetModules()` and dynamically `import()` AFTER stubbing the env to
// blank, or it would just observe the "configured" values `src/test/setup.ts`
// stubs for every other test file (a static top-level import would be
// hoisted and evaluated before this file's own beforeEach ever runs).
// ---------------------------------------------------------------------------
vi.mock('firebase/app', () => ({ initializeApp: () => ({}), getApps: () => [] }))
vi.mock('firebase/auth', () => ({
  getAuth: () => ({}), GoogleAuthProvider: class {}, EmailAuthProvider: { credential: () => ({}) },
  onAuthStateChanged: vi.fn(), signInWithEmailAndPassword: vi.fn(), createUserWithEmailAndPassword: vi.fn(),
  signInWithPopup: vi.fn(), sendPasswordResetEmail: vi.fn(), signOut: vi.fn(), updatePassword: vi.fn(),
  reauthenticateWithCredential: vi.fn(),
}))
vi.mock('firebase/firestore', () => ({
  initializeFirestore: () => ({}), persistentLocalCache: () => ({}), persistentMultipleTabManager: () => ({}),
  collection: () => ({}), doc: () => ({}), getDoc: vi.fn(), onSnapshot: vi.fn(() => () => {}), runTransaction: vi.fn(),
  serverTimestamp: () => ({}), setDoc: vi.fn(), addDoc: vi.fn(), query: vi.fn(), orderBy: vi.fn(), where: vi.fn(),
}))
vi.mock('firebase/storage', () => ({ getStorage: () => ({}), ref: vi.fn(), uploadBytesResumable: vi.fn(), getDownloadURL: vi.fn(), deleteObject: vi.fn() }))
vi.mock('firebase/functions', () => ({ getFunctions: () => ({}), httpsCallable: () => async () => ({ data: undefined }) }))

describe('[regression] Splash does not hang forever when Firebase is unconfigured', () => {
  beforeEach(() => {
    // Faithfully reproduces the reported deploy scenario: required
    // VITE_FIREBASE_* values missing/blank, exactly what happens when a
    // live deploy's repository variables were never configured (see
    // GITHUB_PAGES_PRODUCTION_GUIDE.md). This makes `firebaseReady` false
    // and `auth` undefined for real inside `useAppData`/`Splash`, so this
    // test exercises the actual failure path end-to-end rather than relying
    // on a mock to simulate it.
    vi.resetModules()
    vi.stubEnv('VITE_FIREBASE_API_KEY', '')
    vi.stubEnv('VITE_FIREBASE_PROJECT_ID', '')
    vi.stubEnv('VITE_FIREBASE_APP_ID', '')
  })

  it('useAppData.loading clears and Splash navigates to /welcome instead of sitting on the splash screen indefinitely', async () => {
    const { Splash } = await import('./Splash')
    const { useAppData } = await import('../../hooks/useAppData')

    function SplashHarness() {
      const { user, loading } = useAppData()
      return (
        <Routes>
          <Route path="/" element={<Splash user={user} loading={loading} />} />
          <Route path="/welcome" element={<div>WELCOME SCREEN — PUBLIC ROUTES REACHABLE</div>} />
        </Routes>
      )
    }

    render(
      <MemoryRouter initialEntries={['/']}>
        <SplashHarness />
      </MemoryRouter>,
    )
    expect(screen.getByText(/securing communities together/i)).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText(/welcome screen/i)).toBeInTheDocument(), { timeout: 6000 })
  }, 8000)
})
