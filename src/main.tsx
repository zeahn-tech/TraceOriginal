import React from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { registerSW } from 'virtual:pwa-register'
import App from './App'
import { InstallPrompt } from './components/InstallPrompt'
import { initializeMediaSync } from './services'
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
initializeMediaSync()
createRoot(document.getElementById('root')!).render(<React.StrictMode><BrowserRouter basename={import.meta.env.BASE_URL}><InstallPrompt/><App/></BrowserRouter></React.StrictMode>)
