import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Contact, Plus } from 'lucide-react'
import { type Report, type Tip, type User, type Wanted } from '../../domain'
import { Page, Tabs } from '../../shared/ui'
import { TipCard } from '../../shared/cards/TipCard'
import { WantedCard } from '../../shared/cards/WantedCard'
import { OperationalReport } from '../reports/OperationalReport'
import { PendingDrafts } from '../reports/PendingDrafts'
import type { Toast } from '../../shared/types'

export function OperationsPage({user,reports,tips,wanted,setToast}:{user:User;reports:Report[];tips:Tip[];wanted:Wanted[];setToast:(t:Toast)=>void}){const[tab,setTab]=useState('Incidents');return <Page eyebrow="LAW ENFORCEMENT NODE" title="OPERATIONS" actions={<Link className="small-primary" to="/operations/wanted/new"><Plus/> Post Criminal</Link>}><Tabs tabs={['Incidents','Tips','Wanted','Directory']} value={tab} set={setTab}/>{tab==='Incidents'&&<div className="cards">{reports.filter(r=>!r.isDeleted).map(r=><OperationalReport key={r.id} report={r} setToast={setToast}/>)}</div>}{tab==='Tips'&&<div className="cards">{tips.filter(t=>!t.isDeleted).map(t=><TipCard key={t.id} tip={t} operational setToast={setToast}/>)}</div>}{tab==='Wanted'&&<div className="cards"><PendingDrafts title="Pending Wanted Notices (This Device)" collectionName="wanted_criminals"/>{wanted.filter(w=>!w.isDeleted).map(w=><WantedCard key={w.id} wanted={w} operational/> )}</div>}{tab==='Directory'&&<Link className="directory-open" to="/contacts"><Contact/><span><b>Emergency Contacts Management</b><small>Manage, verify, and toggle visibility of emergency response units across Liberia.</small></span></Link>}</Page>}
