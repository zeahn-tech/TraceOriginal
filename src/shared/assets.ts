// Web-optimized copies of the Android app's drawable assets (see
// LIGHTHOUSE_PRODUCTION_REPORT.md). The originals in
// app/src/main/res/drawable/ are shared with the native Android app
// (launcher icon, native Compose screens) and are NOT touched here —
// these are resized/recompressed specifically for the web PWA's actual
// on-screen display size (logo maxes out at 200x200 CSS px; the
// banner/hero carousel slides render inside a 300x180 box), cutting
// combined page weight from ~2.27 MB to ~150 KB with no visible quality
// loss at the sizes this app actually displays them.
import logoUrl from '../assets/images/tracenet-logo.jpg'
import bannerUrl from '../assets/images/liberia-police-banner.jpg'
import heroUrl from '../assets/images/hero-banner.jpg'
export const logo = logoUrl
export const banner = bannerUrl
export const hero = heroUrl
export const counties = ['Bomi','Bong','Gbarpolu','Grand Bassa','Grand Cape Mount','Grand Gedeh','Grand Kru','Lofa','Margibi','Maryland','Montserrado','Nimba','River Cess','River Gee','Sinoe']
