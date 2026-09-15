import * as admin from 'firebase-admin';
import { HttpsError, type CallableRequest } from 'firebase-functions/v2/https';

export type Role = 'CITIZEN' | 'LAW_ENFORCER' | 'ADMIN';

export interface CallerContext {
  id: string;
  role: Role;
}

/**
 * Re-derives the caller's role (and approval status) server-side from
 * their OWN Firestore user document -- never from a client-supplied claim
 * or field, since a client-supplied value is exactly what a compromised or
 * scripted client could forge. This is the single choke point every
 * privileged Function in this project calls before doing anything else.
 *
 * An unapproved LAW_ENFORCER account has no more standing than a citizen
 * for any role-gated action here -- mirroring the app's own client-side
 * `Protected` route guard (src/routing), approval is a prerequisite for
 * officer privileges, not a separate/later check. This does not apply to
 * ADMIN: an admin account's `isApproved` is set true at creation time (see
 * setUserRole) and is never the gating condition for admin actions.
 */
export async function requireRole(
  request: CallableRequest<unknown>,
  allowed: Role[],
): Promise<CallerContext> {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError('unauthenticated', 'This action requires you to be signed in.');
  }

  const snap = await admin.firestore().doc(`users/${uid}`).get();
  if (!snap.exists) {
    throw new HttpsError('permission-denied', 'No user profile found for this account.');
  }

  const data = snap.data() as { role?: Role; isApproved?: boolean } | undefined;
  const role = data?.role;
  if (!role || !allowed.includes(role)) {
    throw new HttpsError('permission-denied', 'You do not have permission to perform this action.');
  }

  if (role === 'LAW_ENFORCER' && data?.isApproved !== true) {
    throw new HttpsError('permission-denied', 'Your officer account is pending approval.');
  }

  return { id: uid, role };
}
