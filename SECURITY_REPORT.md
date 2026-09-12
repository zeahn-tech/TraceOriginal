# TraceNet Security Report

**Review date:** 2026-08-24  
**Scope:** Firestore Rules, Storage Rules, Firebase Authentication, authorization, secrets, input validation, XSS, CSRF, security headers, and rate limiting.

## Executive Summary

The repository contains a useful client-side security boundary and documented security architecture, but it is not production-ready for a public safety application. Firestore Rules, Storage Rules, Firebase Functions, App Check enforcement, rate limiting, and security headers are not present in the repository. Client route guards and client-side role handling must therefore be treated as user experience only, never authorization.

**Release recommendation:** Do not expose production data or privileged workflows until the high and critical findings below are resolved and tested with Firebase Emulator Suite and browser security tests.

## Findings

### SEC-001: Firestore Rules are missing

**Severity:** Critical  
**Evidence:** No `firestore.rules` file or Firebase deployment configuration exists. The client writes directly through `setDoc` and `addDoc` in `src/services.ts`.

Without deployed Rules, the application has no repository-controlled guarantee for authentication, ownership, field allowlists, role checks, immutable fields, or status transitions. A modified browser client could attempt to read or write any exposed collection.

**Required remediation:** Add and deploy `firestore.rules`. Default to deny. Allow public reads only from sanitized `public_*` projections. Require authenticated ownership for citizen records. Require custom claims for approved law enforcement and administrators. Reject client-controlled role, approval, audit, reputation, publication, and server timestamp fields. Add Emulator Suite tests for every role and negative case.

### SEC-002: Storage Rules are missing

**Severity:** Critical  
**Evidence:** No `storage.rules` file exists. The browser uploads directly to paths selected by client code.

Storage paths, MIME types, file sizes, ownership, and read access are not server-enforced in this repository. Evidence and identity documents could be overwritten, read by unauthorized users, or uploaded with unexpected content if backend rules are permissive.

**Required remediation:** Add and deploy `storage.rules` with path-specific ownership and custom-claim checks. Enforce content type and size limits server-side. Keep identity documents and evidence private. Do not make evidence buckets public. Use server-created metadata and a malware/content moderation pipeline before publication.

### SEC-003: Privileged operations trust direct client writes

**Severity:** High  
**Evidence:** `src/App.tsx` calls `write()` directly for report status, alerts, wanted notices, user status, approvals, and operational notes.

Client route guards can be bypassed. A user can invoke the Firebase SDK directly from DevTools unless Rules and/or Functions reject the operation.

**Required remediation:** Route approval, moderation, publication, role changes, alert broadcasting, deletion, audit logging, reputation changes, and protected status transitions through callable or HTTPS Firebase Functions using Admin SDK. Make client guards advisory only. Enforce all authorization in Rules and Functions.

### SEC-004: Authentication is incomplete for production threat models

**Severity:** High  
**Evidence:** Email/password and Google sign-in are implemented in `src/services.ts`, but there is no visible MFA enforcement, email verification gate, account enumeration policy, session-risk handling, reauthentication policy for sensitive actions, or App Check initialization/enforcement.

The client incorrectly accepts a selected access level as UI state, even though signup downgrades ADMIN to CITIZEN. This must not be relied upon as an authorization control.

**Required remediation:** Require verified email where appropriate, enforce MFA for administrators and privileged operators, use reauthentication for destructive/security actions, configure authorized domains and OAuth redirect origins, enable Firebase App Check for web, and issue roles only through a controlled server workflow using custom claims.

### SEC-005: Secrets handling is mostly correct but needs CI safeguards

**Severity:** Medium  
**Evidence:** Firebase web values are read from `VITE_FIREBASE_*`. These values are public Firebase client configuration and are safe to embed, but the project has no lockfile and the Pages workflow uses `npm ci`.

No Admin SDK credentials should enter `VITE_*` variables because Vite embeds them in public assets. A missing lockfile also prevents reproducible deployment.

**Required remediation:** Commit `package-lock.json`, use GitHub Actions secrets/environments for Admin SDK and deployment credentials, add secret scanning, add `.env*` to `.gitignore` except `.env.example`, rotate any credential ever committed, and review built assets for accidental secret exposure.

### SEC-006: Input validation is insufficient at the trust boundary

**Severity:** High  
**Evidence:** Client forms validate required fields but there is no shared runtime schema validation, strict length/range validation, server-side normalization, or server-side media validation in the repository.

Firestore data is cast with TypeScript assertions in `listen()` rather than validated at runtime. Client validation is bypassable.

**Required remediation:** Validate every command and document with shared schemas such as Zod at the Function/Rules boundary. Enforce maximum lengths, enum values, county values, coordinates, timestamps, array counts, and ownership. Validate media MIME, byte size, dimensions, duration, and content after upload. Reject unexpected fields.

### SEC-007: XSS risk is currently low but requires output discipline

**Severity:** Medium  
**Evidence:** No `dangerouslySetInnerHTML`, `innerHTML`, `eval`, or `document.write` usage was found in the web source. React escapes rendered strings by default.

Risk remains if future rich text, imported HTML, URLs, or moderation content is rendered without contextual sanitization. Firebase Storage URLs and user-controlled strings must not be promoted to trusted HTML or script URLs.

**Required remediation:** Keep React text rendering as the default. Sanitize approved rich text with a maintained sanitizer, validate URL schemes and allowed Firebase Storage hostnames, avoid inline script generation, add a restrictive CSP, and test stored-XSS payloads in report, tip, alert, profile, and media fields.

### SEC-008: CSRF exposure depends on the chosen backend interface

**Severity:** Medium  
**Evidence:** Firebase SDK calls use authenticated browser credentials and no custom cookie-backed API is present. No CSRF protection or same-origin command endpoint is implemented in this repository.

Firebase Auth bearer-token requests are not equivalent to a traditional session-cookie application, but future HTTPS Functions using cookies or credentialed cross-origin requests could introduce CSRF.

**Required remediation:** Prefer Firebase SDK/callable Functions with Firebase ID tokens. If session cookies or browser cookies are introduced, require `SameSite=Lax/Strict`, validate `Origin`/`Referer`, add CSRF tokens for state-changing requests, and reject cross-origin credentialed requests. Keep CORS allowlists explicit.

### SEC-009: Security headers are missing

**Severity:** High  
**Evidence:** `index.html` has basic viewport and theme metadata, but GitHub Pages configuration does not provide HTTP response headers and no `_headers` or equivalent host configuration exists.

GitHub Pages cannot apply arbitrary custom response headers for a static project site. This leaves CSP, framing, referrer, MIME sniffing, and permissions policies dependent on the host defaults.

**Required remediation:** Add headers at a reverse proxy/custom host if security headers are required: strict CSP compatible with Firebase/Auth/Storage, `frame-ancestors 'none'`, `base-uri 'self'`, `object-src 'none'`, `Referrer-Policy: strict-origin-when-cross-origin`, `X-Content-Type-Options: nosniff`, `Permissions-Policy`, and HSTS on HTTPS. Do not claim GitHub Pages alone enforces these headers. Test the final deployed headers.

### SEC-010: Rate limiting and abuse controls are missing

**Severity:** High  
**Evidence:** No rate limiter, App Check enforcement, CAPTCHA/abuse workflow, quotas, or server-side throttling exists in the repository.

Public report, tip, alert, authentication, and media endpoints can be abused for spam, cost amplification, storage exhaustion, or denial of service.

**Required remediation:** Put privileged and write workflows behind Functions/API endpoints with per-user, per-IP, and operation-specific quotas. Enable App Check enforcement. Add Firebase Auth abuse protections and email enumeration-resistant responses. Apply upload quotas, maximum counts, cooldowns, and server-side idempotency keys. Monitor Cloud Logging, Auth, Firestore, and Storage usage with alerts.

## Positive Controls Observed

- React rendering avoids the most obvious raw-HTML sinks.
- Firebase web configuration is supplied through `VITE_*`; the architecture correctly identifies it as public configuration.
- The documented architecture separates public projections from private records.
- The media service validates returned URLs as HTTPS Firebase Storage download URLs.
- Client route guards distinguish citizen, approved law-enforcer, and administrator UX states.

These controls do not replace backend Rules, Functions, App Check, or headers.

## Required Production Gate

Before release, add:

- `firestore.rules`, `storage.rules`, and `firebase.json` under version control.
- Firebase Functions for privileged commands, server timestamps, audit logs, moderation, and idempotency.
- Emulator tests covering unauthenticated, citizen, pending officer, approved officer, and admin access.
- App Check initialization and enforced backend rejection tests.
- Runtime schema validation and media scanning/limits.
- Rate-limit and abuse monitoring with documented operational thresholds.
- Security headers at a host that can actually emit them.
- Dependency lockfile, npm audit policy, secret scanning, and protected CI environments.
- XSS/CSRF tests and a deployed-header verification step.

## Residual Risk

The web app is a static client hosted on GitHub Pages. Anyone can inspect and modify it. Security depends on Firebase Rules, Functions, Storage Rules, Auth configuration, App Check, and host-level controls. No client-only role check, hidden route, environment variable, or UI restriction is an authorization boundary.
