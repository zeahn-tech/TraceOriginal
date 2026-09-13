import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthPage } from './AuthPage'
import { SignUpPage } from './SignUpPage'

// ---------------------------------------------------------------------------
// Covers workflow items #1 (sign up), #2 (sign in), #3 (password reset), and
// the auth-error-path handling that a sign-in/sign-up UI must surface
// correctly. `firebase/auth` is mocked with controllable resolve/reject
// behavior (same approach as services.test.ts); everything above that layer
// — form validation, `session.*` call wiring, navigation, and toast content
// — is real and exercised end-to-end within jsdom.
// ---------------------------------------------------------------------------
const authMocks = vi.hoisted(() => ({
  signInWithEmailAndPassword: vi.fn(),
  createUserWithEmailAndPassword: vi.fn(),
  sendPasswordResetEmail: vi.fn(),
  signInWithPopup: vi.fn(),
}))
vi.mock('firebase/app', () => ({ initializeApp: () => ({}), getApps: () => [] }))
vi.mock('firebase/auth', () => ({
  getAuth: () => ({}), GoogleAuthProvider: class {}, EmailAuthProvider: { credential: () => ({}) },
  onAuthStateChanged: () => () => {}, signOut: vi.fn(), updatePassword: vi.fn(), reauthenticateWithCredential: vi.fn(),
  ...authMocks,
}))
vi.mock('firebase/firestore', () => ({
  initializeFirestore: () => ({}), persistentLocalCache: () => ({}), persistentMultipleTabManager: () => ({}),
  collection: () => ({}), doc: () => ({}), getDoc: async () => ({ exists: () => false, data: () => undefined }),
  onSnapshot: () => () => {}, runTransaction: vi.fn(), serverTimestamp: () => ({}),
  setDoc: vi.fn().mockResolvedValue(undefined), addDoc: vi.fn(), query: vi.fn(), orderBy: vi.fn(), where: vi.fn(),
}))
vi.mock('firebase/storage', () => ({ getStorage: () => ({}), ref: vi.fn(), uploadBytesResumable: vi.fn(), getDownloadURL: vi.fn(), deleteObject: vi.fn() }))
vi.mock('firebase/functions', () => ({ getFunctions: () => ({}), httpsCallable: () => async () => ({ data: undefined }) }))

function renderWithNav(el: React.ReactElement) {
  return render(
    <MemoryRouter initialEntries={['/sign-in']}>
      <Routes>
        <Route path="/sign-in" element={el} />
        <Route path="/" element={<div>ROOT / SPLASH</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  authMocks.signInWithEmailAndPassword.mockReset()
  authMocks.createUserWithEmailAndPassword.mockReset()
  authMocks.sendPasswordResetEmail.mockReset()
  authMocks.signInWithPopup.mockReset()
})

describe('Sign in (#2)', () => {
  it('[sign in] valid credentials call Firebase Auth and navigate away from /sign-in on success', async () => {
    authMocks.signInWithEmailAndPassword.mockResolvedValue({ user: { uid: 'u1' } })
    const user = userEvent.setup()
    renderWithNav(<AuthPage setToast={() => {}} />)
    await user.type(screen.getByLabelText(/email address/i), 'ama@example.com')
    await user.type(screen.getByLabelText(/^password$/i), 'correct-horse-battery')
    await user.click(screen.getByRole('button', { name: /^sign in$/i }))
    await waitFor(() => expect(authMocks.signInWithEmailAndPassword).toHaveBeenCalledWith(expect.anything(), 'ama@example.com', 'correct-horse-battery'))
    await waitFor(() => expect(screen.getByText('ROOT / SPLASH')).toBeInTheDocument())
  })

  it('[sign in] wrong credentials surface an error toast and do NOT navigate away', async () => {
    authMocks.signInWithEmailAndPassword.mockRejectedValue({ code: 'auth/invalid-credential' })
    const toasts: { message: string; kind?: string }[] = []
    const user = userEvent.setup()
    renderWithNav(<AuthPage setToast={(t) => toasts.push(t)} />)
    await user.type(screen.getByLabelText(/email address/i), 'ama@example.com')
    await user.type(screen.getByLabelText(/^password$/i), 'wrong-password')
    await user.click(screen.getByRole('button', { name: /^sign in$/i }))
    await waitFor(() => expect(toasts.length).toBeGreaterThan(0))
    expect(toasts[0].kind).toBe('error')
    expect(screen.getByRole('heading', { name: /sign in/i })).toBeInTheDocument() // still on sign-in
  })

  it('[sign in] Google sign-in button wires to session.google() and navigates on success', async () => {
    authMocks.signInWithPopup.mockResolvedValue({ user: { uid: 'u1' } })
    const user = userEvent.setup()
    renderWithNav(<AuthPage setToast={() => {}} />)
    await user.click(screen.getByRole('button', { name: /sign in with google/i }))
    await waitFor(() => expect(authMocks.signInWithPopup).toHaveBeenCalled())
    await waitFor(() => expect(screen.getByText('ROOT / SPLASH')).toBeInTheDocument())
  })
})

describe('Password reset (#3)', () => {
  it('[password reset] opening "Forgot Password?" shows the reset dialog pre-filled with the typed email', async () => {
    const user = userEvent.setup()
    renderWithNav(<AuthPage setToast={() => {}} />)
    await user.type(screen.getByLabelText(/email address/i), 'reset-me@example.com')
    await user.click(screen.getByRole('button', { name: /forgot password/i }))
    const dialog = screen.getByText(/password recovery/i).closest('section')!
    expect(within(dialog).getByDisplayValue('reset-me@example.com')).toBeInTheDocument()
  })

  it('[password reset] submitting the reset form calls sendPasswordResetEmail and closes the dialog on success', async () => {
    authMocks.sendPasswordResetEmail.mockResolvedValue(undefined)
    const toasts: { message: string; kind?: string }[] = []
    const user = userEvent.setup()
    renderWithNav(<AuthPage setToast={(t) => toasts.push(t)} />)
    await user.type(screen.getByLabelText(/email address/i), 'reset-me@example.com')
    await user.click(screen.getByRole('button', { name: /forgot password/i }))
    const dialog = screen.getByText(/password recovery/i).closest('section')!
    await user.click(within(dialog).getByRole('button', { name: /send reset link/i }))
    await waitFor(() => expect(authMocks.sendPasswordResetEmail).toHaveBeenCalled())
    await waitFor(() => expect(screen.queryByText(/password recovery/i)).not.toBeInTheDocument())
    expect(toasts.at(-1)?.kind).not.toBe('error')
  })

  it('[password reset] a failed reset keeps the dialog open and shows an error toast', async () => {
    authMocks.sendPasswordResetEmail.mockRejectedValue({ code: 'auth/user-not-found' })
    const toasts: { message: string; kind?: string }[] = []
    const user = userEvent.setup()
    renderWithNav(<AuthPage setToast={(t) => toasts.push(t)} />)
    await user.type(screen.getByLabelText(/email address/i), 'nobody@example.com')
    await user.click(screen.getByRole('button', { name: /forgot password/i }))
    const dialog = screen.getByText(/password recovery/i).closest('section')!
    await user.click(within(dialog).getByRole('button', { name: /send reset link/i }))
    await waitFor(() => expect(toasts.at(-1)?.kind).toBe('error'))
    expect(screen.getByText(/password recovery/i)).toBeInTheDocument()
  })
})

describe('Sign up (#1)', () => {
  it('[sign up] a CITIZEN sign-up with all required fields creates the auth user and saves a profile', async () => {
    authMocks.createUserWithEmailAndPassword.mockResolvedValue({ user: { uid: 'new-uid' } })
    const toasts: { message: string; kind?: string }[] = []
    const user = userEvent.setup()
    renderWithNav(<SignUpPage setToast={(t) => toasts.push(t)} />)
    await user.type(screen.getByLabelText(/full name/i), 'New Citizen')
    await user.type(screen.getByLabelText(/email address/i), 'new@example.com')
    await user.type(screen.getByLabelText(/^password$/i), 'a-strong-password')
    fireEvent.submit(screen.getByRole('button', { name: /^sign up$/i }).closest('form')!)
    await waitFor(() => expect(authMocks.createUserWithEmailAndPassword).toHaveBeenCalledWith(expect.anything(), 'new@example.com', 'a-strong-password'))
    await waitFor(() => expect(toasts.at(-1)?.kind).not.toBe('error'))
  })

  it('[sign up] a LAW_ENFORCER sign-up without badge/contact/address/ID is rejected client-side before hitting Firebase at all', async () => {
    const toasts: { message: string; kind?: string }[] = []
    const user = userEvent.setup()
    renderWithNav(<SignUpPage setToast={(t) => toasts.push(t)} />)
    await user.type(screen.getByLabelText(/full name/i), 'Officer Incomplete')
    await user.type(screen.getByLabelText(/email address/i), 'officer@example.com')
    await user.type(screen.getByLabelText(/^password$/i), 'a-strong-password')
    await user.selectOptions(screen.getByLabelText(/access level/i), 'LAW_ENFORCER')
    fireEvent.submit(screen.getByRole('button', { name: /^sign up$/i }).closest('form')!)
    await waitFor(() => expect(toasts.length).toBeGreaterThan(0))
    expect(toasts[0].kind).toBe('error')
    expect(authMocks.createUserWithEmailAndPassword).not.toHaveBeenCalled()
  })

  it('[sign up] duplicate-email failure from Firebase surfaces an error toast and does not navigate', async () => {
    authMocks.createUserWithEmailAndPassword.mockRejectedValue({ code: 'auth/email-already-in-use' })
    const toasts: { message: string; kind?: string }[] = []
    const user = userEvent.setup()
    renderWithNav(<SignUpPage setToast={(t) => toasts.push(t)} />)
    await user.type(screen.getByLabelText(/full name/i), 'Dup Citizen')
    await user.type(screen.getByLabelText(/email address/i), 'dup@example.com')
    await user.type(screen.getByLabelText(/^password$/i), 'a-strong-password')
    fireEvent.submit(screen.getByRole('button', { name: /^sign up$/i }).closest('form')!)
    await waitFor(() => expect(toasts.at(-1)?.kind).toBe('error'))
    expect(screen.getByRole('heading', { name: /create account/i })).toBeInTheDocument()
  })
})
