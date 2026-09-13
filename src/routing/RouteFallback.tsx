import { LoaderCircle } from 'lucide-react'

export function RouteFallback(){
  return <div className="empty"><LoaderCircle className="spin"/><p>Loading…</p></div>
}
