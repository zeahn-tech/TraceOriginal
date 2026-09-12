import * as admin from 'firebase-admin';
import { HttpsError, onCall } from 'firebase-functions/v2/https';
import { requireRole } from '../lib/auth';
import { writeAuditLog } from '../lib/audit';
import { withIdempotency } from '../lib/idempotency';
import { checkFunctionRateLimit } from '../lib/rateLimit';

interface ApproveOfficerRequest {
  uid: string;
  requestId: string;
}
interface ApproveOfficerResponse {
  ok: true;
  alreadyApproved: boolean;
}

/**
 * Approves a pending LAW_ENFORCER account, granting it real officer access.
 * This is the ONLY path by which isApproved can become true for an officer
 * account — firestore.rules explicitly excludes `isApproved` from what a
 * client (including the admin's own client) can write directly to a users
 * document, precisely so this decision always goes through this audited,
 * validated path rather than a raw Firestore write from devtools.
 */
export const approveOfficer = onCall<ApproveOfficerRequest>({ enforceAppCheck: true }, async (request) => {
  const caller = await requireRole(request, ['ADMIN']);
  await checkFunctionRateLimit(caller.id, 'approveOfficer', 30); // 30/hour — generous for bulk approvals, bounded against runaway abuse
  const { uid } = request.data;
  if (!uid || typeof uid !== 'string') {
    throw new HttpsError('invalid-argument', 'uid is required.');
  }

  return withIdempotency<ApproveOfficerResponse>(request.data.requestId, async () => {
    const ref = admin.firestore().doc(`users/${uid}`);
    const snap = await ref.get();
    if (!snap.exists) throw new HttpsError('not-found', 'No such user.');
    const before = snap.data()!;

    if (before.role !== 'LAW_ENFORCER') {
      throw new HttpsError('failed-precondition', 'Only LAW_ENFORCER accounts can be approved.');
    }

    // Idempotent-friendly: re-approving an already-approved officer is a
    // safe no-op rather than an error, so a retried call (or two admins
    // clicking Approve at nearly the same time) can't fail confusingly.
    const alreadyApproved = before.isApproved === true;
    if (!alreadyApproved) {
      await ref.update({ isApproved: true, status: 'ACTIVE' });
    }

    await writeAuditLog({
      action: 'approveOfficer',
      actorUid: caller.id,
      actorRole: caller.role,
      targetCollection: 'users',
      targetId: uid,
      before: { isApproved: before.isApproved ?? false, status: before.status ?? null },
      after: { isApproved: true, status: 'ACTIVE' },
      requestId: request.data.requestId,
      result: 'success',
    });

    return { ok: true, alreadyApproved };
  });
});
