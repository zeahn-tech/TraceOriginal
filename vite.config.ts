import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  return {
  base: mode === 'production' ? (env.VITE_BASE_PATH || '/') : '/',
  build: {
    target: 'es2020',
    cssCodeSplit: true,
    rollupOptions: { output: { manualChunks: { firebase: ['firebase/app','firebase/auth','firebase/firestore','firebase/storage'], icons: ['lucide-react'] } } },
    chunkSizeWarningLimit: 700
  },
  plugins: [react(), VitePWA({
    registerType: 'prompt',
    strategies: 'injectManifest',
    srcDir: 'src',
    filename: 'sw.ts',
    manifest: {
      name: 'TraceNet Liberia', short_name: 'TraceNet', description: 'TraceNet Liberia public safety platform',
      display: 'standalone', orientation: 'portrait-primary', theme_color: '#1E88E5', background_color: '#F5F9FF', start_url: './', scope: './',
      // "any" and "maskable" are deliberately separate icon entries, never
      // combined on one icon (a common mistake: an icon meant to display
      // full-bleed under "any" gets cropped when a maskable-aware OS
      // applies its mask shape to it). icons/maskable-*.png are
      // purpose-built with generous safe-zone padding for that; the
      // pwa-*.png/svg pair is the plain, full-bleed icon. PNG is listed
      // first for broadest installer compatibility; SVG entries remain for
      // consumers that prefer a scalable source.
      icons: [
        { src: 'icons/pwa-192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
        { src: 'icons/pwa-512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
        { src: 'icons/maskable-192.png', sizes: '192x192', type: 'image/png', purpose: 'maskable' },
        { src: 'icons/maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        { src: 'icons/pwa-192.svg', sizes: '192x192', type: 'image/svg+xml', purpose: 'any' },
        { src: 'icons/pwa-512.svg', sizes: '512x512', type: 'image/svg+xml', purpose: 'any' },
      ],
      shortcuts: [
        { name: 'Report incident', short_name: 'Report', url: './reports/new', icons: [{ src: 'icons/pwa-192.png', sizes: '192x192', type: 'image/png' }] },
        { name: 'Emergency contacts', short_name: 'Contacts', url: './contacts', icons: [{ src: 'icons/pwa-192.png', sizes: '192x192', type: 'image/png' }] },
      ],
    },
    injectManifest: { globPatterns: ['**/*.{js,css,html,svg,png,jpg,jpeg}'] },
    devOptions: { enabled: false }
  })]
  }
})
