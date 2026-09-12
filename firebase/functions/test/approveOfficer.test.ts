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
const OFFICER_UID = 'officer-1';
const CITIZEN_UID = 'citizen-1';

let fake: ReturnType<typeof createFakeFirestore>;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let approveOfficer: (req: any) => Promise<unknown>;

beforeAll(async () => {
  const admin = (await import('firebase-admin')) as unknown as { __fake: ReturnType<typeof createFakeFirestore> };
  fake = admin.__fake;
  const mod = await import('../src/functions/approveOfficer');
  approveOfficer = test.wrap(mod.approveOfficer);
});

beforeEach(() => {
  fake._reset();
  fake._seed(`users/${ADMIN_UID}`, { id: ADMIN_UID, role: 'ADMIN', isApproved: true });
  fake._seed(`users/${OFFICER_UID}`, { id: OFFICER_UID, role: 'LAW_ENFORCER', isApproved: false, status: 'PENDING' });
  fake._seed(`users/${CITIZEN_UID}`, { id: CITIZEN_UID, role: 'CITIZEN', isApproved: true });
});

describe('approveOfficer', () => {
  it('[authorized] an ADMIN can approve a pending officer', async () => {
    const result = await approveOfficer({ data: { uid: OFFICER_UID, requestId: 'req-approve-1' }, auth: { uid: ADMIN_UID } });
    expect(result).toEqual({ ok: true, alreadyApproved: false });
    expect(fake._get(`users/${OFFICER_UID}`)).toMatchObject({ isApproved: true, status: 'ACTIVE' });
  });

  it('[unauthorized / privilege escalation] a CITIZEN cannot call approveOfficer', async () => {
    await expect(
      approveOfficer({ data: { uid: OFFICER_UID, requestId: 'req-approve-2' }, auth: { uid: CITIZEN_UID } }),
    ).rejects.toMatchObject({ code: 'permission-denied' });
    expect(fake._get(`users/${OFFICER_UID}`)).toMatchObject({ isApproved: false });
  });

  it('[unauthenticated] a request with no auth context is rejected', async () => {
    await expect(
      approveOfficer({ data: { uid: OFFICER_UID, requestId: 'req-approve-3' }, auth: undefined }),
    ).rejects.toMatchObject({ code: 'unauthenticated' });
  });

  it('[server-side validation] cannot approve a CITIZEN account (never had an officer application)', async () => {
    await expect(
      approveOfficer({ data: { uid: CITIZEN_UID, requestId: 'req-approve-4' }, auth: { uid: ADMIN_UID } }),
    ).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('[not found] approving a nonexistent uid fails cleanly', async () => {
    await expect(
      approveOfficer({ data: { uid: 'ghost', requestId: 'req-approve-5' }, auth: { uid: ADMIN_UID } }),
    ).rejects.toMatchObject({ code: 'not-found' });
  });

  it('[idempotent] retrying with the same requestId after success returns the cached result instead of re-running', async () => {
    const first = await approveOfficer({ data: { uid: OFFICER_UID, requestId: 'req-approve-idem' }, auth: { uid: ADMIN_UID } });
    const second = await approveOfficer({ data: { uid: OFFICER_UID, requestId: 'req-approve-idem' }, auth: { uid: ADMIN_UID } });
    expect(first).toEqual(second);
    // exactly one audit entry despite two calls with the same requestId
    expect(fake._addedDocs.filter((d) => d.collection === 'audit_logs')).toHaveLength(1);
  });

  it('[idempotent-friendly] re-approving an already-approved officer is a safe no-op, not an error', async () => {
    await approveOfficer({ data: { uid: OFFICER_UID, requestId: 'req-approve-first' }, auth: { uid: ADMIN_UID } });
    const result = await approveOfficer({ data: { uid: OFFICER_UID, requestId: 'req-approve-second-attempt' }, auth: { uid: ADMIN_UID } });
    expect(result).toEqual({ ok: true, alreadyApproved: true });
  });

  it('[audit logging] a successful approval writes exactly one structured audit_logs entry', async () => {
    await approveOfficer({ data: { uid: OFFICER_UID, requestId: 'req-approve-audit' }, auth: { uid: ADMIN_UID } });
    const entries = fake._addedDocs.filter((d) => d.collection === 'audit_logs');
    expect(entries).toHaveLength(1);
    expect(entries[0].data).toMatchObject({ action: 'approveOfficer', actorUid: ADMIN_UID, actorRole: 'ADMIN', targetCollection: 'users', targetId: OFFICER_UID, requestId: 'req-approve-audit', result: 'success' });
  });

  it('[audit logging] a rejected (unauthorized) attempt writes no audit entry at all', async () => {
    await approveOfficer({ data: { uid: OFFICER_UID, requestId: 'req-approve-rejected' }, auth: { uid: CITIZEN_UID } }).catch(() => {});
    expect(fake._addedDocs.filter((d) => d.collection === 'audit_logs')).toHaveLength(0);
  });
});
