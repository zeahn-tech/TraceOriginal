import React from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { registerSW } from 'virtual:pwa-register'
import App from './App'
import { InstallPrompt } from './components/InstallPrompt'
import { whenIdle } from './shared/idle'
import './styles.css'
const pagesRedirect=sessionStorage.getItem('tracenet-pages-redirect')
if(pagesRedirect){ sessionStorage.removeItem('tracenet-pages-redirect'); history.replaceState(null,'',pagesRedirect) }
// registerSW's returned function is what actually performs an update: it
// posts {type:'SKIP_WAITING'} to the waiting worker (see src/sw.ts's
// message listener) and, called with `true`, reloads once that worker has
// taken control. Previously this return value was discarded entirely, so
// InstallPrompt's "Refresh" button had no way to actually trigger an
// update — see PWA_PRODUCTION_VALIDATION.md.
const updateServiceWorker=registerSW({onNeedRefresh(){ window.dispatchEvent(new CustomEvent('tracenet-pwa-update',{detail:updateServiceWorker})) }})
// Dynamically imported, deferred to browser idle time (rather than a
// static top-level `import { initializeMediaSync } from './services'`
// evaluated as part of this module) so the ~600 KB Firebase SDK chunk
// this pulls in never competes, on a bandwidth-throttled connection,
// with the resources the first paint actually depends on. The sync
// listeners this sets up (retry-on-reconnect, retry-on-SW-message) still
// get wired up as soon as the browser is idle after the initial render;
// only the timing relative to first paint changed, not the behavior.
// See LIGHTHOUSE_PRODUCTION_REPORT.md.
whenIdle(()=>{ import('./services').then(({ initializeMediaSync }) => initializeMediaSync()) })
createRoot(document.getElementById('root')!).render(<React.StrictMode><BrowserRouter basename={import.meta.env.BASE_URL}><InstallPrompt/><App/></BrowserRouter></React.StrictMode>)
