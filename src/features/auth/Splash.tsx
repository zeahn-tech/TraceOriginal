import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { type User } from '../../domain'
import { logo } from '../../shared/assets'
import { routeFor } from './routeFor'

export function Splash({user,loading}:{user?:User;loading:boolean}){const nav=useNavigate();useEffect(()=>{if(!loading){const id=setTimeout(()=>nav(user?routeFor(user):'/welcome',{replace:true}),2500);return()=>clearTimeout(id)}},[user,loading,nav]);return <main className="splash"><img className="splash-logo" src={logo} alt="TraceNet Liberia" width="200" height="200" fetchPriority="high" decoding="async"/><p>Securing Communities Together</p></main>}
