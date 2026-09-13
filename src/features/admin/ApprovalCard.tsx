import { ShieldCheck } from 'lucide-react'
import { approveOfficer, setUserStatus } from '../../services'
import { type User } from '../../domain'
import type { Toast } from '../../shared/types'

export function ApprovalCard({user,setToast}:{user:User;setToast:(t:Toast)=>void}){return <article className="record-card"><ShieldCheck/><h3>{user.name}</h3><p>{user.email}</p><small>Badge: {user.badgeNumber||'Not supplied'} · {user.contact||'No contact'}</small><div className="card-actions"><button onClick={async()=>{await approveOfficer({uid:user.id});setToast({message:'Law enforcer account approved.'})}}>APPROVE</button><button className="danger-text" onClick={async()=>{await setUserStatus({uid:user.id,status:'REJECTED'});setToast({message:'Application rejected.'})}}>REJECT</button></div></article>}
