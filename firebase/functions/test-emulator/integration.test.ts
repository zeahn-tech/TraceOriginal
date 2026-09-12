/**
 * Emulator-backed integration test.
 *
 * UNLIKE the 54 tests in firebase/functions/test/, these exercise the real
 * HTTPS callable wiring end-to-end: a real Firebase Auth emulator issues a
 * real ID token, a real Functions emulator verifies it, and a real
 * Firestore emulator backs the reads/writes — not a hand-constructed
 * CallableRequest against mocked Firestore.
 *
 * This is complementary, not redundant: the offline suite proves the
 * business logic in every function is correct; this suite proves the
 * plumbing connecting a real signed-in client to that logic actually works
 * (auth token verification, CORS, the callable protocol itself).
 *
 * Run via (see FIREBASE_FUNCTIONS_ARCHITECTURE.md for full context on why
 * this could not be executed in the environment that authored it):
 *
 *   firebase emulators:exec --project tracenet-emulator-test \
 *     --only auth,firestore,functions \
 *     "vitest run --config firebase/functions/test-emulator/vitest.config.ts"
 */
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { initializeApp } from 'firebase/app';
import { connectAuthEmulator, createUserWithEmailAndPassword, getAuth, signInWithEmailAndPassword, signOut } from 'firebase/auth';
import { connectFirestoreEmulator, getFirestore } from 'firebase/firestore';
import { connectFunctionsEmulator, getFunctions, httpsCallable } from 'firebase/functions';
import * as admin from 'firebase-admin';

const PROJECT_ID = 'tracenet-emulator-test';

const app = initializeApp({ projectId: PROJECT_ID, apiKey: 'fake-api-key' });
const auth = getAuth(app);
const db = getFirestore(app);
const functions = getFunctions(app);
connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
connectFirestoreEmulator(db, '127.0.0.1', 8080);
connectFunctionsEmulator(functions, '127.0.0.1', 5001);

// Admin SDK, used only to seed users/* documents directly (bypassing rules,
// exactly as the real Cloud Functions runtime does) — mirrors how
// firestore.rules.test.ts seeds via withSecurityRulesDisabled.
process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080';
const adminApp = admin.initializeApp({ projectId: PROJECT_ID }, 'integration-test-admin');
const adminDb = adminApp.firestore();

async function createSignedInUser(uid: string, profile: Record<string, unknown>) {
  const email = `${uid}@example.test`;
  const password = 'Passw0rd!';
  await createUserWithEmailAndPassword(auth, email, password).catch(() => {}); // idempotent across reruns
  await adminDb.doc(`users/${uid}`).set({ id: uid, ...profile });
  const credential = await signInWithEmailAndPassword(auth, email, password);
  return credential.user.uid;
}

beforeEach(async () => {
  await signOut(auth).catch(() => {});
});

afterAll(async () => {
  await adminApp.delete();
});

describe('Cloud Functions — emulator integration smoke test', () => {
  it('rejects an unauthenticated call to every privileged function', async () => {
    await signOut(auth);
    const calls = ['approveOfficer', 'setUserRole', 'setUserStatus', 'verifyWantedNotice', 'publishAlert', 'transitionReportStatus'];
    for (const name of calls) {
      const fn = httpsCallable(functions, name);
      await expect(fn({ requestId: 'a-valid-request-id' })).rejects.toMatchObject({ code: 'functions/unauthenticated' });
    }
  });

  it('end-to-end: a real signed-in ADMIN can approve a real signed-up officer via the live callable endpoint', async () => {
    const adminUid = `admin-${Date.now()}`;
    const officerUid = `officer-${Date.now()}`;
    await adminDb.doc(`users/${officerUid}`).set({ id: officerUid, role: 'LAW_ENFORCER', isApproved: false, status: 'PENDING' });
    await createSignedInUser(adminUid, { role: 'ADMIN', isApproved: true });

    const approveOfficer = httpsCallable(functions, 'approveOfficer');
    const result = await approveOfficer({ uid: officerUid, requestId: `req-${Date.now()}` });
    expect(result.data).toMatchObject({ ok: true });

    const updated = await adminDb.doc(`users/${officerUid}`).get();
    expect(updated.data()).toMatchObject({ isApproved: true, status: 'ACTIVE' });
  });

  it('end-to-end: a real signed-in CITIZEN is rejected by the live endpoint, not just the unit-test mock', async () => {
    const citizenUid = `citizen-${Date.now()}`;
    const officerUid = `officer-${Date.now()}`;
    await adminDb.doc(`users/${officerUid}`).set({ id: officerUid, role: 'LAW_ENFORCER', isApproved: false });
    await createSignedInUser(citizenUid, { role: 'CITIZEN', isApproved: true });

    const approveOfficer = httpsCallable(functions, 'approveOfficer');
    await expect(approveOfficer({ uid: officerUid, requestId: `req-${Date.now()}` })).rejects.toMatchObject({ code: 'functions/permission-denied' });
  });
});
