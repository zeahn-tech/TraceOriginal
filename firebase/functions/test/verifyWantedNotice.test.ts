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
const UNAPPROVED_OFFICER_UID = 'officer-unapproved';
const CITIZEN_UID = 'citizen-1';
const NOTICE_ID = 'wanted-1';

let fake: ReturnType<typeof createFakeFirestore>;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let verifyWantedNotice: (req: any) => Promise<unknown>;

beforeAll(async () => {
  const admin = (await import('firebase-admin')) as unknown as { __fake: ReturnType<typeof createFakeFirestore> };
  fake = admin.__fake;
  const mod = await import('../src/functions/verifyWantedNotice');
  verifyWantedNotice = test.wrap(mod.verifyWantedNotice);
});

beforeEach(() => {
  fake._reset();
  fake._seed(`users/${ADMIN_UID}`, { id: ADMIN_UID, role: 'ADMIN', isApproved: true });
  fake._seed(`users/${OFFICER_UID}`, { id: OFFICER_UID, role: 'LAW_ENFORCER', isApproved: true });
  fake._seed(`users/${UNAPPROVED_OFFICER_UID}`, { id: UNAPPROVED_OFFICER_UID, role: 'LAW_ENFORCER', isApproved: false });
  fake._seed(`users/${CITIZEN_UID}`, { id: CITIZEN_UID, role: 'CITIZEN', isApproved: true });
  fake._seed(`wanted_criminals/${NOTICE_ID}`, { id: NOTICE_ID, name: 'John Doe', isVerified: false, status: 'SUBMITTED' });
});

describe('verifyWantedNotice', () => {
  it('[authorized] an approved OFFICER can verify a wanted notice', async () => {
    const result = await verifyWantedNotice({ data: { id: NOTICE_ID, verify: true, requestId: 'req-verify-1' }, auth: { uid: OFFICER_UID } });
    expect(result).toEqual({ ok: true });
    expect(fake._get(`wanted_criminals/${NOTICE_ID}`)).toMatchObject({ isVerified: true, status: 'VERIFIED' });
  });

  it('[authorized] an ADMIN can dismiss a verified notice', async () => {
    fake._seed(`wanted_criminals/${NOTICE_ID}`, { id: NOTICE_ID, isVerified: true, status: 'VERIFIED' });
    await verifyWantedNotice({ data: { id: NOTICE_ID, verify: false, requestId: 'req-verify-2' }, auth: { uid: ADMIN_UID } });
    expect(fake._get(`wanted_criminals/${NOTICE_ID}`)).toMatchObject({ isVerified: false, status: 'DISMISSED' });
  });

  it('[unauthorized / privilege escalation] a CITIZEN cannot verify a wanted notice', async () => {
    await expect(
      verifyWantedNotice({ data: { id: NOTICE_ID, verify: true, requestId: 'req-verify-3' }, auth: { uid: CITIZEN_UID } }),
    ).rejects.toMatchObject({ code: 'permission-denied' });
  });

  it('[unauthorized] an UNAPPROVED officer has no more standing than a citizen here either', async () => {
    await expect(
      verifyWantedNotice({ data: { id: NOTICE_ID, verify: true, requestId: 'req-verify-4' }, auth: { uid: UNAPPROVED_OFFICER_UID } }),
    ).rejects.toMatchObject({ code: 'permission-denied' });
  });

  it('[server-side validation] a deleted notice cannot be verified', async () => {
    fake._seed(`wanted_criminals/${NOTICE_ID}`, { id: NOTICE_ID, isVerified: false, isDeleted: true });
    await expect(
      verifyWantedNotice({ data: { id: NOTICE_ID, verify: true, requestId: 'req-verify-5' }, auth: { uid: OFFICER_UID } }),
    ).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('[server-side validation] verify must be a boolean', async () => {
    await expect(
      verifyWantedNotice({ data: { id: NOTICE_ID, verify: 'yes', requestId: 'req-verify-6' }, auth: { uid: OFFICER_UID } }),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('[not found] verifying a nonexistent notice fails cleanly', async () => {
    await expect(
      verifyWantedNotice({ data: { id: 'ghost', verify: true, requestId: 'req-verify-7' }, auth: { uid: OFFICER_UID } }),
    ).rejects.toMatchObject({ code: 'not-found' });
  });

  it('[idempotent] retrying with the same requestId does not re-run the command', async () => {
    const first = await verifyWantedNotice({ data: { id: NOTICE_ID, verify: true, requestId: 'req-verify-idem' }, auth: { uid: OFFICER_UID } });
    const second = await verifyWantedNotice({ data: { id: NOTICE_ID, verify: true, requestId: 'req-verify-idem' }, auth: { uid: OFFICER_UID } });
    expect(first).toEqual(second);
    expect(fake._addedDocs.filter((d) => d.collection === 'audit_logs')).toHaveLength(1);
  });

  it('[audit logging] verification is logged with before/after state', async () => {
    await verifyWantedNotice({ data: { id: NOTICE_ID, verify: true, requestId: 'req-verify-audit' }, auth: { uid: OFFICER_UID } });
    const [entry] = fake._addedDocs.filter((d) => d.collection === 'audit_logs');
    expect(entry.data).toMatchObject({
      action: 'verifyWantedNotice', actorUid: OFFICER_UID, targetCollection: 'wanted_criminals', targetId: NOTICE_ID,
      before: { isVerified: false }, after: { isVerified: true, status: 'VERIFIED' },
    });
  });
});
