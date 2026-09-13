import { useState } from 'react'
import { authErrorMessage, session } from '../../services'
import { Dialog, Input } from '../../shared/ui'
import type { Toast } from '../../shared/types'

export function ResetDialog({initial,close,setToast}:{initial:string;close:()=>void;setToast:(t:Toast)=>void}){const[email,setEmail]=useState(initial);return <Dialog title="Password Recovery" close={close}><p>Enter your account email and we will send a secure password reset link.</p><form className="form" onSubmit={async e=>{e.preventDefault();try{await session.reset(email);setToast({message:'Password reset link sent.'});close()}catch(err){setToast({message:authErrorMessage(err,'Could not send reset link'),kind:'error'})}}}><Input label="Email Address" value={email} onChange={setEmail} type="email"/><button className="primary-button">SEND RESET LINK</button></form></Dialog>}
