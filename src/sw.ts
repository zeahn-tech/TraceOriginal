/// <reference lib="webworker" />
/// <reference types="workbox-background-sync" />
import { clientsClaim } from 'workbox-core'
import { cleanupOutdatedCaches, matchPrecache, precacheAndRoute } from 'workbox-precaching'
import { registerRoute, NavigationRoute, setCatchHandler } from 'workbox-routing'
import { NetworkFirst, StaleWhileRevalidate } from 'workbox-strategies'
import { ExpirationPlugin } from 'workbox-expiration'
declare let self: ServiceWorkerGlobalScope

// clientsClaim() + cleanupOutdatedCaches() + precacheAndRoute() are safe to
// run unconditionally at install/activate time — none of them hand control
// to this worker early. self.skipWaiting() is what does that, and it is
// DELIBERATELY NOT called here. vite.config.ts sets registerType:'prompt',
// meaning a newly-installed worker is meant to sit in the "waiting" state
// until the person explicitly confirms an update (via the in-app "Update
// available" prompt, wired through virtual:pwa-register's updateSW() in
// main.tsx), not activate the instant it finishes installing. Calling
// skipWaiting() unconditionally here would silently take over every open
// tab mid-session — exactly the "leave users on broken cached assets"
// failure mode this project has been explicitly asked to avoid: the
// currently-open tab keeps running the OLD JS bundle while a NEW service
// worker starts answering its fetches, which can serve chunks/routes that
// bundle no longer expects. See PWA_PRODUCTION_VALIDATION.md.
clientsClaim(); cleanupOutdatedCaches(); precacheAndRoute(self.__WB_MANIFEST)

self.addEventListener('message', event => {
	if(event.data?.type==='SKIP_WAITING') self.skipWaiting()
})

registerRoute(new NavigationRoute(new NetworkFirst({cacheName:'tracenet-pages',networkTimeoutSeconds:3})))

// Same-origin images ONLY. This app's own static images (logo, hero/banner
// art) are already precached above via precacheAndRoute's build-time
// manifest, so this route exists purely as a safety net for same-origin
// images outside that manifest — it must never widen to match cross-origin
// requests, because the only cross-origin images this app ever loads are
// Firebase Storage evidence photos (reports, wanted notices, id cards),
// which are access-controlled by storage.rules per-request. The Cache
// Storage API has no concept of that authorization state: caching those
// responses here would mean a photo fetched once while authorized keeps
// being served from cache — bypassing any later rules re-check — for up to
// 7 days or until evicted, even after a role change, a report's visibility
// changing, or the underlying object being deleted. Restricting the
// matcher to same-origin closes that off entirely rather than relying on
// remembering not to widen it later.
registerRoute(
	({request,url})=>request.destination==='image' && url.origin===self.location.origin,
	new StaleWhileRevalidate({cacheName:'tracenet-images',plugins:[new ExpirationPlugin({maxEntries:60,maxAgeSeconds:604800})]}),
)

setCatchHandler(async ({request})=>request.mode==='navigate' ? (await matchPrecache('index.html')) ?? Response.error() : Response.error())

// Background Sync: NOT reliably available everywhere — it's a Chromium
// (Chrome/Edge/Samsung Internet/etc.) feature with no support in Safari/iOS
// or Firefox as of this writing. When the browser DOES support it and DOES
// fire this event, it's purely a best-effort accelerator: all it does is
// tell already-open tabs "you might be able to sync now," the same nudge
// the 'online' window event already gives them. The actual, universal,
// always-available mechanism is services.ts#initializeMediaSync()'s
// 'online' event listener, which works identically on every browser
// whether or not Background Sync exists — that is the real fallback this
// app depends on, not this handler. See PWA_PRODUCTION_VALIDATION.md.
self.addEventListener('sync',event=>{
	if(event.tag!=='tracenet-media-sync') return
	event.waitUntil(self.clients.matchAll({type:'window',includeUncontrolled:true}).then(clients=>clients.forEach(client=>client.postMessage({type:'tracenet-media-sync'}))))
})
