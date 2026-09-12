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
const REPORT_ID = 'report-1';

let fake: ReturnType<typeof createFakeFirestore>;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let transitionReportStatus: (req: any) => Promise<unknown>;

beforeAll(async () => {
  const admin = (await import('firebase-admin')) as unknown as { __fake: ReturnType<typeof createFakeFirestore> };
  fake = admin.__fake;
  const mod = await import('../src/functions/transitionReportStatus');
  transitionReportStatus = test.wrap(mod.transitionReportStatus);
});

beforeEach(() => {
  fake._reset();
  fake._seed(`users/${ADMIN_UID}`, { id: ADMIN_UID, role: 'ADMIN', isApproved: true });
  fake._seed(`users/${OFFICER_UID}`, { id: OFFICER_UID, role: 'LAW_ENFORCER', isApproved: true });
  fake._seed(`users/${CITIZEN_UID}`, { id: CITIZEN_UID, role: 'CITIZEN', isApproved: true });
  fake._seed(`reports/${REPORT_ID}`, { id: REPORT_ID, title: 'Break-in', reporterId: CITIZEN_UID, status: 'PENDING' });
});

describe('transitionReportStatus', () => {
  it('[authorized] an approved OFFICER can move a report to VERIFIED — this is what makes it and its evidence public', async () => {
    const result = await transitionReportStatus({ data: { reportId: REPORT_ID, status: 'VERIFIED', requestId: 'req-status-1' }, auth: { uid: OFFICER_UID } });
    expect(result).toEqual({ ok: true });
    expect(fake._get(`reports/${REPORT_ID}`)).toMatchObject({ status: 'VERIFIED' });
  });

  it('[unauthorized / privilege escalation] the reporting citizen cannot transition their own report\'s status', async () => {
    await expect(
      transitionReportStatus({ data: { reportId: REPORT_ID, status: 'VERIFIED', requestId: 'req-status-2' }, auth: { uid: CITIZEN_UID } }),
    ).rejects.toMatchObject({ code: 'permission-denied' });
    expect(fake._get(`reports/${REPORT_ID}`)).toMatchObject({ status: 'PENDING' });
  });

  it('[server-side validation] an invalid status value is rejected', async () => {
    await expect(
      transitionReportStatus({ data: { reportId: REPORT_ID, status: 'ARCHIVED', requestId: 'req-status-3' }, auth: { uid: OFFICER_UID } }),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('[server-side validation] a soft-deleted report cannot be transitioned', async () => {
    fake._seed(`reports/${REPORT_ID}`, { id: REPORT_ID, status: 'PENDING', isDeleted: true });
    await expect(
      transitionReportStatus({ data: { reportId: REPORT_ID, status: 'VERIFIED', requestId: 'req-status-4' }, auth: { uid: OFFICER_UID } }),
    ).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  it('[not found] transitioning a nonexistent report fails cleanly', async () => {
    await expect(
      transitionReportStatus({ data: { reportId: 'ghost', status: 'VERIFIED', requestId: 'req-status-5' }, auth: { uid: OFFICER_UID } }),
    ).rejects.toMatchObject({ code: 'not-found' });
  });

  it('[idempotent] retrying with the same requestId does not re-run the command', async () => {
    const first = await transitionReportStatus({ data: { reportId: REPORT_ID, status: 'UNDER_INVESTIGATION', requestId: 'req-status-idem' }, auth: { uid: OFFICER_UID } });
    const second = await transitionReportStatus({ data: { reportId: REPORT_ID, status: 'UNDER_INVESTIGATION', requestId: 'req-status-idem' }, auth: { uid: OFFICER_UID } });
    expect(first).toEqual(second);
    expect(fake._addedDocs.filter((d) => d.collection === 'audit_logs')).toHaveLength(1);
  });

  it('[audit logging] a transition is logged with before/after status', async () => {
    await transitionReportStatus({ data: { reportId: REPORT_ID, status: 'RESOLVED', requestId: 'req-status-audit' }, auth: { uid: ADMIN_UID } });
    const [entry] = fake._addedDocs.filter((d) => d.collection === 'audit_logs');
    expect(entry.data).toMatchObject({
      action: 'transitionReportStatus', actorUid: ADMIN_UID, actorRole: 'ADMIN', targetCollection: 'reports', targetId: REPORT_ID,
      before: { status: 'PENDING' }, after: { status: 'RESOLVED' }, result: 'success',
    });
  });
});
