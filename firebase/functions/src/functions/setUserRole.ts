import * as admin from 'firebase-admin';
import { HttpsError, onCall } from 'firebase-functions/v2/https';
import { requireRole, type Role } from '../lib/auth';
import { writeAuditLog } from '../lib/audit';
import { withIdempotency } from '../lib/idempotency';
import { checkFunctionRateLimit } from '../lib/rateLimit';

const VALID_ROLES: Role[] = ['CITIZEN', 'LAW_ENFORCER', 'ADMIN'];

interface SetUserRoleRequest {
  uid: string;
  role: Role;
  requestId: string;
}
interface SetUserRoleResponse {
  ok: true;
}

/**
 * Changes a user's role. This is the single most sensitive operation in the
 * whole app — it is exactly the "privilege escalation" surface every other
 * layer of this project (firestore.rules, storage.rules) has been built to
 * keep a client from reaching directly. It ONLY exists as a Function: there
 * is no client-writable path to a user's `role` field at all anymore (see
 * the tightened firestore.rules `users` update rule).
 *
 * Server-side validation beyond "is the caller an admin":
 *  - `role` must be one of the three real roles (never an arbitrary string)
 *  - demoting the LAST remaining ADMIN account is refused — a check that
 *    genuinely cannot be expressed as a per-document Firestore rule (it
 *    requires counting across the whole collection), which is exactly the
 *    kind of validation that belongs in a Function rather than a rule
 *  - moving a user's role resets `isApproved` to the correct default for
 *    the new role (an officer moved to CITIZEN is auto-approved; a citizen
 *    moved to LAW_ENFORCER starts unapproved again and must go through
 *    approveOfficer like any new officer signup would)
 */
export const setUserRole = onCall<SetUserRoleRequest>({ enforceAppCheck: true }, async (request) => {
  const caller = await requireRole(request, ['ADMIN']);
  await checkFunctionRateLimit(caller.id, 'setUserRole', 30); // 30/hour
  const { uid, role } = request.data;
  if (!uid || typeof uid !== 'string') throw new HttpsError('invalid-argument', 'uid is required.');
  if (!VALID_ROLES.includes(role)) throw new HttpsError('invalid-argument', 'role must be CITIZEN, LAW_ENFORCER, or ADMIN.');

  return withIdempotency<SetUserRoleResponse>(request.data.requestId, async () => {
    const db = admin.firestore();
    const ref = db.doc(`users/${uid}`);
    const snap = await ref.get();
    if (!snap.exists) throw new HttpsError('not-found', 'No such user.');
    const before = snap.data()!;

    if (before.role === 'ADMIN' && role !== 'ADMIN') {
      const adminCountSnap = await db.collection('users').where('role', '==', 'ADMIN').count().get();
      if (adminCountSnap.data().count <= 1) {
        throw new HttpsError('failed-precondition', 'Cannot demote the last remaining ADMIN account.');
      }
    }

    const isApproved = role === 'LAW_ENFORCER' ? false : true;
    const status = 'ACTIVE';
    await ref.update({ role, isApproved, status });

    await writeAuditLog({
      action: 'setUserRole',
      actorUid: caller.id,
      actorRole: caller.role,
      targetCollection: 'users',
      targetId: uid,
      before: { role: before.role, isApproved: before.isApproved ?? false },
      after: { role, isApproved },
      requestId: request.data.requestId,
      result: 'success',
    });

    return { ok: true };
  });
});
