import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import functionsTest from 'firebase-functions-test';
import type { createFakeFirestore } from './mocks/firestore';

vi.mock('firebase-admin', async () => {
  const { createFakeFirestore } = await import('./mocks/firestore');
  const fake = createFakeFirestore();
  return { initializeApp: vi.fn(), firestore: fake.firestore, __fake: fake };
});

const test = functionsTest();

const ADMIN_UID = 'admin-1';
const ONLY_OTHER_ADMIN_UID = 'admin-2';
const OFFICER_UID = 'officer-1';
const CITIZEN_UID = 'citizen-1';

let fake: ReturnType<typeof createFakeFirestore>;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let setUserRole: (req: any) => Promise<unknown>;

beforeAll(async () => {
  const admin = (await import('firebase-admin')) as unknown as { __fake: ReturnType<typeof createFakeFirestore> };
  fake = admin.__fake;
  const mod = await import('../src/functions/setUserRole');
  setUserRole = test.wrap(mod.setUserRole);
});

beforeEach(() => {
  fake._reset();
  fake._seed(`users/${ADMIN_UID}`, { id: ADMIN_UID, role: 'ADMIN', isApproved: true });
  fake._seed(`users/${OFFICER_UID}`, { id: OFFICER_UID, role: 'LAW_ENFORCER', isApproved: true });
  fake._seed(`users/${CITIZEN_UID}`, { id: CITIZEN_UID, role: 'CITIZEN', isApproved: true });
});

describe('setUserRole', () => {
  it('[authorized] an ADMIN can promote a citizen to LAW_ENFORCER (starts unapproved)', async () => {
    const result = await setUserRole({ data: { uid: CITIZEN_UID, role: 'LAW_ENFORCER', requestId: 'req-role-1' }, auth: { uid: ADMIN_UID } });
    expect(result).toEqual({ ok: true });
    expect(fake._get(`users/${CITIZEN_UID}`)).toMatchObject({ role: 'LAW_ENFORCER', isApproved: false });
  });

  it('[authorized] an ADMIN can demote an officer to CITIZEN (auto-approved)', async () => {
    await setUserRole({ data: { uid: OFFICER_UID, role: 'CITIZEN', requestId: 'req-role-2' }, auth: { uid: ADMIN_UID } });
    expect(fake._get(`users/${OFFICER_UID}`)).toMatchObject({ role: 'CITIZEN', isApproved: true });
  });

  it('[unauthorized / privilege escalation] a CITIZEN cannot call setUserRole at all, including on themselves', async () => {
    await expect(
      setUserRole({ data: { uid: CITIZEN_UID, role: 'ADMIN', requestId: 'req-role-3' }, auth: { uid: CITIZEN_UID } }),
    ).rejects.toMatchObject({ code: 'permission-denied' });
    expect(fake._get(`users/${CITIZEN_UID}`)).toMatchObject({ role: 'CITIZEN' });
  });

  it('[unauthorized / privilege escalation] an approved OFFICER cannot call setUserRole', async () => {
    await expect(
      setUserRole({ data: { uid: CITIZEN_UID, role: 'ADMIN', requestId: 'req-role-4' }, auth: { uid: OFFICER_UID } }),
    ).rejects.toMatchObject({ code: 'permission-denied' });
  });

  it('[server-side validation] an invalid role string is rejected', async () => {
    await expect(
      setUserRole({ data: { uid: CITIZEN_UID, role: 'SUPERUSER', requestId: 'req-role-5' }, auth: { uid: ADMIN_UID } }),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('[server-side validation] cannot demote the last remaining ADMIN', async () => {
    await expect(
      setUserRole({ data: { uid: ADMIN_UID, role: 'CITIZEN', requestId: 'req-role-6' }, auth: { uid: ADMIN_UID } }),
    ).rejects.toMatchObject({ code: 'failed-precondition' });
    expect(fake._get(`users/${ADMIN_UID}`)).toMatchObject({ role: 'ADMIN' });
  });

  it('[server-side validation] CAN demote an admin when another admin still remains', async () => {
    fake._seed(`users/${ONLY_OTHER_ADMIN_UID}`, { id: ONLY_OTHER_ADMIN_UID, role: 'ADMIN', isApproved: true });
    await expect(
      setUserRole({ data: { uid: ADMIN_UID, role: 'CITIZEN', requestId: 'req-role-7' }, auth: { uid: ONLY_OTHER_ADMIN_UID } }),
    ).resolves.toEqual({ ok: true });
    expect(fake._get(`users/${ADMIN_UID}`)).toMatchObject({ role: 'CITIZEN' });
  });

  it('[idempotent] retrying with the same requestId does not re-run the command', async () => {
    const first = await setUserRole({ data: { uid: CITIZEN_UID, role: 'LAW_ENFORCER', requestId: 'req-role-idem' }, auth: { uid: ADMIN_UID } });
    const second = await setUserRole({ data: { uid: CITIZEN_UID, role: 'LAW_ENFORCER', requestId: 'req-role-idem' }, auth: { uid: ADMIN_UID } });
    expect(first).toEqual(second);
    expect(fake._addedDocs.filter((d) => d.collection === 'audit_logs')).toHaveLength(1);
  });

  it('[audit logging] role changes are logged with before/after role', async () => {
    await setUserRole({ data: { uid: CITIZEN_UID, role: 'LAW_ENFORCER', requestId: 'req-role-audit' }, auth: { uid: ADMIN_UID } });
    const [entry] = fake._addedDocs.filter((d) => d.collection === 'audit_logs');
    expect(entry.data).toMatchObject({
      action: 'setUserRole', actorUid: ADMIN_UID, targetId: CITIZEN_UID,
      before: { role: 'CITIZEN' }, after: { role: 'LAW_ENFORCER' }, result: 'success',
    });
  });
});
