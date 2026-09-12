import * as admin from 'firebase-admin';
import { HttpsError, onCall } from 'firebase-functions/v2/https';
import { requireRole } from '../lib/auth';
import { writeAuditLog } from '../lib/audit';
import { withIdempotency } from '../lib/idempotency';
import { checkFunctionRateLimit } from '../lib/rateLimit';

interface PublishAlertRequest {
  title: string;
  content: string;
  urgency: number;
  locationName: string;
  county?: string;
  latitude: number;
  longitude: number;
  requestId: string;
}
interface PublishAlertResponse {
  ok: true;
  id: string;
}

/**
 * Publishes an official public safety broadcast. Mirrors AlertDialog's
 * BROADCAST ALERT button.
 *
 * Note on scope, documented plainly rather than silently assumed: a
 * citizen's SOS button creates a document in this SAME `alerts` collection
 * through a direct, rule-governed client write — that path is deliberately
 * left alone (see FIREBASE_SECURITY_IMPLEMENTATION.md §7), because SOS is a
 * time-critical, non-privileged, citizen self-service action, not an
 * "administrative command." This Function is specifically the ADMIN
 * broadcast path, and it's what gains audit logging and stronger
 * validation that a direct client write to `alerts` cannot get (there's no
 * field in the schema distinguishing "official broadcast" from "SOS," so a
 * Firestore rule alone can't tell them apart — a Function that only Admins
 * can successfully call is what actually closes that gap for the
 * admin-broadcast case specifically).
 */
export const publishAlert = onCall<PublishAlertRequest>({ enforceAppCheck: true }, async (request) => {
  const caller = await requireRole(request, ['ADMIN']);
  await checkFunctionRateLimit(caller.id, 'publishAlert', 10); // 10/hour — real emergency broadcasts are rare
  const { title, content, urgency, locationName, county, latitude, longitude } = request.data;

  if (!title || typeof title !== 'string') throw new HttpsError('invalid-argument', 'title is required.');
  if (!content || typeof content !== 'string') throw new HttpsError('invalid-argument', 'content is required.');
  if (!locationName || typeof locationName !== 'string') throw new HttpsError('invalid-argument', 'locationName is required.');
  if (typeof urgency !== 'number' || urgency < 1 || urgency > 5) {
    throw new HttpsError('invalid-argument', 'urgency must be a number between 1 and 5.');
  }
  if (typeof latitude !== 'number' || typeof longitude !== 'number') {
    throw new HttpsError('invalid-argument', 'latitude/longitude must be numbers.');
  }

  return withIdempotency<PublishAlertResponse>(request.data.requestId, async () => {
    // The alert's own document id IS the idempotency key — a retried
    // publish with the same requestId can never create two alerts, by
    // construction, independent of the function_calls bookkeeping above.
    const id = request.data.requestId;
    const alert = { id, title, content, urgency, locationName, county: county ?? null, latitude, longitude, timestamp: Date.now() };
    await admin.firestore().doc(`alerts/${id}`).set(alert);

    await writeAuditLog({
      action: 'publishAlert',
      actorUid: caller.id,
      actorRole: caller.role,
      targetCollection: 'alerts',
      targetId: id,
      before: null,
      after: alert,
      requestId: request.data.requestId,
      result: 'success',
    });

    return { ok: true, id };
  });
});
