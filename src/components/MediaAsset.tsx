import { useState } from 'react'
import { ImageOff, Video as VideoIcon, Volume2 } from 'lucide-react'

type Kind = 'image'|'video'|'audio'
type Size = 'thumb'|'wanted'

// Mirrors the existing `.thumbs img` (60x60, 8px radius) and
// `.wanted-card>img` (80x96, 12px radius) rules in styles.css exactly, so
// video tiles and the missing-media fallback line up pixel-for-pixel with
// the existing image tiles without touching the stylesheet.
const DIMENSIONS: Record<Size,{ width:number; height:number; borderRadius:number }> = {
  thumb: { width:60, height:60, borderRadius:8 },
  wanted: { width:80, height:96, borderRadius:12 },
}

function kindFromUrl(url:string):Kind {
  if(/\.(mp4|webm|mov|m4v)(\?|$)/i.test(url)) return 'video'
  if(/\.(mp3|wav|ogg|m4a|weba)(\?|$)/i.test(url)) return 'audio'
  return 'image'
}

/**
 * Renders one piece of evidence (image, video, or audio) with:
 *  - lazy loading: native `loading="lazy"` for images; `preload="none"` for
 *    video/audio, the standards-based equivalent since neither element has a
 *    `loading` attribute
 *  - missing/broken-media handling: an `onError` handler swaps in a
 *    same-sized fallback tile instead of a broken-image icon or a silently
 *    failing player
 *
 * Always renders a single root element (never a wrapper <div>) so it drops
 * into existing tag-based CSS selectors like `.thumbs img` and
 * `.wanted-card>img` without changing how the working case looks.
 */
export function MediaThumb({ url, kind, size='thumb', alt='Evidence' }:{ url?:string; kind?:Kind; size?:Size; alt?:string }){
  const [failed, setFailed] = useState(!url)
  const resolvedKind = kind ?? (url ? kindFromUrl(url) : 'image')
  const dims = DIMENSIONS[size]

  if(failed || !url){
    const Icon = resolvedKind==='video' ? VideoIcon : resolvedKind==='audio' ? Volume2 : ImageOff
    return (
      <span
        className="media-fallback"
        role="img"
        aria-label={`${alt} unavailable`}
        style={{ width:dims.width, height:dims.height, borderRadius:dims.borderRadius, display:'flex', alignItems:'center', justifyContent:'center', background:'#eceff1', color:'#90a4ae', flexShrink:0 }}
      >
        <Icon size={Math.round(dims.height * 0.4)}/>
      </span>
    )
  }

  if(resolvedKind==='video') return (
    <video
      src={url}
      muted
      playsInline
      preload="none"
      onError={()=>setFailed(true)}
      style={{ width:dims.width, height:dims.height, borderRadius:dims.borderRadius, objectFit:'cover', flexShrink:0 }}
    />
  )

  if(resolvedKind==='audio') return (
    <audio
      src={url}
      controls
      preload="none"
      onError={()=>setFailed(true)}
      style={{ width:'100%', height:36 }}
    />
  )

  return <img src={url} alt={alt} loading="lazy" decoding="async" onError={()=>setFailed(true)}/>
}
