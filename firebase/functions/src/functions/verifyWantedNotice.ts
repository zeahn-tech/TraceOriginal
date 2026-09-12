import * as admin from 'firebase-admin';
import { HttpsError, onCall } from 'firebase-functions/v2/https';
import { requireRole } from '../lib/auth';
import { writeAuditLog } from '../lib/audit';
import { withIdempotency } from '../lib/idempotency';
import { checkFunctionRateLimit } from '../lib/rateLimit';

interface VerifyWantedNoticeRequest {
  id: string;
  verify: boolean;
  requestId: string;
}
interface VerifyWantedNoticeResponse {
  ok: true;
}

/**
 * Verifies or dismisses a wanted-person notice. Mirrors WantedCard's
 * VERIFY/DISMISS NOTICE button. This is the gate between "an officer
 * submitted a notice" and "the public can see this person's name and photo"
 * — firestore.rules only lets the public read a wanted_criminals document
 * once isVerified is true, so this Function is the sole trigger for a
 * notice becoming publicly visible.
 */
export const verifyWantedNotice = onCall<VerifyWantedNoticeRequest>({ enforceAppCheck: true }, async (request) => {
  const caller = await requireRole(request, ['LAW_ENFORCER', 'ADMIN']);
  await checkFunctionRateLimit(caller.id, 'verifyWantedNotice', 60); // 60/hour
  const { id, verify } = request.data;
  if (!id || typeof id !== 'string') throw new HttpsError('invalid-argument', 'id is required.');
  if (typeof verify !== 'boolean') throw new HttpsError('invalid-argument', 'verify must be a boolean.');

  return withIdempotency<VerifyWantedNoticeResponse>(request.data.requestId, async () => {
    const ref = admin.firestore().doc(`wanted_criminals/${id}`);
    const snap = await ref.get();
    if (!snap.exists) throw new HttpsError('not-found', 'No such wanted notice.');
    const before = snap.data()!;

    if (before.isDeleted === true) {
      throw new HttpsError('failed-precondition', 'This notice has been removed and cannot be verified.');
    }

    const status = verify ? 'VERIFIED' : 'DISMISSED';
    await ref.update({ isVerified: verify, status, updatedAt: Date.now() });

    await writeAuditLog({
      action: 'verifyWantedNotice',
      actorUid: caller.id,
      actorRole: caller.role,
      targetCollection: 'wanted_criminals',
      targetId: id,
      before: { isVerified: before.isVerified ?? false, status: before.status ?? null },
      after: { isVerified: verify, status },
      requestId: request.data.requestId,
      result: 'success',
    });

    return { ok: true };
  });
});
