import * as admin from 'firebase-admin';
import { HttpsError, onCall } from 'firebase-functions/v2/https';
import { requireRole } from '../lib/auth';
import { writeAuditLog } from '../lib/audit';
import { withIdempotency } from '../lib/idempotency';
import { checkFunctionRateLimit } from '../lib/rateLimit';

interface SubscribeToAlertsRequest {
  token: string;
  requestId: string;
}
interface SubscribeToAlertsResponse {
  ok: true;
}

/**
 * Subscribes one device's FCM registration token to the public `alerts`
 * topic, so it receives a push notification whenever `notifyOnAlertCreated`
 * fires (see that file) — official broadcasts and SOS alerts, the same
 * ones every signed-in user already sees inside the app.
 *
 * Deliberately NOT admin/officer-gated the way most of this project's
 * Functions are: there is nothing privileged about a user opting their own
 * device in to notifications for content they can already read. `token`
 * comes from the client's own `getToken()` call (Firebase Cloud Messaging)
 * and identifies only a device+app installation, never a person on its
 * own — `admin.messaging().subscribeToTopic` is the only way to manage
 * topic subscriptions at all, since that API does not exist in the client
 * SDK, which is why this needs to be a Function rather than a direct
 * client call. `requireRole` is still used (with every valid role
 * accepted) purely to require the caller be signed in with a real profile,
 * consistent with how every other Function in this project authenticates
 * -- the one side-effect worth naming is that an unapproved LAW_ENFORCER
 * account cannot subscribe until approved, the same restriction that
 * applies to every other action `requireRole` gates.
 */
export const subscribeToAlerts = onCall<SubscribeToAlertsRequest>({ enforceAppCheck: true }, async (request) => {
  const caller = await requireRole(request, ['CITIZEN', 'LAW_ENFORCER', 'ADMIN']);
  await checkFunctionRateLimit(caller.id, 'subscribeToAlerts', 20); // 20/hour — a device only needs to (re)subscribe occasionally
  const { token } = request.data;
  if (!token || typeof token !== 'string' || token.length < 10) {
    throw new HttpsError('invalid-argument', 'A valid FCM registration token is required.');
  }

  return withIdempotency<SubscribeToAlertsResponse>(request.data.requestId, async () => {
    await admin.messaging().subscribeToTopic(token, 'alerts');

    await writeAuditLog({
      action: 'subscribeToAlerts',
      actorUid: caller.id,
      actorRole: caller.role,
      targetCollection: 'fcm_topic_subscriptions',
      targetId: caller.id,
      before: null,
      after: { topic: 'alerts' },
      requestId: request.data.requestId,
      result: 'success',
    });

    return { ok: true };
  });
});
