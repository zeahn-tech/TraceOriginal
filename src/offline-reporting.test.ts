import { beforeEach, describe, expect, it, vi } from 'vitest'

// Same mocking approach as services.test.ts, kept in its own file so this
// suite reads as a standalone, literal walkthrough of the 15-step workflow
// rather than being buried among the unit-level state-machine tests.
vi.mock('firebase/app', () => ({ initializeApp: () => ({}), getApps: () => [] }))
vi.mock('firebase/functions', () => ({ getFunctions: () => ({}), httpsCallable: () => async () => ({ data: undefined }) }))
vi.mock('firebase/auth', () => ({
  getAuth: () => ({}), GoogleAuthProvider: class {}, EmailAuthProvider: { credential: () => ({}) },
  onAuthStateChanged: () => () => {},
  signInWithEmailAndPassword: vi.fn(), createUserWithEmailAndPassword: vi.fn(), signInWithPopup: vi.fn(),
  sendPasswordResetEmail: vi.fn(), signOut: vi.fn(), updatePassword: vi.fn(), reauthenticateWithCredential: vi.fn(),
}))

type FirestoreDoc = Record<string, unknown>
const firestoreState = { docs: new Map<string, FirestoreDoc>(), setDocCalls: [] as { path:string; data:FirestoreDoc }[] }
vi.mock('firebase/firestore', () => ({
  initializeFirestore: () => ({}), persistentLocalCache: () => ({}), persistentMultipleTabManager: () => ({}),
  collection: (_db:unknown, name:string) => ({ name }),
  doc: (_db:unknown, name:string, id:string) => ({ path: `${name}/${id}` }),
  getDoc: async (ref:{ path:string }) => { const data = firestoreState.docs.get(ref.path); return { exists: () => data !== undefined, data: () => data } },
  setDoc: async (ref:{ path:string }, data:FirestoreDoc) => { firestoreState.setDocCalls.push({ path: ref.path, data }); firestoreState.docs.set(ref.path, data) },
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

const storageState = { uploadCount: 0 }
vi.mock('firebase/storage', () => ({
  getStorage: () => ({}),
  ref: (_storage:unknown, path:string) => ({ path }),
  uploadBytesResumable: (target:{ path:string }) => {
    storageState.uploadCount++
    return { snapshot: { ref: target }, on: (_e:string, _n:unknown, _err:unknown, onComplete:()=>void) => queueMicrotask(onComplete) }
  },
  getDownloadURL: async (target:{ path:string }) => `https://firebasestorage.googleapis.com/v0/b/test-project.appspot.com/o/${encodeURIComponent(target.path)}?alt=media`,
  deleteObject: async () => {},
}))

const file = (name:string, type:string) => new File([new Uint8Array(2048)], name, { type })
const setOnline = (value:boolean) => Object.defineProperty(navigator, 'onLine', { value, configurable: true })

describe('true offline-first reporting: full workflow demonstration', () => {
  beforeEach(() => {
    vi.resetModules()
    firestoreState.docs.clear()
    firestoreState.setDocCalls.length = 0
    storageState.uploadCount = 0
    setOnline(true)
  })

  it('demonstrates the complete offline -> online workflow, steps 1-15', async () => {
    // ---- Step 1: Open TraceNet without internet -------------------------
    // The PWA shell (index.html + JS/CSS bundles) is precached by the
    // service worker (see src/sw.ts: NavigationRoute + setCatchHandler
    // falling back to the precached app shell) and Firestore's
    // persistentLocalCache serves any previously-synced reports from
    // IndexedDB. Neither depends on services.ts, so this step is verified
    // by inspection of vite.config.ts/sw.ts rather than a unit test here —
    // documented in OFFLINE_REPORTING_PRODUCTION.md. From this point on we
    // simulate the device being offline for the reporting flow itself.
    setOnline(false)
    const services = await import('./services')

    // ---- Steps 2-6: Create an incident report; enter all information;
    // capture/select images, video, and audio ----------------------------
    // (In the real UI these are ReportForm's text fields, Select dropdowns,
    // and FileInput's native camera/file capture — all of which work with
    // zero network access, since they only touch the DOM/File APIs.)
    const reportId = 'e2e-report-1' // stands in for the uuid() generated once at ReportForm mount
    const payload = {
      title: 'Break-in on Randall Street', description: 'Two suspects, one armed', type: 'BURGLARY', county: 'Montserrado',
      latitude: 6.3, longitude: -10.8, timestamp: Date.now(), updatedAt: Date.now(), reporterId: 'citizen-1',
      isAnonymous: false, status: 'PENDING', incidentTimestamp: Date.now(), aiScreeningStatus: 'CLEAR', aiFlaggedReasons: [],
    }
    const files = [file('scene.jpg', 'image/jpeg'), file('clip.mp4', 'video/mp4'), file('audio-note.mp3', 'audio/mpeg')]

    // ---- Step 7: Submit the report ---------------------------------------
    // This is the durable local transaction: submitReportWithMedia persists
    // the draft AND every file to IndexedDB before touching the network at
    // all — "do not upload media first and then create the report" is
    // satisfied by construction, not convention (see services.ts,
    // submitReportWithMedia + processDraft).
    const { draftId, status: submitStatus } = await services.submitReportWithMedia({
      collection: 'reports', reportId, storagePath: `reports/${reportId}`, payload, files,
    })
    expect(submitStatus).toBe('LOCAL_PENDING') // PENDING state — offline, nothing uploaded yet
    expect(firestoreState.setDocCalls).toHaveLength(0) // definitely no report created yet
    expect(storageState.uploadCount).toBe(0) // definitely no media uploaded yet

    const draftAfterSubmit = await services.localDb!.reportDrafts.get(draftId)
    expect(draftAfterSubmit?.status).toBe('LOCAL_PENDING')
    const jobsAfterSubmit = await services.localDb!.mediaJobs.where('draftId').equals(draftId).toArray()
    expect(jobsAfterSubmit).toHaveLength(3) // image + video + audio, all persisted locally
    expect(jobsAfterSubmit.every(j => j.status === 'LOCAL_PENDING')).toBe(true)

    // ---- Step 8: Close/reopen the browser ---------------------------------
    // Simulated the only way that's meaningful for a unit test: tear down
    // the JS module graph entirely (vi.resetModules) so NO in-memory state
    // survives, then re-import services.ts as a fresh module — exactly what
    // happens on an actual browser restart. The underlying IndexedDB
    // (fake-indexeddb here, real IndexedDB in a browser) is untouched by
    // this, because it's browser/global state, not JS module state.
    vi.resetModules()
    const servicesAfterRestart = await import('./services')

    // ---- Step 9: Remain able to recover the pending report -----------------
    const recovered = await servicesAfterRestart.localDb!.reportDrafts.get(draftId)
    expect(recovered).toBeTruthy()
    expect(recovered!.status).toBe('LOCAL_PENDING')
    expect(recovered!.payload.title).toBe('Break-in on Randall Street')
    const recoveredJobs = await servicesAfterRestart.localDb!.mediaJobs.where('draftId').equals(draftId).toArray()
    expect(recoveredJobs).toHaveLength(3) // image, video, and audio files all survived the "restart"
    const pendingList = await servicesAfterRestart.getPendingReportDrafts()
    expect(pendingList.some(d => d.id === draftId)).toBe(true) // visible in the UI's pending list

    // ---- Step 10: Reconnect to the internet ---------------------------------
    setOnline(true)

    // ---- Steps 11-13: Automatically synchronize; upload media; associate
    // media URLs with the report --------------------------------------------
    // This is exactly what the 'online' window event handler inside
    // initializeMediaSync() triggers in the real app (processReportQueue()).
    await servicesAfterRestart.processReportQueue()
    expect(storageState.uploadCount).toBe(3) // all three files actually uploaded, for real (mocked network)

    // ---- Step 14: Create/update the Firestore report ------------------------
    const written = firestoreState.setDocCalls.find(c => c.path === `reports/${reportId}`)
    expect(written).toBeTruthy()
    expect((written!.data.imageUrls as string[])[0]).toMatch(/^https:\/\/firebasestorage\.googleapis\.com\//)
    expect((written!.data.videoUrls as string[])[0]).toMatch(/^https:\/\/firebasestorage\.googleapis\.com\//)
    expect((written!.data.audioUrls as string[])[0]).toMatch(/^https:\/\/firebasestorage\.googleapis\.com\//)
    // NON-NEGOTIABLE rule, re-verified at the end of the full journey:
    for(const list of [written!.data.imageUrls, written!.data.videoUrls, written!.data.audioUrls] as string[][]){
      for(const url of list) expect(url).not.toMatch(/^(file|content|blob):/)
    }

    // ---- Step 15: Mark synchronization complete ------------------------------
    const finalDraft = await servicesAfterRestart.localDb!.reportDrafts.get(draftId)
    expect(finalDraft).toBeUndefined() // SYNCED is terminal — pruned locally, no longer "pending" anything
    const finalJobs = await servicesAfterRestart.localDb!.mediaJobs.where('draftId').equals(draftId).toArray()
    expect(finalJobs).toHaveLength(0) // no orphaned local blobs left behind either
    const finalPendingList = await servicesAfterRestart.getPendingReportDrafts()
    expect(finalPendingList.some(d => d.id === draftId)).toBe(false) // gone from the "pending" UI — it's just a normal synced report now
  })

  it('prevents duplicate submissions: rapid double-submit (e.g. a double-tap) while offline produces exactly one draft, one set of jobs, and — once synced — exactly one Firestore document', async () => {
    setOnline(false)
    const services = await import('./services')
    const reportId = 'e2e-double-submit'
    const payload = { title: 'Duplicate test', description: 'x', type: 'OTHER', timestamp: 1, updatedAt: 1, reporterId: 'citizen-2', isAnonymous: false, status: 'PENDING', incidentTimestamp: 1, aiScreeningStatus: 'CLEAR', aiFlaggedReasons: [] }
    const files = [file('a.jpg', 'image/jpeg')]

    // Two near-simultaneous calls with the SAME stable id (as would happen
    // if a slow tap handler fired twice, or the user double-tapped submit).
    const [first, second] = await Promise.all([
      services.submitReportWithMedia({ collection: 'reports', reportId, storagePath: `reports/${reportId}`, payload, files }),
      services.submitReportWithMedia({ collection: 'reports', reportId, storagePath: `reports/${reportId}`, payload, files }),
    ])
    expect(first.draftId).toBe(second.draftId)

    const drafts = await services.localDb!.reportDrafts.toArray()
    expect(drafts.filter(d => d.reportId === reportId)).toHaveLength(1) // one draft, not two
    const jobs = await services.localDb!.mediaJobs.where('draftId').equals(first.draftId).toArray()
    expect(jobs).toHaveLength(1) // one media job, not two

    setOnline(true)
    await services.processReportQueue()
    const writes = firestoreState.setDocCalls.filter(c => c.path === `reports/${reportId}`)
    expect(writes).toHaveLength(1) // exactly one Firestore document created, never two
  })
})
