import { useState } from 'react'
import { authErrorMessage, session } from '../../services'
import { Dialog, Input } from '../../shared/ui'
import type { Toast } from '../../shared/types'

export function PasswordDialog({close,setToast}:{close:()=>void;setToast:(t:Toast)=>void}){const[oldPass,setOldPass]=useState('');const[next,setNext]=useState('');const[confirm,setConfirm]=useState('');return <Dialog title="Change Password" close={close}><form className="form" onSubmit={async e=>{e.preventDefault();if(next.length<6||next!==confirm){setToast({message:'Passwords must match and be at least 6 characters.',kind:'error'});return}try{await session.password(oldPass,next);setToast({message:'Password updated successfully.'});close()}catch(err){setToast({message:authErrorMessage(err,'Unable to change password'),kind:'error'})}}}><Input label="Current Password" value={oldPass} onChange={setOldPass} type="password"/><Input label="New Password" value={next} onChange={setNext} type="password"/><Input label="Confirm New Password" value={confirm} onChange={setConfirm} type="password"/><button className="primary-button">UPDATE PASSWORD</button></form></Dialog>}
