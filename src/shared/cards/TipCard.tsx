import { Check } from 'lucide-react'
import { write } from '../../services'
import { type Tip } from '../../domain'
import type { Toast } from '../types'

export function TipCard({tip,operational,setToast}:{tip:Tip;operational?:boolean;setToast?:(t:Toast)=>void}){return <article className="record-card"><span className={`status ${tip.isReviewed?'verified':'pending'}`}>{tip.isReviewed?'REVIEWED':'PENDING REVIEW'}</span><p>{tip.content}</p><small>{tip.isAnonymous?'Anonymous source':'Known source'} · {tip.aiScreeningStatus}</small>{operational&&<button onClick={async()=>{await write('tips',{...tip,isReviewed:true});setToast?.({message:'Tip marked as reviewed'})}}><Check/> Mark Reviewed</button>}</article>}
