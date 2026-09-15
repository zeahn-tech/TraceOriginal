import { useState } from 'react'
import { ImageOff, Video as VideoIcon, Volume2 } from 'lucide-react'
import { Dialog } from '../shared/ui'

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

const openOnKey = (open:()=>void) => (e:React.KeyboardEvent) => {
  if(e.key==='Enter' || e.key===' '){ e.preventDefault(); open() }
}

/**
 * Renders one piece of evidence (image, video, or audio) with:
 *  - lazy loading: native `loading="lazy"` for images; `preload="none"` for
 *    video/audio, the standards-based equivalent since neither element has a
 *    `loading` attribute
 *  - missing/broken-media handling: an `onError` handler swaps in a
 *    same-sized fallback tile instead of a broken-image icon or a silently
 *    failing player
 *  - a full-size viewer: tapping/clicking an image or video thumbnail opens
 *    it at readable size with real playback controls (see the "Fixed"
 *    note below) via the existing `Dialog` component — reusing the same
 *    `.scrim`/`.dialog` styling already used everywhere else in the app,
 *    so no new visual language is introduced for this.
 *
 * Fixed: video thumbnails used to be rendered `muted` with no `controls`
 * and no way to interact with them at all — there was previously no way,
 * on any platform, to actually watch a video attachment. The small inline
 * thumbnail stays muted/non-interactive (it's a preview, not a player,
 * same as a video-call "camera off" tile), but it's now clickable/focusable
 * and opens a real, unmuted, full-size `<video controls>` in a dialog.
 * `playsInline` is kept on both so tapping play never forces iOS into its
 * native fullscreen video player.
 *
 * The always-a-single-root-element contract from before is preserved for
 * CSS purposes: the interactivity is added directly to the `<img>`/`<video>`
 * element itself (onClick/role=button/tabIndex), not via a wrapping
 * element, and the conditionally-rendered `Dialog` lives in a `<>` Fragment
 * alongside it — Fragments add no DOM node, so `.thumbs img` and
 * `.wanted-card>img` still match exactly the same element they always did.
 */
export function MediaThumb({ url, kind, size='thumb', alt='Evidence' }:{ url?:string; kind?:Kind; size?:Size; alt?:string }){
  const [failed, setFailed] = useState(!url)
  const [expanded, setExpanded] = useState(false)
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
    <>
      <video
        src={url}
        muted
        playsInline
        preload="none"
        onError={()=>setFailed(true)}
        onClick={()=>setExpanded(true)}
        onKeyDown={openOnKey(()=>setExpanded(true))}
        role="button"
        tabIndex={0}
        aria-label={`Play ${alt}`}
        style={{ width:dims.width, height:dims.height, borderRadius:dims.borderRadius, objectFit:'cover', flexShrink:0, cursor:'pointer' }}
      />
      {expanded && (
        <Dialog title={alt} close={()=>setExpanded(false)}>
          <video
            src={url}
            controls
            playsInline
            preload="metadata"
            autoFocus
            style={{ width:'100%', maxHeight:'70vh', borderRadius:12, background:'#000' }}
          />
        </Dialog>
      )}
    </>
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

  return (
    <>
      <img
        src={url}
        alt={alt}
        loading="lazy"
        decoding="async"
        onError={()=>setFailed(true)}
        onClick={()=>setExpanded(true)}
        onKeyDown={openOnKey(()=>setExpanded(true))}
        role="button"
        tabIndex={0}
        style={{ cursor:'pointer' }}
      />
      {expanded && (
        <Dialog title={alt} close={()=>setExpanded(false)}>
          <img src={url} alt={alt} style={{ width:'100%', borderRadius:12 }}/>
        </Dialog>
      )}
    </>
  )
}
