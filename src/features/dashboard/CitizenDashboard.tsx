import { useState } from 'react'
import { Link } from 'react-router-dom'
import { FilePlus2, Phone, ShieldAlert, ShieldCheck } from 'lucide-react'
import { type Alert, type Report, type Tip, type User, type Wanted } from '../../domain'
import { Dashboard, Empty } from '../../shared/ui'
import { AlertCard } from '../../shared/cards/AlertCard'
import { ReportCard } from '../../shared/cards/ReportCard'
import { TipCard } from '../../shared/cards/TipCard'
import { WantedCard } from '../../shared/cards/WantedCard'
import { PendingDrafts } from '../reports/PendingDrafts'
import type { Toast } from '../../shared/types'
import { Carousel } from './Carousel'

export function CitizenDashboard({user,reports,tips,alerts,publicWanted,setToast}:{user:User;reports:Report[];tips:Tip[];alerts:Alert[];publicWanted:Wanted[];setToast:(t:Toast)=>void}){const [section,setSection]=useState<'home'|'alerts'>('home');const own=reports.filter(r=>r.reporterId===user.id&&!r.isDeleted);return <Dashboard user={user} tab={section}><div className="dash-tabs"><button className={section==='home'?'selected':''} onClick={()=>setSection('home')}>Home</button><button className={section==='alerts'?'selected':''} onClick={()=>setSection('alerts')}>Alerts</button></div>{section==='home'?<div className="content"><Carousel/><div className="quick-grid"><Link to="/reports/new" className="action-card"><FilePlus2/><b>Report</b><span>Submit an incident</span></Link><Link to="/tips/new" className="action-card green"><ShieldAlert/><b>Anonymous Tip</b><span>Share safely</span></Link></div><Link className="portal-card" to="/public"><ShieldCheck/><div><b>Public Viewing Portal</b><span>Official alerts and verified incidents</span></div></Link><Link className="portal-card emergency" to="/contacts"><Phone/><div><b>Emergency Directory</b><span>Verified law enforcement & rescue contacts</span></div></Link><section><h2>Current Wanted Notices</h2><div className="cards">{publicWanted.filter(w=>!w.isDeleted).map(w=><WantedCard key={w.id} wanted={w}/>)||<Empty message="No wanted notices available"/>}</div></section><section><PendingDrafts title="Pending Reports (This Device)" collectionName="reports" ownerField="reporterId" ownerId={user.id}/><h2>My Reports</h2>{own.length?own.map(r=><ReportCard key={r.id} report={r} setToast={setToast}/>):<Empty message="You haven't submitted any reports yet"/>}</section></div>:<div className="content"><h2>Public Alerts</h2>{alerts.length?alerts.map(a=><AlertCard alert={a} key={a.id}/>):<Empty message="No current emergency broadcasts"/>}<h2>My Secure Tips</h2>{tips.filter(t=>t.submitterId===user.id).map(t=><TipCard key={t.id} tip={t}/>)||<Empty message="No secure tips sent yet"/>}</div>}</Dashboard>}
