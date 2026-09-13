import { Siren, Edit3 } from 'lucide-react'
import { write } from '../../services'
import { type Alert } from '../../domain'

export function AlertCard({alert,admin}:{alert:Alert;admin?:boolean}){return <article className={`alert-card u${alert.urgency}`}><Siren/><div><span>URGENT LEVEL {alert.urgency}</span><h3>{alert.title}</h3><p>{alert.content}</p><small>{alert.locationName}</small></div>{admin&&<button className="icon-button" onClick={()=>write('alerts',{...alert,title:`UPDATED: ${alert.title}`})}><Edit3/></button>}</article>}
