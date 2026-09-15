import { app, firebaseReady, subscribeToAlerts } from './services'

/**
 * Fixed: this app previously had zero Notifications/Push implementation
 * anywhere — the "receive instant notifications" line on the dashboard
 * carousel was marketing copy describing a feature that did not exist
 * (see CROSS_PLATFORM_TEST_REPORT.md §3). This module is the real
 * implementation: Notification permission + Firebase Cloud Messaging,
 * subscribed to the public 'alerts' topic that `notifyOnAlertCreated`
 * (a Cloud Function) publishes to whenever an official broadcast or an
 * SOS alert is created.
 *
 * Deliberately degrades to "the feature just isn't offered" rather than
 * throwing, in every one of these cases, none of which are bugs:
 *  - Firebase isn't configured at all (firebaseReady === false)
 *  - the browser has no Notification API (no such thing on some embedded
 *    webviews) or no FCM support at all (isSupported() from the SDK,
 *    which already accounts for Safari/iOS's more limited push support)
 *  - VITE_FIREBASE_VAPID_KEY isn't set for this deployment
 *  - the person declines the permission prompt
 * `services.ts` is the single source of truth for whether Firebase itself
 * is configured — this module never re-implements that check.
 */
export type PushOutcome = { ok:true } | { ok:false; reason:string }

async function loadMessaging(){
  if(!firebaseReady || !app) return undefined
  const mod = await import('firebase/messaging')
  if(!(await mod.isSupported())) return undefined
  return { mod, messaging: mod.getMessaging(app) }
}

export async function enablePushNotifications():Promise<PushOutcome>{
  if(typeof Notification==='undefined') return {ok:false,reason:'Notifications are not supported by this browser.'}
  const loaded = await loadMessaging()
  if(!loaded) return {ok:false,reason:'Push notifications are not available for this deployment or browser.'}
  const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY
  if(!vapidKey) return {ok:false,reason:'Push notifications are not configured for this deployment.'}

  const permission = await Notification.requestPermission()
  if(permission!=='granted') return {ok:false,reason:'Notification permission was not granted.'}

  try {
    const registration = await navigator.serviceWorker.ready
    const token = await loaded.mod.getToken(loaded.messaging,{ vapidKey, serviceWorkerRegistration:registration })
    if(!token) return {ok:false,reason:'Could not obtain a push token from this browser.'}
    await subscribeToAlerts({ token })
    return {ok:true}
  } catch (err) {
    return {ok:false,reason: err instanceof Error ? err.message : 'Could not enable push notifications.'}
  }
}

/**
 * Foreground-only: a push arriving while a tab already has focus never
 * reaches the service worker's background handler at all (that's a
 * platform guarantee of the Push API, not something this app controls),
 * so it has to be surfaced here instead. Returns an unsubscribe function;
 * safe to call even when push isn't available (resolves to a no-op).
 */
export function listenForForegroundAlerts(onAlert:(title:string,body:string)=>void):()=>void{
  let unsubscribed = false
  let unsubscribe = ()=>{}
  loadMessaging().then(loaded=>{
    if(!loaded || unsubscribed) return
    unsubscribe = loaded.mod.onMessage(loaded.messaging, payload=>{
      onAlert(payload.notification?.title ?? 'TraceNet Alert', payload.notification?.body ?? '')
    })
  })
  return ()=>{ unsubscribed = true; unsubscribe() }
}
