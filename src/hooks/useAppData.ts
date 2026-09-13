import { useEffect, useState } from 'react'
import { collections, observeAuth, profile, saveProfile } from '../services'
import { type Alert, type Report, type Tip, type User, type Wanted } from '../domain'

export function useAppData(){ const [user,setUser]=useState<User>(); const [loading,setLoading]=useState(true); const [reports,setReports]=useState<Report[]>([]); const [tips,setTips]=useState<Tip[]>([]); const [alerts,setAlerts]=useState<Alert[]>([]); const [wanted,setWanted]=useState<Wanted[]>([]); const [users,setUsers]=useState<User[]>([]); const [publicReports,setPublicReports]=useState<Report[]>([]); const [publicWanted,setPublicWanted]=useState<Wanted[]>([])
 useEffect(()=>observeAuth(async fb=>{ if(fb){ const existing=await profile(fb.uid); const defaultUser:User=existing??{id:fb.uid,name:fb.displayName||fb.email?.split('@')[0]||'TraceNet User',email:fb.email||'',role:'CITIZEN',isApproved:true,status:'ACTIVE',reputationScore:100}; if(!existing) await saveProfile(defaultUser); setUser(defaultUser) }else setUser(undefined); setLoading(false)}),[])
 useEffect(()=>{
   // Alerts and the "public/verified" feeds are always safe and always on,
   // for signed-out visitors and every role alike — these are exactly the
   // subsets firestore.rules grants without requiring an identity at all.
   const stops=[collections.alerts(setAlerts),collections.publicReports(setPublicReports),collections.publicWanted(setPublicWanted)]
   // Everything else is scoped to what the current role is actually allowed
   // to list under firestore.rules — an unfiltered collection listen would
   // be rejected outright by the server for anyone who isn't LAW_ENFORCER/
   // ADMIN, so a citizen gets an owner-filtered query instead of the full
   // collection (see services.ts collections.myReports/myTips and
   // FIREBASE_SECURITY_IMPLEMENTATION.md).
   const isPrivileged=user?.role==='LAW_ENFORCER'||user?.role==='ADMIN'
   if(isPrivileged) stops.push(collections.allReports(setReports),collections.allTips(setTips),collections.allWanted(setWanted))
   else if(user) stops.push(collections.myReports(user.id,setReports),collections.myTips(user.id,setTips))
   if(user?.role==='ADMIN') stops.push(collections.allUsers(setUsers))
   // Clear whichever slices the branches above did NOT just (re)subscribe to,
   // so a role/login change (e.g. an admin signing out) can never leave a
   // previous role's data lingering in state — this resets local state to
   // match which external subscriptions are actually active, the same class
   // of effect-scoped reset used for the media-preview object URL lifecycle
   // in components/MediaPreview.tsx.
   // eslint-disable-next-line react-hooks/set-state-in-effect -- see comment above
   if(!isPrivileged) setWanted([])
   if(!user){ setReports([]); setTips([]) }
   if(user?.role!=='ADMIN') setUsers([])
   return ()=>stops.forEach(stop=>stop())
 },[user?.id,user?.role])
 return {user,setUser,loading,reports,tips,alerts,wanted,users,publicReports,publicWanted}}
