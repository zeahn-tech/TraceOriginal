import { type User } from '../../domain'

export function routeFor(user:User){return user.role==='ADMIN'?'/admin':user.role==='LAW_ENFORCER'&&user.isApproved?'/operations':'/dashboard'}
