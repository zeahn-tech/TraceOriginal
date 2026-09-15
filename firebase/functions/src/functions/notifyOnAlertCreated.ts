import * as admin from 'firebase-admin';
import { onDocumentCreated } from 'firebase-functions/v2/firestore';
import { logger } from 'firebase-functions/v2';

/**
 * Fires for every new document in `alerts/{alertId}` — both the ADMIN
 * broadcast path (publishAlert.ts) and the citizen SOS path (a direct,
 * rule-governed client write; see publishAlert.ts's own comment on why
 * SOS is intentionally NOT routed through a callable Function) land in
 * this same collection, so a single Firestore trigger here covers both
 * without either path needing to know about push notifications at all.
 *
 * Sends to the 'alerts' topic (see subscribeToAlerts.ts for how a device
 * joins it) rather than iterating individual tokens — topic fan-out is
 * Firebase's own recommended mechanism for "everyone subscribed" delivery
 * and avoids this project needing to store/manage a token list itself.
 *
 * A push-delivery failure must never fail the alert write itself — the
 * alert already exists and is visible in-app regardless of whether the
 * push fan-out succeeds — so this logs and returns rather than throwing,
 * which would just cause a pointless retry of a trigger that already did
 * the one thing it can't safely redo (the document already exists either
 * way, so retrying would just resend the same push, not fix anything).
 */
export const notifyOnAlertCreated = onDocumentCreated('alerts/{alertId}', async (event) => {
  const data = event.data?.data();
  if (!data) return;

  const title = typeof data.title === 'string' && data.title ? data.title : 'TraceNet Alert';
  const body =
    typeof data.content === 'string' && data.content
      ? data.content
      : 'A new public safety alert has been published.';

  try {
    await admin.messaging().send({
      topic: 'alerts',
      notification: { title, body },
      webpush: { notification: { icon: 'icons/pwa-192.png', badge: 'icons/pwa-192.png', tag: 'tracenet-alert' } },
    });
  } catch (error) {
    logger.error('notifyOnAlertCreated: FCM send failed', error);
  }
});
