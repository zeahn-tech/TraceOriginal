import { useEffect, useState } from 'react'
import { createMediaPreview, revokeMediaPreview } from '../services'

type MediaPreviewProps = { file?:Blob; url?:string; alt?:string; className?:string; controls?:boolean }

export function MediaPreview({file,url,alt='Media preview',className,controls=true}:MediaPreviewProps){
  const [previewUrl,setPreviewUrl] = useState(url)
  // Object URL lifecycle (create on file/url change, revoke on cleanup) must be
  // managed imperatively in an effect; computing it during render would leak
  // blob URLs since there would be no reliable point to call revokeObjectURL.
  useEffect(()=>{
    if(!file){
      // eslint-disable-next-line react-hooks/set-state-in-effect -- see comment above
      setPreviewUrl(url)
      return
    }
    const objectUrl=createMediaPreview(file)
    setPreviewUrl(objectUrl)
    return ()=>revokeMediaPreview(objectUrl)
  },[file,url])
  if(!previewUrl) return null
  const type=file?.type ?? ''
  if(type.startsWith('video/') || (!file && /\.(mp4|webm|mov)(\?|$)/i.test(previewUrl))) return <video className={className} src={previewUrl} controls={controls} playsInline preload="metadata" />
  if(type.startsWith('audio/') || (!file && /\.(mp3|wav|ogg|m4a|webm)(\?|$)/i.test(previewUrl))) return <audio className={className} src={previewUrl} controls={controls} preload="metadata" />
  return <img className={className} src={previewUrl} alt={alt} loading="lazy" />
}
