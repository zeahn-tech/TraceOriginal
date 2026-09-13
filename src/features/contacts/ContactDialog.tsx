import { useState } from 'react'
import { uuid } from '../../domain'
import { Dialog, Input, Select } from '../../shared/ui'
import { counties } from '../../shared/assets'

export function ContactDialog({close,add}:{close:()=>void;add:(c:{id:string;agency:string;office:string;county:string;person:string;position:string;phone:string;email:string;status:string;category:string})=>void}){const[agency,setAgency]=useState('');const[office,setOffice]=useState('');const[county,setCounty]=useState('Montserrado');const[phone,setPhone]=useState('');return <Dialog title="Add Emergency Contact" close={close}><form className="form" onSubmit={e=>{e.preventDefault();add({id:uuid(),agency,office,county,person:'Contact Person',position:'Emergency Contact',phone,email:'',status:'Available',category:'Police'});close()}}><Input label="Agency Name" value={agency} onChange={setAgency}/><Input label="Office Name" value={office} onChange={setOffice}/><Select label="County" value={county} onChange={setCounty} options={counties}/><Input label="Phone Number" value={phone} onChange={setPhone}/><button className="primary-button">SAVE CONTACT</button></form></Dialog>}
