import { useLocation } from 'react-router-dom'
import { Flag, Map as MapIcon } from 'lucide-react'
import { type Report } from '../../domain'
import { Page } from '../../shared/ui'

export function MapPage({reports}:{reports:Report[]}){const loc=useLocation();const params=new URLSearchParams(loc.search);const lat=params.get('lat');const lng=params.get('lng');return <Page title="Incident Map"><div className="map"><MapIcon size={64}/><h2>Incident Map</h2><p>{lat&&lng?`Emergency target: ${lat}, ${lng}`:'Configure a browser-restricted maps provider key to display this map.'}</p><div className="markers">{reports.filter(r=>r.latitude||r.longitude).map(r=><div key={r.id}><Flag/>{r.title}<small>{r.latitude.toFixed(4)}, {r.longitude.toFixed(4)}</small></div>)}</div></div></Page>}
