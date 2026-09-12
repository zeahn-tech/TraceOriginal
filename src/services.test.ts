import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// ---------------------------------------------------------------------------
// Mocks: firebase/app, firebase/auth are barely touched by this suite but
// must not throw on import. firebase/firestore and firebase/storage are
// mocked with controllable, stateful behavior so we can exercise real
// success / failure / retry / conflict paths without any network access.
// ---------------------------------------------------------------------------
vi.mock('firebase/app', () => ({ initializeApp: () => ({}), getApps: () => [] }))
vi.mock('firebase/functions', () => ({ getFunctions: () => ({}), httpsCallable: () => async () => ({ data: undefined }) }))
vi.mock('firebase/auth', () => ({
  getAuth: () => ({}),
  GoogleAuthProvider: class {},
  EmailAuthProvider: { credential: () => ({}) },
  onAuthStateChanged: () => () => {},
  signInWithEmailAndPassword: vi.fn(), createUserWithEmailAndPassword: vi.fn(), signInWithPopup: vi.fn(),
  sendPasswordResetEmail: vi.fn(), signOut: vi.fn(), updatePassword: vi.fn(), reauthenticateWithCredential: vi.fn(),
}))

type FirestoreDoc = Record<string, unknown>
const firestoreState = { docs: new Map<string, FirestoreDoc>(), setDocCalls: [] as { path:string; data:FirestoreDoc }[] }

vi.mock('firebase/firestore', () => ({
  initializeFirestore: () => ({}),
  persistentLocalCache: () => ({}),
  persistentMultipleTabManager: () => ({}),
  collection: (_db:unknown, name:string) => ({ name }),
  doc: (_db:unknown, name:string, id:string) => ({ path: `${name}/${id}` }),
  getDoc: async (ref:{ path:string }) => {
    const data = firestoreState.docs.get(ref.path)
    return { exists: () => data !== undefined, data: () => data }
  },
  setDoc: async (ref:{ path:string }, data:FirestoreDoc) => {
    firestoreState.setDocCalls.push({ path: ref.path, data })
    firestoreState.docs.set(ref.path, data)
  },
  addDoc: vi.fn(), onSnapshot: vi.fn(() => () => {}), query: vi.fn(), orderBy: vi.fn(), where: vi.fn(),
  serverTimestamp: () => ({ toMillis: () => Date.now() }),
  runTransaction: async (_db:unknown, updateFn:(tx:{ get:(ref:{path:string})=>Promise<{exists:()=>boolean;data:()=>FirestoreDoc|undefined}>; set:(ref:{path:string},data:FirestoreDoc)=>void }) => Promise<unknown>) => {
    const tx = {
      get: async (ref:{ path:string }) => { const data = firestoreState.docs.get(ref.path); return { exists: () => data !== undefined, data: () => data } },
      set: (ref:{ path:string }, data:FirestoreDoc) => {
        if(ref.path.startsWith('reports/')) firestoreState.setDocCalls.push({ path: ref.path, data })
        firestoreState.docs.set(ref.path, data)
      },
    }
    return updateFn(tx)
  },
}))

type UploadBehavior = 'succeed' | 'fail'
const storageState = { behavior: 'succeed' as UploadBehavior, deleteObjectCalls: [] as string[], uploadCalls: [] as { path:string; contentType?:string; customMetadata?:Record<string,string> }[] }

vi.mock('firebase/storage', () => ({
  getStorage: () => ({}),
  ref: (_storage:unknown, path:string) => ({ path }),
  uploadBytesResumable: (target:{ path:string }, _file:Blob, metadata?:{ contentType?:string; customMetadata?:Record<string,string> }) => {
    storageState.uploadCalls.push({ path: target.path, contentType: metadata?.contentType, customMetadata: metadata?.customMetadata })
    return {
      snapshot: { ref: target },
      on: (_event:string, _next:unknown, onError:(e:Error)=>void, onComplete:()=>void) => {
        queueMicrotask(() => storageState.behavior==='fail' ? onError(new Error('simulated upload failure')) : onComplete())
      },
    }
  },
  getDownloadURL: async (target:{ path:string }) => `https://firebasestorage.googleapis.com/v0/b/test-project.appspot.com/o/${encodeURIComponent(target.path)}?alt=media`,
  deleteObject: async (target:{ path:string }) => { storageState.deleteObjectCalls.push(target.path) },
}))

// ---------------------------------------------------------------------------

const file = (name:string, type:string, sizeBytes = 1024) => new File([new Uint8Array(sizeBytes)], name, { type })
const setOnline = (value:boolean) => Object.defineProperty(navigator, 'onLine', { value, configurable: true })

describe('media production pipeline', () => {
  let services: typeof import('./services')

  beforeEach(async () => {
    vi.resetModules()
    firestoreState.docs.clear()
    firestoreState.setDocCalls.length = 0
    storageState.behavior = 'succeed'
    storageState.deleteObjectCalls.length = 0
    storageState.uploadCalls.length = 0
    setOnline(true)
    services = await import('./services')
  })

  afterEach(() => vi.restoreAllMocks())

  // -- VALIDATE ---------------------------------------------------------
  it('rejects unsupported file types before anything is queued', () => {
    const result = services.validateMediaFile(file('malware.exe', 'application/x-msdownload'))
    expect(result.ok).toBe(false)
  })

  it('rejects images over the 15 MiB limit and other media over the 50 MiB limit', () => {
    const bigImage = services.validateMediaFile(file('huge.jpg', 'image/jpeg', 16 * 1024 * 1024))
    const bigVideo = services.validateMediaFile(file('huge.mp4', 'video/mp4', 51 * 1024 * 1024))
    expect(bigImage.ok).toBe(false)
    expect(bigVideo.ok).toBe(false)
  })

  it('accepts a valid image/video/audio file under the size limit', () => {
    expect(services.validateMediaFile(file('a.jpg', 'image/jpeg')).ok).toBe(true)
    expect(services.validateMediaFile(file('a.mp4', 'video/mp4')).ok).toBe(true)
    expect(services.validateMediaFile(file('a.mp3', 'audio/mpeg')).ok).toBe(true)
  })

  it('sends the correct MIME type and Storage metadata for each upload', async () => {
    await services.submitReportWithMedia({
      collection: 'reports', reportId: 'r-metadata', storagePath: 'reports/r-metadata',
      payload: { title: 'Metadata check', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      files: [file('clip.mp4', 'video/mp4')],
    })
    const call = storageState.uploadCalls.find(c => c.path.includes('reports/r-metadata'))!
    expect(call.contentType).toBe('video/mp4')
    expect(call.customMetadata).toMatchObject({ draftId: 'reports:r-metadata', kind: 'video', fieldKey: 'videoUrls', originalFileName: 'clip.mp4' })
    expect(call.customMetadata?.mediaId).toBeTruthy()
    expect(call.customMetadata?.uploadedAt).toBeTruthy()
  })

  // -- ONLINE happy path: image, video, audio ---------------------------
  it('online image: uploads, writes an HTTPS-only Firestore doc, and prunes local copies (no orphan)', async () => {
    setOnline(true)
    const { draftId, status } = await services.submitReportWithMedia({
      collection: 'reports', reportId: 'r-image', storagePath: 'reports/r-image',
      payload: { title: 'Break-in', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      files: [file('photo.jpg', 'image/jpeg')],
    })
    expect(status).toBe('SYNCED')
    const written = firestoreState.setDocCalls.find(c => c.path === 'reports/r-image')
    expect(written).toBeTruthy()
    expect((written!.data.imageUrls as string[])[0]).toMatch(/^https:\/\/firebasestorage\.googleapis\.com\//)
    expect(written!.data.videoUrls).toEqual([])
    expect(written!.data.audioUrls).toEqual([])
    // no orphan media: local job/draft rows are pruned once Firestore is the source of truth
    expect(await services.localDb!.mediaJobs.where('draftId').equals(draftId).count()).toBe(0)
    expect(await services.localDb!.reportDrafts.get(draftId)).toBeUndefined()
  })

  it('online video: correct kind/field and HTTPS URL', async () => {
    const { status } = await services.submitReportWithMedia({
      collection: 'reports', reportId: 'r-video', storagePath: 'reports/r-video',
      payload: { title: 'Assault', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      files: [file('clip.mp4', 'video/mp4')],
    })
    expect(status).toBe('SYNCED')
    const written = firestoreState.setDocCalls.find(c => c.path === 'reports/r-video')!
    expect((written.data.videoUrls as string[])[0]).toMatch(/^https:\/\/firebasestorage\.googleapis\.com\//)
    expect(written.data.imageUrls).toEqual([])
  })

  it('online audio: correct kind/field and HTTPS URL', async () => {
    const { status } = await services.submitReportWithMedia({
      collection: 'reports', reportId: 'r-audio', storagePath: 'reports/r-audio',
      payload: { title: 'Noise complaint', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      files: [file('clip.mp3', 'audio/mpeg')],
    })
    expect(status).toBe('SYNCED')
    const written = firestoreState.setDocCalls.find(c => c.path === 'reports/r-audio')!
    expect((written.data.audioUrls as string[])[0]).toMatch(/^https:\/\/firebasestorage\.googleapis\.com\//)
  })

  // -- OFFLINE: image, video, audio — the exact scenario that was
  // completely broken before this pass (Critical finding 1.4 in the audit)
  it('offline image/video/audio: draft and every file persist locally as LOCAL_PENDING; nothing is written to Firestore; nothing is lost', async () => {
    setOnline(false)
    const { draftId, status } = await services.submitReportWithMedia({
      collection: 'reports', reportId: 'r-offline', storagePath: 'reports/r-offline',
      payload: { title: 'Offline incident', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      files: [file('a.jpg', 'image/jpeg'), file('b.mp4', 'video/mp4'), file('c.mp3', 'audio/mpeg')],
    })
    expect(status).toBe('LOCAL_PENDING')
    expect(firestoreState.setDocCalls.length).toBe(0) // no write ever attempted while offline
    const jobs = await services.localDb!.mediaJobs.where('draftId').equals(draftId).toArray()
    expect(jobs).toHaveLength(3)
    expect(jobs.every(j => j.status === 'LOCAL_PENDING')).toBe(true)
    const draft = await services.localDb!.reportDrafts.get(draftId)
    expect(draft?.mediaJobIds).toHaveLength(3) // report <-> media association preserved locally
  })

  // -- RECONNECTION ------------------------------------------------------
  it('reconnection: a draft saved while offline completes automatically once back online', async () => {
    setOnline(false)
    const { draftId } = await services.submitReportWithMedia({
      collection: 'reports', reportId: 'r-reconnect', storagePath: 'reports/r-reconnect',
      payload: { title: 'Will sync later', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      files: [file('a.jpg', 'image/jpeg')],
    })
    expect((await services.localDb!.reportDrafts.get(draftId))?.status).toBe('LOCAL_PENDING')

    setOnline(true)
    const status = await services.processDraft(draftId) // what the 'online' event handler triggers via processReportQueue()
    expect(status).toBe('SYNCED')
    expect(firestoreState.setDocCalls.some(c => c.path === 'reports/r-reconnect')).toBe(true)
  })

  // -- RETRY after a transient failure ------------------------------------
  it('retry: a failed upload can be retried and completes once the transient error clears', async () => {
    storageState.behavior = 'fail'
    const { draftId, status } = await services.submitReportWithMedia({
      collection: 'reports', reportId: 'r-retry', storagePath: 'reports/r-retry',
      payload: { title: 'Flaky network', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      files: [file('a.jpg', 'image/jpeg')],
    }, )
    expect(status).toBe('FAILED')
    const failedJob = (await services.localDb!.mediaJobs.where('draftId').equals(draftId).toArray())[0]
    expect(failedJob.status).toBe('FAILED')
    expect(failedJob.lastError).toBeTruthy()

    storageState.behavior = 'succeed'
    await services.retryDraft(draftId)
    expect((await services.localDb!.reportDrafts.get(draftId))?.status).toBeUndefined() // pruned: reached SYNCED
    expect(firestoreState.setDocCalls.some(c => c.path === 'reports/r-retry')).toBe(true)
  })

  // -- FAILED upload state (permanent) -------------------------------------
  it('failed upload: stays visible via getPendingReportDrafts rather than disappearing silently', async () => {
    storageState.behavior = 'fail'
    const { draftId } = await services.submitReportWithMedia({
      collection: 'reports', reportId: 'r-permfail', storagePath: 'reports/r-permfail',
      payload: { title: 'Always fails', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      files: [file('a.jpg', 'image/jpeg')],
    })
    const pending = await services.getPendingReportDrafts()
    expect(pending.some(d => d.id === draftId && d.status === 'FAILED')).toBe(true)
  })

  // -- Browser refresh mid-upload -------------------------------------------
  it('browser refresh during upload: a draft stuck at UPLOADING (from a torn-down session) resumes correctly on next processDraft call, using the same persisted blob', async () => {
    // Simulate exactly what a refresh leaves behind: durable rows already
    // written (as submitReportWithMedia always does BEFORE any network
    // activity), with a job stuck mid-flight because the tab was torn down.
    const draftKey = 'reports:r-refresh'
    const jobKey = `${draftKey}:imageUrls:a.jpg:1024:0`
    await services.localDb!.mediaJobs.put({
      id: jobKey, draftId: draftKey, fieldKey: 'imageUrls', file: file('a.jpg', 'image/jpeg'),
      fileName: 'a.jpg', contentType: 'image/jpeg', size: 1024, path: 'reports/r-refresh/image/a.jpg',
      kind: 'image', order: 0, status: 'UPLOADING', createdAt: 1, updatedAt: 1, attempts: 0,
    })
    await services.localDb!.reportDrafts.put({
      id: draftKey, collection: 'reports', reportId: 'r-refresh', path: 'reports/r-refresh',
      payload: { title: 'Interrupted', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      mediaJobIds: [jobKey], status: 'UPLOADING', createdAt: 1, updatedAt: 1, attempts: 0,
    })
    const status = await services.processDraft(draftKey) // what app start-up (processReportQueue) does
    expect(status).toBe('SYNCED')
  })

  // -- Browser closed / reopened --------------------------------------------
  it('browser closed and reopened: a draft that was never processed before close is picked up by processReportQueue on next launch', async () => {
    setOnline(false)
    const { draftId } = await services.submitReportWithMedia({
      collection: 'reports', reportId: 'r-reopen', storagePath: 'reports/r-reopen',
      payload: { title: 'Closed before sync', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      files: [file('a.jpg', 'image/jpeg')],
    })
    // "close and reopen": no in-memory state carries over — only what processReportQueue can discover from Dexie
    setOnline(true)
    await services.processReportQueue()
    expect((await services.localDb!.reportDrafts.get(draftId))).toBeUndefined() // pruned: reached SYNCED
    expect(firestoreState.setDocCalls.some(c => c.path === 'reports/r-reopen')).toBe(true)
  })

  // -- Second device viewing media ------------------------------------------
  it('second device viewing media: the synced document contains only plain HTTPS URLs a second client can read and render directly', async () => {
    await services.submitReportWithMedia({
      collection: 'reports', reportId: 'r-shared', storagePath: 'reports/r-shared',
      payload: { title: 'Shared evidence', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      files: [file('a.jpg', 'image/jpeg')],
    })
    // A second device only ever sees Firestore, never the first device's IndexedDB.
    const remoteDoc = firestoreState.docs.get('reports/r-shared')!
    for(const url of remoteDoc.imageUrls as string[]){
      expect(url.startsWith('https://firebasestorage.googleapis.com/')).toBe(true)
      expect(url).not.toMatch(/^(file|content|blob):/)
    }
  })

  // -- Duplicate prevention ---------------------------------------------------
  it('duplicate prevention: submitting the same file twice for the same report never creates a second job or a second Firestore write', async () => {
    setOnline(false)
    const files = [file('dup.jpg', 'image/jpeg', 2048)]
    const first = await services.submitReportWithMedia({ collection:'reports', reportId:'r-dup', storagePath:'reports/r-dup', payload:{ title:'Dup', description:'x', reporterId:'u1', timestamp:1, updatedAt:1 }, files })
    const second = await services.submitReportWithMedia({ collection:'reports', reportId:'r-dup', storagePath:'reports/r-dup', payload:{ title:'Dup', description:'x', reporterId:'u1', timestamp:1, updatedAt:1 }, files })
    expect(first.draftId).toBe(second.draftId)
    const jobs = await services.localDb!.mediaJobs.where('draftId').equals(first.draftId).toArray()
    expect(jobs).toHaveLength(1) // not 2 — deterministic id upserts instead of duplicating

    setOnline(true)
    await services.processDraft(first.draftId)
    const writesForThisReport = firestoreState.setDocCalls.filter(c => c.path === 'reports/r-dup')
    expect(writesForThisReport).toHaveLength(1)
  })

  it('idempotent re-sync: calling processDraft again after a draft already synced is a safe no-op, not a duplicate write', async () => {
    const { draftId } = await services.submitReportWithMedia({
      collection: 'reports', reportId: 'r-idempotent', storagePath: 'reports/r-idempotent',
      payload: { title: 'Once', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      files: [file('a.jpg', 'image/jpeg')],
    })
    expect(firestoreState.setDocCalls.filter(c => c.path === 'reports/r-idempotent')).toHaveLength(1)
    const secondAttempt = await services.processDraft(draftId) // draft row no longer exists — already pruned
    expect(secondAttempt).toBeUndefined()
    expect(firestoreState.setDocCalls.filter(c => c.path === 'reports/r-idempotent')).toHaveLength(1) // still exactly one write
  })

  // -- Conflict ---------------------------------------------------------------
  it('conflict: a different document already occupying the same id is flagged CONFLICT instead of being silently overwritten', async () => {
    firestoreState.docs.set('reports/r-conflict', { reporterId: 'someone-else', title: 'Not mine' })
    const { status } = await services.submitReportWithMedia({
      collection: 'reports', reportId: 'r-conflict', storagePath: 'reports/r-conflict',
      payload: { title: 'Mine', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      files: [],
    })
    expect(status).toBe('CONFLICT')
    expect(firestoreState.docs.get('reports/r-conflict')).toEqual({ reporterId: 'someone-else', title: 'Not mine' }) // not overwritten
  })

  // -- Media deletion lifecycle / discard --------------------------------------
  it('discard: removes an already-uploaded (but not yet synced) file from Storage and clears local bookkeeping — no orphan left behind', async () => {
    storageState.behavior = 'fail' // force the draft to stop at FAILED with one job already UPLOADED conceptually — simulate directly
    const draftKey = 'reports:r-discard'
    await services.localDb!.mediaJobs.put({
      id: `${draftKey}:imageUrls:a.jpg:1:1`, draftId: draftKey, fieldKey: 'imageUrls', file: file('a.jpg', 'image/jpeg'),
      fileName: 'a.jpg', contentType: 'image/jpeg', size: 1, path: 'reports/r-discard/image/a.jpg',
      kind: 'image', order: 0, status: 'UPLOADED', downloadUrl: 'https://firebasestorage.googleapis.com/v0/b/x/o/y', createdAt: 1, updatedAt: 1, attempts: 0,
    })
    await services.localDb!.reportDrafts.put({
      id: draftKey, collection: 'reports', reportId: 'r-discard', path: 'reports/r-discard',
      payload: { title: 'Abandoned', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      mediaJobIds: [`${draftKey}:imageUrls:a.jpg:1:1`], status: 'FAILED', createdAt: 1, updatedAt: 1, attempts: 1,
    })
    await services.discardDraft(draftKey)
    expect(storageState.deleteObjectCalls).toContain('reports/r-discard/image/a.jpg')
    expect(await services.localDb!.mediaJobs.where('draftId').equals(draftKey).count()).toBe(0)
    expect(await services.localDb!.reportDrafts.get(draftKey)).toBeUndefined()
  })

  // -- Abuse protection: per-user daily report quota --------------------------
  it('abuse protection: a report submission is refused once the reporter has hit their daily quota, and nothing orphaned is left behind', async () => {
    firestoreState.docs.set('rate_limits/u-quota_reports', { count: 10, windowStart: { toMillis: () => Date.now() } })
    const { draftId, status } = await services.submitReportWithMedia({
      collection: 'reports', reportId: 'r-quota', storagePath: 'reports/r-quota',
      payload: { title: 'Over quota', description: 'x', reporterId: 'u-quota', timestamp: 1, updatedAt: 1 },
      files: [],
    })
    expect(status).toBe('FAILED')
    const draft = await services.localDb!.reportDrafts.get(draftId)
    expect(draft?.lastError).toMatch(/daily limit/i)
    expect(firestoreState.docs.has('reports/r-quota')).toBe(false) // definitely never created
  })

  // -- Abuse protection: retry attempt ceiling ---------------------------------
  it('abuse protection: a draft stops being auto-retried once it hits the retry ceiling, and retryDraft() refuses to reset it further', async () => {
    const draftKey = 'reports:r-maxretries'
    await services.localDb!.reportDrafts.put({
      id: draftKey, collection: 'reports', reportId: 'r-maxretries', path: 'reports/r-maxretries',
      payload: { title: 'Exhausted', description: 'x', reporterId: 'u1', timestamp: 1, updatedAt: 1 },
      mediaJobIds: [], status: 'FAILED', createdAt: 1, updatedAt: 1, attempts: services.MAX_RETRY_ATTEMPTS,
    })
    // processDraft must not attempt anything further — no network activity, status stays FAILED as-is
    const status = await services.processDraft(draftKey)
    expect(status).toBe('FAILED')
    // retryDraft() must refuse to reset it back to LOCAL_PENDING
    await services.retryDraft(draftKey)
    const draft = await services.localDb!.reportDrafts.get(draftKey)
    expect(draft?.status).toBe('FAILED')
    expect(draft?.attempts).toBe(services.MAX_RETRY_ATTEMPTS) // unchanged — never quietly reset
  })

  // -- NON-NEGOTIABLE rule: never a local path in Firestore --------------------
  it('non-negotiable rule: assertRemoteMedia rejects blob:/file:/content: URLs even if something tried to sneak one into a write', async () => {
    await expect(services.write('reports', {
      id: 'r-bad-url', title: 'x', description: 'x', type: 'OTHER', latitude: 0, longitude: 0,
      timestamp: 1, updatedAt: 1, reporterId: 'u1', isAnonymous: false, status: 'PENDING',
      imageUrls: ['blob:http://localhost/abc-123'], videoUrls: [], audioUrls: [],
      incidentTimestamp: 1, aiScreeningStatus: 'CLEAR', aiFlaggedReasons: [],
    } as never)).rejects.toThrow(/HTTPS Firebase Storage/)
  })
})
