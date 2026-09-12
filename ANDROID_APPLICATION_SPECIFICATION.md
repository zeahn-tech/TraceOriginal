# TraceNet Liberia — Complete Android Application Specification

**Document type:** Functional reverse-engineering specification  
**Source:** Uploaded TraceNet Android project (Jetpack Compose)  
**Purpose:** Authoritative blueprint for a future Progressive Web App. This document describes **what the Android application does**, not how to translate its code line-by-line.  
**Web implementation:** Not in scope. No web code is specified here.

---

## 1. Product identity

TraceNet Liberia is a public-safety collaboration platform for Liberia. Citizens, law enforcers, and administrators share incident reports, anonymous tips, wanted/missing notices, emergency alerts, SOS distress signals, maps, analytics, and a verified emergency-contact directory.

**Displayed product names:** TraceNet, TraceNet Liberia, TRACENET LIBERIA  
**Launcher label:** TraceNet  
**Splash tagline:** Securing Communities Together  
**Description string:** TraceNet is a public safety platform where law enforcement and citizens collaborate to find missing persons, locate wanted individuals, and improve community safety with real-time alerts and encrypted communications.

**National context:** Liberia-focused copy, Liberian flag treatment of the word Liberia in branded text, Liberian counties in location pickers, default map/geofence near Monrovia / Liberia National Police HQ.

| Item | Value |
|---|---|
| Gradle namespace | com.example |
| Gradle applicationId | com.aistudio.tracenet.kqjwpz |
| Firebase package_name | com.aistudio.tracenet.kqjwpz |
| Firebase project | tracenet-23a64 |
| Storage bucket | tracenet-23a64.firebasestorage.app |
| Google web client (strings.xml) | 563728002879-5520b613d3b9k9c78j5uh4i9dfdv8gah.apps.googleusercontent.com |
| Min SDK | 24 |
| UI | Jetpack Compose + Material 3 |
| Local DB | Room `tracenet_db` schema version 17 (no destructive migration) |
| Maps | Google Maps SDK (MAPS_API_KEY) |
| AI | Firebase Vertex AI Gemini `gemini-1.5-flash` (JSON, temperature 0.2) |

---

## 2. Brand, theme, and visual language

| Token | Hex | Role |
|---|---|---|
| Primary blue | #1E88E5 | Primary actions, headers |
| Secondary blue | #42A5F5 | Splash gradient start |
| Accent blue | #64B5F6 | Accents |
| Splash/welcome gradient | #42A5F5 → #2196F3 → #1E88E5 | Full-screen branded backgrounds |
| Liberian red | #D32F2F | National red, SOS, emergency |
| Light red | #EF5350 | Light emergency |
| Background | #F5F9FF | App background |
| Surface | #FFFFFF | Cards |
| Text | #212121 | Primary text |
| Text dim | #616161 | Secondary text |
| Error | #D32F2F | Destructive |

Status colors: pending/submitted amber `#FBBF24`; verified/investigation blues; resolved/captured emerald `#10B981`; arrested indigo `#6366F1`; closed gray; false-report red; SOS red.

**Branding rules:** “TraceNet Liberia” often renders TraceNet in red and Liberia in white/on-surface with shadow. Circular logo on splash and welcome. Liberia flag component. Public-share copy uses TRACENET LIBERIA.

**Authenticated dashboard carousel** (auto-advance 4 seconds):
1. Community Safety — Collaborate with local nodes to keep your neighborhood secure.
2. Real-time Alerts — Receive instant notifications about emergency situations nearby.
3. Node Network — TraceNet connects citizens and law enforcement seamlessly.

Public viewing has a separate sliding carousel of public-alert slides.

---

## 3. Roles and security model

### 3.1 Roles (`UserRole`)

| Role | Enum | Who | Default approval |
|---|---|---|---|
| Citizen | CITIZEN | General public | Auto-approved (`isApproved = true`) |
| Law enforcer | LAW_ENFORCER | Police / authorized officers | Not approved until an admin approves |
| Administrator | ADMIN | Platform operators | Cannot be self-assigned at signup |

**Signup security:** Selecting Administrator at registration (email or Google) **forces Citizen** and auto-approves. No public become-admin path.

### 3.2 Account status (`User.status`)

ACTIVE (default), SUSPENDED, BANNED. Sign-in blocked for SUSPENDED or BANNED.

### 3.3 Law-enforcer approval gate

Law-enforcer signup requires badge number, contact number, physical address, and ID card image. Until `isApproved == true`, splash/sign-in **routes the officer to the Citizen dashboard**. Admin Approvals tab: view ID card, Approve or Reject (reject deletes the user locally and in Firestore). Law-enforcer dashboard is role-guarded.

### 3.4 Access-level check at sign-in

User must pick Access Level: Citizen Access, Law Enforcer Access, Administrator Access. If stored role does not match, sign-in is rejected.

### 3.5 Session

Encrypted SharedPreferences `session_prefs` store logged-in user JSON. SessionManager restores on start. Logout clears session. Firebase Auth state is also observed.

### 3.6 Password policy (session prefs)

| Setting | Default | Behavior |
|---|---|---|
| Global password expiry | 90 days | If password age ≥ expiry, blocking Password Expired overlay |
| Mandatory 2FA flag | true | Stored as `global_two_factor_enabled`; not a real TOTP/SMS UI |
| Per-user last password change | timestamp | Set on first read if missing |

**Password Expired overlay (non-dismissible while logged in):** title Password Expired; Current Password; New Password; Confirm New Password; primary **UPDATE & UNLOCK**; Firebase re-auth + password update. Admin can simulate password age.

### 3.7 Biometrics

Manifest USE_BIOMETRIC. BiometricHelper title “TraceNet Security”, subtitle “Log in using your biometric credential”, negative Cancel. User field `biometricEnabled` syncs to Firestore. **Compose screens never invoke the prompt.**

### 3.8 Reputation

`reputationScore` starts at 100. Report status VERIFIED → +10; FALSE_REPORT → −30; DISMISSED → −5. Score below 30 → status SUSPENDED. Audit REPUTATION_UPDATE.

---

## 4. Navigation map

Compose NavHost starts at Splash. Notification deep links can open Map with latitude/longitude (`navigate_to = map`).

| Route | Screen | Auth |
|---|---|---|
| Splash | Splash | No |
| Welcome | Welcome | No |
| SignIn | Sign in | No |
| Login | Sign up | No |
| PublicViewing | Public viewing portal | No (tips need login) |
| CitizenDashboard | Citizen home | Yes (or unapproved LE) |
| LawEnforcerDashboard | Law enforcer home | Approved LAW_ENFORCER |
| AdminDashboard | Admin home | ADMIN |
| PostCriminal | Post wanted/missing notice | LE / Admin |
| SubmitReport | Submit incident report | Logged-in citizen path |
| SubmitTip | Submit anonymous tip | Logged in |
| MapScreen(lat?, lng?) | Incident map | Contextual |
| SOSScreen | SOS distress | Citizen |
| Profile | Profile | Yes |
| Analytics | National analytics | Yes / public entry |
| About | About TraceNet Liberia | Optional |
| EmergencyContacts | Emergency directory | Role-based manage vs view |

**Splash routing (~2500 ms):** no session → Welcome; unapproved LE → Citizen dashboard; CITIZEN → Citizen; approved LAW_ENFORCER → LE dashboard; ADMIN → Admin; else Sign in.

---

## 5. Android permissions

| Permission | Why |
|---|---|
| INTERNET | Firebase, maps, media |
| POST_NOTIFICATIONS | Alerts, reports, SOS, geofence (API 33+) |
| ACCESS_FINE_LOCATION | Report GPS, SOS, map, geofence, nearest stations |
| ACCESS_COARSE_LOCATION | Fallback |
| ACCESS_BACKGROUND_LOCATION | Geofence after foreground grant |
| USE_BIOMETRIC | Helper unused in UI |
| RECORD_AUDIO | Report audio |
| MODIFY_AUDIO_SETTINGS | SOS siren |

Startup: request notifications + fine + coarse; if granted and API ≥ 29, request background location; then register geofence SafeZone_HQ at 6.3006, −10.7969, radius 1000 m. Submit-report GET GPS toast if denied: Location permission required to capture GPS coordinates.

---

## 6. Screens — controls, menus, dialogs

### 6.1 Splash

Full-screen blue gradient, circular logo, fade/scale. Footer: Securing Communities Together. Timeout 2500 ms. No buttons.

### 6.2 Welcome

Gradient, circular logo. Welcome to + TRACENET LIBERIA (TRACENET red, LIBERIA white with shadow). App description.

| Control | Label | Action |
|---|---|---|
| Outlined button | SIGN IN | → Sign in |
| Outlined button | CREATE ACCOUNT | → Sign up |
| Text button | Continue as Public Viewer | → Public viewing |

### 6.3 Sign in

Fields: Email Address; Password (show/hide); Access Level (Citizen Access / Law Enforcer Access / Administrator Access).

| Control | Action |
|---|---|
| Forgot Password? | Reset-link dialog |
| SIGN IN | Email/password Auth; role must match; status ACTIVE; route by role |
| Continue with Google | Google Sign-In → Firebase credential |
| PUBLIC VIEWING PORTAL | → Public viewing |
| Sign-up text | → Sign up |

Forgot Password dialog: enter registered email; send secure reset link; Email field; Send Link (sendPasswordResetEmail); Cancel; loading state.

### 6.4 Sign up

All roles: Full Name, Email Address, Create Password, Select Access Level, Physical Location / Address.  
Law Enforcer extra: Badge Number, Contact Number, ID card image (“Tap to upload ID Card Image”); badge, contact, address, ID required (including before Google).  
Legal: Terms of Service dialog, Privacy Policy dialog, must accept.

| Control | Action |
|---|---|
| SIGN UP | createUserWithEmailAndPassword; upload ID to id_cards/{uid}.jpg; write profile; session |
| Continue with Google | Same rules; Admin coerced to Citizen |
| Sign-in text | → Sign in |

Law enforcer after signup is logged in but treated as citizen until approval.

### 6.5 Public viewing portal

Read-only public safety portal. Top: back; Share. Content: public carousel; official alerts; **verified reports only** (status VERIFIED, not deleted); wanted/missing notices; disclaimer that unverified reports and accused names are withheld; National Analytics; Submit anonymous tip if logged in and category enabled; GPS tap → map; image/video/audio players; SUBMIT TIP or TIPS CLOSED. Share dialog uses system share / email / social; toasts if app missing.

### 6.6 Citizen dashboard

Bottom nav: Home; Alerts (badge of reviewed-not-dismissed); SOS (confirmation first); Map; Profile.

Home: share-app icon; Report shortcut; Public Viewing Portal card; Emergency Directory card (OFFICIAL); stats Active Alerts / Watch Level; My reports (empty: You haven't submitted any reports yet); National Analytics; Submit Anonymous Tip; carousel.

SOS confirmation: Broadcast SOS → SOS screen; Cancel.

My report card: media, status chip, Edit (Title, Description, Save/Cancel), Delete confirm, tap location → map.

Alerts tab: Public Alerts | My Secure Tips. Empty alerts: No current emergency broadcasts. Empty tips: No secure tips sent yet. Chips TIP REVIEWED / PENDING REVIEW.

### 6.7 Submit report

Required Short Title and Detailed Description. Incident date/time tappable. GET GPS. PHOTO (reports/images, 15 MB). VIDEO (reports/videos, 50 MB). AUDIO (reports/audio, 50 MB). Report type dropdown (all ReportType). County dropdown (15 counties). Submit as Anonymous Node checkbox (hides contact). Contact Information if not anonymous. Legal declaration required. Submit disabled until declared. SUBMIT TO TRACENET creates PENDING report, uploads media, AI screens, audit REPORT_SUBMISSION.

Success: Report Submitted Successfully; recommended county Police station; Call {station}; Message on WhatsApp; OK, GOT IT. Failure toast: check connection.

### 6.8 Submit tip

Category dropdown (admin-enabled types only). Tip details. Always anonymous. reportId may be real or category_{TYPE}. Back; Submit; success AlertDialog OK. Disabled categories: TIPS CLOSED.

### 6.9 SOS

Starts looping siren; GPS; writes Alert title `SOS ACTIVE: Citizen Distress Signal`, urgency 3, location Transmitting Citizen Location. Re-transmit title `SOS RE-TRANSMITTED: Citizen Distress`. CALL EMERGENCY CONTACTS → directory. CANCEL SOS stops siren and pops. Notifications for SOS: 🚨 EMERGENCY: {title} with coordinates → map.

### 6.10 Map

Title Incident Map. Google Map markers for reports. Optional focused marker from notification/deep link. Back.

### 6.11 Profile

Header: Help & Support; About. Fields: Full Name; Email (identity); Physical Location / Address; profile photo folder profiles; SAVE CHANGES / SAVING...

CHANGE PASSWORD (Current, New, Confirm, UPDATE/CANCEL). RATE OUR APP (1–5 stars + comment, SUBMIT/CANCEL, thank-you toast). LOGOUT SESSION confirm. Delete My Account: Delete Account permanently? DELETE FOREVER / CANCEL; audit ACCOUNT_DELETED.

Help dialog: email admin, dial support, in-app message → help_messages, toast Help request sent to administrators!, Send / Back / Close. Privacy and Terms dialogs.

### 6.12 About

About TraceNet Liberia. Back. Our Mission. Three Tiers of Access. Privacy & Anonymity. Verified Alerts Only. Open, But Accountable.

### 6.13 Emergency contacts

Citizen: search; county filter; category chips All, Police, Traffic, Criminal Investigation, Women & Children, Emergency Response, Fire Service, Ambulance, Immigration; Nearest Police Stations if GPS; hide isDisabled; verified badge; Call Now, WhatsApp, Email, Directions.

Staff (LE/Admin): Add Contact; Edit/Delete; see disabled; verify/disable toggles. Persistence SharedPreferences emergency_contacts_prefs / contacts_json (**not Firestore**). Seeded with 11 verified Liberian agencies (LNP HQ, ERU, Anti-Robbery, WACPS, Traffic, Grand Bassa, Nimba, LDEA, Fire Service, LIS/RIA, Red Cross ambulance).

Add/Edit: Add Verified Contact / Edit Verified Contact. Fields: Agency, Office/Division/Station, County, District/City, Category, Contact Officer, Position, Phone, WhatsApp, Email, Address, Hours, Emergency Availability (24/7 vs office hours), Live Availability (Available/Busy/Offline), Lat, Lng, Verified switch, Disable switch. Cancel; Save Details.

### 6.14 Analytics

Filter by county. Charts: Crime Rates by County; Safest Counties; Wanted names vs report text; Crime Status Overview; Monthly Trends; Distribution by Type; Operational Outcomes; totals Total Reports, TraceNet Users, Wanted Criminals; Peak Day/Month/Week; Highest Crime by Type per County. Outcome labels: Arrested, Found, Resolved, Active, Under Investigation, Pending.

### 6.15 Law enforcer dashboard

Tabs: Incidents | Tips | Wanted | Contacts. Chrome: Post Criminal; National Analytics; Public Viewing Portal.

Incidents: media; status SUBMITTED, UNDER_REVIEW, VERIFIED, DISMISSED, FALSE_REPORT; Notes; Assign; Merge (non-CLOSED others); Escalate; Contact Reporter; Delete Incident?.

Tips: select all; bulk Delete (n); mark reviewed; empty No anonymous tips received.

Wanted: category chip default Wanted person notices; statuses ACTIVE, ARRESTED, CAPTURED, COLD CASE, SUBMITTED, VERIFIED, DISMISSED.

Contacts: Open Contacts Manager.

### 6.16 Post criminal

Criminal Name; Information/Crimes; Last Seen Location; Reward optional; Category: Missing person alerts, Wanted person notices (default), Stolen property alerts, Witness requests, Emergency warnings; SELECT IMAGES up to 4 → Storage criminals; BROADCAST ALERT creates WantedCriminal status SUBMITTED, isVerified false. Failure toast Failed to broadcast alert. Audit CRIMINAL_POSTED. Admin must verify before public-verified.

### 6.17 Admin dashboard

Tabs: Approvals, Reports, Tips, Wanted, Alerts, Users, Contacts, Stats, Logs, Settings, Trash, Support. Badges: pending reports, unreviewed tips, pending wanted, unread help.

Approvals: pending LE with ID card Reject/Approve; pending wanted Reject/Approve.

Reports: edit title/description; status PENDING, ACTIVE, VERIFIED, RESOLVED, ARRESTED, FOUND, CLOSED; soft delete.

Tips: select all, bulk delete, mark reviewed.

Wanted: edit Name, Description, Last Seen, Reward; verify/status/trash.

Alerts: list; create Alert Title, Affected Area, Lat, Lng, Broadcast Message, urgency; edit; Delete Public Alert?.

Users: change role; status ACTIVE / SUSPENDED / BANNED; Delete User.

Contacts: Open Contacts Manager.

Stats: reporting statistics, wanted counts, reports by status.

Logs: select all, bulk delete, restore, permanent delete.

Settings: App Categories (enable/disable ReportType, Select All/Deselect All, Save); Security Settings (Mandatory 2FA, Password Expiry days, simulate age, Save Changes); AI Configuration (sensitivity 0–100%, default 70%, Apply); Cloud Synchronization (syncNow); System Maintenance (Full Database Backup, Cleanup Old Logs).

Trash: Deleted Reports, Wanted, Tips, Audit Logs; Restore; Delete Forever.

Support: unread highlight; Mark Read/Unread; Reply email subject TraceNet Admin Support Reply; Delete Message.

### 6.18 Shared dialogs

Delete confirmation (DELETE/CANCEL). Share app. Terms ACCEPT. Privacy DISMISS. Merge reports. Assign investigator. Internal notes. Contact reporter OK. Password expired UPDATE & UNLOCK. SOS confirm.

---

## 7. Workflows

**Citizen report:** sign in → submit form → local media then upload → Room + Firestore reports/{id} → AI CLEAN/FLAGGED → notification New Incident Reported → success Call/WhatsApp county police → My Reports edit/delete.

**Anonymous tip:** home/portal/report SUBMIT TIP → enabled category → tips collection anonymous → AI screen → LE/Admin review → Alerts My Secure Tips.

**SOS:** confirm → siren + GPS + Alert urgency 3 → notify all → re-transmit / call directory / cancel.

**Wanted notice:** LE/Admin Post Criminal → SUBMITTED unverified → Admin Approve VERIFIED or Reject → public portal shows verified → LE can mark ARRESTED/CAPTURED/COLD CASE → trash/restore.

**LE onboarding:** signup + id_cards/{uid} → isApproved false citizen-mode → Admin Approve LAW_ENFORCER_APPROVED or Reject delete user → next login LE dashboard.

**Investigation:** status sync; assign; notes; escalate; merge; reputation on VERIFIED/FALSE_REPORT/DISMISSED; contact reporter if not anonymous.

**Admin broadcast:** alert with location + urgency → alerts → local notifications.

**Account:** Google/email → users/{uid}; profile photo profiles/; reset email; expiry lock; delete removes Room + Firestore user.

---

## 8. Data model (Room)

**users:** id (Auth UID), name, email, role, isApproved, profileImageUrl, biometricEnabled, badgeNumber, idCardUrl, contact, address, status (ACTIVE/SUSPENDED/BANNED), reputationScore (default 100).

**reports:** id, title, description, type, county, latitude, longitude, timestamp, reporterId, isAnonymous, status, imageUrls, videoUrls, audioUrls, incidentTimestamp, aiScreeningStatus (NOT_SCREENED/CLEAN/FLAGGED), aiFlaggedReasons, internalNotes, assignedInvestigator, isEscalated, contactInfo, isDeleted.

**tips:** id, reportId, content, isAnonymous default true, timestamp, submitterId, isReviewed, reviewTimestamp, aiScreeningStatus, aiFlaggedReasons, isDeleted.

**alerts:** id, title, content, urgency 1/2/3, locationName, county, latitude, longitude, timestamp. Hard delete.

**wanted_criminals:** id, name, description, lastSeen, county, reward, imageUrls, isArrested, timestamp, isDeleted, category default Wanted person notices, status SUBMITTED/VERIFIED/DISMISSED plus UI extras, isVerified.

**audit_logs:** id, action, description, userId, userName, timestamp, isDeleted.

**help_messages:** id, senderName, senderEmail, senderContact, message, timestamp, isRead. Hard delete.

**Emergency contacts:** not Room; EmergencyContact + VerifiedContacts seed; local JSON prefs.

---

## 9. Firebase

### Auth

Email/password (sign up, sign in, change, reset, delete). Google Sign-In. Auth state listener.

### Firestore collections (doc id = entity id)

users, reports, tips, alerts, wanted_criminals, audit_logs, help_messages.

Pattern: Room first, then set full document. Realtime addSnapshotListener; ignore pending writes; REMOVED deletes local. Bootstrap syncLocalDatabaseToFirebase. Fetch user by id or email query.

No custom Firestore persistence API; Android default disk cache. Room is the real offline source of truth.

### Field maps

users: id, name, email, role, isApproved, profileImageUrl, biometricEnabled, badgeNumber, idCardUrl, contact, address, status, reputationScore

reports: id, title, description, type, county, latitude, longitude, timestamp, reporterId, isAnonymous, status, imageUrls, videoUrls, audioUrls, incidentTimestamp, aiScreeningStatus, aiFlaggedReasons, internalNotes, assignedInvestigator, isEscalated, contactInfo, isDeleted

tips: id, reportId, content, isAnonymous, timestamp, submitterId, isReviewed, reviewTimestamp, aiScreeningStatus, aiFlaggedReasons (Room isDeleted may be missing from map)

alerts: id, title, content, urgency, locationName, county, latitude, longitude, timestamp

wanted_criminals: id, name, description, lastSeen, county, reward, imageUrls, isArrested, timestamp, isDeleted, category, status, isVerified

audit_logs: id, action, description, userId, userName, timestamp

help_messages: id, senderName, senderEmail, senderContact, message, timestamp, isRead

### Storage folders

| Path | Content |
|---|---|
| profiles/{uuid}.jpg | Profile photos |
| id_cards/{userId}.jpg | Officer ID |
| criminals/{uuid}.{ext} | Wanted/missing images |
| reports/images/{uuid}.{ext} | Report photos (also byte retry) |
| reports/videos/{uuid}.{ext} | Report video |
| reports/audio/{uuid}.{ext} | Report audio |

Images max 15 MB; video/audio max 50 MB. Cache-Control public, max-age=31536000. Failed upload keeps local URI until sync rewrites to download URLs.

No storage for contacts, alerts, tips text, audit logs, help messages.

---

## 10. Authentication flows

1. Splash restores encrypted session.
2. Email sign-up → Auth → optional ID upload → users doc → session.
3. Email sign-in → Auth → local then cloud user → status/role checks → session.
4. Google → ID token → credential → existing or create Citizen/LE profile.
5. Forgot password → reset link email (not in-app OTP).
6. Change password → reauth + updatePassword; refresh last-changed.
7. Forced expiry overlay.
8. Sign out → Auth signOut + clear prefs.
9. Delete account → delete Firestore user + local user + session.

---

## 11. Notifications

Channel id tracenet_alerts_channel, name TraceNet Real-Time Alerts, description Notifications for urgent security alerts and incidents, HIGH.

Local observers (not FCM in this codebase):

| Event | Title | Opens |
|---|---|---|
| New alert non-SOS | New TraceNet Alert: {title} | Map if extras |
| New SOS | 🚨 EMERGENCY: {title} | Map with lat/lng |
| New report | New Incident Reported: {title} | default |
| Geofence ENTER | Safety Zone Alert | — |

Tap extras: navigate_to=map, latitude, longitude. IDs rotate via prefs counter.

---

## 12. Offline behaviour

Room is the UI source. Writes hit Room first then Firestore set. Media copied internally then uploaded; local URI on failure; later upload replaces URLs. Listeners skip pending writes. Manual admin syncNow. No custom Firestore queue UI. Contacts fully offline. Session remains until logout. AI falls back to keywords. Maps/GPS/WhatsApp/phone need device features.

---

## 13. Media workflow

Picker → MediaStorageHelper internal copy → FirebaseStorageManager upload → Coil/SafeImage http or file → in-app video/audio players → galleries on cards.

---

## 14. AI screening

Firebase Vertex AI gemini-1.5-flash JSON temperature 0.2. After report and tip submit. Checks insults, threats, hate, revenge, nonsense. JSON status CLEAN or FLAGGED plus reasons. FLAGGED report → PENDING_REVIEW; tip stays unreviewed.

Local fallback: abuse words (fuck, shit, bitch, asshole, bastard, idiot, stupid, dumb, jerk, crap, cunt, dick, abuse, abusive, harass); threats (kill, die, shoot, attack, bomb, murder, harm, destroy, burn, assassinate, stab, threat); hate (slur, hate, scum, trash); text length < 5 → Suspiciously Short Content.

Admin AI slider default 0.7 is in-memory product setting.

---

## 15. Report types, counties, statuses

**ReportType:** MISSING_PERSON, WANTED_INDIVIDUAL, SUSPICIOUS_ACTIVITY, CRIME_REPORT, DRUGS_DEALER, HIT_AND_RUN, THEFT, BURGLARY, ARMED_ROBBERY, KIDNAPPING, FRAUD, VANDALISM, RAPE, CAR_ACCIDENT, SUICIDE, DOMESTIC_VIOLENCE, ASSAULT.

**Counties:** Bomi, Bong, Gbarpolu, Grand Bassa, Grand Cape Mount, Grand Gedeh, Grand Kru, Lofa, Margibi, Maryland, Montserrado, Nimba, River Cess, River Gee, Sinoe.

**Status vocabularies (not fully unified — all must be preserved by role UI):**
- Entity comment: PENDING, UNDER_INVESTIGATION, RESOLVED, ARRESTED, CLOSED
- Citizen chips also VERIFIED
- LE incidents: SUBMITTED, UNDER_REVIEW, VERIFIED, DISMISSED, FALSE_REPORT
- Admin reports: PENDING, ACTIVE, VERIFIED, RESOLVED, ARRESTED, FOUND, CLOSED
- Analytics: ARRESTED, FOUND, RESOLVED, ACTIVE, VERIFIED+UNDER_INVESTIGATION, PENDING
- Wanted entity: SUBMITTED, VERIFIED, DISMISSED plus UI ACTIVE, ARRESTED, CAPTURED, COLD CASE
- AI: NOT_SCREENED, CLEAN, FLAGGED; report PENDING_REVIEW after flag

---

## 16. Audit actions observed

SECURITY_SETTINGS_UPDATED, CATEGORIES_CONFIG_UPDATED, AI_CONFIG_UPDATED, USER_STATUS_UPDATE, USER_ROLE_UPDATED, USER_DELETED, CRIMINAL_POSTED, CRIMINAL_UPDATED, CRIMINAL_SOFT_DELETE, CRIMINAL_VERIFICATION, CRIMINAL_STATUS_UPDATE, CRIMINAL_RESTORE, CRIMINAL_PERMANENT_DELETE, REPORT_SUBMISSION, REPORT_SOFT_DELETE, REPORT_RESTORE, REPORT_PERMANENT_DELETE, TIP_SUBMISSION, TIP_SOFT_DELETE, TIP_RESTORE, TIP_PERMANENT_DELETE, MULTI_TIP_SOFT_DELETE, MULTI_TIP_PERMANENT_DELETE, TIP_REVIEWED, ALERT_SUBMITTED, ALERT_DELETED, ALERT_UPDATED, LAW_ENFORCER_APPROVED, LAW_ENFORCER_REJECTED, REPORT_STATUS_UPDATE, REPUTATION_UPDATE, REPORTS_MERGED, INVESTIGATOR_ASSIGNED, INTERNAL_NOTES_UPDATED, REPORT_ESCALATED, REPORT_DE_ESCALATED, PROFILE_UPDATED, ACCOUNT_DELETED, HELP_MESSAGE_SUBMITTED, HELP_MESSAGE_DELETED.

---

## 17. Citizen features

Welcome/sign in/up/Google/reset; public viewing without account; dashboard carousel and share; geo-tagged multimedia reports; Call/WhatsApp police after submit; anonymous tips; my reports view/edit/delete; alerts + my tips; SOS; map; emergency directory; analytics; profile/photo/password/rate/help/legal/logout/delete; local notifications; offline Room+media.

Cannot: approve officers, broadcast official alerts, verify wanted notices, manage users, see internal notes, assign investigators, access admin trash/settings/audit, open LE dashboard.

---

## 18. Law enforcement features

Public portal verification; incidents investigation tools; tips inbox bulk delete/review; wanted board + Post Criminal; emergency contacts management; analytics; profile. Blocked until approval (citizen-mode). Cannot approve other enforcers, change global security/AI/categories, manage all users, or use admin trash/settings/support inbox.

---

## 19. Admin features

All LE features plus: approve/reject enforcers and wanted broadcasts; user directory role/status/delete; broadcast/edit/delete alerts; category enablement, 2FA flag, password expiry, AI sensitivity, cloud sync, maintenance; audit logs; global trash; support inbox. Admins are not created via public signup.

---

## 20. Geofencing

Receiver GeofenceBroadcastReceiver (exported false). Transition ENTER. Sample fence SafeZone_HQ 6.3006, −10.7969, 1000 m. Notification Safety Zone Alert. Requires fine + background location.

---

## 21. Notes for a future PWA (non-code)

Dual write Room+Firestore becomes Firestore + browser persistence with the same collection names and Storage paths. Emergency contacts are device-local in Android. Categories/AI/2FA are mostly local/in-memory except password expiry/2FA prefs. Notifications are local observers, not FCM. Role labels: Citizen, Law Enforcer, Administrator. Liberia-only 15 counties. Public portal must not show unverified reports. SOS is an Alert urgency 3, not a separate collection. Tips may use synthetic category_{TYPE} report ids.

---

## 22. Screen inventory

1. Splash  
2. Welcome  
3. Sign In (+ Forgot Password)  
4. Sign Up (+ Terms, Privacy, ID upload)  
5. Public Viewing Portal (+ Share)  
6. Citizen Dashboard (Home / Alerts / SOS confirm / Map / Profile)  
7. Submit Report (+ success Call/WhatsApp)  
8. Submit Tip (+ success)  
9. SOS  
10. Map  
11. Profile (+ password, rate, help, legal, logout, delete)  
12. About  
13. Emergency Contacts (+ add/edit for staff)  
14. Analytics  
15. Law Enforcer Dashboard (4 tabs + dialogs)  
16. Post Criminal  
17. Admin Dashboard (12 tabs + settings/alert/user dialogs)  
18. App-wide Password Expired overlay  

---

*End of specification. Generated from the Android source as the functional blueprint. No Progressive Web App code is included.*
