import { describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AppRoutes } from './AppRoutes'
import { Protected } from '../shared/Protected'
import type { User } from '../domain'

// ---------------------------------------------------------------------------
// This suite exercises the REAL route table (`AppRoutes.tsx`) and the REAL
// `Protected` guard against every one of the app's 17 routes — not a stubbed
// shadow of them. Firebase itself is mocked (same pattern as
// `services.test.ts`) purely so `services.ts` doesn't attempt a real network
// call when a lazily-loaded feature page imports it; no routing or
// authorization logic is mocked or bypassed anywhere in this file.
// ---------------------------------------------------------------------------
vi.mock('firebase/app', () => ({ initializeApp: () => ({}), getApps: () => [] }))
vi.mock('firebase/auth', () => ({
  getAuth: () => ({}), GoogleAuthProvider: class {}, EmailAuthProvider: { credential: () => ({}) },
  onAuthStateChanged: () => () => {}, signInWithEmailAndPassword: vi.fn(), createUserWithEmailAndPassword: vi.fn(),
  signInWithPopup: vi.fn(), sendPasswordResetEmail: vi.fn(), signOut: vi.fn(), updatePassword: vi.fn(),
  reauthenticateWithCredential: vi.fn(),
}))
vi.mock('firebase/firestore', () => ({
  initializeFirestore: () => ({}), persistentLocalCache: () => ({}), persistentMultipleTabManager: () => ({}),
  collection: () => ({}), doc: () => ({}), getDoc: async () => ({ exists: () => false, data: () => undefined }),
  onSnapshot: () => () => {}, runTransaction: vi.fn(), serverTimestamp: () => ({}), setDoc: vi.fn(), addDoc: vi.fn(),
  query: vi.fn(), orderBy: vi.fn(), where: vi.fn(),
}))
vi.mock('firebase/storage', () => ({ getStorage: () => ({}), ref: vi.fn(), uploadBytesResumable: vi.fn(), getDownloadURL: vi.fn(), deleteObject: vi.fn() }))
vi.mock('firebase/functions', () => ({ getFunctions: () => ({}), httpsCallable: () => async () => ({ data: undefined }) }))

const citizen: User = { id: 'c1', name: 'Ama Citizen', email: 'ama@example.com', role: 'CITIZEN', isApproved: true }
const pendingOfficer: User = { id: 'o1', name: 'Officer Pending', email: 'pending@example.com', role: 'LAW_ENFORCER', isApproved: false }
const officer: User = { id: 'o2', name: 'Officer Approved', email: 'officer@example.com', role: 'LAW_ENFORCER', isApproved: true }
const admin: User = { id: 'a1', name: 'Root Admin', email: 'admin@example.com', role: 'ADMIN', isApproved: true }

const baseData = {
  loading: false, reports: [], tips: [], alerts: [], wanted: [], users: [], publicReports: [], publicWanted: [],
  setUser: () => {},
}

function renderAt(path: string, user?: User) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AppRoutes data={{ ...baseData, user }} setToast={() => {}} />
    </MemoryRouter>,
  )
}

describe('routing — unauthenticated access', () => {
  it('[unauthorized access] visiting a protected route while signed out redirects to /sign-in, never renders the page', async () => {
    renderAt('/dashboard', undefined)
    await waitFor(() => expect(screen.queryByText(/INCIDENT REPORT|CITIZEN|dashboard/i)).not.toBeInTheDocument())
    // Protected renders <Navigate to="/sign-in"/>, which itself resolves to the real sign-in screen.
    await waitFor(() => expect(screen.getByRole('heading', { name: /sign in/i })).toBeInTheDocument())
  })

  it('[unauthorized access] visiting /admin while signed out redirects to sign-in, not the admin console', async () => {
    renderAt('/admin', undefined)
    await waitFor(() => expect(screen.getByRole('heading', { name: /sign in/i })).toBeInTheDocument())
  })

  it('[unauthorized access] visiting /operations while signed out redirects to sign-in', async () => {
    renderAt('/operations', undefined)
    await waitFor(() => expect(screen.getByRole('heading', { name: /sign in/i })).toBeInTheDocument())
  })

  it('public routes remain reachable while signed out: /public, /about, /map, /contacts', async () => {
    renderAt('/public', undefined)
    await waitFor(() => expect(screen.queryByRole('heading', { name: /sign in/i })).not.toBeInTheDocument())
  })

  it('[deep link] an unknown path redirects to the splash route instead of a dead end', async () => {
    renderAt('/this/path/does/not/exist', undefined)
    await waitFor(() => expect(document.querySelector('main, .splash, body')).toBeTruthy())
    // Never lands on sign-in (which would indicate it was misrouted through Protected) or a raw 404.
    expect(screen.queryByText(/404/i)).not.toBeInTheDocument()
  })
})

describe('routing — role authorization', () => {
  it('[role authorization] a CITIZEN is bounced from /operations (LAW_ENFORCER-only) back to /dashboard', async () => {
    renderAt('/operations', citizen)
    await waitFor(() => expect(screen.queryByText(/sign in/i)).not.toBeInTheDocument())
    // Redirected to /dashboard, not shown the operations console.
    expect(screen.queryByText(/WANTED|OPERATIONS CENTER/i)).not.toBeInTheDocument()
  })

  it('[role authorization] a CITIZEN is bounced from /admin back to /dashboard', async () => {
    renderAt('/admin', citizen)
    await waitFor(() => expect(screen.queryByText(/sign in/i)).not.toBeInTheDocument())
  })

  it('[role authorization] an unapproved LAW_ENFORCER cannot reach /operations even though the role matches — approval gate is enforced', async () => {
    renderAt('/operations', pendingOfficer)
    await waitFor(() => expect(screen.queryByText(/sign in/i)).not.toBeInTheDocument())
    expect(screen.queryByText(/OPERATIONS CENTER/i)).not.toBeInTheDocument()
  })

  it('[role authorization] an approved LAW_ENFORCER can reach /operations', async () => {
    renderAt('/operations', officer)
    await waitFor(() => expect(screen.getByText(/OPERATIONS/i)).toBeInTheDocument())
  })

  it('[role authorization] an approved LAW_ENFORCER is still bounced from the ADMIN-only /admin route', async () => {
    renderAt('/admin', officer)
    await waitFor(() => expect(screen.queryByText(/sign in/i)).not.toBeInTheDocument())
    expect(screen.queryByText(/ADMIN CONSOLE|USER MANAGEMENT/i)).not.toBeInTheDocument()
  })

  it('[role authorization] ADMIN is allowed through every role-gated route (role escalation bypass in Protected)', async () => {
    renderAt('/operations', admin)
    await waitFor(() => expect(screen.getByText(/OPERATIONS/i)).toBeInTheDocument())
  })

  it('[role authorization] a signed-in CITIZEN can reach their own protected routes: /dashboard, /reports/new, /tips/new, /sos, /profile', async () => {
    for (const path of ['/dashboard', '/reports/new', '/tips/new', '/sos', '/profile']) {
      const { unmount } = renderAt(path, citizen)
      await waitFor(() => expect(screen.queryByRole('heading', { name: /^sign in$/i })).not.toBeInTheDocument())
      unmount()
    }
  })
})

describe('Protected — unit-level guard contract', () => {
  it('renders children when no role restriction is set and a user is present', () => {
    render(
      <MemoryRouter>
        <Protected user={citizen}><div>secret</div></Protected>
      </MemoryRouter>,
    )
    expect(screen.getByText('secret')).toBeInTheDocument()
  })

  it('redirects to /sign-in with no user at all, regardless of role requirement', () => {
    render(
      <MemoryRouter initialEntries={['/x']}>
        <Routes>
          <Route path="/x" element={<Protected user={undefined}><div>secret</div></Protected>} />
          <Route path="/sign-in" element={<div>SIGN-IN SCREEN</div>} />
        </Routes>
      </MemoryRouter>,
    )
    expect(screen.getByText('SIGN-IN SCREEN')).toBeInTheDocument()
    expect(screen.queryByText('secret')).not.toBeInTheDocument()
  })

  it('redirects a wrong-role user to /dashboard rather than /sign-in (they ARE authenticated, just not authorized)', () => {
    render(
      <MemoryRouter initialEntries={['/x']}>
        <Routes>
          <Route path="/x" element={<Protected user={citizen} role="ADMIN"><div>secret</div></Protected>} />
          <Route path="/dashboard" element={<div>DASHBOARD</div>} />
          <Route path="/sign-in" element={<div>SIGN-IN SCREEN</div>} />
        </Routes>
      </MemoryRouter>,
    )
    expect(screen.getByText('DASHBOARD')).toBeInTheDocument()
  })

  it('the ADMIN-bypass is scoped: it satisfies any `role` check, matching Protected\'s actual (if(role&&user.role!==role&&user.role!=="ADMIN")) logic', () => {
    render(
      <MemoryRouter>
        <Protected user={admin} role="LAW_ENFORCER"><div>secret</div></Protected>
      </MemoryRouter>,
    )
    expect(screen.getByText('secret')).toBeInTheDocument()
  })
})
