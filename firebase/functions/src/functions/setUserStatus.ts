import * as admin from 'firebase-admin';
import { HttpsError, onCall } from 'firebase-functions/v2/https';
import { requireRole } from '../lib/auth';
import { writeAuditLog } from '../lib/audit';
import { withIdempotency } from '../lib/idempotency';
import { checkFunctionRateLimit } from '../lib/rateLimit';

const VALID_STATUSES = ['ACTIVE', 'SUSPENDED', 'BANNED', 'REJECTED'] as const;
type UserStatus = (typeof VALID_STATUSES)[number];

interface SetUserStatusRequest {
  uid: string;
  status: UserStatus;
  requestId: string;
}
interface SetUserStatusResponse {
  ok: true;
}

/**
 * Suspends, bans, reinstates, or rejects a user account. Mirrors
 * ApprovalCard's REJECT and UserCard's TOGGLE STATUS / BAN buttons — moved
 * server-side for the same reason as approveOfficer/setUserRole: `status`
 * is excluded from what a client can write directly to a users document.
 */
export const setUserStatus = onCall<SetUserStatusRequest>({ enforceAppCheck: true }, async (request) => {
  const caller = await requireRole(request, ['ADMIN']);
  await checkFunctionRateLimit(caller.id, 'setUserStatus', 30); // 30/hour
  const { uid, status } = request.data;
  if (!uid || typeof uid !== 'string') throw new HttpsError('invalid-argument', 'uid is required.');
  if (!VALID_STATUSES.includes(status)) {
    throw new HttpsError('invalid-argument', `status must be one of: ${VALID_STATUSES.join(', ')}.`);
  }
  if (uid === caller.id) {
    throw new HttpsError('failed-precondition', 'An admin cannot change their own account status.');
  }

  return withIdempotency<SetUserStatusResponse>(request.data.requestId, async () => {
    const ref = admin.firestore().doc(`users/${uid}`);
    const snap = await ref.get();
    if (!snap.exists) throw new HttpsError('not-found', 'No such user.');
    const before = snap.data()!;

    await ref.update({ status });

    await writeAuditLog({
      action: 'setUserStatus',
      actorUid: caller.id,
      actorRole: caller.role,
      targetCollection: 'users',
      targetId: uid,
      before: { status: before.status ?? 'ACTIVE' },
      after: { status },
      requestId: request.data.requestId,
      result: 'success',
    });

    return { ok: true };
  });
});
