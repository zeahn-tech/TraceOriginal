import * as admin from 'firebase-admin';
import { HttpsError } from 'firebase-functions/v2/https';

const WINDOW_MS = 60 * 60 * 1000; // 1 hour, fixed window

/**
 * Per-caller, per-action rate limiting for privileged Cloud Functions --
 * a separate mechanism from the client-facing daily submission quotas
 * enforced in firestore.rules for ordinary report/tip/alert writes (see
 * services.ts#writeWithQuota). This one protects the privileged Functions
 * THEMSELVES against a compromised or scripted admin/officer account, and
 * is keyed by (uid, action) so one caller hammering one action can never
 * exhaust their budget for a different action.
 *
 * Fixed-window counter, not a sliding one: acceptable here because this is
 * a defense-in-depth backstop against runaway/scripted abuse, not a
 * precision billing mechanism -- consistent with this project's existing
 * quota mechanism, which makes the same fixed-window trade-off.
 */
export async function checkFunctionRateLimit(
  uid: string,
  action: string,
  limitPerHour: number,
): Promise<void> {
  const ref = admin.firestore().doc(`function_rate_limits/${uid}_${action}`);
  const snap = await ref.get();
  const now = Date.now();
  const data = snap.exists ? (snap.data() as { windowStart: number; count: number }) : undefined;

  if (!data || now - data.windowStart >= WINDOW_MS) {
    await ref.set({ windowStart: now, count: 1 });
    return;
  }

  if (data.count >= limitPerHour) {
    throw new HttpsError('resource-exhausted', `Rate limit exceeded for ${action}. Try again later.`);
  }

  await ref.set({ windowStart: data.windowStart, count: data.count + 1 });
}
