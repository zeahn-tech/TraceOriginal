import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { LoaderCircle } from 'lucide-react'
import { authErrorMessage, session } from '../../services'
import { type Role } from '../../domain'
import { Brand, Input, Select } from '../../shared/ui'
import { logo } from '../../shared/assets'
import type { Toast } from '../../shared/types'
import { ResetDialog } from './ResetDialog'

export function AuthPage({setToast}:{setToast:(t:Toast)=>void}){const nav=useNavigate();const[email,setEmail]=useState('');const[password,setPassword]=useState('');const[role,setRole]=useState<Role>('CITIZEN');const[busy,setBusy]=useState(false);const[forgot,setForgot]=useState(false);const submit=async(e:FormEvent)=>{e.preventDefault();setBusy(true);try{await session.signIn(email.trim(),password);setToast({message:'Signed in successfully'});nav('/') }catch(err){setToast({message:authErrorMessage(err,'Sign in failed'),kind:'error'})}finally{setBusy(false)}};return <main className="auth-page"><img className="auth-logo" src={logo}/><Brand/><h1>SIGN IN</h1><p className="auth-copy">Secure access to TraceNet Liberia</p><form className="auth-form" onSubmit={submit}><Input label="Email Address" value={email} onChange={setEmail} type="email"/><Input label="Password" value={password} onChange={setPassword} type="password"/><button type="button" className="auth-link right" onClick={()=>setForgot(true)}>Forgot Password?</button><Select label="Access Level" value={role} onChange={v=>setRole(v as Role)} options={['CITIZEN','LAW_ENFORCER','ADMIN']}/><button className="outline-button" disabled={busy}>{busy?<LoaderCircle className="spin"/>:'SIGN IN'}</button><button type="button" className="google" onClick={async()=>{try{await session.google();nav('/')}catch(err){setToast({message:authErrorMessage(err,'Google sign in failed'),kind:'error'})}}}>G <b>SIGN IN WITH GOOGLE</b></button><Link className="auth-link" to="/sign-up">Create a new account</Link><Link className="auth-link" to="/public">Continue as Public Viewer</Link></form>{forgot&&<ResetDialog initial={email} close={()=>setForgot(false)} setToast={setToast}/>}</main>}
