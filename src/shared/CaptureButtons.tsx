import { useEffect, useRef, useState } from 'react'
import { Camera, Mic, Square, Video as VideoIcon } from 'lucide-react'
import { capabilities, captureMedia, startRecording, type MediaRecording } from '../services'
import { Dialog } from './ui'
import type { Toast } from './types'

type Kind = 'photo'|'video'|'audio'

/**
 * Fixed: `captureMedia`/`startRecording` (formerly `recordMedia`) already
 * existed in services.ts but were never called from any component — the
 * shipped evidence-attachment UI was file-picker-only. This wires both in:
 *
 *  - "Take Photo" hands off to the platform's own camera app via
 *    `captureMedia('image', ...)` (`input.capture='environment'`) — the
 *    right choice for a phone, where the native camera app is faster and
 *    better than an in-page one.
 *  - "Record Video"/"Record Audio" use `startRecording()` for an in-page
 *    getUserMedia/MediaRecorder capture with a live preview and a Stop
 *    button — this is what makes recording possible at all on a desktop
 *    browser (no native camera app to hand off to), and gives every
 *    platform a working microphone-recording path, closing the gap noted
 *    in CROSS_PLATFORM_TEST_REPORT.md §2.2 and §5.3.
 *
 * Gracefully degrades: `capabilities.mediaRecorder` is feature-detected
 * before showing the Record buttons at all, so a browser without
 * MediaRecorder support (or one where getUserMedia is blocked, e.g. no
 * camera/mic hardware) simply doesn't offer them — the plain file picker
 * next to this component remains the universal fallback either way.
 */
export function CaptureButtons({onFiles,setToast,allow=['photo','video','audio']}:{onFiles:(files:File[])=>void;setToast:(t:Toast)=>void;allow?:Kind[]}){
  const [recordingKind,setRecordingKind]=useState<'video'|'audio'>()
  const [recording,setRecording]=useState<MediaRecording>()
  const [elapsed,setElapsed]=useState(0)
  const videoRef=useRef<HTMLVideoElement>(null)

  useEffect(()=>{
    if(!recording || recordingKind!=='video' || !videoRef.current) return
    videoRef.current.srcObject=recording.stream
  },[recording,recordingKind])

  useEffect(()=>{
    if(!recording) return
    const id=window.setInterval(()=>setElapsed(v=>v+1),1000)
    return ()=>window.clearInterval(id)
  },[recording])

  const takePhoto=async()=>{
    try{ const files=await captureMedia('image','image/*'); if(files.length) onFiles(files) }
    catch{ setToast({message:'Unable to open the camera.',kind:'error'}) }
  }

  const beginRecording=async(kind:'video'|'audio')=>{
    if(!capabilities.mediaRecorder){
      setToast({message:`${kind==='video'?'Video':'Audio'} recording isn't supported by this browser — use the file picker instead.`,kind:'error'})
      return
    }
    try{
      const controller=await startRecording(kind)
      setElapsed(0); setRecordingKind(kind); setRecording(controller)
      const file=await controller.result
      setRecording(undefined); setRecordingKind(undefined)
      onFiles([file])
    }catch(err){
      setRecording(undefined); setRecordingKind(undefined)
      setToast({message:err instanceof Error?err.message:'Recording failed. Check camera/microphone permission.',kind:'error'})
    }
  }

  const mm=String(Math.floor(elapsed/60)).padStart(2,'0'); const ss=String(elapsed%60).padStart(2,'0')

  return <>
    <div className="capture-row">
      {allow.includes('photo') && <button type="button" className="capture-button" onClick={takePhoto}><Camera size={16}/> Take Photo</button>}
      {allow.includes('video') && capabilities.mediaRecorder && <button type="button" className="capture-button" onClick={()=>beginRecording('video')}><VideoIcon size={16}/> Record Video</button>}
      {allow.includes('audio') && capabilities.mediaRecorder && <button type="button" className="capture-button" onClick={()=>beginRecording('audio')}><Mic size={16}/> Record Audio</button>}
    </div>
    {recording && (
      <Dialog title={recordingKind==='video' ? 'Recording video' : 'Recording audio'} close={()=>recording.stop()}>
        <div className="recording-live">
          {recordingKind==='video' && <video ref={videoRef} autoPlay muted playsInline/>}
          <span className="rec-dot"><span/> REC {mm}:{ss}</span>
          <button type="button" className="primary-button danger" onClick={()=>recording.stop()}><Square size={16}/> Stop &amp; Attach</button>
        </div>
      </Dialog>
    )}
  </>
}
