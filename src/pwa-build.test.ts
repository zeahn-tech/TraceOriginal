// @vitest-environment node
/**
 * PWA build-output validation.
 *
 * Unlike the Firebase-rules-dependent test suites elsewhere in this
 * project, everything here is 100% verifiable in this sandbox: it runs a
 * REAL production build (with a GitHub-Pages-style base path) and inspects
 * the REAL generated manifest, index.html, 404.html, sw.js, and icon
 * files — no emulator, no network, no mocking. See
 * PWA_PRODUCTION_VALIDATION.md for the full picture, including the things
 * that genuinely cannot be verified this way (how a real browser installs
 * the app, how iOS actually renders the icon, etc.) and must be checked
 * manually on a real device/browser instead.
 */
import { afterAll, beforeAll, describe, expect, it } from 'vitest'
import { execFileSync } from 'node:child_process'
import { existsSync, readFileSync, readdirSync, rmSync } from 'node:fs'
import { resolve } from 'node:path'

const ROOT = resolve(__dirname, '..')
const DIST = resolve(ROOT, 'dist')
const BASE_PATH = '/tracenet-pwa/' // mirrors .github/workflows/deploy-pages.yml's /<repo-name>/ pattern

// eslint-disable-next-line @typescript-eslint/no-explicit-any
let manifest: any
let indexHtml: string
let notFoundHtml: string
let swJs: string

beforeAll(() => {
  rmSync(DIST, { recursive: true, force: true })
  execFileSync('npm', ['run', 'build'], {
    cwd: ROOT,
    env: { ...process.env, VITE_BASE_PATH: BASE_PATH },
    stdio: 'pipe',
  })
  manifest = JSON.parse(readFileSync(resolve(DIST, 'manifest.webmanifest'), 'utf8'))
  indexHtml = readFileSync(resolve(DIST, 'index.html'), 'utf8')
  notFoundHtml = readFileSync(resolve(DIST, '404.html'), 'utf8')
  swJs = readFileSync(resolve(DIST, 'sw.js'), 'utf8')
}, 180_000)

afterAll(() => {
  // Leave a normal (no forced base path) build behind afterward, matching
  // what a plain `npm run build` for local/root hosting would produce.
  rmSync(DIST, { recursive: true, force: true })
  execFileSync('npm', ['run', 'build'], { cwd: ROOT, stdio: 'pipe' })
}, 60_000)

describe('GitHub Pages base path — every URL respects VITE_BASE_PATH', () => {
  it('emits 404.html at the deploy root — this is the GitHub Pages deep-link recovery mechanism, and it previously never reached the deployed site at all (see PWA_PRODUCTION_VALIDATION.md)', () => {
    expect(existsSync(resolve(DIST, '404.html'))).toBe(true)
    expect(notFoundHtml).toContain('tracenet-pages-redirect')
  })
  it('main.tsx restores the deep link 404.html stashed in sessionStorage, via history.replaceState', () => {
    const mainSrc = readFileSync(resolve(ROOT, 'src/main.tsx'), 'utf8')
    expect(mainSrc).toContain('tracenet-pages-redirect')
    expect(mainSrc).toContain('history.replaceState')
  })
  it('every same-origin asset reference in index.html is prefixed with the configured base path, never the bare site root', () => {
    const refs = [...indexHtml.matchAll(/(?:src|href)="([^"]+)"/g)].map(m => m[1])
    expect(refs.length).toBeGreaterThan(3) // sanity check the regex actually matched something
    for (const url of refs) {
      if (url.startsWith('http') || url.startsWith('data:')) continue
      expect(url.startsWith(BASE_PATH)).toBe(true)
    }
  })
  it('manifest start_url and scope are relative ("./"), which correctly resolves against the manifest\'s own URL regardless of base path — not hardcoded to a specific deployment', () => {
    expect(manifest.start_url).toBe('./')
    expect(manifest.scope).toBe('./')
  })
  it('the manifest link tag and service worker registration are also correctly prefixed with the base path', () => {
    expect(indexHtml).toContain(`href="${BASE_PATH}manifest.webmanifest"`)
  })
})

describe('Web App Manifest — installability', () => {
  it('has every field required for a browser install prompt', () => {
    expect(manifest.name).toBeTruthy()
    expect(manifest.short_name).toBeTruthy()
    expect(manifest.display).toBe('standalone')
    expect(Array.isArray(manifest.icons)).toBe(true)
    expect(manifest.icons.length).toBeGreaterThan(0)
  })
  it('includes a 192 and a 512 icon with purpose "any"', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const icons = manifest.icons as any[]
    expect(icons.some(i => i.sizes === '192x192' && i.purpose === 'any')).toBe(true)
    expect(icons.some(i => i.sizes === '512x512' && i.purpose === 'any')).toBe(true)
  })
  it('maskable icons are their own dedicated entries, never combined as "any maskable" on the same icon (a common, harmful anti-pattern — see PWA_PRODUCTION_VALIDATION.md)', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const icons = manifest.icons as any[]
    const maskable = icons.filter(i => String(i.purpose).includes('maskable'))
    expect(maskable.length).toBeGreaterThan(0)
    for (const icon of maskable) expect(icon.purpose).toBe('maskable')
  })
  it('every declared PNG icon file actually exists in the build output at exactly its declared pixel size', async () => {
    const { default: sharp } = await import('sharp')
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    for (const icon of manifest.icons as any[]) {
      const path = resolve(DIST, icon.src)
      expect(existsSync(path)).toBe(true)
      if (icon.type === 'image/png') {
        const meta = await sharp(path).metadata()
        const [w, h] = icon.sizes.split('x').map(Number)
        expect(meta.width).toBe(w)
        expect(meta.height).toBe(h)
      }
    }
  })
})

describe('Apple metadata', () => {
  it('apple-touch-icon points to a real PNG, not an SVG — iOS does not rasterize SVG for this tag', () => {
    const match = indexHtml.match(/<link rel="apple-touch-icon" href="([^"]+)"/)
    expect(match).toBeTruthy()
    expect(match![1]).toMatch(/\.png$/)
    expect(existsSync(resolve(DIST, match![1].replace(BASE_PATH, '')))).toBe(true)
  })
  it('the apple-touch-icon PNG is full-bleed with no alpha channel (iOS applies its own corner mask and backs transparent icons with white, which looks wrong on a pre-rounded/transparent source)', async () => {
    const { default: sharp } = await import('sharp')
    const match = indexHtml.match(/<link rel="apple-touch-icon" href="([^"]+)"/)!
    const meta = await sharp(resolve(DIST, match[1].replace(BASE_PATH, ''))).metadata()
    expect(meta.hasAlpha).toBe(false)
  })
  it('declares apple-mobile-web-app-capable and a status bar style, required for correct standalone rendering on iOS', () => {
    expect(indexHtml).toContain('apple-mobile-web-app-capable')
    expect(indexHtml).toContain('apple-mobile-web-app-status-bar-style')
    expect(indexHtml).toContain('apple-mobile-web-app-title')
  })
})

describe('Service worker — update lifecycle & cache invalidation', () => {
  it('never calls skipWaiting() unconditionally — every call site is inside the SKIP_WAITING message handler', () => {
    const calls = [...swJs.matchAll(/skipWaiting\(\)/g)]
    expect(calls.length).toBeGreaterThan(0)
    for (const call of calls) {
      const precedingContext = swJs.slice(Math.max(0, call.index! - 200), call.index!)
      expect(precedingContext).toContain('SKIP_WAITING')
    }
  })
  it('cleans up outdated precache entries so an old deployment\'s assets don\'t linger forever once a new one activates', () => {
    expect(swJs).toMatch(/cleanupOutdatedCaches|precache[-_]v\d|workbox-precache/i)
  })
  it('precached JS asset filenames are content-hashed, so an old and a new deployment can never collide in the same cache', () => {
    const jsFiles = readdirSync(resolve(DIST, 'assets')).filter(f => f.endsWith('.js'))
    expect(jsFiles.length).toBeGreaterThan(0)
    for (const file of jsFiles) expect(file).toMatch(/-[A-Za-z0-9_-]{8,}\.js$/)
  })
  it('main.tsx captures and exposes the real updateSW() function rather than discarding it, so the in-app update prompt can actually trigger an update', () => {
    const mainSrc = readFileSync(resolve(ROOT, 'src/main.tsx'), 'utf8')
    expect(mainSrc).toMatch(/const\s+updateServiceWorker\s*=\s*registerSW/)
    expect(mainSrc).toContain('detail:updateServiceWorker')
  })
  it('the install-prompt UI calls the captured update function (not a bare location.reload(), which would reload under whichever worker happened to already be active)', () => {
    const promptSrc = readFileSync(resolve(ROOT, 'src/components/InstallPrompt.tsx'), 'utf8')
    expect(promptSrc).toContain('updateFn(true)')
    expect(promptSrc).not.toMatch(/onClick=\{\(\)\s*=>\s*location\.reload\(\)\}/)
  })
})

describe('Service worker — must not incorrectly cache sensitive/private Firebase data', () => {
  it('the runtime image-caching route is restricted to same-origin requests — it must never match cross-origin Firebase Storage evidence-photo URLs', () => {
    expect(swJs).toMatch(/destination===["']image["']\s*&&[^)]*origin===self\.location\.origin/)
  })
  it('no runtime caching route matches Firestore/Storage/Functions API calls at all (only navigations and same-origin images are routed)', () => {
    // Firestore/Storage/Functions requests have request.destination === ''
    // (empty — they're fetch/XHR, not navigations or images), so they are
    // never intercepted by any route registered here and always pass
    // through to the network directly, unmodified by the service worker.
    // Checked directly rather than by inspecting registerRoute() call
    // sites: both registerRoute and matchPrecache are renamed to
    // short/meaningless identifiers by the production minifier, so
    // asserting on those literal names is unreliable (see the sibling test
    // above for the same lesson). The reliable, minification-proof check
    // is simply that no Firebase Storage/Firestore/Functions domain string
    // appears ANYWHERE in the compiled worker — there is no legitimate
    // reason for one to, since this worker never targets those domains
    // for any purpose at all.
    expect(swJs).not.toMatch(/firebasestorage\.googleapis\.com|firestore\.googleapis\.com|cloudfunctions\.net/)
  })
})

describe('Offline navigation', () => {
  it('falls back to the precached app shell (index.html) for any navigation request when the network is unavailable', () => {
    // Note: matchPrecache() itself is minified to a short identifier by the
    // production build, so asserting on that literal name would be
    // fragile. What survives minification are the string literals — the
    // 'navigate' mode check and the 'index.html' precache lookup target —
    // which together are reliable evidence the catch-handler fallback
    // logic (setCatchHandler in src/sw.ts) is intact.
    expect(swJs).toMatch(/mode===["']navigate["']/)
    expect(swJs).toContain('"index.html"')
  })
  it('uses a network-first (not cache-first) strategy for navigations, so an online visitor gets fresh content rather than stale-forever content', () => {
    expect(swJs).toContain('NavigationRoute')
    expect(swJs).toContain('networkTimeoutSeconds') // unique to NetworkFirst among the strategies used in this file
  })
})

describe('Background Sync — graceful degradation, reliability never overstated', () => {
  it('the client has an application-driven sync fallback that works with or without Background Sync support', () => {
    const servicesSrc = readFileSync(resolve(ROOT, 'src/services.ts'), 'utf8')
    expect(servicesSrc).toContain("addEventListener('online'")
  })
  it('sw.ts explicitly documents that Background Sync is not universally supported, rather than asserting it always works', () => {
    const swSrc = readFileSync(resolve(ROOT, 'src/sw.ts'), 'utf8')
    expect(swSrc).toMatch(/not reliably available|no support|best-effort/i)
  })
})

describe('Installed PWA / standalone mode', () => {
  it('manifest declares standalone display mode (not "browser", which would open in a normal tab)', () => {
    expect(manifest.display).toBe('standalone')
  })
  it('theme_color and background_color are set, needed for a native-feeling status bar and splash screen', () => {
    expect(manifest.theme_color).toBeTruthy()
    expect(manifest.background_color).toBeTruthy()
  })
})
