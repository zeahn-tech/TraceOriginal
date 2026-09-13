import { UserCircle } from 'lucide-react'
import { setUserStatus } from '../../services'
import { label, type User } from '../../domain'
import type { Toast } from '../../shared/types'

export function UserCard({user,setToast}:{user:User;setToast:(t:Toast)=>void}){return <article className="record-card"><UserCircle/><h3>{user.name}</h3><p>{user.email}</p><small>{label(user.role)} · {user.status||'ACTIVE'} · Reputation {user.reputationScore??100}</small><div className="card-actions"><button onClick={async()=>{await setUserStatus({uid:user.id,status:user.status==='SUSPENDED'?'ACTIVE':'SUSPENDED'});setToast({message:'User status updated.'})}}>TOGGLE STATUS</button><button className="danger-text" onClick={async()=>{await setUserStatus({uid:user.id,status:'BANNED'});setToast({message:'User banned.'})}}>BAN</button></div></article>}
