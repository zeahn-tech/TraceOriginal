import Dexie, { type EntityTable } from 'dexie'
import { initializeApp, getApps } from 'firebase/app'
import { getAuth, GoogleAuthProvider, onAuthStateChanged, signInWithEmailAndPassword, createUserWithEmailAndPassword, signInWithPopup, sendPasswordResetEmail, signOut, updatePassword, reauthenticateWithCredential, EmailAuthProvider, connectAuthEmulator, type User as FirebaseUser } from 'firebase/auth'
import { initializeFirestore, persistentLocalCache, persistentMultipleTabManager, collection, doc, getDoc, onSnapshot, runTransaction, serverTimestamp, setDoc, addDoc, query, orderBy, where, connectFirestoreEmulator, type Unsubscribe } from 'firebase/firestore'
import { getStorage, ref, uploadBytesResumable, getDownloadURL, deleteObject, connectStorageEmulator } from 'firebase/storage'
import { getFunctions, httpsCallable, type Functions } from 'firebase/functions'
import type { Alert, Report, Role, Tip, User, Wanted } from './domain'

// ---------------------------------------------------------------------------
// Firebase bootstrap
// ---------------------------------------------------------------------------
const config = { apiKey:import.meta.env.VITE_FIREBASE_API_KEY, authDomain:import.meta.env.VITE_FIREBASE_AUTH_DOMAIN, projectId:import.meta.env.VITE_FIREBASE_PROJECT_ID, storageBucket:import.meta.env.VITE_FIREBASE_STORAGE_BUCKET, appId:import.meta.env.VITE_FIREBASE_APP_ID, messagingSenderId:import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID }
export const firebaseReady = Boolean(config.apiKey && config.projectId && config.appId)
export const app = firebaseReady ? (getApps()[0] ?? initializeApp(config)) : undefined
export const auth = app ? getAuth(app) : undefined
export const db = app ? initializeFirestore(app,{localCache:persistentLocalCache({tabManager:persistentMultipleTabManager()})}) : undefined
export const storage = app ? getStorage(app) : undefined
export const functions: Functions | undefined = app ? getFunctions(app) : undefined

// Opt-in, dev/test-only connection to the local Firebase Emulator Suite.
// Only takes effect when VITE_USE_FIREBASE_EMULATOR is explicitly set to
// 'true' at build time (see playwright.config.ts's `preview` webServer) —
// unset in every normal build, so this block never runs in production and
// never touches the mocked firebase/* modules used by the vitest suites
// (services.test.ts etc. mock these modules out entirely and never set the
// env var, so this branch is simply skipped there, same as `firebaseReady`).
if (app && import.meta.env.VITE_USE_FIREBASE_EMULATOR === 'true') {
  if (auth) connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true })
  if (db) connectFirestoreEmulator(db, '127.0.0.1', 8080)
  if (storage) connectStorageEmulator(storage, '127.0.0.1', 9199)
}

// ---------------------------------------------------------------------------
// Firebase App Check — proves a request comes from a genuine instance of
// this app, not a script/bot/forged client hitting Firestore/Storage/
// Functions directly. VITE_RECAPTCHA_SITE_KEY is a reCAPTCHA v3 SITE key,
// which is public by design (Google's own reCAPTCHA docs: site keys are
// meant to be embedded in client-side code). The paired SECRET key that
// actually verifies challenges lives entirely on Google's servers and is
// never held by this app, in the browser or otherwise — see
// ABUSE_PROTECTION_REPORT.md for the full audit of what is and isn't safe
// to expose here.
//
// App Check on its own only *authenticates the app instance* — it does not
// replace firestore.rules/storage.rules (identity/role authorization) or
// the per-user quotas below (abuse limits). All three layers are meant to
// operate together. Enforcement toward Firestore/Storage/Functions must
// also be turned on in the Firebase Console (a project-level setting, not
// expressible in code) — see the report for the exact steps.
if(app && import.meta.env.VITE_RECAPTCHA_SITE_KEY){
  void import('firebase/app-check').then(({ initializeAppCheck, ReCaptchaV3Provider }) => {
    initializeAppCheck(app, {
      provider: new ReCaptchaV3Provider(import.meta.env.VITE_RECAPTCHA_SITE_KEY),
      isTokenAutoRefreshEnabled: true,
    })
  })
}

// ---------------------------------------------------------------------------
// Browser-capability detection (requirement: browser compatibility)
// Every offline/media feature degrades gracefully instead of throwing when a
// capability is missing, rather than assuming every browser supports it.
// ---------------------------------------------------------------------------
export const capabilities = {
  indexedDb: typeof indexedDB !== 'undefined',
  serviceWorker: 'serviceWorker' in navigator,
  backgroundSync: 'serviceWorker' in navigator && 'SyncManager' in window,
  mediaRecorder: typeof MediaRecorder !== 'undefined' && Boolean(navigator.mediaDevices?.getUserMedia),
  createImageBitmap: typeof createImageBitmap !== 'undefined',
}

// ---------------------------------------------------------------------------
// Local persistence (Dexie / IndexedDB)
// ---------------------------------------------------------------------------
export type MediaKind = 'image'|'video'|'audio'

/**
 * Media upload state machine (per file):
 *   LOCAL_PENDING -> UPLOADING -> UPLOADED
 *                              \-> FAILED (retried back to UPLOADING)
 */
export type MediaStatus = 'LOCAL_PENDING'|'UPLOADING'|'UPLOADED'|'FAILED'

/**
 * Report/Wanted-notice draft state machine (per submission):
 *   LOCAL_PENDING -> UPLOADING -> UPLOADED -> REPORT_PENDING -> SYNCED
 *        ^               \-> FAILED <-/           \-> CONFLICT
 *        \------------------- FAILED / CONFLICT retried back to LOCAL_PENDING
 *
 * LOCAL_PENDING    saved to IndexedDB, no network activity attempted yet
 * UPLOADING        one or more attached media files are actively uploading
 * UPLOADED         every attached media file has a confirmed HTTPS download URL
 * REPORT_PENDING   media is fully uploaded; the Firestore document write is queued/in-flight
 * SYNCED           Firestore document confirmed written; terminal state, safe to prune locally
 * FAILED           an unrecoverable error occurred; safe to retry from LOCAL_PENDING
 * CONFLICT         a remote document with the same id already exists and disagrees with this draft
 */
export type DraftStatus = 'LOCAL_PENDING'|'UPLOADING'|'UPLOADED'|'REPORT_PENDING'|'SYNCED'|'FAILED'|'CONFLICT'

export type MediaFieldKey = 'imageUrls'|'videoUrls'|'audioUrls'

export type MediaJob = {
  id: string                 // stable & deterministic — see mediaJobId()
  draftId: string            // owning ReportDraft.id (foreign key)
  fieldKey: MediaFieldKey
  file: Blob
  fileName: string
  contentType: string
  size: number
  path: string                // Firebase Storage object path
  kind: MediaKind
  order: number                // preserves original selection order within its field
  status: MediaStatus
  createdAt: number
  updatedAt: number
  attempts: number
  lastError?: string
  downloadUrl?: string          // set only once status === 'UPLOADED'
}

export type ReportDraft = {
  id: string                  // `${collection}:${reportId}` — stable & deterministic
  collection: string           // 'reports' | 'wanted_criminals'
  reportId: string
  path: string                  // base Storage path, e.g. `reports/${reportId}`
  payload: Record<string, unknown>  // full document payload EXCLUDING imageUrls/videoUrls/audioUrls
  mediaJobIds: string[]
  status: DraftStatus
  createdAt: number
  updatedAt: number
  attempts: number
  lastError?: string
}

type Outbox = { id:string; type:string; payload:unknown; createdAt:number; status:'queued'|'failed'|'conflict'; attempts?:number; lastError?:string; rateLimit?:RateLimitConfig }

// ---------------------------------------------------------------------------
// Per-user daily submission quotas (report/tip/alert spam & cost-abuse protection)
// ---------------------------------------------------------------------------
// Real, server-enforced limits — not a client-side courtesy. The client-side
// atomic transaction below is what makes the everyday case correct and
// race-free; firestore.rules independently re-derives and re-checks the
// exact same constraint (see ABUSE_PROTECTION_REPORT.md), so a modified
// client cannot bypass this by skipping the transaction and writing the
// target document directly — the create rule for reports/tips/alerts
// requires the matching counter to have been incremented, server-side,
// in the very same request.
export type RateLimitConfig = { uid:string; kind:'reports'|'tips'|'alerts'; limit:number }
export const DAILY_LIMITS: Record<RateLimitConfig['kind'], number> = { reports:10, tips:15, alerts:5 }

/**
 * Atomically creates `item` in `name` AND increments the caller's rolling
 * 24-hour counter for `rateLimit.kind`, in a single Firestore transaction.
 * Throws (without creating anything) if the caller has already reached
 * their daily limit. The counter document's own field-level rules
 * (firestore.rules) independently guarantee the increment itself is
 * genuine — this function cooperates with that, it isn't the only thing
 * enforcing it.
 */
async function writeWithQuota<T extends {id:string}>(name:string, item:T, rateLimit:RateLimitConfig){
  if(!db) throw new Error('Firebase configuration is required.')
  const counterRef = doc(db,'rate_limits',`${rateLimit.uid}_${rateLimit.kind}`)
  const itemRef = doc(db,name,item.id)
  await runTransaction(db, async (tx) => {
    const counterSnap = await tx.get(counterRef)
    const now = Date.now()
    const existing = counterSnap.exists() ? (counterSnap.data() as { count:number; windowStart:{ toMillis:()=>number } }) : undefined
    const windowExpired = !existing || (now - existing.windowStart.toMillis() > 24 * 60 * 60 * 1000)
    const nextCount = windowExpired ? 1 : existing!.count + 1
    if(nextCount > rateLimit.limit){
      throw new Error(`Daily limit reached (${rateLimit.limit} ${rateLimit.kind} per day). Please try again after the window resets.`)
    }
    tx.set(itemRef, item)
    tx.set(counterRef, { uid:rateLimit.uid, kind:rateLimit.kind, count:nextCount, windowStart: windowExpired ? serverTimestamp() : existing!.windowStart })
  })
}

class TraceDb extends Dexie {
  outbox!: EntityTable<Outbox,'id'>
  reportDrafts!: EntityTable<ReportDraft,'id'>
  mediaJobs!: EntityTable<MediaJob,'id'>
  constructor(){
    super('tracenet-client')
    // v1/v2 kept for upgrade-path compatibility with any already-installed client.
    this.version(1).stores({ outbox:'id,status,createdAt', drafts:'id,updatedAt' })
    this.version(2).stores({ outbox:'id,status,createdAt', drafts:'id,updatedAt', mediaJobs:'id,status,createdAt,path' })
    // v3: replaces the old untyped `drafts`/`mediaJobs` shape with a properly
    // linked reportDrafts <-> mediaJobs schema that can express the full
    // state machine and never loses the report<->media association.
    this.version(3).stores({ outbox:'id,status,createdAt', drafts:null, reportDrafts:'id,status,createdAt,updatedAt', mediaJobs:'id,draftId,status,createdAt' })
  }
}
export const localDb = capabilities.indexedDb ? new TraceDb() : undefined

// ---------------------------------------------------------------------------
// Auth / profile / generic Firestore helpers (unchanged from prior pass)
// ---------------------------------------------------------------------------
// observeAuth ALWAYS resolves the caller's loading state — this is the one
// thing every consumer of it depends on (see useAppData.ts, which flips
// `loading` to false only inside this callback, and Splash.tsx, which only
// navigates away from the splash screen once `loading` is false). Before
// this fix, an unconfigured/failed Firebase setup (auth undefined) made
// this a permanent no-op: the callback never fired, `loading` never
// cleared, and the entire app hung on the splash screen forever with no
// error shown — public, unauthenticated routes (Welcome, Public Portal,
// Sign In, About) were unreachable even though none of them need a signed-
// in user. Firing `callback(null)` (asynchronously, matching Firebase's own
// always-async contract for onAuthStateChanged) treats "not configured" the
// same as "signed out": the app proceeds to /welcome, every public route is
// reachable, and only the auth-requiring actions themselves (session.signIn
// etc., already wired above to reject with a clear message) fail — not
// navigation.
export const observeAuth = (callback:(u:FirebaseUser|null)=>void) => {
  if (auth) return onAuthStateChanged(auth,callback)
  const id = setTimeout(() => callback(null), 0)
  return () => clearTimeout(id)
}
export const session = {
  signIn:(email:string,password:string)=> auth ? signInWithEmailAndPassword(auth,email,password) : Promise.reject(new Error('Firebase configuration is required to sign in.')),
  signUp:(email:string,password:string)=> auth ? createUserWithEmailAndPassword(auth,email,password) : Promise.reject(new Error('Firebase configuration is required to sign up.')),
  google:()=> auth ? signInWithPopup(auth,new GoogleAuthProvider()) : Promise.reject(new Error('Firebase configuration is required to sign in.')),
  reset:(email:string)=> auth ? sendPasswordResetEmail(auth,email) : Promise.reject(new Error('Firebase configuration is required to reset a password.')),
  logout:()=>auth ? signOut(auth) : Promise.resolve(),
  password:async(oldPass:string,next:string)=>{ if(!auth?.currentUser?.email) throw new Error('No email/password account is active.'); await reauthenticateWithCredential(auth.currentUser,EmailAuthProvider.credential(auth.currentUser.email,oldPass)); await updatePassword(auth.currentUser,next) }
}

/**
 * Translates Firebase Auth error codes into clear, actionable messages —
 * most importantly `auth/too-many-requests`, which IS Firebase's own
 * automatic authentication-abuse protection kicking in (a temporary,
 * Google-managed lockout after repeated failed attempts against a single
 * account/IP; not something this app configures or controls, only
 * something it must surface clearly rather than showing a raw
 * "Firebase: Error (auth/too-many-requests)." string). See
 * ABUSE_PROTECTION_REPORT.md for the full picture of what Firebase Auth
 * already protects against automatically vs. what this app adds on top.
 */
export function authErrorMessage(err:unknown, fallback:string):string {
  const code = (err && typeof err==='object' && 'code' in err) ? String((err as {code:unknown}).code) : undefined
  switch(code){
    case 'auth/too-many-requests': return 'Too many attempts. For your account\u2019s security, sign-in has been temporarily locked — please wait a few minutes and try again.'
    case 'auth/invalid-credential': case 'auth/wrong-password': case 'auth/user-not-found': return 'Incorrect email or password.'
    case 'auth/email-already-in-use': return 'An account with this email already exists.'
    case 'auth/weak-password': return 'Choose a stronger password (at least 6 characters).'
    case 'auth/invalid-email': return 'Enter a valid email address.'
    case 'auth/network-request-failed': return 'Network error. Check your connection and try again.'
    case 'auth/popup-closed-by-user': return 'Sign-in was cancelled.'
    default: return err instanceof Error ? err.message : fallback
  }
}

export async function profile(uid:string):Promise<User|undefined>{ if(!db)return; const snap=await getDoc(doc(db,'users',uid)); return snap.exists()?snap.data() as User:undefined }

const isFirebaseDownloadUrl = (value:string) => /^https:\/\/firebasestorage\.googleapis\.com\/v0\/b\/[^/]+\/o\/.+/.test(value)

/**
 * NON-NEGOTIABLE enforcement point: Firestore may only ever receive HTTPS
 * Firebase Storage download URLs for media fields — never file://, content://,
 * blob:, or any other local/temporary path. Every write path in this module
 * (write, create, syncOutbox, resolveConflict, and the draft pipeline below)
 * routes through this guard before touching Firestore.
 */
function assertRemoteMedia(item:unknown){
  if(!item || typeof item!=='object') return
  for(const [key,value] of Object.entries(item as Record<string,unknown>)){
    const isMediaField = ['imageUrls','videoUrls','audioUrls'].includes(key) || /(?:profile|idCard)Url$/i.test(key)
    if(!isMediaField) continue
    const values = Array.isArray(value) ? value : [value]
    if(values.some(entry => typeof entry!=='string' || !isFirebaseDownloadUrl(entry))) throw new Error('Only HTTPS Firebase Storage download URLs may be stored.')
  }
}

export async function saveProfile(user:User){ if(!db) throw new Error('Firebase configuration is required.'); assertRemoteMedia(user); await setDoc(doc(db,'users',user.id),user,{merge:true}) }
export function listen<T>(name:string, callback:(items:T[])=>void):Unsubscribe { if(!db){ callback([]); return ()=>{} } return onSnapshot(query(collection(db,name),orderBy('timestamp','desc')),snap=>callback(snap.docs.map(d=>({id:d.id,...d.data()}) as T))) }
/**
 * Role-scoped equivalent of listen(): applies an equality filter matching
 * what firestore.rules requires for a non-privileged `list` query to be
 * provably safe (Firestore rejects an unfiltered list unless the rule holds
 * unconditionally for every document in the collection — see
 * firebase/firestore.rules and FIREBASE_SECURITY_IMPLEMENTATION.md). Sorts
 * client-side by `timestamp` rather than combining `where` + `orderBy`
 * server-side, so no composite index needs to be deployed for these
 * intentionally small, per-user/public-safety-scale result sets.
 */
export function listenWhere<T extends { timestamp?:number }>(name:string, field:string, value:unknown, callback:(items:T[])=>void):Unsubscribe {
  if(!db){ callback([]); return ()=>{} }
  return onSnapshot(query(collection(db,name),where(field,'==',value)),snap=>callback(snap.docs.map(d=>({id:d.id,...d.data()}) as unknown as T).sort((a,b)=>(b.timestamp??0)-(a.timestamp??0))))
}

async function saveOutbox<T extends {id:string}>(name:string,item:T,status:'queued'|'failed'='queued',error?:unknown,rateLimit?:RateLimitConfig){ if(!localDb) throw new Error('Offline storage is not available in this browser.'); await localDb.outbox.put({id:item.id,type:name,payload:item,createdAt:Date.now(),status,attempts:0,lastError:error instanceof Error ? error.message : undefined,rateLimit}) }

/** Generic write for documents that never carry pending local media (tips, alerts, status/notes edits, user records, wanted-notice verification toggles). Report/Wanted *creation* with attached files always goes through submitReportWithMedia() instead — see below — so a Firestore write can never happen before its media URLs exist. Pass `rateLimit` for genuine creations of a quota-protected collection (tips, alerts) — never for updates. */
export async function write<T extends {id:string}>(name:string,item:T,options?:{rateLimit?:RateLimitConfig}){
  assertRemoteMedia(item)
  const rateLimit = options?.rateLimit
  if(!db || !navigator.onLine){ await saveOutbox(name,item,'queued',undefined,rateLimit); return }
  try { if(rateLimit) await writeWithQuota(name,item,rateLimit); else await setDoc(doc(db,name,item.id),item) }
  catch(error){ await saveOutbox(name,item,'failed',error,rateLimit); throw error }
}
export async function create<T extends {id:string}>(name:string,item:T){ assertRemoteMedia(item); if(!db || !navigator.onLine){ await saveOutbox(name,item); return item.id } try { const result=await addDoc(collection(db,name),item); return result.id } catch(error){ await saveOutbox(name,item,'failed',error); throw error } }

export async function syncOutbox(){
  if(!db || !navigator.onLine || !localDb) return {completed:0,conflicts:0,failed:0}
  const entries=await localDb.outbox.where('status').anyOf('queued','failed').toArray(); let completed=0; let conflicts=0; let failed=0
  for(const entry of entries){
    try{
      const payload=entry.payload as {id:string;updatedAt?:number}; const remote=await getDoc(doc(db,entry.type,payload.id)); const remoteUpdated=remote.exists() ? Number((remote.data() as {updatedAt?:number}).updatedAt ?? 0) : 0
      if(remote.exists() && remoteUpdated > Number(payload.updatedAt ?? entry.createdAt)){ await localDb.outbox.update(entry.id,{status:'conflict',lastError:'Remote record changed while this item was offline.'}); conflicts++; continue }
      assertRemoteMedia(payload)
      if(entry.rateLimit) await writeWithQuota(entry.type,payload,entry.rateLimit)
      else await setDoc(doc(db,entry.type,payload.id),payload)
      await localDb.outbox.delete(entry.id); completed++
    }catch(error){ await localDb.outbox.update(entry.id,{status:'failed',attempts:(entry.attempts ?? 0)+1,lastError:error instanceof Error ? error.message : 'Synchronization failed'}); failed++ }
  }
  return {completed,conflicts,failed}
}
export async function getPendingSync(){ if(!localDb) return []; return localDb.outbox.where('status').anyOf('queued','failed','conflict').toArray() }
export async function resolveConflict(id:string,choice:'keep-local'|'discard-local'){
  if(!localDb) throw new Error('Offline storage is not available in this browser.')
  const entry=await localDb.outbox.get(id)
  if(!entry || entry.status!=='conflict') throw new Error('Synchronization conflict was not found.')
  if(choice==='discard-local'){ await localDb.outbox.delete(id); return }
  if(!db || !navigator.onLine){ await localDb.outbox.update(id,{status:'queued',lastError:undefined,attempts:0}); return }
  const payload=entry.payload as {id:string}; assertRemoteMedia(payload)
  if(entry.rateLimit) await writeWithQuota(entry.type,payload,entry.rateLimit)
  else await setDoc(doc(db,entry.type,payload.id),payload)
  await localDb.outbox.delete(id)
}

// ---------------------------------------------------------------------------
// Media validation, compression, capture (SELECT/CAPTURE -> VALIDATE)
// ---------------------------------------------------------------------------
const MAX_IMAGE_BYTES = 15 * 1024 * 1024
const MAX_MEDIA_BYTES = 50 * 1024 * 1024
// Retry limit: the `attempts` counter already existed on MediaJob/ReportDraft
// but was previously tracked without ever being enforced as a ceiling — an
// unbounded retry loop (buggy client, or a user tapping "Retry" forever)
// could otherwise hammer Storage/Firestore indefinitely for a draft that's
// never going to succeed. Once reached, the draft must be explicitly
// discarded and resubmitted (a genuinely new attempt, new id) rather than
// endlessly retried.
export const MAX_RETRY_ATTEMPTS = 5
const allowedMediaTypes = /^(image\/(jpeg|png|webp|gif)|video\/(mp4|webm|quicktime)|audio\/(mpeg|mp4|wav|webm|ogg))$/i
const sleep = (ms:number) => new Promise(resolve => setTimeout(resolve,ms))
const mediaKind = (type:string):MediaKind => type.startsWith('image/') ? 'image' : type.startsWith('video/') ? 'video' : 'audio'
const fieldKeyFor = (kind:MediaKind):MediaFieldKey => kind==='image' ? 'imageUrls' : kind==='video' ? 'videoUrls' : 'audioUrls'

/** Large-file protection + MIME allow-list. Runs BEFORE anything is queued, so an oversized/unsupported file never enters the pipeline and never produces an orphaned job. */
export function validateMediaFile(file:File):{ok:true}|{ok:false;reason:string}{
  if(!allowedMediaTypes.test(file.type)) return {ok:false,reason:`${file.name}: unsupported file type.`}
  const limit = file.type.startsWith('image/') ? MAX_IMAGE_BYTES : MAX_MEDIA_BYTES
  if(file.size > limit) return {ok:false,reason:`${file.name}: exceeds the ${file.type.startsWith('image/') ? 15 : 50} MiB limit.`}
  return {ok:true}
}

async function compressImage(file:File):Promise<Blob>{
  if(!file.type.startsWith('image/') || file.size <= 2 * 1024 * 1024 || !capabilities.createImageBitmap) return file
  try {
    const bitmap = await createImageBitmap(file)
    const scale = Math.min(1, 2048 / Math.max(bitmap.width,bitmap.height))
    const canvas = document.createElement('canvas'); canvas.width = Math.max(1,Math.round(bitmap.width * scale)); canvas.height = Math.max(1,Math.round(bitmap.height * scale))
    canvas.getContext('2d')?.drawImage(bitmap,0,0,canvas.width,canvas.height); bitmap.close()
    return await new Promise((resolve,reject)=>canvas.toBlob(blob=>blob ? resolve(blob) : reject(new Error('Image compression failed.')),'image/webp',0.82))
  } catch { return file } // compression is a best-effort optimization; never block a valid upload because of it
}

export function createMediaPreview(file:Blob){ return URL.createObjectURL(file) }
export function revokeMediaPreview(url:string){ URL.revokeObjectURL(url) }

/**
 * Opens the platform's native file/camera chooser. `capture='environment'`
 * is set for both image AND video (not just image, as before) — this is
 * the standards-based way to ask a mobile browser to launch the rear
 * camera directly instead of a generic file/gallery picker; it's simply
 * ignored (falls back to an ordinary file picker) on desktop browsers and
 * on any platform that doesn't support it, so it's always safe to set.
 * Actually wired into the UI now — see CaptureButtons.tsx.
 */
export function captureMedia(kind:MediaKind,accept:string){
  const input=document.createElement('input'); input.type='file'; input.accept=accept; input.multiple=true; if(kind==='image' || kind==='video') input.capture='environment'; input.click()
  return new Promise<File[]>((resolve,reject)=>{ input.onchange=()=>resolve(Array.from(input.files ?? [])); input.onerror=()=>reject(new Error('Media selection failed.')) })
}

export interface MediaRecording { stream:MediaStream; stop():void; result:Promise<File> }

/**
 * Starts an in-page camera/mic recording via getUserMedia + MediaRecorder,
 * with Safari-aware codec fallback (vp9/opus webm, then plain webm, then
 * mp4). Unlike a one-shot recorder, this returns a live controller so the
 * UI can show a real-time preview and a Stop button — `result` resolves
 * with the finished File whenever `stop()` is called, or automatically
 * once `maxDurationMs` elapses as a hard safety cap either way. This is
 * the in-browser recording path used by CaptureButtons.tsx for desktop
 * webcams and any device where a person prefers recording without leaving
 * the app, as distinct from `captureMedia`'s native-camera-app handoff.
 */
export function startRecording(kind:Exclude<MediaKind,'image'>, maxDurationMs=120_000):Promise<MediaRecording>{
  if(!capabilities.mediaRecorder) return Promise.reject(new Error('Recording is not supported by this browser.'))
  return navigator.mediaDevices.getUserMedia(kind==='video' ? {video:true,audio:true} : {audio:true}).then(stream=>{
    const preferred = kind==='video' ? ['video/webm;codecs=vp9,opus','video/webm','video/mp4'] : ['audio/webm;codecs=opus','audio/webm','audio/mp4']
    const mimeType=preferred.find(type=>MediaRecorder.isTypeSupported(type)) ?? ''
    const recorder=new MediaRecorder(stream,mimeType ? {mimeType} : undefined); const chunks:BlobPart[]=[]
    let settled=false
    const result=new Promise<File>((resolve,reject)=>{
      const timer=window.setTimeout(()=>{ if(recorder.state!=='inactive') recorder.stop() },maxDurationMs)
      recorder.ondataavailable=event=>{ if(event.data.size) chunks.push(event.data) }
      recorder.onerror=()=>{ if(settled) return; settled=true; window.clearTimeout(timer); stream.getTracks().forEach(track=>track.stop()); reject(new Error('Media recording failed.')) }
      recorder.onstop=()=>{ if(settled) return; settled=true; window.clearTimeout(timer); stream.getTracks().forEach(track=>track.stop()); const type=recorder.mimeType || (kind==='video' ? 'video/webm' : 'audio/webm'); resolve(new File([new Blob(chunks,{type})],`${kind}-${Date.now()}.webm`,{type})) }
      recorder.start(250)
    })
    return { stream, stop:()=>{ if(recorder.state!=='inactive') recorder.stop() }, result }
  })
}

// ---------------------------------------------------------------------------
// Stable IDs (duplicate prevention)
// ---------------------------------------------------------------------------
const draftId = (collectionName:string, reportId:string) => `${collectionName}:${reportId}`
/** Deterministic per-file id: re-queueing the SAME file (name+size+lastModified) for the SAME report/field always resolves to the same job row via Dexie's `.put()` upsert, so retries and accidental double-submits can never create duplicate media jobs or duplicate uploads. */
const mediaJobId = (owner:string, fieldKey:MediaFieldKey, file:File) => `${owner}:${fieldKey}:${file.name}:${file.size}:${file.lastModified}`
const sanitize = (name:string) => name.replace(/[^a-zA-Z0-9_.-]/g,'_')

// ---------------------------------------------------------------------------
// Upload engine (VALIDATE -> LOCAL PERSISTENCE -> UPLOAD -> STORAGE -> URL)
// ---------------------------------------------------------------------------
async function uploadJobFile(job:MediaJob):Promise<string>{
  if(!storage) throw new Error('Firebase Storage configuration is required.')
  let lastError:unknown
  for(let attempt=0; attempt<3; attempt++){
    try{
      const task=uploadBytesResumable(ref(storage,job.path), job.file, {
        contentType: job.contentType,
        cacheControl: 'private, max-age=31536000',
        customMetadata: { // requirement: correct metadata
          mediaId: job.id,
          draftId: job.draftId,
          kind: job.kind,
          fieldKey: job.fieldKey,
          originalFileName: job.fileName,
          uploadedAt: String(Date.now()),
        },
      })
      const url = await new Promise<string>((resolve,reject)=>task.on('state_changed', undefined, reject, async()=>resolve(await getDownloadURL(task.snapshot.ref))))
      if(!isFirebaseDownloadUrl(url)) throw new Error('Firebase Storage returned an invalid media URL.')
      return url
    }catch(error){ lastError=error; if(attempt<2) await sleep(500 * 2 ** attempt + Math.random() * 250) }
  }
  throw lastError instanceof Error ? lastError : new Error('Media upload failed.')
}

/** Best-effort Storage cleanup for a single job (requirement: media deletion lifecycle / no orphan media). Never throws — deleting an already-missing object, or being unable to delete due to rules/network, must not block the caller. */
async function deleteJobFromStorage(job:MediaJob){
  if(job.status!=='UPLOADED' || !storage) return
  try { await deleteObject(ref(storage,job.path)) } catch { /* best-effort: object may already be gone, or rules may deny it */ }
}

// ---------------------------------------------------------------------------
// The draft + media state machine
// ---------------------------------------------------------------------------

/**
 * Entry point used by any form that creates a report-like document with
 * attached media (incident reports, wanted-person notices). Replaces the old
 * "upload files, then write the document" flow, which lost the entire
 * submission the moment a single upload failed or the device was offline.
 *
 * Guarantees:
 *  - The draft (and every attached file) is written to IndexedDB BEFORE any
 *    network activity is attempted, online or offline alike. A refresh or
 *    crash immediately after calling this can never lose the submission.
 *  - The Firestore document is written ONLY after every attached file has a
 *    confirmed HTTPS Firebase Storage download URL — a synced document can
 *    never reference missing/local media, and a report is never created
 *    without its evidence attached.
 *  - Deterministic IDs mean calling this twice for the same files/report is
 *    always a no-op re-queue, never a duplicate.
 */
export async function submitReportWithMedia(options:{
  collection: 'reports'|'wanted_criminals'
  reportId: string
  storagePath: string // e.g. `reports/${reportId}` or `criminals/${reportId}`
  payload: Record<string, unknown> // full document fields EXCEPT imageUrls/videoUrls/audioUrls
  files: File[]
}):Promise<{ draftId:string; status:DraftStatus }> {
  if(!localDb) throw new Error('Offline storage is not available in this browser. Please use an up-to-date browser to submit a report.')
  const invalid = options.files.map(validateMediaFile).find(result => !result.ok)
  if(invalid && !invalid.ok) throw new Error(invalid.reason)

  const id = draftId(options.collection, options.reportId)
  const now = Date.now()
  const jobs:MediaJob[] = options.files.map((file, index) => {
    const kind = mediaKind(file.type)
    const fieldKey = fieldKeyFor(kind)
    return {
      id: mediaJobId(id, fieldKey, file),
      draftId: id,
      fieldKey,
      file,
      fileName: file.name,
      contentType: file.type,
      size: file.size,
      path: `${options.storagePath}/${kind}/${mediaJobId(id,fieldKey,file)}-${sanitize(file.name)}`,
      kind,
      order: index,
      status: 'LOCAL_PENDING',
      createdAt: now,
      updatedAt: now,
      attempts: 0,
    }
  })

  await localDb.transaction('rw', localDb.reportDrafts, localDb.mediaJobs, async () => {
    await localDb!.mediaJobs.bulkPut(jobs)
    await localDb!.reportDrafts.put({
      id,
      collection: options.collection,
      reportId: options.reportId,
      path: options.storagePath,
      payload: options.payload,
      mediaJobIds: jobs.map(j => j.id),
      status: 'LOCAL_PENDING',
      createdAt: now,
      updatedAt: now,
      attempts: 0,
    })
  })

  if(capabilities.backgroundSync){
    void navigator.serviceWorker.ready.then(registration => registration.sync.register('tracenet-media-sync')).catch(() => {})
  }

  const status = await processDraft(id) // awaited so an online caller gets an accurate immediate result; cheap early-return if offline (see processDraft)
  return { draftId: id, status: status ?? 'LOCAL_PENDING' }
}

/** Advances a single draft through the state machine as far as current conditions (network, Storage availability) allow. Idempotent and safe to call repeatedly — from initial submit, from the reconnect handler, from a manual "retry" tap, or from app startup. */
export async function processDraft(id:string):Promise<DraftStatus|undefined>{
  if(!localDb) return undefined
  const draft = await localDb.reportDrafts.get(id)
  if(!draft) return undefined
  if(draft.status==='SYNCED') return 'SYNCED'
  if(draft.status==='FAILED' && draft.attempts>=MAX_RETRY_ATTEMPTS) return 'FAILED' // retry limit reached — requires an explicit discardDraft(), never auto-retried further

  const jobs = await localDb.mediaJobs.where('draftId').equals(id).toArray()
  const pendingJobs = jobs.filter(j => j.status!=='UPLOADED')

  if(pendingJobs.length){
    if(!navigator.onLine || !storage){
      await localDb.reportDrafts.update(id, { status:'LOCAL_PENDING', updatedAt:Date.now() })
      return 'LOCAL_PENDING' // nothing more we can do until we're back online — draft and every file remain safely queued locally
    }
    await localDb.reportDrafts.update(id, { status:'UPLOADING', updatedAt:Date.now() })
    let anyFailed = false
    for(const job of pendingJobs){
      await localDb.mediaJobs.update(job.id, { status:'UPLOADING', updatedAt:Date.now() })
      try {
        const url = await uploadJobFile(job)
        await localDb.mediaJobs.update(job.id, { status:'UPLOADED', downloadUrl:url, updatedAt:Date.now() })
      } catch(error) {
        anyFailed = true
        await localDb.mediaJobs.update(job.id, { status:'FAILED', attempts: job.attempts+1, lastError: error instanceof Error ? error.message : 'Upload failed', updatedAt:Date.now() })
      }
    }
    if(anyFailed){
      await localDb.reportDrafts.update(id, { status:'FAILED', attempts: draft.attempts+1, lastError:'One or more attached files failed to upload.', updatedAt:Date.now() })
      return 'FAILED'
    }
  }

  // Every attached file now has a confirmed HTTPS download URL — assemble the
  // final document. This is the ONLY place a report/wanted document is ever
  // written for a draft created via submitReportWithMedia, which is what
  // guarantees a synced document can never be missing its media.
  const finalJobs = await localDb.mediaJobs.where('draftId').equals(id).toArray()
  const urlsFor = (fieldKey:MediaFieldKey) => finalJobs.filter(j => j.fieldKey===fieldKey).sort((a,b)=>a.order-b.order).map(j => j.downloadUrl!).filter(Boolean)
  const finalPayload = {
    ...draft.payload,
    id: draft.reportId,
    imageUrls: urlsFor('imageUrls'),
    videoUrls: urlsFor('videoUrls'),
    audioUrls: urlsFor('audioUrls'),
  }

  try { assertRemoteMedia(finalPayload) }
  catch(error) {
    await localDb.reportDrafts.update(id, { status:'FAILED', lastError: error instanceof Error ? error.message : 'Invalid media URL.', updatedAt:Date.now() })
    return 'FAILED'
  }

  await localDb.reportDrafts.update(id, { status:'UPLOADED', updatedAt:Date.now() })

  if(!db || !navigator.onLine){
    await localDb.reportDrafts.update(id, { status:'REPORT_PENDING', updatedAt:Date.now() })
    return 'REPORT_PENDING'
  }

  await localDb.reportDrafts.update(id, { status:'REPORT_PENDING', updatedAt:Date.now() })
  try {
    const existing = await getDoc(doc(db, draft.collection, draft.reportId))
    if(existing.exists()){
      const remote = existing.data() as Record<string, unknown>
      // Same id already present remotely. Since ids are randomly generated
      // UUIDs at draft creation, this is expected to mean "our own earlier
      // write already succeeded but this draft never got marked SYNCED
      // locally" (e.g. the tab closed between the write and the local
      // update) — an idempotent no-op. We only treat it as a genuine
      // CONFLICT if the remote document's core identity fields disagree
      // with what we're about to write, which would mean something else
      // legitimately created a different document at this id.
      const sameIdentity = draft.collection === 'reports'
        ? remote.reporterId === draft.payload.reporterId && remote.title === draft.payload.title
        : remote.name === draft.payload.name
      if(!sameIdentity){
        await localDb.reportDrafts.update(id, { status:'CONFLICT', lastError:'A different document already exists with this id.', updatedAt:Date.now() })
        return 'CONFLICT'
      }
      // Idempotent no-op: the document already exists with matching
      // identity, so it was already successfully created (and, for
      // reports, already charged against the reporter's daily quota) on a
      // prior attempt — finish here without writing or re-charging quota again.
    } else if(draft.collection === 'reports'){
      // Real, server-enforced per-user daily submission limit — see
      // ABUSE_PROTECTION_REPORT.md. Throws (without creating the report)
      // if the reporter has already reached DAILY_LIMITS.reports today;
      // the catch block below turns that into a normal FAILED draft with
      // a clear, user-visible reason, exactly like any other write failure.
      await writeWithQuota('reports', finalPayload, { uid: draft.payload.reporterId as string, kind:'reports', limit: DAILY_LIMITS.reports })
    } else {
      await setDoc(doc(db, draft.collection, draft.reportId), finalPayload)
    }
    await localDb.reportDrafts.update(id, { status:'SYNCED', updatedAt:Date.now() })
    await pruneSyncedDraft(id) // requirement: no orphan media — local blobs are no longer needed once Firestore is the source of truth
    return 'SYNCED'
  } catch(error) {
    await localDb.reportDrafts.update(id, { status:'FAILED', attempts: draft.attempts+1, lastError: error instanceof Error ? error.message : 'Unable to save the report.', updatedAt:Date.now() })
    return 'FAILED'
  }
}

/** Removes the local blob copies and draft bookkeeping for a fully-synced draft. The Storage objects and Firestore document remain — only the now-redundant local copies are cleaned up (requirement: resource cleanup). */
async function pruneSyncedDraft(id:string){
  if(!localDb) return
  const jobs = await localDb.mediaJobs.where('draftId').equals(id).toArray()
  await localDb.transaction('rw', localDb.mediaJobs, localDb.reportDrafts, async () => {
    await localDb!.mediaJobs.bulkDelete(jobs.map(j=>j.id))
    await localDb!.reportDrafts.delete(id)
  })
}

/** Every draft that isn't SYNCED, in one call — used by the reconnect handler, service-worker background-sync message, and app startup. */
export async function processReportQueue(){
  if(!localDb) return
  const drafts = await localDb.reportDrafts.where('status').anyOf('LOCAL_PENDING','UPLOADING','UPLOADED','REPORT_PENDING','FAILED').toArray()
  for(const d of drafts) await processDraft(d.id)
}

/** Drafts the UI can surface as "pending sync" (requirement: failed-upload state / persistent upload queue visibility). */
export async function getPendingReportDrafts():Promise<ReportDraft[]>{
  if(!localDb) return []
  return localDb.reportDrafts.where('status').notEqual('SYNCED').toArray()
}

/** Explicit user-triggered retry for a FAILED or CONFLICT draft. */
export async function retryDraft(id:string){
  if(!localDb) return
  const draft = await localDb.reportDrafts.get(id)
  if(!draft) return
  if(draft.attempts>=MAX_RETRY_ATTEMPTS) return // retry limit reached — the UI should offer only Discard at this point, but this is the actual enforcement, not just hidden UI
  const jobs = await localDb.mediaJobs.where('draftId').equals(id).toArray()
  await localDb.transaction('rw', localDb.mediaJobs, localDb.reportDrafts, async () => {
    for(const job of jobs) if(job.status==='FAILED') await localDb!.mediaJobs.update(job.id, { status:'LOCAL_PENDING', lastError:undefined })
    await localDb!.reportDrafts.update(id, { status:'LOCAL_PENDING', lastError:undefined })
  })
  await processDraft(id)
}

/** User-initiated cancel of a draft that never finished syncing. Cleans up any files that DID already reach Storage so nothing is left orphaned, then removes all local bookkeeping. */
export async function discardDraft(id:string){
  if(!localDb) return
  const jobs = await localDb.mediaJobs.where('draftId').equals(id).toArray()
  await Promise.all(jobs.map(deleteJobFromStorage))
  await localDb.transaction('rw', localDb.mediaJobs, localDb.reportDrafts, async () => {
    await localDb!.mediaJobs.bulkDelete(jobs.map(j=>j.id))
    await localDb!.reportDrafts.delete(id)
  })
}

// ---------------------------------------------------------------------------
// Plain (non-report) media upload — id cards, profile photos: single file,
// no associated document draft, but the same validate/retry/HTTPS-only rules.
// ---------------------------------------------------------------------------
export async function uploadSingle(file:File, path:string):Promise<string>{
  const validation = validateMediaFile(file)
  if(!validation.ok) throw new Error(validation.reason)
  const prepared = await compressImage(file)
  if(!storage || !navigator.onLine) throw new Error('An internet connection is required to upload this file.')
  const job:MediaJob = { id:crypto.randomUUID(), draftId:'__standalone__', fieldKey:'imageUrls', file:prepared, fileName:file.name, contentType:prepared.type, size:prepared.size, path, kind:mediaKind(prepared.type), order:0, status:'LOCAL_PENDING', createdAt:Date.now(), updatedAt:Date.now(), attempts:0 }
  return uploadJobFile(job)
}

// ---------------------------------------------------------------------------
// Sync orchestration entry point (called once from main.tsx)
// ---------------------------------------------------------------------------
export function initializeMediaSync(){
  const retry=()=>{ void processReportQueue(); void syncOutbox() }
  window.addEventListener('online',retry)
  navigator.serviceWorker?.addEventListener('message',event=>{ if(event.data?.type==='tracenet-media-sync') retry() })
  void retry()
  return ()=>window.removeEventListener('online',retry)
}

export const collections = {
  // Public, unfiltered for everyone (including signed-out visitors) — matches
  // the "alerts are public safety broadcasts" rule with no owner concept.
  alerts: (cb:(v:Alert[])=>void) => listen<Alert>('alerts', cb),

  // Privileged-only (LAW_ENFORCER/ADMIN): the rule's role branch doesn't
  // depend on document content, so Firestore allows an unfiltered list.
  allReports: (cb:(v:Report[])=>void) => listen<Report>('reports', cb),
  allWanted: (cb:(v:Wanted[])=>void) => listen<Wanted>('wanted_criminals', cb),
  allTips: (cb:(v:Tip[])=>void) => listen<Tip>('tips', cb),
  allUsers: (cb:(v:User[])=>void) => listen<User>('users', cb),

  // Owner-scoped (any signed-in citizen): matches the rule's isOwner() branch.
  myReports: (uid:string, cb:(v:Report[])=>void) => listenWhere<Report>('reports', 'reporterId', uid, cb),
  myTips: (uid:string, cb:(v:Tip[])=>void) => listenWhere<Tip>('tips', 'submitterId', uid, cb),

  // Public-safe subset (anyone, including signed-out visitors): matches the
  // rule's isPublicVerified() branch. Used for the Public Portal and
  // anywhere else that should show community-wide verified information
  // without requiring — or granting — access to the full collection.
  publicReports: (cb:(v:Report[])=>void) => listenWhere<Report>('reports', 'status', 'VERIFIED', cb),
  publicWanted: (cb:(v:Wanted[])=>void) => listenWhere<Wanted>('wanted_criminals', 'isVerified', true, cb),
}

// ---------------------------------------------------------------------------
// Privileged backend operations (Cloud Functions)
// ---------------------------------------------------------------------------
// Everything here is an operation an untrusted browser client must never be
// trusted to perform directly: role assignment, officer approval, admin
// account actions, wanted-notice verification, official alert publication,
// and report status transitions. Each is a real Cloud Function
// (firebase/functions/src/functions/*.ts) that re-derives the caller's role
// from their own Firestore user document server-side — never from anything
// the client sends — validates the request, writes a structured audit_logs
// entry, and is safe to retry. See FIREBASE_FUNCTIONS_ARCHITECTURE.md.
//
// Every call is automatically given a fresh, random idempotency key
// (requestId) here in one place, so every call site below gets safe-retry
// behavior for free without needing to think about it.
function callFunction<TReq extends object, TRes>(name: string) {
  return async (payload: TReq): Promise<TRes> => {
    if (!functions) throw new Error('Firebase configuration is required.')
    const callable = httpsCallable<TReq & { requestId: string }, TRes>(functions, name)
    const requestId = crypto.randomUUID()
    const result = await callable({ ...payload, requestId } as TReq & { requestId: string })
    return result.data
  }
}

export const approveOfficer = callFunction<{ uid:string }, { ok:true; alreadyApproved:boolean }>('approveOfficer')
export const setUserRole = callFunction<{ uid:string; role:Role }, { ok:true }>('setUserRole')
export const setUserStatus = callFunction<{ uid:string; status:string }, { ok:true }>('setUserStatus')
export const verifyWantedNotice = callFunction<{ id:string; verify:boolean }, { ok:true }>('verifyWantedNotice')
export const publishAlert = callFunction<{ title:string; content:string; urgency:number; locationName:string; county?:string; latitude:number; longitude:number }, { ok:true; id:string }>('publishAlert')
export const transitionReportStatus = callFunction<{ reportId:string; status:string }, { ok:true }>('transitionReportStatus')
// Not role-gated server-side (see subscribeToAlerts.ts) — any signed-in
// user may subscribe their own device's push token to the public 'alerts'
// topic; there is nothing privileged about opting a device in to receiving
// the same official broadcasts/SOS alerts every user already sees in-app.
export const subscribeToAlerts = callFunction<{ token:string }, { ok:true }>('subscribeToAlerts')
