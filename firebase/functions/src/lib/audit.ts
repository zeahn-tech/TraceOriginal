import * as admin from 'firebase-admin';
import type { Role } from './auth';

export interface AuditLogEntry {
  action: string;
  actorUid: string;
  actorRole: Role;
  targetCollection: string;
  targetId: string;
  before: unknown;
  after: unknown;
  requestId: string;
  result: 'success' | 'failure';
}

/**
 * Every privileged Function writes exactly one of these on completion --
 * success or failure -- so there is a durable, queryable record of who did
 * what to which document and when, independent of Firestore's own
 * (unaudited) write history. `audit_logs` is append-only from the client
 * side under firestore.rules; this is the only writer.
 */
export async function writeAuditLog(entry: AuditLogEntry): Promise<void> {
  await admin.firestore().collection('audit_logs').add({ ...entry, timestamp: Date.now() });
}
