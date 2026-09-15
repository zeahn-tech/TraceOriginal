import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import functionsTest from 'firebase-functions-test';
import type { createFakeFirestore } from './mocks/firestore';

vi.mock('firebase-admin', async () => {
  const { createFakeFirestore } = await import('./mocks/firestore');
  const fake = createFakeFirestore();
  const subscribeToTopic = vi.fn().mockResolvedValue(undefined);
  return {
    initializeApp: vi.fn(),
    firestore: fake.firestore,
    messaging: () => ({ subscribeToTopic }),
    __fake: fake,
    __subscribeToTopic: subscribeToTopic,
  };
});

const test = functionsTest();

const CITIZEN_UID = 'citizen-1';
const UNAPPROVED_OFFICER_UID = 'officer-pending-1';
const VALID_TOKEN = 'a-valid-fcm-registration-token';

let fake: ReturnType<typeof createFakeFirestore>;
let subscribeToTopic: ReturnType<typeof vi.fn>;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let subscribeToAlerts: (req: any) => Promise<unknown>;

beforeAll(async () => {
  const admin = (await import('firebase-admin')) as unknown as {
    __fake: ReturnType<typeof createFakeFirestore>;
    __subscribeToTopic: ReturnType<typeof vi.fn>;
  };
  fake = admin.__fake;
  subscribeToTopic = admin.__subscribeToTopic;
  const mod = await import('../src/functions/subscribeToAlerts');
  subscribeToAlerts = test.wrap(mod.subscribeToAlerts);
});

beforeEach(() => {
  fake._reset();
  subscribeToTopic.mockClear();
  fake._seed(`users/${CITIZEN_UID}`, { id: CITIZEN_UID, role: 'CITIZEN', isApproved: true });
  fake._seed(`users/${UNAPPROVED_OFFICER_UID}`, { id: UNAPPROVED_OFFICER_UID, role: 'LAW_ENFORCER', isApproved: false });
});

describe('subscribeToAlerts', () => {
  it('[authorized] a signed-in citizen can subscribe their device token to the alerts topic', async () => {
    const result = await subscribeToAlerts({ data: { token: VALID_TOKEN, requestId: 'req-subscribe-1' }, auth: { uid: CITIZEN_UID } });
    expect(result).toEqual({ ok: true });
    expect(subscribeToTopic).toHaveBeenCalledWith(VALID_TOKEN, 'alerts');
  });

  it('[unauthenticated] a signed-out caller is rejected', async () => {
    await expect(
      subscribeToAlerts({ data: { token: VALID_TOKEN, requestId: 'req-subscribe-2' }, auth: undefined }),
    ).rejects.toMatchObject({ code: 'unauthenticated' });
    expect(subscribeToTopic).not.toHaveBeenCalled();
  });

  it('[approval gate] an unapproved LAW_ENFORCER account cannot subscribe', async () => {
    await expect(
      subscribeToAlerts({ data: { token: VALID_TOKEN, requestId: 'req-subscribe-3' }, auth: { uid: UNAPPROVED_OFFICER_UID } }),
    ).rejects.toMatchObject({ code: 'permission-denied' });
  });

  it('[server-side validation] a missing/too-short token is rejected', async () => {
    await expect(
      subscribeToAlerts({ data: { token: 'x', requestId: 'req-subscribe-4' }, auth: { uid: CITIZEN_UID } }),
    ).rejects.toMatchObject({ code: 'invalid-argument' });
    expect(subscribeToTopic).not.toHaveBeenCalled();
  });

  it('[idempotent] retrying with the same requestId does not call subscribeToTopic twice', async () => {
    const first = await subscribeToAlerts({ data: { token: VALID_TOKEN, requestId: 'req-subscribe-idem' }, auth: { uid: CITIZEN_UID } });
    const second = await subscribeToAlerts({ data: { token: VALID_TOKEN, requestId: 'req-subscribe-idem' }, auth: { uid: CITIZEN_UID } });
    expect(first).toEqual(second);
    expect(subscribeToTopic).toHaveBeenCalledTimes(1);
  });

  it('[audit logging] a successful subscription is logged', async () => {
    await subscribeToAlerts({ data: { token: VALID_TOKEN, requestId: 'req-subscribe-audit' }, auth: { uid: CITIZEN_UID } });
    const [entry] = fake._addedDocs.filter((d) => d.collection === 'audit_logs');
    expect(entry.data).toMatchObject({ action: 'subscribeToAlerts', actorUid: CITIZEN_UID, targetId: CITIZEN_UID });
  });
});
