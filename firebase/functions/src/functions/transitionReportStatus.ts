import * as admin from 'firebase-admin';
import { HttpsError, onCall } from 'firebase-functions/v2/https';
import { requireRole } from '../lib/auth';
import { writeAuditLog } from '../lib/audit';
import { withIdempotency } from '../lib/idempotency';
import { checkFunctionRateLimit } from '../lib/rateLimit';

const VALID_STATUSES = ['PENDING', 'UNDER_INVESTIGATION', 'VERIFIED', 'RESOLVED', 'DISMISSED', 'FALSE_REPORT'] as const;
type ReportStatus = (typeof VALID_STATUSES)[number];

interface TransitionReportStatusRequest {
  reportId: string;
  status: ReportStatus;
  requestId: string;
}
interface TransitionReportStatusResponse {
  ok: true;
}

/**
 * Transitions an incident report's status. Mirrors OperationalReport's
 * Status dropdown. This is the single most consequential field on a
 * report: moving a report to VERIFIED is what makes it — and its attached
 * evidence — publicly visible under firestore.rules/storage.rules. Moved
 * server-side specifically for that reason, distinct from
 * internalNotes/assignedInvestigator/isEscalated, which remain ordinary
 * privileged direct writes under firestore.rules (see
 * FIREBASE_SECURITY_IMPLEMENTATION.md for that scoping decision) — they
 * don't change who can see the report, only its internal handling
 * metadata, so they don't carry the same "this changes public visibility"
 * stakes that specifically motivate moving `status` behind an audited,
 * validated Function.
 *
 * This app's existing UI allows any-to-any status transitions (no strict
 * workflow graph is enforced today, even in the Select dropdown) — this
 * Function preserves that same behavior rather than inventing new
 * restrictions the app doesn't already have, while still refusing to
 * transition a soft-deleted report (which the UI also has no path to do).
 */
export const transitionReportStatus = onCall<TransitionReportStatusRequest>({ enforceAppCheck: true }, async (request) => {
  const caller = await requireRole(request, ['LAW_ENFORCER', 'ADMIN']);
  await checkFunctionRateLimit(caller.id, 'transitionReportStatus', 120); // 120/hour — officers processing many reports
  const { reportId, status } = request.data;
  if (!reportId || typeof reportId !== 'string') throw new HttpsError('invalid-argument', 'reportId is required.');
  if (!VALID_STATUSES.includes(status)) {
    throw new HttpsError('invalid-argument', `status must be one of: ${VALID_STATUSES.join(', ')}.`);
  }

  return withIdempotency<TransitionReportStatusResponse>(request.data.requestId, async () => {
    const ref = admin.firestore().doc(`reports/${reportId}`);
    const snap = await ref.get();
    if (!snap.exists) throw new HttpsError('not-found', 'No such report.');
    const before = snap.data()!;

    if (before.isDeleted === true) {
      throw new HttpsError('failed-precondition', 'This report has been deleted and cannot be transitioned.');
    }

    await ref.update({ status, updatedAt: Date.now() });

    await writeAuditLog({
      action: 'transitionReportStatus',
      actorUid: caller.id,
      actorRole: caller.role,
      targetCollection: 'reports',
      targetId: reportId,
      before: { status: before.status },
      after: { status },
      requestId: request.data.requestId,
      result: 'success',
    });

    return { ok: true };
  });
});
