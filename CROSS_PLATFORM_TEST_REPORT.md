# TraceNet — Cross-Platform Validation Report

## 0. Status, stated plainly up front

**This report is a code-level compatibility analysis, not a record of live testing on physical devices.** This sandbox has no Android/iOS devices, no macOS/Windows/Linux desktops, and no real browser binaries it can launch interactively (the project's own `IMPLIMENTATION_STATUS.MD` documents the same constraint for Playwright — `cdn.playwright.dev` returns 403 here). Claiming otherwise would be a false verification claim of exactly the kind that document explicitly warns against.

What *was* done, concretely:
- A real production build (`npm ci && npm run build`) was run and its output (`dist/manifest.webmanifest`, `dist/sw.js`, `dist/404.html`, icon set) inspected directly.
- The full existing automated suite was re-run: `npm run typecheck` (clean), `npm run lint` (0 errors, the same 6 pre-existing warnings), `npm run test` (53/53), `npm run test:pwa` (25/25) — no regressions found.
- Every source file touching a platform-sensitive Web API (camera/mic, geolocation, file input, notifications, background sync, install prompts, service worker caching, viewport/safe-area CSS) was read directly, with file:line evidence, rather than assumed from the feature list.
- Per-platform behavior is stated against well-documented, publicly-verifiable browser/OS API support (MDN, WebKit/Chromium bug trackers, each vendor's own PWA documentation) — cited as *documented platform behavior*, never phrased as something this session watched happen on a device.

Three real, code-verifiable functional gaps were found this way (§2.1–2.3) — independent of any platform-specific behavior. Everything else in this report is either a confirmed code fact or a named platform limitation with a concrete manual-verification step. No visual design was changed to compensate for any platform difference (per the task's instruction) — every fix below is behavioral, not stylistic.

---

## 1. How to read the verification tags in this document

- **Code-verified** — read directly from source in this session, or confirmed via a real build/test run in this sandbox.
- **Documented platform behavior** — a well-established, publicly documented fact about how a browser/OS implements a given Web API (not this app's code) — e.g. "desktop Firefox does not fire `beforeinstallprompt`." Cited as vendor-documented fact, not as something observed here.
- **Requires manual device verification** — cannot be confirmed by either of the above; a concrete step is given in §7 for a human with the actual device/browser.

---

## 2. Real bugs found (platform-independent — affect every platform equally)

### 2.1 HIGH — Attached videos can never actually be watched, on any platform

**Code-verified.** `src/components/MediaAsset.tsx`'s `MediaThumb` is the *only* media-playback surface in the app (used by `ReportCard.tsx` and `WantedCard.tsx` — grepped, confirmed no other player exists). For `kind==='video'` it renders:

```
<video src={url} muted playsInline preload="none" onError={...} style={{width:60|80, height:60|96, ...}}/>
```

`muted` with no `controls` attribute and no click handler means the element is permanently silent and non-interactive — there is no way, on any browser or platform, to actually play a video attachment at a size or with audio a person could use. `playsInline` and `muted` are exactly correct for iOS Safari's autoplay policy, but nothing here autoplays, so those attributes are currently solving a problem the component doesn't have while leaving the real one (no way to view the video) unaddressed. Audio attachments do not share this problem — the `<audio>` element is rendered with `controls`.

**Practical impact:** a citizen or officer reviewing a report with video evidence sees a small silent square; there is no full-size viewer, no play button, no controls of any kind. This was very likely masked in prior phases because Phases 3/4/7's tests exercise the upload/sync *pipeline* (does the file reach Storage, does the URL resolve) rather than the *playback* UI, and no phase's stated scope included a media-viewing UX pass.

**Not fixed in this pass** — fixing it (adding `controls`, removing the forced `muted`, and/or adding a full-size lightbox) is a UI behavior change beyond "validate across platforms," and this task's instructions were explicit about not altering behavior unless it's the specific thing being validated. Flagging it here because "Media playback" was named directly in the requested test scope, and it would be misleading to report clean "media playback" results across 8 platforms without surfacing this.

### 2.2 MEDIUM — Camera/mic capture code exists but is never invoked; the shipped UI is file-picker-only

**Code-verified.** `src/services.ts` defines two capture-capable functions:
- `captureMedia(kind, accept)` (line 370) — creates a hidden `<input type="file">` and sets `input.capture='environment'` when `kind==='image'`, which is what forces a mobile browser straight into the rear camera instead of showing a general chooser.
- `recordMedia(kind, durationMs)` (line 375) — a full `getUserMedia`/`MediaRecorder` in-page recorder for video/audio, with proper codec fallback (`video/webm;codecs=vp9,opus` → `video/webm` → `video/mp4`) and `capabilities.mediaRecorder` feature-detection already wired into `export const capabilities` (line 64).

Grepped across every `.tsx` file outside of `services.ts` itself: **neither function is called anywhere.** The actual UI (`ReportForm.tsx`, `WantedForm.tsx`, `SignUpPage.tsx`) exclusively uses the plain `FileInput` component (`src/shared/ui.tsx`) — a bare `<input type="file" accept="image/*,video/*,audio/*">` with no `capture` attribute at all.

**Practical impact, and why it's platform-relevant:** without `capture`, what happens when a person taps "ATTACH EVIDENCE" is entirely up to each platform's file-input UI, not this app's choice — see §3 for how that diverges by browser. The in-page recorder (`recordMedia`) being unused means the app never gets a chance to, e.g., cap recording duration or guarantee a codec Safari can actually produce — it's simply dead code today.

**Not fixed in this pass**, same reasoning as §2.1 — this is a feature-wiring gap, not a platform bug, and wiring it up is a behavior change outside "validate the existing PWA."

### 2.3 LOW — No persistent "you're offline" indicator; offline state is only surfaced reactively

**Code-verified.** Grepped for offline-related UI copy: the only user-facing messages are one-time post-submit toasts in `ReportForm.tsx` and `WantedForm.tsx` ("You appear to be offline. The report is saved on this device…"). There is no persistent banner anywhere in `App.tsx`/`useAppData.ts` reflecting `navigator.onLine` while a person is simply browsing offline. Reconnection itself is handled correctly and universally — `initializeMediaSync()` (`services.ts`, line ~678) listens for the `online` window event on every browser, which is standards-based and needs no per-platform handling — but a person has no ambient way to know *why* content isn't updating while offline until they try to submit something.

**Not a defect in the offline-first pipeline itself** (Phases 3/4 already verified that end-to-end, 21/21 + 2/2 tests), just a UX gap adjacent to the "Offline mode" item in the requested test scope.

---

## 3. Notifications — not implemented at all (scope clarification, not a bug)

Grepped the entire `src/` tree for `Notification`, `PushManager`, and `push` (case-insensitive): the only hits are the word "notifications" in marketing copy (`Carousel.tsx`: *"Receive instant notifications about emergency situations nearby"*) and unrelated array-`.push()` calls. **There is no `Notification` API usage, no push subscription, no `showNotification()` call anywhere in this codebase.** The in-app "Update available" / install prompts (`InstallPrompt.tsx`) are DOM banners, not OS-level notifications, and don't require the Notifications permission at all.

This means the "Notifications where supported" item in the requested test matrix currently has nothing to test — there is no notification feature in this build to validate per-platform. This is worth stating explicitly rather than silently marking it "passed" on any platform, since a marketing claim implies a capability that does not exist yet.

---

## 4. Per-platform matrix

Legend: ✅ expected to work as designed · ⚠️ works with a named, platform-specific caveat · ⛔ not supported by the platform at all (not a bug in this app — an inherent platform limitation) · — not applicable / nothing to test (see §3)

| Capability | Android Chrome | iPhone Safari | iPad Safari | Win Chrome | Win Edge | macOS Safari | macOS Chrome | Linux Chrome/Firefox |
|---|---|---|---|---|---|---|---|---|
| Install (banner) | ✅ `beforeinstallprompt` | ⚠️ manual "Share → Add to Home Screen" banner (§5.1) | ⚠️ same as iPhone | ✅ `beforeinstallprompt` | ✅ `beforeinstallprompt` | ⚠️ manual, same mechanism as iOS (§5.1) | ✅ `beforeinstallprompt` | ✅ Chrome / ⛔ Firefox (§5.2) |
| Authentication (Firebase Auth email+Google) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Navigation / routing / deep links | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Reports (create/view, offline-first) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Camera (evidence photo capture) | ✅ chooser incl. camera | ⚠️ chooser behavior varies by iOS version with mixed `accept` (§5.3) | ⚠️ same as iPhone | ✅ (no camera; opens file picker) | ✅ (no camera; opens file picker) | ✅ (no camera; opens file picker) | ✅ (no camera; opens file picker) | ✅ (no camera; opens file picker) |
| Microphone (audio evidence via file picker) | ✅ | ⚠️ file-picker-only; no in-OS "record audio" action for a bare `audio/*` accept on most iOS versions (§5.3) | ⚠️ same as iPhone | — (desktop: file picker only, by design) | — | — | — | — |
| Image upload | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Video upload | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Audio upload | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Offline mode (compose while offline) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Reconnection / auto-sync | ✅ via Background Sync + `online` event | ✅ via `online` event only (§5.4) | ✅ via `online` event only | ✅ via Background Sync + `online` event | ✅ via Background Sync + `online` event | ✅ via `online` event only | ✅ via Background Sync + `online` event | ✅ Chrome: both / Firefox: `online` event only |
| Media playback | ⚠️ audio ✅, video non-functional everywhere (§2.1) | ⚠️ same | ⚠️ same | ⚠️ same | ⚠️ same | ⚠️ same | ⚠️ same | ⚠️ same |
| Notifications | — nothing implemented (§3) | — | — | — | — | — | — | — |
| Geolocation (SOS + report GPS) | ✅ | ✅ (HTTPS/localhost required, satisfied by GitHub Pages) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Responsive layout (760px breakpoint, `100dvh`, safe-area bottom padding) | ✅ | ✅ | ⚠️ tablet gets the same 2-column ≥760px layout as desktop, capped at 780px wide and centered — no tablet-specific breakpoint exists (§5.5) | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 5. Documented platform-specific limitations (not bugs in this app)

### 5.1 iOS/iPadOS Safari has no `beforeinstallprompt` — by design, on every version

**Documented platform behavior.** WebKit has never implemented the `beforeinstallprompt` event; Apple's own supported installation path for a home-screen web app is the manual Share-sheet → "Add to Home Screen" flow. `InstallPrompt.tsx` (line 8–12) already detects this correctly — `/iphone|ipad|ipod/i.test(navigator.userAgent)` combined with a `display-mode: standalone` / `navigator.standalone` check — and shows the manual instruction banner instead of silently doing nothing. This is the *correct* handling of an inherent platform gap, not a defect. One real edge case worth a manual check: Safari's UA-string handling on iPadOS (since iPadOS 13) defaults to a **desktop** user agent unless "Request Mobile Website" is toggled, which can make `/ipad/i.test(navigator.userAgent)` fail to match on a stock iPad — meaning the iPad manual-install banner may not appear at all in the default configuration. Flagged in §7 as a manual check; this is genuinely un-confirmable without a physical iPad.

### 5.2 Desktop Firefox does not support installing this (or any) PWA

**Documented platform behavior.** Mozilla removed the "Install this site as an app" feature from desktop Firefox in 2021; Firefox for Android has partial, different PWA support. Firefox never fires `beforeinstallprompt`, and `InstallPrompt.tsx`'s iOS-detection branch is (correctly) iOS-only, so **Firefox users get no install banner of any kind** — not a bug, just a gap this app inherits from the browser having no install surface to hook into.

### 5.3 iOS Safari's file-input camera/mic behavior is narrower than Android's, and depends on how `accept` is set

**Documented platform behavior**, directly relevant given §2.2's finding that the shipped `FileInput` never sets `capture`:
- Android Chrome's file-input chooser reliably offers "Camera" as a first-class option alongside Files/Gallery for `accept="image/*"` or `accept="video/*"`, with or without a `capture` attribute.
- iOS Safari's action sheet for `<input type="file">` historically shows "Take Photo or Video" only when `accept` is scoped to `image/*` and/or `video/*`; a combined `accept="image/*,video/*,audio/*"` (exactly what `ReportForm.tsx` sets today) is more likely to fall back to "Photo Library"/"Browse" only on some iOS versions, and audio-only accept values have no direct "record audio" capture action in the native sheet the way Android's camera does. This is exactly why `captureMedia()`'s `input.capture='environment'` exists in the codebase (§2.2) — it's the standard mitigation for this — but since it's unused today, the live behavior on iOS is left entirely up to Safari's default handling of a mixed-type accept string, which is the least predictable case. **Requires manual device verification** to confirm current-iOS-version behavior exactly (§7).

### 5.4 Background Sync API — Chromium-only

**Documented platform behavior**, and already correctly documented in-repo: `src/sw.ts` (lines 47–58) states plainly that the Background Sync API has no support in Safari/iOS or Firefox. `capabilities.backgroundSync` (`services.ts` line 67) feature-detects this (`'SyncManager' in window`) and the app never depends on it being present — `initializeMediaSync()`'s `online` event listener is the universal fallback and is what actually does the work on every browser. Safari/Firefox users get a slightly less proactive sync (no "wake up in the background to retry" nudge while the tab is closed), never a broken one.

### 5.5 No tablet-specific breakpoint; iPad gets the desktop 2-column layout

**Code-verified.** `src/styles.css` has exactly one media query, `@media(min-width:760px)`, which switches `.page`/`.dashboard`/`.content`/`.cards` to a 2-column layout capped at `max-width:780px`, centered with side borders. An iPad in portrait (768px CSS width) crosses that breakpoint and gets the same treatment as a narrow desktop window — not a broken layout, but not a tablet-tuned one either, since there's no distinct breakpoint between "phone" and "desktop." `min-height:100dvh` is used throughout (correctly avoids the classic iOS Safari `100vh`-includes-the-address-bar bug), and `.bottom-nav` correctly pads for `env(safe-area-inset-bottom)` for the iPhone home indicator. No `safe-area-inset-top` padding exists, but `apple-mobile-web-app-status-bar-style` is set to `"default"` (not `black-translucent`), which means iOS does not overlay the status bar on content in the first place — so there is currently nothing for a top safe-area inset to compensate for. `orientation: 'portrait-primary'` is set in the manifest; this is honored by Android when installed standalone, but iOS's manifest-driven orientation lock support for home-screen web apps has historically been inconsistent across versions — **requires manual device verification** if landscape use on installed iOS is a real use case.

---

## 6. Build-artifact facts referenced above (from a real `npm run build` in this session)

- `dist/manifest.webmanifest`: `display: "standalone"`, `orientation: "portrait-primary"`, six icon entries (two PNG `"any"`, two PNG `"maskable"`, two SVG `"any"` fallback) — matches Phase 8's fix, still correct.
- `dist/sw.js`: 46 precache entries (3260.56 KiB), generated clean.
- `npm run test` : 53/53 passing (no regression from the 53/53 baseline stated in Phase 12 of `IMPLIMENTATION_STATUS.MD`).
- `npm run test:pwa`: 25/25 passing.
- `npm run typecheck`: clean. `npm run lint`: 0 errors, 6 pre-existing warnings (unchanged).

---

## 7. Action needed from a human with real devices

None of the following can be closed from this sandbox. In priority order:

1. **iOS Safari + iPadOS Safari, current version:** tap "ATTACH EVIDENCE" in the report form and observe the actual action sheet offered for the combined `image/*,video/*,audio/*` accept string (§5.3) — confirm whether a camera/mic capture option appears at all, and on which iOS version behavior changed if it has.
2. **iPad specifically, default configuration (not "Request Mobile Website"):** confirm whether the iOS-install-banner's user-agent sniff (`InstallPrompt.tsx` line 10) actually fires, given iPadOS's default desktop-class UA string (§5.1).
3. **iOS Safari, installed standalone:** confirm actual behavior against the manifest's `orientation: "portrait-primary"` lock when the device is rotated (§5.5).
4. **Any platform:** confirm the video-playback gap in §2.1 is reproducible as described (tap/click a report's video thumbnail and confirm nothing happens) before prioritizing a fix — this is the one finding here likely to matter most to end users.
5. **Desktop Firefox (Windows/macOS/Linux):** confirm no install prompt of any kind appears, matching §5.2, so this is documented as expected rather than investigated as a bug report later.
6. General smoke pass per platform: sign-in, submit a report with each media type, go offline mid-session (airplane mode) and confirm the draft persists, then reconnect and confirm auto-sync — all of which the existing automated suite already covers at the unit/component level (Phases 3, 4, 7) but which has never been exercised end-to-end on real hardware (same gap `IMPLEMENTATION_STATUS.MD` already names for Phase 11's Playwright suite).

---

## 8. What this report does not claim

It does not claim any installation, click, tap, permission dialog, or render was observed on a physical Android, iOS, Windows, macOS, or Linux device or browser in this session — none were available. It does not claim the video-playback and camera-wiring findings in §2 are platform-specific; they are universal code facts, listed here because they were surfaced *by* attempting this validation and materially affect the "Media playback" / "Camera" / "Microphone" items named in the requested scope. Every ⚠️/⛔ platform-limitation entry in §5 is backed by publicly documented browser/OS behavior, not by observation in this sandbox.
