# TraceNet Liberia — Visual Design System

**Document type:** Visual extraction specification  
**Source:** Uploaded TraceNet Android project (Jetpack Compose + Material 3)  
**Purpose:** Exact visual reconstruction of the Android application as a Progressive Web App.  
**Web implementation:** Not in scope. No web code is specified here.

---

## Fidelity rule

This is an extraction of the existing Jetpack Compose interface. A PWA implementing it must preserve the Android product’s appearance, component hierarchy, labels, visual density, visual states, motion, and layout. Do not substitute another design system, introduce a new typeface, introduce desktop-first navigation, change spacing, flatten gradients, restyle cards, or otherwise modernize the look.

Compose dimensions below are density-independent pixels (`dp`). Type sizes are scale-independent pixels (`sp`). Map these 1:1 to CSS pixels at the Android reference density. Keep the Android-style mobile column; do not redesign for large screens.

---

## 1. Brand identity

### 1.1 Product names

| Surface | Exact treatment |
|---|---|
| Launcher label | TraceNet |
| Wordmark on blue headers / splash / welcome / auth | `TRACENET` in **Android `Color.Red` (`#FF0000`)**, weight Black, plus a space, plus `LIBERIA` in **white**, weight Black, **black text shadow blur 4** |
| Tracking on that lockup | `-0.5 sp` |
| Citizen / welcome style | `headlineMedium` (24 / 32 / Bold / tracking -0.2) with extra tracking -0.5 |
| Admin / law-enforcer header | `headlineSmall` (Material default), max 1 line, ellipsis |
| Phrase highlighter | Any string containing `TraceNet Liberia` renders `TraceNet ` in `#FF0000` and `Liberia` in the passed color (default white) with black shadow blur 4 |
| Splash tagline | `Securing Communities Together` — `bodyMedium`, white at 60% opacity, weight Medium, 48 dp from bottom |
| About subtitle | `Government-Grade Civic Platform` — `bodyLarge`, secondary blue |
| Auth footer | `POWERED BY` — `labelSmall`, white 40% opacity, Medium; then `Zeahn's Tech` — `titleMedium`, white, ExtraBold |
| Version on About | `Version 1.0.0` — `labelSmall`, outline color |

Do not replace `#FF0000` wordmark red with Liberian flag red `#D32F2F`. The flag uses `#D32F2F`; the product lockup uses `Color.Red`.

### 1.2 Logo

**File:** `app/src/main/res/drawable/img_tracenet_logo_1783258216661` (JPEG referenced by that resource name)

| Placement | Size | Clip | Scale |
|---|---|---|---|
| Splash, Welcome, Sign In, Sign Up | 200 dp × 200 dp | `CircleShape` | Splash default crop-as-drawn; Welcome `ContentScale.Fit` |
| Dashboard carousel slide 3 | Full 300×180 card | Card 16 dp | `ContentScale.Crop` |
| Adaptive launcher foreground | 72 dp centered on black background | Adaptive icon | As XML |

**Launcher:** adaptive icon, background `#000000`, foreground the TraceNet logo. Round and square variants use the same layers. Notification small icon is `ic_launcher_foreground`.

Do not redraw, recolor, or substitute this logo.

### 1.3 Other artwork (do not replace)

Referenced by resource name. Copy the existing bitmap files; do not generate replacements.

| Resource | Use |
|---|---|
| `img_tracenet_logo_1783258216661` | Logo, carousel slide 3, launcher |
| `img_liberia_police_banner_1783259052567` | Dashboard carousel “Community Safety” |
| `img_hero_banner_1783106015108` | Dashboard carousel “Real-time Alerts” |
| `img_liberia_police_logo_1784771614492` | Public viewing header police mark |
| `img_crime_scene_1783453712511` | Public carousel |
| `img_forensic_investigation_1783448806634` | Public carousel |
| `img_theft_monitoring_1783448820563` | Public carousel |
| `img_emergency_response_1783448835242` | Public carousel |
| `img_robbery_intercept_1783448848720` | Public carousel |
| `img_tactical_rescue_1783448861647` | Public carousel |
| `img_cybercrime_tracker_1783448875037` | Public carousel |
| `ic_google` | 24×24 vector Google “G” (`#EA4335`, `#34A853`, `#FBBC05`, `#4285F4`) |

Carousel and editorial images use **crop fill**. Logo identity uses **fit** inside a circle.

### 1.4 Liberia flag (`LiberiaFlag`)

Custom Canvas, **not an emoji and not a PNG**.

| Property | Value |
|---|---|
| Size | 38 dp × 20 dp |
| Clip | `RoundedCornerShape(2.dp)` |
| Stripes | 11 equal horizontal stripes |
| Stripe colors | Even index `#D32F2F`, odd index `#FFFFFF` |
| Canton | Square, height = 5 stripe heights, color `#0F47AF` |
| Star | 5-point star, outer radius 30% of canton, inner radius 40% of outer, white, starts at -90° |

On dashboard headers the flag sits **2 dp** above the wordmark, then **4 dp** spacer.

---

## 2. Color system

### 2.1 Theme tokens (`Color.kt` + `Theme.kt`)

Light and dark schemes are **the same values**. System dark mode does not invert the UI. Do not invent a true dark theme.

| Token | Hex | Material 3 role |
|---|---|---|
| TraceNetPrimary | `#1E88E5` | primary |
| TraceNetSecondary | `#42A5F5` | secondary |
| TraceNetAccent | `#64B5F6` | tertiary |
| LiberianRed | `#D32F2F` | national red; error |
| LiberianLightRed | `#EF5350` | light emergency |
| TraceNetBackground | `#F5F9FF` | background |
| TraceNetSurface | `#FFFFFF` | surface |
| TraceNetText | `#212121` | onBackground, onSurface |
| TraceNetTextDim | `#616161` | onSurfaceVariant |
| TraceNetError | `#D32F2F` | error |
| onPrimary / onSecondary / onTertiary / onError | `#FFFFFF` | |
| surfaceVariant | `#E3F2FD` | chips, image fallback, selected menu fill |
| XML launcher unused Material samples | purple/teal in `colors.xml` | **not used by Compose UI** |

### 2.2 Brand gradient (splash, welcome, sign-in, sign-up, dashboard headers)

Linear gradient, source order:

1. `#42A5F5`
2. `#2196F3`
3. `#1E88E5`

Used full-screen on unauthenticated screens and as the **top app header** on Citizen, Law Enforcer, and Admin dashboards.

### 2.3 Form field literals (repeated across auth and operational forms)

| Role | Hex |
|---|---|
| Field text | `#212121` |
| Focused border, focused label, cursor | `#1E88E5` |
| Unfocused border | `#BDBDBD` |
| Unfocused label | `#757575` |
| Field fill | `#FFFFFF` |
| Dropdown menu surface | `#F8F9FA` |
| Unselected county/type chip | `#E3F2FD` |
| Chip text unselected | `#0D47A1` |
| Chip text selected | `#FFFFFF` on `#1E88E5` |

### 2.4 Dialog literals

| Role | Hex |
|---|---|
| AlertDialog container | `#EEEEEE` |
| Title / body | `#212121` |
| Confirm DELETE | error `#D32F2F` |
| CANCEL | default primary text button |

### 2.5 Semantic / status colors used in screens

| Meaning | Hex / token |
|---|---|
| Active (wanted/public) | `#3B82F6` |
| Verified / resolved / captured / arrested (emerald) | `#10B981` |
| Resolved chip text (citizen) | `#059669` |
| Submitted / pending amber | `#FBBF24` |
| Cold case | `#6B7280` |
| Active location pulse | `#22C55E` |
| Audio / mic accent | `#10B981` |
| Phone-call action | `#1565C0` |
| WhatsApp action | `#15803D` |
| Disabled slate | `#94A3B8` |
| Disabled fill | `#F1F5F9` |
| Hairline | `#E2E8F0` |
| Success text (password overlay) | `#2E7D32` |
| Active category badge fill/text | `#E8F5E9` / `#2E7D32` |
| Inactive category badge | `#FFEBEE` / `#C62828` |
| Selected list row | `#E3F2FD` with `#0D47A1` text |
| Public SOS banner fill | `#FDE8E8` |
| Gray body on public cards | `#374151` |
| Enforcer tab outline | `Color.Red` 1 dp, selected fill red 20% |
| Admin selected tab | `#2196F3` at 85% fill, 80% border |
| Admin unselected tab border | `#2196F3` at 30% |
| Pending-enforcer banner | `secondaryContainer` |
| Disabled submit | `Color.Gray` or gray 50% |

### 2.6 Emergency-contact category colors

| Category | Icon (Material filled) | Color |
|---|---|---|
| Police | LocalPolice | `#3B82F6` |
| Traffic | Traffic | `#F59E0B` |
| Criminal Investigation | Security | `#8B5CF6` |
| Women & Children | Face | `#EC4899` |
| Emergency Response | Warning | `#D32F2F` |
| Fire Service | LocalFireDepartment | `#F97316` |
| Ambulance | LocalHospital | `#10B981` |
| Other | Shield | `#64748B` |

GPS “near you” chip: fill `#D1FAE5`, border `#10B981` at 40%.

### 2.7 Public-viewing share row (literal brand colors)

| Channel | Background |
|---|---|
| Facebook | `#1877F2` |
| WhatsApp | `#25D366` |
| Instagram | linear `#833AB4` → `#FD1D1D` → `#F77737` |
| YouTube | `#FF0000` |
| Gmail | `#EA4335` |
| Messenger | `#0084FF` |

Tiles: 12 dp corner radius.

### 2.8 Public carousel accent colors

| Slide | Color |
|---|---|
| Crime scene / forensic | `#FF3B30` |
| Theft monitoring | `#3B82F6` |
| Emergency response | `#EC4899` |
| Robbery intercept | `#F59E0B` |
| Tactical rescue | `#8B5CF6` |
| Cybercrime tracker | `#10B981` |

### 2.9 Analytics chart palettes (exact sequences)

**Crime rates by county**

`#1E88E5`, `#00ACC1`, `#0288D1`, `#0097A7`, `#1976D2`, `#00838F`, `#039BE5`, `#00B8D4`, `#3F51B5`, `#00E5FF`

**Safest counties**

`#10B981`, `#00897B`, `#43A047`, `#00BFA5`, `#2E7D32`, `#7CB342`, `#059669`, `#0F766E`, `#66BB6A`, `#1DE9B6`

**Crime types**

`#FB8C00`, `#FFB300`, `#F57C00`, `#FF8F00`, `#E65100`, `#FFA000`, `#FF6D00`, `#FFAB00`, `#EF6C00`, `#FFD600`

**Wanted criminals**

`#E53935`, `#D81B60`, `#C62828`, `#AD1457`, `#B71C1C`, `#E91E63`, `#FF1744`, `#F44336`, `#880E4F`, `#FF4081`

**Distribution pie**

`#8E24AA`, `#3949AB`, `#5E35B1`, `#6A1B9A`, `#4A148C`

### 2.10 Overlays

| Overlay | Treatment |
|---|---|
| Carousel text scrim | vertical transparent → black 80% |
| Post-criminal upload blocker | black 50%, blocks clicks |
| Password-expired gate | black 92%, full viewport |
| Image load | surfaceVariant 50% + 24 dp primary spinner, stroke 2 dp |
| Image error | surfaceVariant fill, 40 dp Image icon at 60% onSurfaceVariant, caption “Image unavailable” |
| Video player | black 12 dp rounded; 48 dp white Play; error white 50% warning + retry |
| SOS pulse ring | tertiary (`#64B5F6`) at 15% alpha |

---

## 3. Typography

**Family:** `FontFamily.Default` (Android system sans-serif). Do not load Inter, Roboto-as-webfont, or a display face.

Only these five Material 3 slots are customized (`Type.kt`). All other slots (`headlineSmall`, `titleMedium`, `bodyMedium`, `bodySmall`, `labelLarge`, etc.) are **stock Material 3 defaults**.

| Token | Size | Line height | Tracking | Weight |
|---|---|---|---|---|
| headlineLarge | 32 sp | 40 sp | -0.5 sp | Light |
| headlineMedium | 24 sp | 32 sp | -0.2 sp | Bold |
| titleLarge | 18 sp | 24 sp | 0 | SemiBold |
| bodyLarge | 16 sp | 24 sp | +0.5 sp | Normal |
| labelSmall | 10 sp | 16 sp | +0.2 sp | Bold |

### 3.1 Recurring local overrides

| Use | Treatment |
|---|---|
| `TRACENET LIBERIA` lockup | Black weight; `LIBERIA` white + shadow |
| Auth page titles `SIGN IN` / `SIGN UP` | `headlineLarge` + Black + white |
| Sign-up eyebrow `NODE REGISTRATION` | `labelSmall`, white 70% |
| Node eyebrows (`REPORT NODE`, `ADMIN NODE`, `EMERGENCY NODE`) | `labelSmall`, primary or tertiary |
| Operational titles (`POST WANTED CRIMINAL DATA`, `SOS ACTIVE`) | Bold / Black, primary or tertiary |
| `NATIONAL ANALYTICS` app bar | `titleLarge`, Black, primary |
| Map title `Incident Map` | `#1E88E5` |
| Primary buttons | uppercase, Bold (`titleMedium` or `labelLarge`) |
| Bottom nav labels | `labelSmall` |
| Carousel title | `titleLarge` + Bold 18 sp, white |
| Carousel body | `bodySmall`, white 80%, max 2 lines |
| `NEW` badge | `labelSmall` Bold white |
| Stat numbers | `headlineMedium` |
| Anonymous-tip card title | `titleLarge` forced to 14 sp, white |
| Anonymous-tip body | `bodyLarge` forced to 12 sp, white 80% |
| About body | `bodyMedium`, line height 20 sp on sections |

---

## 4. Shape, elevation, spacing

### 4.1 Material shapes (`Shape.kt`)

| Token | Radius |
|---|---|
| extraSmall | 4 dp |
| small | 8 dp |
| medium | 12 dp |
| large | 16 dp |
| extraLarge | 24 dp |

Do not collapse these to a single radius.

### 4.2 Recurring radii (as coded, not only theme tokens)

| Element | Radius |
|---|---|
| Flag | 2 dp |
| Status dots / badges | 4–6 dp |
| Thumbnails, close chips, small pills | 8 dp |
| Auth outlined CTAs, sign-in fields, citizen FAB, many icon buttons | 12 dp |
| Admin header profile | 12 dp |
| Law-enforcer header profile | 16 dp |
| Sign-up / report / tip / post-criminal fields, SOS buttons | 16 dp |
| Dropdown menu | often 12 dp field + 8 dp inner chips |
| Tip success dialog | 24 dp |
| Dashboard promo / analytics / stat cards | 24 dp |
| Anonymous tip banner | 32 dp |
| Admin/LE bottom action pills | 14 dp |
| Public carousel card | 20 dp, elevation 6 dp |
| FAB height (citizen Report) | 40 dp |
| Primary button height | 56 dp |
| Google / outlined auth | 56 dp |
| Admin bottom actions | 48 dp |
| Icon buttons | 40–48 dp |

### 4.3 Elevation / borders

| Element | Elevation / border |
|---|---|
| Dashboard carousel card | 4 dp elevation; 1.5 dp primary at 50% |
| Public carousel card | 6 dp |
| Admin bottom bar | tonal 8 dp, shadow 16 dp |
| Upload progress card | tonal 8 dp |
| SOS HELP button | shadow 20 dp |
| Delete dialog | default AlertDialog + tonal 6 dp on SOS confirm |
| Generic cards | 1 dp or 1.5 dp low-contrast / primary-alpha borders |
| Welcome/auth outlined buttons | 1.5 dp white |
| Public viewing portal button on sign-in | 1 dp white 30% |
| Enforcer tabs | 1 dp red |
| Media attach tiles | 1 dp onSurface 15% |
| Emergency category chip | 1 dp category color 30%; 1.5 dp if emergency |

### 4.4 Spacing rhythm

Common spacers: **2, 4, 6, 8, 12, 16, 20, 24, 32, 48, 64 dp**.

| Context | Padding |
|---|---|
| Welcome / splash / auth column | 32 dp |
| Form screens (report, tip, post criminal) | 24 dp; back row bottom 32 dp |
| Dashboard content | horizontal 16 dp; item gap 16 dp |
| Citizen header | horizontal 10 dp, vertical 20 dp |
| Admin / LE header | horizontal 20 dp, vertical 12 dp |
| About | 24 dp |
| Analytics list | 16 dp content padding, 24 dp item gap |
| Map missing-key state | 32 dp |
| Card interiors | 16–20 dp |
| Password-expired overlay | 24 dp outer and inner |
| Empty tips state | vertical 60 dp |

Safe areas: `statusBarsPadding` + `navigationBarsPadding` on headers and full-screen forms; `imePadding` on auth/forms.

---

## 5. Icons

Use **Material Icons filled** (and AutoMirrored ArrowBack / ArrowForward) as in Compose. Do not switch to outline-only or a custom icon font.

Recurring glyphs: Home, Notifications, Warning (SOS), Map, Person, Share, Add, Analytics, ArrowForward, ArrowBack, ArrowDropDown, DateRange, LocationOn, Mic, PlayArrow, Close, Phone, Send, Email, CameraAlt, BarChart, ChevronRight, Info, HelpOutline, Edit, Password, Star, ExitToApp, Delete, Visibility / VisibilityOff, Lock, CheckCircle, VerifiedUser, Image, LocalPolice, Traffic, Security, Face, LocalFireDepartment, LocalHospital, Shield, Search, Clear, MyLocation, GpsFixed, Explore, DirectionsRun, Badge, Home (address), AssignmentInd, MergeType, NoteAdd, Flag / OutlinedFlag, GppBad, EditNote, PieChart.

Google sign-in uses `ic_google`, 24 dp, 12 dp gap before label.

---

## 6. Motion

| Motion | Spec |
|---|---|
| Splash fade | alpha 0 → 1, tween **1500 ms** |
| Splash scale | 0.8 → 1, spring MediumBouncy, Low stiffness |
| Splash dwell | **2500 ms** then navigate |
| Dashboard carousel | auto-advance **4000 ms**, `animateScrollToPage` |
| Public carousel | same pager auto-advance pattern |
| Image load | Coil **crossfade true** |
| SOS pulse | infinite tween **1200 ms**, FastOutSlowIn, scale on 170 dp ring |
| Public “live” pulse | infinite tween **800 ms**, alpha + scale, green `#22C55E` dots |
| Map camera | animate to lat/lng zoom **15** if target; else zoom **2** at 0,0 |
| Password overlay | no enter animation specified; blocking scrim |

Do not add extra page-transition choreography that the Android app does not have.

---

## 7. Global layout

Reference: **portrait, edge-to-edge phone**.

- Full-screen forms: vertical scroll column, controls stretch to full width.
- Dashboards: header (gradient) + scroll body + bottom bar / FAB.
- On wide viewports: keep a **phone-width column**; do not add a sidebar.
- Password-expired card: `widthIn(max = 500.dp)`.
- Dropdown menus: `fillMaxWidth(0.85–0.9)`, `heightIn(max = 280–300.dp)`.
- Horizontal scroll: carousels, category chips, admin/LE tab rows. Do not wrap those into multi-row grids.

---

## 8. Components

### 8.1 Gradient app header

Shared by Citizen, Law Enforcer, Admin:

1. Full-width linear gradient `#42A5F5 → #2196F3 → #1E88E5`
2. Status-bar padding
3. Row: flag + wordmark (flex) vs action
4. 6 dp spacer
5. `app_description` string in white (`bodySmall` citizen/LE, `bodyMedium` admin)

Citizen action: 48 dp Share, white 28 dp icon.  
Admin action: 40 dp profile, 12 dp radius, primary fill or cropped photo.  
LE action: 48 dp profile, 16 dp radius.

Pending LE banner **above** the gradient: `secondaryContainer`, 12 dp padding, Info icon + 8 dp gap + `bodySmall` copy.

### 8.2 Citizen bottom navigation

`NavigationBar` container `#1E88E5`, content white, tonal elevation 0.

Five items, `labelSmall`:

| Index | Icon | Label | Behavior |
|---|---|---|---|
| 0 | Home | Home | tab |
| 1 | Notifications | Alerts | tab; red `Badge` if reviewed tips not dismissed |
| 2 | Warning | SOS | never selected; opens confirm dialog; indicator white 10% |
| 3 | Map | Map | never selected; navigates |
| 4 | Person | Profile | never selected; navigates |

Selected icon/text: white. Unselected: white 50%.

### 8.3 Citizen FAB

Extended FAB, `#1E88E5`, white content, **12 dp** radius, **40 dp** height, 18 dp Add icon, 4 dp gap, label `Report` in `labelSmall`.

### 8.4 Admin bottom bar

Blue `#1E88E5` surface, 16 dp horizontal / 12 dp vertical, 12 dp gap.

Two equal **white** filled buttons, content `#1E88E5`, **14 dp** radius, **48 dp** height: **Public Alert** (Warning 18 dp) and **Post Criminal** (Person 18 dp).

### 8.5 Law-enforcer FAB

Extended FAB `#1E88E5`, white, posts wanted/missing (Person icon). Same family as citizen FAB.

### 8.6 Tabs

**Law enforcer** (inside gradient header, only if approved): `ScrollableTabRow`, transparent, no indicator/divider, edgePadding 16 dp. Each tab: 1 dp **red** border, 12 dp radius, 8 dp end padding. Selected fill red 20%. Selected text white Bold; unselected **gray** Medium. Titles: Incidents, Tips, Wanted, Contacts.

**Admin** (in body): `ScrollableTabRow`, transparent, no indicator. Border `#2196F3` 80% selected / 30% unselected, 12 dp radius. Selected fill `#2196F3` 85%, white Bold text. Unselected onSurfaceVariant Medium. Red circular count badges (6 dp h / 2 dp v pad, 10 sp Black). Titles: Approvals, Reports, Tips, Wanted, Alerts, Users, Contacts, Stats, Logs, Settings, Trash, Support.

**Citizen Alerts sub-tabs:** Public Alerts | My Secure Tips (standard TabRow).

### 8.7 Buttons

| Variant | Spec |
|---|---|
| Welcome / auth primary | Outlined, full width, 56 dp, 12 dp radius, 1.5 dp white, white Bold `titleMedium`, transparent fill |
| Google | Surface white, black content, 56 dp, 12 dp radius; 24 dp `ic_google` + 12 dp + `SIGN IN WITH GOOGLE` / equivalent Bold `labelLarge`; spinner 20 dp, stroke 2 dp, black |
| Operational submit | Filled primary `#1E88E5`, 56 dp, 16 dp (tip/post) or 12 dp (report), white Bold |
| Disabled submit | Gray / gray 50% |
| Text on gradient | white Bold |
| Destructive filled | error / `Color.Red`, white ExtraBold (`Broadcast SOS`) |
| Destructive outline | error content |
| Phone action | `#1565C0` content, border `#1E88E5` 25% or `#E2E8F0` disabled |
| WhatsApp action | `#15803D` content, border `#16A34A` 25% |
| Admin white-on-blue bar | see 8.4 |
| Soft primary | primary 15% fill |
| Soft success | `#10B981` 10% fill / `#10B981` content |
| Soft error | error 10% fill |

### 8.8 Text fields

Outlined, white container, `#212121` `bodyLarge`, focus `#1E88E5`, unfocus `#BDBDBD` / `#757575`.

- Sign-in: **12 dp**
- Sign-up, report, tip, post-criminal: often **16 dp** (report title/description **12 dp** in places)
- Password: mask + Visibility / VisibilityOff
- Multiline: minLines 4 on crime description
- Dropdowns: read-only field + ArrowDropDown, menu `#F8F9FA`, selected row `#E3F2FD`

### 8.9 Cards

| Card | Recipe |
|---|---|
| Dashboard carousel | 300×180, 16 dp, 1.5 dp primary 50%, elevation 4, crop image, black 80% gradient, 20 dp text pad, NEW pill 8 dp |
| Public carousel | 20 dp corners, elevation 6, category color accent |
| Analytics / public / directory promo | transparent, 24 dp, 1.5 dp primary 60%, 20 dp pad, 48 dp icon well 12 dp radius primary 20% |
| Anonymous tip | fill `#1E88E5`, 32 dp, 1.5 dp white, 20 dp pad, 48 dp icon well white 20% / 16 dp radius |
| Stat | surface 50%, 24 dp, 1 dp onSurface 15%, 16 dp pad, gray `labelSmall` + `headlineMedium` value |
| Incident / wanted / tip rows | white surface, 12–24 dp corners, status chip 8 dp, 8–12 dp thumbs |
| About warning | default card colors, 16 dp pad, error Warning icon, `onErrorContainer` `bodySmall` |
| Emergency contact | 16 dp, elevation 2, category chip |
| SYSTEM NOTICES wrapper | 16 dp, 1.5 dp primary 50%, surfaceVariant 15% |

### 8.10 Dialogs

Material `AlertDialog`, container `#EEEEEE`, title/body `#212121`.

| Dialog | Visual notes |
|---|---|
| DeleteConfirmationDialog | Title default “Confirm Deletion”; DELETE error; CANCEL |
| SOS confirm | Warning icon red; title “Activate SOS Alert?”; Broadcast SOS red ExtraBold; Cancel |
| Tip success | 24 dp shape; 48 dp CheckCircle; OK Bold |
| Report success | CheckCircle; call / WhatsApp action pair |
| Share app | share channel tiles (public viewing uses brand-colored 12 dp tiles) |
| Forgot password | `#EEEEEE`; “Reset Password”; fields + send |
| Password expired | not AlertDialog: 92% black scrim; 16 dp card max 500 dp; 64 dp error Lock; “Password Expired”; three lock fields; error red / success `#2E7D32`; primary update; reset-email link; Cancel & Log Out error |
| Upload blocking | 16 dp surface, 32 dp pad, spinner stroke 3 dp, Bold primary status, gray “Please do not close the app” |

### 8.11 Media

**SafeAsyncImage:** crop default; loading spinner 24 dp / 2 dp; error Image 40 dp + “Image unavailable”.

**Video:** black, 12 dp clip; lazy 48 dp white Play; ExoPlayer controls without next/prev.

**Audio:** 16 dp secondaryContainer row; play/pause; thin progress; elapsed; error Warning + Refresh.

**Evidence thumbs:** 60–68 dp cells, 8 dp image radius, 20 dp circular Close overlay.

**Mic tile:** `#10B981` 20% fill, mic icon `#10B981`.

### 8.12 Map

TopAppBar title “Incident Map” `#1E88E5`, back icon. Default Google markers for reports. SOS target: **red hue** marker, title `🚨 ACTIVE SOS EMERGENCY`. Missing key: 64 dp Map icon primary, error title, centered body.

---

## 9. Screen visual recipes

Reproduce these compositions; do not restyle.

### Splash

Edge-to-edge blue gradient. Center: 200 dp circular logo, fade 1500 ms, scale spring 0.8→1. Bottom 48 dp: `Securing Communities Together`, white 60%, Medium. Timeout 2500 ms.

### Welcome

Same gradient; status + nav padding; scroll; 32 dp pad; centered. 200 dp circular logo Fit. 24 dp. `Welcome to` `titleLarge` white 90%. 4 dp. `TRACENET LIBERIA` lockup. `app_description` `bodyMedium` white, top 16 dp. 48 dp. **SIGN IN** outlined. 16 dp. **CREATE ACCOUNT** outlined. 32 dp. TextButton **Continue as Public Viewer** white Bold `bodyMedium`.

### Sign In

Same gradient and 200 dp logo. Lockup. 8 dp. **SIGN IN** `headlineLarge` Black white. Description white `bodyMedium`. 32 dp. Email + password 12 dp fields (white). Access-level dropdown (`Citizen Access`, `Law Enforcer Access`, `Administrator Access`) with `#F8F9FA` menu and `#E3F2FD` / `#1E88E5` chips. 56 dp **SIGN IN**. TextButton sign-up. Divider white 20% + `OR CONTINUE WITH` `labelSmall` white 50%. 24 dp Google white button. 24 dp **PUBLIC VIEWING PORTAL** outlined 1 dp white 30%, 18 dp Info. 32 dp Powered-by block.

Forgot-password dialog `#EEEEEE`.

### Sign Up

Same gradient and logo. Lockup. **NODE REGISTRATION** `labelSmall` white 70%. **SIGN UP** `headlineLarge` Black. Fields 16 dp: Full Name, Email, Password, role dropdown, then if Law Enforcer: badge, contact, address, ID photo well (AddAPhoto). Google sign-up. Powered-by. Same field colors as sign-in.

### Citizen Home

Gradient header (flag, lockup, share). Body 16 dp: Analytics card; public-viewing promo 24 dp; carousel; anonymous tip 32 dp blue; stats; wanted/report lists; 80 dp bottom spacer. Blue 5-item nav + Report FAB.

Alerts tab: title Emergency & Safety Alerts; subtabs Public Alerts / My Secure Tips; empty tips = 36 dp gray Lock + “No secure tips sent yet”.

### Law Enforcer

Gradient header + profile 48/16. Approved: red-outline scroll tabs. FAB post criminal. Body: SYSTEM NOTICES carousel; analytics promo; incident/tip/wanted/contact cards; gray icon buttons for notes/assign/merge; error Delete; Flag escalate.

Unapproved officers are **not** shown this chrome; they see Citizen.

### Admin

Gradient header + 40/12 profile. Body scroll tabs with red count badges. SYSTEM NOTICES + analytics promo. Bottom white Public Alert + Post Criminal. Trash / support / user rows use soft green/red/primary 15% buttons.

### Submit Report

Light background `#F5F9FF`. Back + **REPORT NODE** `labelSmall` primary + black title. Optional error-container 50% warning 16 dp. Info bar `#1E88E5` 16 dp, 1 dp white. Fields 12–16 dp. Date / GPS / photo / video / audio tiles 16 dp, 15% onSurface border. Thumbs 60 dp. County/type dropdowns `#F8F9FA` / `#E3F2FD`. Phone field. Checkbox accent `#1E88E5`. Submit 56 dp. Success dialog with phone `#1565C0` and WhatsApp `#15803D` actions.

### Submit Tip

Back + node header. Type dropdown with Active `#E8F5E9/#2E7D32` and Inactive `#FFEBEE/#C62828` 10 sp badges. Details field 16 dp. Submit 56/16, gray if inactive type. Success 24 dp dialog, 48 dp CheckCircle, OK.

### Post Criminal

Back + **ADMIN NODE** / **POST WANTED CRIMINAL DATA**. Fields 16 dp. Category dropdown. Up to 4 photos, 8 dp thumbs, Close. Upload overlay black 50% + 16 dp card.

### SOS

Background theme background. Back 16 dp. Centered 32 dp column. **EMERGENCY NODE** `labelSmall` tertiary. **SOS ACTIVE** `headlineLarge` Black tertiary. 64 dp. 200 dp stack: 170 dp pulsing tertiary 15% circle; 180 dp tertiary progress stroke 4 dp; 140 dp tertiary circle HELP `headlineMedium` Black onTertiary, shadow 20 dp. 64 dp. **SIGNAL TRANSMITTED** Bold primary. Body 70% onBackground. 48 dp. **CALL EMERGENCY CONTACTS** 56/16 filled + Phone. 12 dp. **CANCEL SOS** 56/16, primary 5% fill, tertiary content, 1 dp tertiary 30% border.

### Emergency contacts

TopAppBar. Search 20 dp field. Location chips. Category colors §2.6. Cards 16 dp elevation 2. Admin/LE: Extended FAB Add. Edit/Delete 18 dp.

### Profile

TopAppBar with Help and Info. Avatar Person. Edit. Rows Person / Email / LocationOn. Approval CheckCircle vs Info. VerifiedUser. Password / Star / ExitToApp 56/12. Delete account error text. Dialogs 24 dp for help, rate, terms, password.

### Public viewing

Back + Share. Police logo. Live green pulses. Public carousel (crop + category color). Alert / wanted / report cards with status chips §2.5. Share dialog brand tiles §2.7. Tip CTA outlined or filled primary 10%.

### Analytics

TopAppBar `NATIONAL ANALYTICS` Black primary. Surface background. Summary stats row. Chart cards with BarChart / Security / PieChart icons, 200 dp chart height, 24 dp gaps. Palettes §2.9.

### About

TopAppBar `About TraceNet Liberia` (red/onSurface highlighter). 64 dp Info primary. Title highlighter + Bold. Secondary subtitle. Sections titleMedium Bold primary. Feature rows 24 dp primary icons, 16 dp gap, 12 dp vertical. Warning card. `Version 1.0.0`.

### Map

See §8.12.

---

## 10. Navigation chrome by role

| Role | Top | Bottom / FAB |
|---|---|---|
| Unauthenticated | none (full-bleed gradient) | none |
| Citizen | gradient header + share | 5-item blue nav + Report FAB |
| Unapproved law enforcer | citizen chrome + pending banner | citizen nav |
| Approved law enforcer | gradient + profile + red tabs | Post FAB |
| Admin | gradient + profile | two white bar buttons; tabs in body |
| Inner screens | Material TopAppBar or custom back + node title | none |

Do not convert admin/LE tabs into a desktop sidebar.

---

## 11. Copy that is also visual lockup

Keep letter case and punctuation:

- Splash: `Securing Communities Together`
- Welcome: `Welcome to` / `TRACENET LIBERIA` / `SIGN IN` / `CREATE ACCOUNT` / `Continue as Public Viewer`
- Sign in: `SIGN IN` / `OR CONTINUE WITH` / `SIGN IN WITH GOOGLE` / `PUBLIC VIEWING PORTAL` / `POWERED BY` / `Zeahn's Tech`
- Sign up: `NODE REGISTRATION` / `SIGN UP`
- Citizen FAB: `Report`
- SOS: `EMERGENCY NODE` / `SOS ACTIVE` / `HELP` / `SIGNAL TRANSMITTED` / `CALL EMERGENCY CONTACTS` / `CANCEL SOS`
- Admin bar: `Public Alert` / `Post Criminal`
- Analytics: `NATIONAL ANALYTICS`
- Post criminal: `ADMIN NODE` / `POST WANTED CRIMINAL DATA`
- Report: `REPORT NODE`
- About: `Government-Grade Civic Platform` / `Version 1.0.0`
- Carousel: `NEW` / Community Safety / Real-time Alerts / Node Network (exact descriptions in Android carousel component)
- App description string: full paragraph from `strings.xml` (white on headers)

---

## 12. Asset transfer checklist

Copy without reprocessing or visual replacement:

1. TraceNet logo JPEG used at 200 dp circle and in carousel
2. Adaptive launcher (black background + logo)
3. `ic_google` four-color vector
4. Liberia police banner, hero banner, police logo
5. Seven public-safety investigation/rescue/monitoring images
6. Canvas Liberia flag (reimplement identically; do not use a stock flag)
7. Material filled icon set as listed
8. Wordmark: `TRACENET` `#FF0000` Black + `LIBERIA` white Black + shadow 4

---

## 13. What not to change

- Do not restyle Material 3 into another kit (Fluent, iOS, “modern SaaS”).
- Do not replace the blue gradient with a flat header or glassmorphism.
- Do not introduce dark-mode inversion; the coded dark scheme is the light palette.
- Do not swap system sans for a branded webfont.
- Do not round everything to 8 dp or 999 px pills.
- Do not replace the five-item citizen bar with a hamburger.
- Do not replace `#FF0000` wordmark with flag red, or flag red with wordmark red.
- Do not replace bitmap photography with illustrations or AI-generated stand-ins.

The PWA must look like this Android application.
