import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// ---------------------------------------------------------------------------
// Regression test for a real production bug: when Firebase isn't configured
// (missing VITE_FIREBASE_* — e.g. a live deploy whose repository
// variables/secrets were never set, see GITHUB_PAGES_PRODUCTION_GUIDE.md) or
// otherwise fails to initialize, `auth` is `undefined`. `observeAuth` used to
// silently return a no-op unsubscribe in that case, so its callback NEVER
// fired. `useAppData.ts` only clears `loading` inside that callback, and
// `Splash.tsx` only navigates away from the splash screen once `loading` is
// false — so the entire app was permanently stuck on the splash screen with
// zero error shown, even though every public route (Welcome, Public Portal,
// Sign In, About) needs no signed-in user at all. This file proves the fix:
// observeAuth now always resolves its callback (with `null`, i.e. "treat as
// signed out") instead of hanging forever.
// ---------------------------------------------------------------------------
vi.mock('firebase/app', () => ({ initializeApp: () => ({}), getApps: () => [] }))
vi.mock('firebase/auth', () => ({
  getAuth: () => ({}), GoogleAuthProvider: class {}, EmailAuthProvider: { credential: () => ({}) },
  onAuthStateChanged: vi.fn(), signInWithEmailAndPassword: vi.fn(), createUserWithEmailAndPassword: vi.fn(),
  signInWithPopup: vi.fn(), sendPasswordResetEmail: vi.fn(), signOut: vi.fn(), updatePassword: vi.fn(),
  reauthenticateWithCredential: vi.fn(), connectAuthEmulator: vi.fn(),
}))
vi.mock('firebase/firestore', () => ({
  initializeFirestore: () => ({}), persistentLocalCache: () => ({}), persistentMultipleTabManager: () => ({}),
  collection: () => ({}), doc: () => ({}), getDoc: vi.fn(), onSnapshot: vi.fn(() => () => {}), runTransaction: vi.fn(),
  serverTimestamp: () => ({}), setDoc: vi.fn(), addDoc: vi.fn(), query: vi.fn(), orderBy: vi.fn(), where: vi.fn(),
  connectFirestoreEmulator: vi.fn(),
}))
vi.mock('firebase/storage', () => ({ getStorage: () => ({}), ref: vi.fn(), uploadBytesResumable: vi.fn(), getDownloadURL: vi.fn(), deleteObject: vi.fn(), connectStorageEmulator: vi.fn() }))
vi.mock('firebase/functions', () => ({ getFunctions: () => ({}), httpsCallable: () => async () => ({ data: undefined }) }))

afterEach(() => { vi.unstubAllEnvs(); vi.resetModules() })

describe('observeAuth — Firebase not configured', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.stubEnv('VITE_FIREBASE_API_KEY', '')
    vi.stubEnv('VITE_FIREBASE_PROJECT_ID', '')
    vi.stubEnv('VITE_FIREBASE_APP_ID', '')
  })

  it('firebaseReady is false when required config is missing (sanity check for the scenario)', async () => {
    const { firebaseReady } = await import('./services')
    expect(firebaseReady).toBe(false)
  })

  it('[regression] resolves the callback with null instead of hanging forever, so `loading` can clear and the app can reach /welcome', async () => {
    const { observeAuth } = await import('./services')
    const results: unknown[] = []
    const unsubscribe = observeAuth(u => results.push(u))
    await vi.waitFor(() => expect(results).toEqual([null]))
    unsubscribe()
  })

  it('[regression] the returned unsubscribe function is safe to call and does not throw', async () => {
    const { observeAuth } = await import('./services')
    const unsubscribe = observeAuth(() => {})
    expect(() => unsubscribe()).not.toThrow()
  })
})

describe('observeAuth — Firebase configured (unaffected by the fix)', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.stubEnv('VITE_FIREBASE_API_KEY', 'test-api-key')
    vi.stubEnv('VITE_FIREBASE_PROJECT_ID', 'test-project')
    vi.stubEnv('VITE_FIREBASE_APP_ID', '1:test:web:test')
  })

  it('delegates to the real onAuthStateChanged when Firebase IS configured', async () => {
    const { onAuthStateChanged } = await import('firebase/auth')
    const fakeUnsubscribe = vi.fn()
    vi.mocked(onAuthStateChanged).mockReturnValue(fakeUnsubscribe)
    const { observeAuth } = await import('./services')
    const callback = vi.fn()
    const returned = observeAuth(callback)
    expect(onAuthStateChanged).toHaveBeenCalledWith(expect.anything(), callback)
    expect(returned).toBe(fakeUnsubscribe)
  })
})
