import { useEffect, useState } from 'react'
import { WifiOff } from 'lucide-react'

/**
 * Fixed: previously the only offline-related UI anywhere in the app was a
 * one-time post-submit toast in ReportForm/WantedForm — there was no
 * ambient way for a person to know they were offline while just browsing.
 * Reconnection itself was, and remains, handled correctly everywhere via
 * the standards-based `online`/`offline` window events (see
 * initializeMediaSync in services.ts) — this component uses the exact same
 * events, purely for display, so it needs no new detection logic and no
 * platform-specific handling (`online`/`offline` fire identically on every
 * browser this app targets).
 */
export function OfflineBanner(){
  const [online,setOnline]=useState(()=>typeof navigator==='undefined' || navigator.onLine)
  useEffect(()=>{
    const goOnline=()=>setOnline(true); const goOffline=()=>setOnline(false)
    window.addEventListener('online',goOnline); window.addEventListener('offline',goOffline)
    return ()=>{ window.removeEventListener('online',goOnline); window.removeEventListener('offline',goOffline) }
  },[])
  if(online) return null
  return <div className="offline-banner" role="status"><WifiOff size={14}/> You&apos;re offline — anything you submit now is saved on this device and will sync once you reconnect.</div>
}
