import * as admin from 'firebase-admin';
import { HttpsError } from 'firebase-functions/v2/https';

// Deliberately shorter than every real requestId this app ever generates
// (crypto.randomUUID()-derived strings on the client, or the
// `req-<action>-<n>` fixtures in tests) but long enough to reject an
// obviously-placeholder value outright rather than silently proceeding
// without real idempotency protection.
const MIN_REQUEST_ID_LENGTH = 10;

/**
 * Ensures a callable Function's side-effecting command runs AT MOST ONCE
 * per requestId, no matter how many times the client retries the call --
 * a network retry after a successful-but-unacknowledged response must
 * never re-run the underlying write (double-approve an officer, double-
 * publish an alert, etc).
 *
 * `function_calls/{requestId}` is the durability record: first call claims
 * it and runs `command`, recording success (with the result, so a retry
 * can return the exact same response) or failure. A retried call against a
 * PREVIOUSLY FAILED requestId is refused outright (`aborted`) rather than
 * silently re-attempting the command forever -- a genuinely new attempt
 * needs a new requestId, by design (see idempotency.test.ts).
 */
export async function withIdempotency<T>(requestId: string, command: () => Promise<T>): Promise<T> {
  if (!requestId || requestId.length < MIN_REQUEST_ID_LENGTH) {
    throw new HttpsError('invalid-argument', 'A valid requestId (idempotency key) is required.');
  }

  const ref = admin.firestore().doc(`function_calls/${requestId}`);
  const existing = await ref.get();

  if (existing.exists) {
    const data = existing.data() as { status: 'success' | 'failed'; result?: T };
    if (data.status === 'success') return data.result as T;
    throw new HttpsError('aborted', 'This request previously failed. Retry with a new requestId.');
  }

  try {
    const result = await command();
    await ref.set({ status: 'success', result: result ?? null, completedAt: Date.now() });
    return result;
  } catch (error) {
    await ref.set({
      status: 'failed',
      error: error instanceof Error ? error.message : String(error),
      completedAt: Date.now(),
    });
    throw error;
  }
}
