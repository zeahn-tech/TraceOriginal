// Defers non-critical startup work (loading the Firebase SDK chunk) until
// the browser is idle, instead of kicking it off in the same initial burst
// as the resources the first paint actually depends on (main bundle, CSS,
// LCP image). `requestIdleCallback` isn't implemented in Safari, hence the
// `setTimeout` fallback. See LIGHTHOUSE_PRODUCTION_REPORT.md.
export function whenIdle(cb: () => void): () => void {
  if (typeof requestIdleCallback === 'function') {
    const handle = requestIdleCallback(cb, { timeout: 2000 })
    return () => cancelIdleCallback(handle)
  }
  const handle = setTimeout(cb, 0)
  return () => clearTimeout(handle)
}
