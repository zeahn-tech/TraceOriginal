import { afterEach, vi } from 'vitest'
import { cleanup } from '@testing-library/react'
import 'fake-indexeddb/auto'
import '@testing-library/jest-dom/vitest'

// Component tests (routing.test.tsx, auth component tests) render into the
// jsdom document; without an explicit unmount between tests, later `render()`
// calls accumulate and duplicate-match queries like getByText/getByRole.
afterEach(() => cleanup())

// Fake but well-formed Firebase config so firebaseReady evaluates true and
// services.ts exercises its real "configured" code paths. No network calls
// are ever made with these — firebase/app, firebase/auth, firebase/firestore,
// and firebase/storage are mocked per test file where a real call would
// otherwise be attempted.
vi.stubEnv('VITE_FIREBASE_API_KEY', 'test-api-key')
vi.stubEnv('VITE_FIREBASE_AUTH_DOMAIN', 'test.firebaseapp.com')
vi.stubEnv('VITE_FIREBASE_PROJECT_ID', 'test-project')
vi.stubEnv('VITE_FIREBASE_STORAGE_BUCKET', 'test-project.appspot.com')
vi.stubEnv('VITE_FIREBASE_APP_ID', '1:test:web:test')
vi.stubEnv('VITE_FIREBASE_MESSAGING_SENDER_ID', '000000000000')
