import { type ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { type Role, type User } from '../domain'

export function Protected({user,role,children}:{user?:User;role?:Role;children:ReactNode}){if(!user)return <Navigate to="/sign-in" replace/>;if(role&&user.role!==role&&user.role!=='ADMIN')return <Navigate to="/dashboard" replace/>;if(role==='LAW_ENFORCER'&&!user.isApproved)return <Navigate to="/dashboard" replace/>;return <>{children}</>}
