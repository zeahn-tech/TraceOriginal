import { beforeEach, describe, expect, it, vi } from 'vitest';

const send = vi.fn().mockResolvedValue('message-id-1');

vi.mock('firebase-admin', () => ({
  initializeApp: vi.fn(),
  messaging: () => ({ send }),
}));

// firebase-functions/v2's `logger` writes structured logs; not relevant to
// what this test verifies, and noisy in test output otherwise.
vi.mock('firebase-functions/v2', () => ({ logger: { error: vi.fn() } }));

import { notifyOnAlertCreated } from '../src/functions/notifyOnAlertCreated';

// `onDocumentCreated`'s returned CloudFunction exposes `.run(event)` for
// exactly this kind of direct unit test, without needing the emulator or
// firebase-functions-test's more elaborate wrap() (that helper is built
// around onCall's request shape, not Firestore trigger CloudEvents).
function fakeEvent(data: Record<string, unknown> | undefined) {
  return { data: data === undefined ? undefined : { data: () => data } } as never;
}

describe('notifyOnAlertCreated', () => {
  beforeEach(() => {
    send.mockClear();
  });

  it('[admin broadcast] sends a push to the alerts topic using the alert title/content', async () => {
    await notifyOnAlertCreated.run(
      fakeEvent({ title: 'Flood Warning', content: 'Rising water levels reported near the river.', urgency: 4 }),
    );
    expect(send).toHaveBeenCalledWith(
      expect.objectContaining({
        topic: 'alerts',
        notification: { title: 'Flood Warning', body: 'Rising water levels reported near the river.' },
      }),
    );
  });

  it('[SOS alert] sends a push using the same title/content fields SOS writes', async () => {
    await notifyOnAlertCreated.run(
      fakeEvent({ title: 'SOS ACTIVE: Citizen Distress Signal', content: 'Distress signal broadcast from mobile node. Sequence #1.' }),
    );
    expect(send).toHaveBeenCalledWith(
      expect.objectContaining({
        notification: { title: 'SOS ACTIVE: Citizen Distress Signal', body: 'Distress signal broadcast from mobile node. Sequence #1.' },
      }),
    );
  });

  it('[malformed document] falls back to generic copy rather than sending an empty/undefined notification', async () => {
    await notifyOnAlertCreated.run(fakeEvent({ urgency: 2 }));
    expect(send).toHaveBeenCalledWith(
      expect.objectContaining({
        notification: { title: 'TraceNet Alert', body: 'A new public safety alert has been published.' },
      }),
    );
  });

  it('[no snapshot data] does nothing rather than throwing', async () => {
    await expect(notifyOnAlertCreated.run(fakeEvent(undefined))).resolves.toBeUndefined();
    expect(send).not.toHaveBeenCalled();
  });

  it('[delivery failure] a rejected send() does not throw out of the trigger', async () => {
    send.mockRejectedValueOnce(new Error('FCM unavailable'));
    await expect(notifyOnAlertCreated.run(fakeEvent({ title: 'Test', content: 'Test body' }))).resolves.toBeUndefined();
  });
});
