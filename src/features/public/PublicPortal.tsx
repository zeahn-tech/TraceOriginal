import { Link } from 'react-router-dom'
import { ArrowLeft, Info, Send } from 'lucide-react'
import { type Alert, type Report, type Wanted } from '../../domain'
import { Brand, Empty, Notice } from '../../shared/ui'
import { AlertCard } from '../../shared/cards/AlertCard'
import { ReportCard } from '../../shared/cards/ReportCard'
import { WantedCard } from '../../shared/cards/WantedCard'
import { Carousel } from '../dashboard/Carousel'

export function PublicPortal({alerts,reports,wanted}:{alerts:Alert[];reports:Report[];wanted:Wanted[]}){return <main className="public-page"><header className="public-header"><Link to="/welcome"><ArrowLeft/></Link><Brand/><button onClick={()=>navigator.share?.({title:'TraceNet Liberia',text:'TraceNet public safety platform',url:location.href})}><Send/></button></header><Carousel/><section><h1>OFFICIAL PUBLIC SAFETY PORTAL</h1><p>Verified alerts, incidents and wanted notices from TraceNet Liberia.</p></section><section><h2>Active Alerts</h2>{alerts.map(a=><AlertCard key={a.id} alert={a}/>)||<Empty message="No active official alerts"/>}</section><section><h2>Verified Incidents</h2>{reports.filter(r=>r.status==='VERIFIED'&&!r.isDeleted).map(r=><ReportCard key={r.id} report={r}/>)||<Empty message="No verified incidents available"/>}</section><section><h2>Wanted Persons</h2>{wanted.filter(w=>w.isVerified&&!w.isDeleted).map(w=><WantedCard key={w.id} wanted={w}/>)||<Empty message="No verified wanted notices available"/>}</section><Notice icon={<Info/>}>Information is verified before publication. Do not approach suspects; contact law enforcement with relevant information.</Notice></main>}
