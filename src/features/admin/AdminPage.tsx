import { useState } from 'react'
import { Bell } from 'lucide-react'
import { type Alert, type Report, type Tip, type User, type Wanted } from '../../domain'
import { Empty, Page, Stat, Tabs } from '../../shared/ui'
import { AlertCard } from '../../shared/cards/AlertCard'
import { WantedCard } from '../../shared/cards/WantedCard'
import { OperationalReport } from '../reports/OperationalReport'
import type { Toast } from '../../shared/types'
import { AlertDialog } from './AlertDialog'
import { ApprovalCard } from './ApprovalCard'
import { SettingsPanel } from './SettingsPanel'
import { UserCard } from './UserCard'

export function AdminPage({reports,tips,alerts,wanted,users,setToast}:{reports:Report[];tips:Tip[];alerts:Alert[];wanted:Wanted[];users:User[];setToast:(t:Toast)=>void}){const[tab,setTab]=useState('Reports');const[alertDialog,setAlertDialog]=useState(false);return <Page eyebrow="ADMINISTRATION NODE" title="COMMAND CENTER" actions={<button className="icon-button" onClick={()=>setAlertDialog(true)}><Bell/></button>}><div className="stat-row"><Stat n={reports.length} l="Reports"/><Stat n={tips.length} l="Tips"/><Stat n={wanted.length} l="Notices"/></div><Tabs tabs={['Reports','Approvals','Wanted','Alerts','Users','Settings']} value={tab} set={setTab}/>{tab==='Reports'&&<div className="cards">{reports.map(r=><OperationalReport key={r.id} report={r} setToast={setToast} admin/>)}</div>}{tab==='Approvals'&&<div className="cards">{users.filter(u=>u.role==='LAW_ENFORCER'&&!u.isApproved).map(u=><ApprovalCard key={u.id} user={u} setToast={setToast}/>)||<Empty message="No officer approvals pending"/>}</div>}{tab==='Wanted'&&<div className="cards">{wanted.map(w=><WantedCard key={w.id} wanted={w} operational admin/>)}</div>}{tab==='Alerts'&&<div className="cards">{alerts.map(a=><AlertCard key={a.id} alert={a} admin/>)}</div>}{tab==='Users'&&<div className="cards">{users.map(u=><UserCard key={u.id} user={u} setToast={setToast}/>)}</div>}{tab==='Settings'&&<SettingsPanel setToast={setToast}/>} {alertDialog&&<AlertDialog close={()=>setAlertDialog(false)} setToast={setToast}/>}</Page>}
