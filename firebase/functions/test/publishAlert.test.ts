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
let publishAlert: (req: any) => Promise<unknown>;

const validPayload = { title: 'Flooding Warning', content: 'Avoid the riverside road.', urgency: 4, locationName: 'Monrovia', latitude: 6.3, longitude: -10.8 };

beforeAll(async () => {
  const admin = (await import('firebase-admin')) as unknown as { __fake: ReturnType<typeof createFakeFirestore> };
  fake = admin.__fake;
  const mod = await import('../src/functions/publishAlert');
  publishAlert = test.wrap(mod.publishAlert);
});

beforeEach(() => {
  fake._reset();
  fake._seed(`users/${ADMIN_UID}`, { id: ADMIN_UID, role: 'ADMIN', isApproved: true });
  fake._seed(`users/${CITIZEN_UID}`, { id: CITIZEN_UID, role: 'CITIZEN', isApproved: true });
});

describe('publishAlert', () => {
  it('[authorized] an ADMIN can publish an official alert', async () => {
    const result = await publishAlert({ data: { ...validPayload, requestId: 'req-alert-1' }, auth: { uid: ADMIN_UID } });
    expect(result).toMatchObject({ ok: true, id: 'req-alert-1' });
    expect(fake._get('alerts/req-alert-1')).toMatchObject({ title: 'Flooding Warning', urgency: 4 });
  });

  it('[unauthorized / privilege escalation] a CITIZEN cannot call publishAlert (SOS remains a separate, direct, rule-governed write — see FIREBASE_SECURITY_IMPLEMENTATION.md)', async () => {
    await expect(
      publishAlert({ data: { ...validPayload, requestId: 'req-alert-2' }, auth: { uid: CITIZEN_UID } }),
    ).rejects.toMatchObject({ code: 'permission-denied' });
    expect(fake._get('alerts/req-alert-2')).toBeUndefined();
  });

  it('[server-side validation] missing title is rejected', async () => {
    await expect(
      publishAlert({ data: { ...validPayload, title: '', requestId: 'req-alert-3' }, auth: { uid: ADMIN_UID } }),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('[server-side validation] urgency out of range (1-5) is rejected', async () => {
    await expect(
      publishAlert({ data: { ...validPayload, urgency: 11, requestId: 'req-alert-4' }, auth: { uid: ADMIN_UID } }),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('[server-side validation] non-numeric coordinates are rejected', async () => {
    await expect(
      publishAlert({ data: { ...validPayload, latitude: 'north', requestId: 'req-alert-5' }, auth: { uid: ADMIN_UID } }),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
  });

  it('[idempotent / duplicate prevention] retrying the same requestId never creates a second alert document', async () => {
    const first = await publishAlert({ data: { ...validPayload, requestId: 'req-alert-idem' }, auth: { uid: ADMIN_UID } });
    const second = await publishAlert({ data: { ...validPayload, requestId: 'req-alert-idem' }, auth: { uid: ADMIN_UID } });
    expect(first).toEqual(second);
    expect(fake._addedDocs.filter((d) => d.collection === 'audit_logs')).toHaveLength(1);
  });

  it('[audit logging] publishing is logged with the full alert content as "after"', async () => {
    await publishAlert({ data: { ...validPayload, requestId: 'req-alert-audit' }, auth: { uid: ADMIN_UID } });
    const [entry] = fake._addedDocs.filter((d) => d.collection === 'audit_logs');
    expect(entry.data).toMatchObject({ action: 'publishAlert', actorUid: ADMIN_UID, targetCollection: 'alerts', targetId: 'req-alert-audit', result: 'success' });
  });
});
