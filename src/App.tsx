import { useEffect, useState } from 'react'
import { AlertTriangle, Check } from 'lucide-react'
import { useAppData } from './hooks/useAppData'
import { AppRoutes } from './routing/AppRoutes'
import { OfflineBanner } from './components/OfflineBanner'
import { listenForForegroundAlerts } from './push'
import type { Toast } from './shared/types'

function App(){ const data=useAppData(); const [toast,setToast]=useState<Toast>(); const [update,setUpdate]=useState(false); useEffect(()=>{const fn=()=>setUpdate(true);window.addEventListener('tracenet-pwa-update',fn);return()=>window.removeEventListener('tracenet-pwa-update',fn)},[]); useEffect(()=>{if(toast){const id=setTimeout(()=>setToast(undefined),3500);return()=>clearTimeout(id)}},[toast]); useEffect(()=>listenForForegroundAlerts((title,body)=>setToast({message:body?`${title}: ${body}`:title})),[]);
 return <><OfflineBanner/><AppRoutes data={data} setToast={setToast}/>{toast&&<div className={`toast ${toast.kind??'success'}`}>{toast.kind==='error'?<AlertTriangle/>:<Check/>}{toast.message}</div>}{update&&<button className="update" onClick={()=>location.reload()}>An update is ready. Refresh now</button>}</> }
export default App
