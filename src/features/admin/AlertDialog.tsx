import { useState } from 'react'
import { publishAlert } from '../../services'
import { Dialog, Input, Select, TextArea } from '../../shared/ui'
import type { Toast } from '../../shared/types'

export function AlertDialog({close,setToast}:{close:()=>void;setToast:(t:Toast)=>void}){const[title,setTitle]=useState('');const[content,setContent]=useState('');const[location,setLocation]=useState('');const[urgency,setUrgency]=useState('3');return <Dialog title="Create Emergency Alert" close={close}><form className="form" onSubmit={async e=>{e.preventDefault();await publishAlert({title,content,urgency:Number(urgency),locationName:location,latitude:0,longitude:0});setToast({message:'Emergency alert broadcast.'});close()}}><Input label="Alert Title" value={title} onChange={setTitle}/><TextArea label="Alert Content" value={content} onChange={setContent}/><Input label="Location Name" value={location} onChange={setLocation}/><Select label="Urgency" value={urgency} onChange={setUrgency} options={[{value:'1',label:'Low'},{value:'2',label:'Medium'},{value:'3',label:'High'}]}/><button className="primary-button danger">BROADCAST ALERT</button></form></Dialog>}
