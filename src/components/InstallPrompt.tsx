import { useEffect, useState } from 'react'

type InstallEvent = Event & { prompt:()=>Promise<void>; userChoice:Promise<{outcome:'accepted'|'dismissed'}> }
type UpdateFn = (reloadPage?:boolean)=>Promise<void>

export function InstallPrompt(){
  const [installEvent,setInstallEvent]=useState<InstallEvent>()
  const [showIos,setShowIos]=useState(()=>{
    const standalone=window.matchMedia('(display-mode: standalone)').matches || (navigator as Navigator & {standalone?:boolean}).standalone
    const ios=/iphone|ipad|ipod/i.test(navigator.userAgent) && !standalone
    return ios && !sessionStorage.getItem('tracenet-ios-install-seen')
  })
  const [updateFn,setUpdateFn]=useState<UpdateFn>()
  useEffect(()=>{
    const onInstall=(event:Event)=>{ event.preventDefault(); setInstallEvent(event as InstallEvent) }
    const onUpdate=(event:Event)=>setUpdateFn(()=>(event as CustomEvent<UpdateFn>).detail)
    window.addEventListener('beforeinstallprompt',onInstall)
    window.addEventListener('tracenet-pwa-update',onUpdate)
    return ()=>{ window.removeEventListener('beforeinstallprompt',onInstall); window.removeEventListener('tracenet-pwa-update',onUpdate) }
  },[])
  const install=async()=>{ if(!installEvent) return; await installEvent.prompt(); const choice=await installEvent.userChoice; if(choice.outcome==='accepted') setInstallEvent(undefined) }
  // Calling updateFn(true) sends {type:'SKIP_WAITING'} to the waiting
  // worker and reloads once it takes control — a plain location.reload()
  // here would just reload under whichever worker was ALREADY active,
  // which is not necessarily the new one (see src/sw.ts).
  if(updateFn) return <aside className="pwa-prompt"><span>A new TraceNet version is ready.</span><button onClick={()=>updateFn(true)}>Refresh</button><button aria-label="Dismiss update" onClick={()=>setUpdateFn(undefined)}>Dismiss</button></aside>
  if(showIos) return <aside className="pwa-prompt"><span>Install TraceNet: tap Share, then Add to Home Screen.</span><button onClick={()=>{sessionStorage.setItem('tracenet-ios-install-seen','1');setShowIos(false)}}>Got it</button></aside>
  if(!installEvent) return null
  return <aside className="pwa-prompt"><span>Install TraceNet for faster offline access.</span><button onClick={install}>Install</button><button aria-label="Dismiss install prompt" onClick={()=>setInstallEvent(undefined)}>Dismiss</button></aside>
}
