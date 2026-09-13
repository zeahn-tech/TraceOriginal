import { useEffect, useState } from 'react'
import { RotateCcw, Trash2 } from 'lucide-react'
import { discardDraft, getPendingReportDrafts, MAX_RETRY_ATTEMPTS, retryDraft, type ReportDraft } from '../../services'
import { draftStatusDetail, draftStatusLabel } from './draftStatus'

export function PendingDrafts({title,collectionName,ownerField,ownerId}:{title:string;collectionName:string;ownerField?:string;ownerId?:string}){
  const[drafts,setDrafts]=useState<ReportDraft[]>([])
  const[busyId,setBusyId]=useState<string>()
  const matches=(d:ReportDraft)=>d.collection===collectionName && (!ownerField || d.payload[ownerField]===ownerId)
  useEffect(()=>{
    let cancelled=false
    const refresh=async()=>{const all=await getPendingReportDrafts();if(!cancelled)setDrafts(all.filter(matches))}
    void refresh()
    const interval=window.setInterval(refresh,4000)
    window.addEventListener('online',refresh)
    return ()=>{cancelled=true;window.clearInterval(interval);window.removeEventListener('online',refresh)}
    // eslint-disable-next-line react-hooks/exhaustive-deps -- matches() is derived purely from the primitive props below
  },[collectionName,ownerField,ownerId])
  if(!drafts.length) return null
  const act=async(id:string,fn:(id:string)=>Promise<void>)=>{setBusyId(id);await fn(id);const all=await getPendingReportDrafts();setDrafts(all.filter(matches));setBusyId(undefined)}
  return <section><h2>{title}</h2><div className="cards">{drafts.map(d=><article className="record-card" key={d.id}><span className={`status ${d.status==='FAILED'||d.status==='CONFLICT'?'red':''}`}>{draftStatusLabel(d.status)}</span><h3>{String(d.payload.title??d.payload.name??'Untitled')}</h3><small>{draftStatusDetail(d)}</small>{(d.status==='FAILED'||d.status==='CONFLICT')&&<div className="card-actions">{d.attempts<MAX_RETRY_ATTEMPTS&&<button disabled={busyId===d.id} onClick={()=>act(d.id,retryDraft)}><RotateCcw/> Retry</button>}<button className="danger-text" disabled={busyId===d.id} onClick={()=>act(d.id,discardDraft)}><Trash2/> Discard</button></div>}</article>)}</div></section>
}
