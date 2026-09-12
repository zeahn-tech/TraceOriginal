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
const CITIZEN_UID = 'citizen-1';

let fake: ReturnType<typeof createFakeFirestore>;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let setUserStatus: (req: any) => Promise<unknown>;

beforeAll(async () => {
  const admin = (await import('firebase-admin')) as unknown as { __fake: ReturnType<typeof createFakeFirestore> };
  fake = admin.__fake;
  const mod = await import('../src/functions/setUserStatus');
  setUserStatus = test.wrap(mod.setUserStatus);
});

beforeEach(() => {
  fake._reset();
  fake._seed(`users/${ADMIN_UID}`, { id: ADMIN_UID, role: 'ADMIN', isApproved: true, status: 'ACTIVE' });
  fake._seed(`users/${CITIZEN_UID}`, { id: CITIZEN_UID, role: 'CITIZEN', isApproved: true, status: 'ACTIVE' });
});

describe('setUserStatus', () => {
  it('[authorized] an ADMIN can suspend a citizen', async () => {
    const result = await setUserStatus({ data: { uid: CITIZEN_UID, status: 'SUSPENDED', requestId: 'req-status-1' }, auth: { uid: ADMIN_UID } });
    expect(result).toEqual({ ok: true });
    expect(fake._get(`users/${CITIZEN_UID}`)).toMatchObject({ status: 'SUSPENDED' });
  });

  it('[authorized] an ADMIN can reinstate a banned user back to ACTIVE', async () => {
    fake._seed(`users/${CITIZEN_UID}`, { id: CITIZEN_UID, role: 'CITIZEN', status: 'BANNED' });
    await setUserStatus({ data: { uid: CITIZEN_UID, status: 'ACTIVE', requestId: 'req-status-2' }, auth: { uid: ADMIN_UID } });
    expect(fake._get(`users/${CITIZEN_UID}`)).toMatchObject({ status: 'ACTIVE' });
  });

  it('[unauthorized / privilege escalation] a CITIZEN cannot call setUserStatus', async () => {
    await expect(
      setUserStatus({ data: { uid: CITIZEN_UID, status: 'ACTIVE', requestId: 'req-status-3' }, auth: { uid: CITIZEN_UID } }),
    ).rejects.toMatchObject({ code: 'permission-denied' });
  });

  it('[server-side validation] an invalid status string is rejected', async () => {
    await expect(
      setUserStatus({ data: { uid: CITIZEN_UID, status: 'FROZEN', requestId: 'req-status-4' }, auth: { uid: ADMIN_UID } }),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('[server-side validation] an admin cannot change their own account status', async () => {
    await expect(
      setUserStatus({ data: { uid: ADMIN_UID, status: 'BANNED', requestId: 'req-status-5' }, auth: { uid: ADMIN_UID } }),
    ).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('[idempotent] retrying with the same requestId does not re-run the command', async () => {
    const first = await setUserStatus({ data: { uid: CITIZEN_UID, status: 'BANNED', requestId: 'req-status-idem' }, auth: { uid: ADMIN_UID } });
    const second = await setUserStatus({ data: { uid: CITIZEN_UID, status: 'BANNED', requestId: 'req-status-idem' }, auth: { uid: ADMIN_UID } });
    expect(first).toEqual(second);
    expect(fake._addedDocs.filter((d) => d.collection === 'audit_logs')).toHaveLength(1);
  });

  it('[audit logging] status changes are logged with before/after status', async () => {
    await setUserStatus({ data: { uid: CITIZEN_UID, status: 'BANNED', requestId: 'req-status-audit' }, auth: { uid: ADMIN_UID } });
    const [entry] = fake._addedDocs.filter((d) => d.collection === 'audit_logs');
    expect(entry.data).toMatchObject({ action: 'setUserStatus', targetId: CITIZEN_UID, before: { status: 'ACTIVE' }, after: { status: 'BANNED' } });
  });
});
