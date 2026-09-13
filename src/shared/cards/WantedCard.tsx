import { UserCircle } from 'lucide-react'
import { verifyWantedNotice } from '../../services'
import { type Wanted } from '../../domain'
import { MediaThumb } from '../../components/MediaAsset'

export function WantedCard({wanted,operational,admin}:{wanted:Wanted;operational?:boolean;admin?:boolean}){return <article className="wanted-card">{wanted.imageUrls[0]?<MediaThumb url={wanted.imageUrls[0]} kind="image" size="wanted" alt={wanted.name}/>:<UserCircle size={64}/>}<div><span className={`status ${wanted.isVerified?'verified':'pending'}`}>{wanted.isVerified?'VERIFIED':'SUBMITTED'}</span><h3>{wanted.name}</h3><p>{wanted.description}</p><small>Last seen: {wanted.lastSeen||'Not stated'}</small>{wanted.reward&&<b>Reward: {wanted.reward}</b>}{operational&&<button onClick={()=>verifyWantedNotice({id:wanted.id,verify:!wanted.isVerified})}>{wanted.isVerified?'DISMISS NOTICE':'VERIFY NOTICE'}</button>}</div></article>}
