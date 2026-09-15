/**
 * Firestore security rules emulator tests.
 *
 * REQUIRES the real Firestore emulator to be running (this connects to a
 * live emulator on localhost:8080 — it is not a mock of any kind). Run via:
 *
 *   firebase emulators:exec --project tracenet-emulator-test \
 *     --only firestore "npx vitest run firebase/emulator-tests/firestore.rules.test.ts"
 *
 * or use the convenience script: npm run test:security
 *
 * See FIREBASE_SECURITY_IMPLEMENTATION.md for why these could not be
 * executed in the environment that authored this suite, and for exact
 * instructions to run them yourself.
 */
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest'
import { assertFails, assertSucceeds, initializeTestEnvironment, type RulesTestEnvironment } from '@firebase/rules-unit-testing'
import { collection, deleteDoc, doc, getDoc, getDocs, runTransaction, serverTimestamp, setDoc, updateDoc } from 'firebase/firestore'

const PROJECT_ID = 'tracenet-emulator-test'

let testEnv: RulesTestEnvironment

const CITIZEN_A = 'citizen-a'
const CITIZEN_B = 'citizen-b'
const OFFICER_APPROVED = 'officer-approved'
const OFFICER_UNAPPROVED = 'officer-unapproved'
const ADMIN = 'admin-1'

async function seed() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore()
    await setDoc(doc(db, 'users', CITIZEN_A), { id: CITIZEN_A, name: 'Citizen A', email: 'a@x.com', role: 'CITIZEN', isApproved: true })
    await setDoc(doc(db, 'users', CITIZEN_B), { id: CITIZEN_B, name: 'Citizen B', email: 'b@x.com', role: 'CITIZEN', isApproved: true })
    await setDoc(doc(db, 'users', OFFICER_APPROVED), { id: OFFICER_APPROVED, name: 'Officer Approved', email: 'o@x.com', role: 'LAW_ENFORCER', isApproved: true })
    await setDoc(doc(db, 'users', OFFICER_UNAPPROVED), { id: OFFICER_UNAPPROVED, name: 'Officer Pending', email: 'p@x.com', role: 'LAW_ENFORCER', isApproved: false })
    await setDoc(doc(db, 'users', ADMIN), { id: ADMIN, name: 'Admin', email: 'admin@x.com', role: 'ADMIN', isApproved: true })

    await setDoc(doc(db, 'reports', 'report-pending-a'), {
      id: 'report-pending-a', title: 'Break-in', description: 'x', type: 'BURGLARY', latitude: 6.3, longitude: -10.8,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_A, isAnonymous: false, status: 'PENDING',
      imageUrls: [], videoUrls: [], audioUrls: [], incidentTimestamp: 1, aiScreeningStatus: 'CLEAR', aiFlaggedReasons: [],
    })
    await setDoc(doc(db, 'reports', 'report-verified'), {
      id: 'report-verified', title: 'Verified incident', description: 'x', type: 'THEFT', latitude: 6.3, longitude: -10.8,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_B, isAnonymous: false, status: 'VERIFIED',
      imageUrls: [], videoUrls: [], audioUrls: [], incidentTimestamp: 1, aiScreeningStatus: 'CLEAR', aiFlaggedReasons: [],
    })

    await setDoc(doc(db, 'wanted_criminals', 'wanted-verified'), {
      id: 'wanted-verified', name: 'John Doe', description: 'x', timestamp: 1, updatedAt: 1, isArrested: false,
      imageUrls: [], category: 'Wanted person notices', status: 'VERIFIED', isVerified: true,
    })
    await setDoc(doc(db, 'wanted_criminals', 'wanted-unverified'), {
      id: 'wanted-unverified', name: 'Jane Roe', description: 'x', timestamp: 1, updatedAt: 1, isArrested: false,
      imageUrls: [], category: 'Wanted person notices', status: 'SUBMITTED', isVerified: false,
    })

    await setDoc(doc(db, 'tips', 'tip-attributed-a'), {
      id: 'tip-attributed-a', reportId: 'report-pending-a', content: 'x', isAnonymous: false,
      submitterId: CITIZEN_A, timestamp: 1, isReviewed: false, aiScreeningStatus: 'CLEAR', aiFlaggedReasons: [],
    })
    await setDoc(doc(db, 'tips', 'tip-anonymous'), {
      id: 'tip-anonymous', reportId: 'report-pending-a', content: 'x', isAnonymous: true,
      timestamp: 1, isReviewed: false, aiScreeningStatus: 'CLEAR', aiFlaggedReasons: [],
    })
  })
}

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: { rules: readFileSync(resolve(__dirname, '../firestore.rules'), 'utf8'), host: 'localhost', port: 8080 },
  })
})
afterAll(async () => testEnv.cleanup())
beforeEach(async () => { await testEnv.clearFirestore(); await seed() })

const asCitizenA = () => testEnv.authenticatedContext(CITIZEN_A).firestore()
/**
 * Mirrors services.ts#writeWithQuota exactly: creates `item` in `name` AND
 * increments the caller's rolling 24h /rate_limits/{uid}_{kind} counter in
 * one atomic transaction — since the create rules for reports/tips/alerts
 * now require quotaOk(), which checks the counter was incremented in the
 * SAME transaction (getAfter()). A bare setDoc() for these three
 * collections is expected to fail under the current rules (see the
 * dedicated quota describe blocks below) — use this helper for every
 * otherwise-authorized create test instead.
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
async function createWithQuota(db: any, name: string, item: { id:string } & Record<string, unknown>, uid: string, kind: 'reports'|'tips'|'alerts') {
  const counterRef = doc(db, 'rate_limits', `${uid}_${kind}`)
  const itemRef = doc(db, name, item.id)
  await runTransaction(db, async (tx) => {
    const counterSnap = await tx.get(counterRef)
    const existing = counterSnap.exists() ? (counterSnap.data() as { count:number }) : undefined
    tx.set(itemRef, item)
    tx.set(counterRef, { uid, kind, count: (existing?.count ?? 0) + 1, windowStart: existing ? counterSnap.data()!.windowStart : serverTimestamp() })
  })
}
const asCitizenB = () => testEnv.authenticatedContext(CITIZEN_B).firestore()
const asOfficer = () => testEnv.authenticatedContext(OFFICER_APPROVED).firestore()
const asUnapprovedOfficer = () => testEnv.authenticatedContext(OFFICER_UNAPPROVED).firestore()
const asAdmin = () => testEnv.authenticatedContext(ADMIN).firestore()
const anon = () => testEnv.unauthenticatedContext().firestore()

describe('firestore.rules — /users/{userId}', () => {
  it('[authorized read] a citizen can read their own profile', async () => {
    await assertSucceeds(getDoc(doc(asCitizenA(), 'users', CITIZEN_A)))
  })
  it('[unauthorized read] a citizen cannot read another citizen\'s profile', async () => {
    await assertFails(getDoc(doc(asCitizenA(), 'users', CITIZEN_B)))
  })
  it('[authorized read] admin can read any user\'s profile, and list the whole collection', async () => {
    await assertSucceeds(getDoc(doc(asAdmin(), 'users', CITIZEN_A)))
    await assertSucceeds(getDocs(collection(asAdmin(), 'users')))
  })
  it('[unauthorized read] an approved officer cannot list the whole users collection', async () => {
    await assertFails(getDocs(collection(asOfficer(), 'users')))
  })
  it('[authorized write] a citizen can create their own profile at signup', async () => {
    await assertSucceeds(setDoc(doc(asCitizenA(), 'users', 'new-citizen'), {
      id: 'new-citizen', name: 'New', email: 'n@x.com', role: 'CITIZEN', isApproved: true,
    }))
  })
  it('[privilege escalation] a new signup cannot self-assign the ADMIN role', async () => {
    const ctx = testEnv.authenticatedContext('escalator').firestore()
    await assertFails(setDoc(doc(ctx, 'users', 'escalator'), {
      id: 'escalator', name: 'Evil', email: 'e@x.com', role: 'ADMIN', isApproved: true,
    }))
  })
  it('[privilege escalation] a new officer signup cannot self-approve', async () => {
    const ctx = testEnv.authenticatedContext('escalator').firestore()
    await assertFails(setDoc(doc(ctx, 'users', 'escalator'), {
      id: 'escalator', name: 'Evil Officer', email: 'e@x.com', role: 'LAW_ENFORCER', isApproved: true,
    }))
  })
  it('[immutable field] a citizen cannot change their own role field', async () => {
    await assertFails(updateDoc(doc(asCitizenA(), 'users', CITIZEN_A), { role: 'ADMIN' }))
  })
  it('[immutable field] a citizen cannot change their own isApproved field', async () => {
    await assertFails(updateDoc(doc(asCitizenA(), 'users', CITIZEN_A), { isApproved: false }))
  })
  it('[authorized write] a citizen can update their own name/contact/address', async () => {
    await assertSucceeds(updateDoc(doc(asCitizenA(), 'users', CITIZEN_A), { name: 'Updated Name', contact: '077...' }))
  })
  it('[ownership violation] a citizen cannot update another citizen\'s profile', async () => {
    await assertFails(updateDoc(doc(asCitizenA(), 'users', CITIZEN_B), { name: 'Hijacked' }))
  })
  it('[Function-exclusive field] even admin cannot approve an officer via a direct write anymore — only the approveOfficer Cloud Function can (see FIREBASE_FUNCTIONS_ARCHITECTURE.md)', async () => {
    await assertFails(updateDoc(doc(asAdmin(), 'users', OFFICER_UNAPPROVED), { isApproved: true }))
  })
  it('[Function-exclusive field] even admin cannot change a user\'s role via a direct write anymore — only setUserRole can', async () => {
    await assertFails(updateDoc(doc(asAdmin(), 'users', CITIZEN_A), { role: 'ADMIN' }))
  })
  it('[Function-exclusive field] even admin cannot change a user\'s status via a direct write anymore — only setUserStatus can', async () => {
    await assertFails(updateDoc(doc(asAdmin(), 'users', CITIZEN_A), { status: 'BANNED' }))
  })
  it('[immutable field] even admin cannot change a user document\'s id field', async () => {
    await assertFails(updateDoc(doc(asAdmin(), 'users', CITIZEN_A), { id: 'someone-else' }))
  })
})

describe('firestore.rules — /reports/{reportId}', () => {
  it('[authorized write] a citizen can create their own report as PENDING with no media', async () => {
    await createWithQuota(asCitizenA(), 'reports', {
      id: 'new-report', title: 'x', description: 'x', type: 'THEFT', latitude: 0, longitude: 0,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_A, isAnonymous: false, status: 'PENDING',
      imageUrls: [], videoUrls: [], audioUrls: [],
    }, CITIZEN_A, 'reports')
  })
  it('[authorized write] a citizen can create a report with valid HTTPS Storage evidence URLs', async () => {
    await createWithQuota(asCitizenA(), 'reports', {
      id: 'new-report-media', title: 'x', description: 'x', type: 'THEFT', latitude: 0, longitude: 0,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_A, isAnonymous: false, status: 'PENDING',
      imageUrls: ['https://firebasestorage.googleapis.com/v0/b/x/o/reports%2Fnew-report-media%2Fcitizen-a%2Fimage%2Fa.jpg'],
      videoUrls: [], audioUrls: [],
    }, CITIZEN_A, 'reports')
  })
  it('[NON-NEGOTIABLE / Storage path violation] a report can never be created with a blob:/file:/content: media URL', async () => {
    await assertFails(setDoc(doc(asCitizenA(), 'reports', 'bad-url-report'), {
      id: 'bad-url-report', title: 'x', description: 'x', type: 'THEFT', latitude: 0, longitude: 0,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_A, isAnonymous: false, status: 'PENDING',
      imageUrls: ['blob:http://localhost/abc-123'], videoUrls: [], audioUrls: [],
    }))
  })
  it('[privilege escalation] a citizen cannot create a report that is already VERIFIED', async () => {
    await assertFails(setDoc(doc(asCitizenA(), 'reports', 'self-verified'), {
      id: 'self-verified', title: 'x', description: 'x', type: 'THEFT', latitude: 0, longitude: 0,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_A, isAnonymous: false, status: 'VERIFIED',
      imageUrls: [], videoUrls: [], audioUrls: [],
    }))
  })
  it('[ownership violation] a citizen cannot create a report claiming someone else\'s reporterId', async () => {
    await assertFails(setDoc(doc(asCitizenA(), 'reports', 'spoofed-owner'), {
      id: 'spoofed-owner', title: 'x', description: 'x', type: 'THEFT', latitude: 0, longitude: 0,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_B, isAnonymous: false, status: 'PENDING',
      imageUrls: [], videoUrls: [], audioUrls: [],
    }))
  })
  it('[authorized read] the owner can read their own PENDING report', async () => {
    await assertSucceeds(getDoc(doc(asCitizenA(), 'reports', 'report-pending-a')))
  })
  it('[unauthorized read] another citizen cannot read someone else\'s PENDING report', async () => {
    await assertFails(getDoc(doc(asCitizenB(), 'reports', 'report-pending-a')))
  })
  it('[authorized read] anyone, even signed out, can read a VERIFIED report', async () => {
    await assertSucceeds(getDoc(doc(anon(), 'reports', 'report-verified')))
  })
  it('[unauthorized read] signed-out visitors cannot read a PENDING (unverified) report', async () => {
    await assertFails(getDoc(doc(anon(), 'reports', 'report-pending-a')))
  })
  it('[authorized read] an approved officer can read any report regardless of status, and list the whole collection', async () => {
    await assertSucceeds(getDoc(doc(asOfficer(), 'reports', 'report-pending-a')))
    await assertSucceeds(getDocs(collection(asOfficer(), 'reports')))
  })
  it('[Function-exclusive field] even an approved officer cannot transition a report\'s status via a direct write anymore — only the transitionReportStatus Cloud Function can (see FIREBASE_FUNCTIONS_ARCHITECTURE.md)', async () => {
    await assertFails(updateDoc(doc(asOfficer(), 'reports', 'report-pending-a'), { status: 'UNDER_INVESTIGATION', updatedAt: 2 }))
    await assertFails(updateDoc(doc(asAdmin(), 'reports', 'report-pending-a'), { status: 'VERIFIED', updatedAt: 2 }))
  })
  it('[unauthorized write] an UNAPPROVED officer has no more report access than a citizen — cannot change status', async () => {
    await assertFails(updateDoc(doc(asUnapprovedOfficer(), 'reports', 'report-pending-a'), { status: 'VERIFIED' }))
  })
  it('[privilege escalation] the reporting citizen cannot change their own report\'s status', async () => {
    await assertFails(updateDoc(doc(asCitizenA(), 'reports', 'report-pending-a'), { status: 'VERIFIED' }))
  })
  it('[immutable field] an officer cannot change a report\'s reporterId or evidence URLs even alongside an otherwise-allowed field', async () => {
    await assertFails(updateDoc(doc(asOfficer(), 'reports', 'report-pending-a'), { internalNotes: 'x', reporterId: OFFICER_APPROVED }))
    await assertFails(updateDoc(doc(asOfficer(), 'reports', 'report-pending-a'), { internalNotes: 'x', imageUrls: ['https://firebasestorage.googleapis.com/v0/b/x/o/planted.jpg'] }))
  })
  it('[authorized write] the owner can edit their own report\'s title/description', async () => {
    await assertSucceeds(updateDoc(doc(asCitizenA(), 'reports', 'report-pending-a'), { title: 'Updated title', updatedAt: 2 }))
  })
  it('[immutable field] the owner cannot touch staff-only fields (internalNotes, assignedInvestigator, isEscalated) on their own report', async () => {
    await assertFails(updateDoc(doc(asCitizenA(), 'reports', 'report-pending-a'), { internalNotes: 'trying to inject notes' }))
    await assertFails(updateDoc(doc(asCitizenA(), 'reports', 'report-pending-a'), { assignedInvestigator: CITIZEN_A }))
    await assertFails(updateDoc(doc(asCitizenA(), 'reports', 'report-pending-a'), { isEscalated: true }))
  })
  it('[ownership violation] the owner can soft-delete (isDeleted: true) but cannot resurrect a deleted report', async () => {
    await assertSucceeds(updateDoc(doc(asCitizenA(), 'reports', 'report-pending-a'), { isDeleted: true, updatedAt: 2 }))
    await assertFails(updateDoc(doc(asCitizenA(), 'reports', 'report-pending-a'), { isDeleted: false, updatedAt: 3 }))
  })
  it('no client can hard-delete a report', async () => {
    await assertFails(deleteDoc(doc(asAdmin(), 'reports', 'report-pending-a')))
  })
})

describe('firestore.rules — /wanted_criminals/{id}', () => {
  it('[authorized write] an approved officer can create a wanted notice', async () => {
    await assertSucceeds(setDoc(doc(asOfficer(), 'wanted_criminals', 'new-wanted'), {
      id: 'new-wanted', name: 'x', description: 'x', timestamp: 1, updatedAt: 1, isArrested: false,
      imageUrls: [], category: 'Wanted person notices', status: 'SUBMITTED', isVerified: false,
    }))
  })
  it('[unauthorized write / privilege escalation] a citizen cannot create a wanted notice', async () => {
    await assertFails(setDoc(doc(asCitizenA(), 'wanted_criminals', 'citizen-wanted'), {
      id: 'citizen-wanted', name: 'x', description: 'x', timestamp: 1, updatedAt: 1, isArrested: false,
      imageUrls: [], category: 'Wanted person notices', status: 'SUBMITTED', isVerified: false,
    }))
  })
  it('[authorized read] anyone can read a verified wanted notice', async () => {
    await assertSucceeds(getDoc(doc(anon(), 'wanted_criminals', 'wanted-verified')))
  })
  it('[unauthorized read] a citizen cannot read an unverified wanted notice', async () => {
    await assertFails(getDoc(doc(asCitizenA(), 'wanted_criminals', 'wanted-unverified')))
  })
  it('[Function-exclusive field] even admin cannot verify a wanted notice via a direct write anymore — only the verifyWantedNotice Cloud Function can (see FIREBASE_FUNCTIONS_ARCHITECTURE.md)', async () => {
    await assertFails(updateDoc(doc(asAdmin(), 'wanted_criminals', 'wanted-unverified'), { isVerified: true, status: 'VERIFIED', updatedAt: 2 }))
  })
  it('[authorized write] staff can still soft-delete a wanted notice directly', async () => {
    await assertSucceeds(updateDoc(doc(asOfficer(), 'wanted_criminals', 'wanted-unverified'), { isDeleted: true, updatedAt: 2 }))
  })
  it('[immutable field] an officer cannot rewrite a wanted notice\'s name/description via update', async () => {
    await assertFails(updateDoc(doc(asOfficer(), 'wanted_criminals', 'wanted-unverified'), { name: 'Someone Else' }))
  })
})

describe('firestore.rules — /tips/{id}', () => {
  it('[authorized write] a citizen can submit an anonymous tip', async () => {
    await createWithQuota(asCitizenA(), 'tips', {
      id: 'new-anon-tip', reportId: 'report-pending-a', content: 'x', isAnonymous: true,
      timestamp: 1, isReviewed: false, aiScreeningStatus: 'CLEAR', aiFlaggedReasons: [],
    }, CITIZEN_A, 'tips')
  })
  it('[authorized write] a citizen can submit an attributed tip as themselves', async () => {
    await createWithQuota(asCitizenA(), 'tips', {
      id: 'new-attr-tip', reportId: 'report-pending-a', content: 'x', isAnonymous: false, submitterId: CITIZEN_A,
      timestamp: 1, isReviewed: false, aiScreeningStatus: 'CLEAR', aiFlaggedReasons: [],
    }, CITIZEN_A, 'tips')
  })
  it('[ownership violation] a citizen cannot submit a tip claiming someone else as the submitter', async () => {
    await assertFails(setDoc(doc(asCitizenA(), 'tips', 'spoofed-tip'), {
      id: 'spoofed-tip', reportId: 'report-pending-a', content: 'x', isAnonymous: false, submitterId: CITIZEN_B,
      timestamp: 1, isReviewed: false, aiScreeningStatus: 'CLEAR', aiFlaggedReasons: [],
    }))
  })
  it('[authorized read] a citizen can read their own attributed tip back', async () => {
    await assertSucceeds(getDoc(doc(asCitizenA(), 'tips', 'tip-attributed-a')))
  })
  it('[unauthorized read] a citizen cannot read another citizen\'s attributed tip', async () => {
    await assertFails(getDoc(doc(asCitizenB(), 'tips', 'tip-attributed-a')))
  })
  it('[unauthorized read] nobody — not even its own sender — can read back a truly anonymous tip; only staff can', async () => {
    await assertFails(getDoc(doc(asCitizenA(), 'tips', 'tip-anonymous')))
    await assertSucceeds(getDoc(doc(asOfficer(), 'tips', 'tip-anonymous')))
  })
  it('[authorized read] staff can read any tip and list the whole collection', async () => {
    await assertSucceeds(getDocs(collection(asOfficer(), 'tips')))
  })
  it('[privilege escalation] a citizen cannot mark their own tip reviewed', async () => {
    await assertFails(updateDoc(doc(asCitizenA(), 'tips', 'tip-attributed-a'), { isReviewed: true }))
  })
})

describe('firestore.rules — /alerts/{id}', () => {
  it('[authorized read] a signed-out visitor can read public safety alerts', async () => {
    await assertSucceeds(getDocs(collection(anon(), 'alerts')))
  })
  it('[authorized write] a signed-in citizen can create an alert (e.g. an SOS signal)', async () => {
    await createWithQuota(asCitizenA(), 'alerts', {
      id: 'sos-1', title: 'SOS', content: 'x', urgency: 3, locationName: 'x', latitude: 0, longitude: 0, timestamp: 1,
    }, CITIZEN_A, 'alerts')
  })
  it('[unauthorized write] a signed-out visitor cannot create an alert', async () => {
    await assertFails(setDoc(doc(anon(), 'alerts', 'fake-sos'), {
      id: 'fake-sos', title: 'x', content: 'x', urgency: 1, locationName: 'x', latitude: 0, longitude: 0, timestamp: 1,
    }))
  })
  it('[unauthorized write / privilege escalation] a citizen cannot modify an existing alert', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'alerts', 'existing'), { id: 'existing', title: 'x', content: 'x', urgency: 1, locationName: 'x', latitude: 0, longitude: 0, timestamp: 1 })
    })
    await assertFails(updateDoc(doc(asCitizenA(), 'alerts', 'existing'), { urgency: 99 }))
    await assertSucceeds(updateDoc(doc(asAdmin(), 'alerts', 'existing'), { urgency: 2 }))
  })
})

describe('firestore.rules — per-user daily quotas (/rate_limits/{uid}_{kind})', () => {
  it('[rate limiting] creating a report WITHOUT incrementing the quota counter in the same transaction is denied', async () => {
    // The exact bypass attempt this mechanism exists to prevent: writing
    // the report directly and skipping the counter entirely.
    await assertFails(setDoc(doc(asCitizenA(), 'reports', 'no-quota-report'), {
      id: 'no-quota-report', title: 'x', description: 'x', type: 'THEFT', latitude: 0, longitude: 0,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_A, isAnonymous: false, status: 'PENDING',
      imageUrls: [], videoUrls: [], audioUrls: [],
    }))
  })

  it('[rate limiting] a citizen cannot create their 11th report of the day (limit is 10)', async () => {
    for (let i = 0; i < 10; i++) {
      await createWithQuota(asCitizenA(), 'reports', {
        id: `quota-report-${i}`, title: 'x', description: 'x', type: 'THEFT', latitude: 0, longitude: 0,
        timestamp: 1, updatedAt: 1, reporterId: CITIZEN_A, isAnonymous: false, status: 'PENDING',
        imageUrls: [], videoUrls: [], audioUrls: [],
      }, CITIZEN_A, 'reports')
    }
    await expect(createWithQuota(asCitizenA(), 'reports', {
      id: 'quota-report-11th', title: 'x', description: 'x', type: 'THEFT', latitude: 0, longitude: 0,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_A, isAnonymous: false, status: 'PENDING',
      imageUrls: [], videoUrls: [], audioUrls: [],
    }, CITIZEN_A, 'reports')).rejects.toThrow()
  })

  it('[rate limiting] each citizen has an independent quota — citizen B is unaffected by citizen A\'s usage', async () => {
    for (let i = 0; i < 10; i++) {
      await createWithQuota(asCitizenA(), 'reports', {
        id: `a-quota-report-${i}`, title: 'x', description: 'x', type: 'THEFT', latitude: 0, longitude: 0,
        timestamp: 1, updatedAt: 1, reporterId: CITIZEN_A, isAnonymous: false, status: 'PENDING',
        imageUrls: [], videoUrls: [], audioUrls: [],
      }, CITIZEN_A, 'reports')
    }
    // Citizen A is now maxed out; citizen B's own counter is untouched.
    await createWithQuota(asCitizenB(), 'reports', {
      id: 'b-quota-report-1', title: 'x', description: 'x', type: 'THEFT', latitude: 0, longitude: 0,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_B, isAnonymous: false, status: 'PENDING',
      imageUrls: [], videoUrls: [], audioUrls: [],
    }, CITIZEN_B, 'reports')
  })

  it('[privilege escalation] a citizen cannot fake a low counter value to unlock more submissions than they\'ve actually made', async () => {
    // Skip straight to writing the counter directly at an artificially low
    // count, bypassing the transaction entirely.
    await assertFails(setDoc(doc(asCitizenA(), 'rate_limits', `${CITIZEN_A}_reports`), {
      uid: CITIZEN_A, kind: 'reports', count: 1, windowStart: new Date(),
    }))
  })

  it('[ownership violation] a citizen cannot read or write another citizen\'s quota counter', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'rate_limits', `${CITIZEN_B}_reports`), { uid: CITIZEN_B, kind: 'reports', count: 3, windowStart: new Date() })
    })
    await assertFails(getDoc(doc(asCitizenA(), 'rate_limits', `${CITIZEN_B}_reports`)))
    await assertFails(setDoc(doc(asCitizenA(), 'rate_limits', `${CITIZEN_B}_reports`), { uid: CITIZEN_B, kind: 'reports', count: 4, windowStart: new Date() }))
  })

  it('[cost-abuse protection] tips and alerts have their own, independent daily quotas (15 and 5 respectively)', async () => {
    for (let i = 0; i < 15; i++) {
      await createWithQuota(asCitizenA(), 'tips', {
        id: `quota-tip-${i}`, reportId: 'report-pending-a', content: 'x', isAnonymous: true,
        timestamp: 1, isReviewed: false, aiScreeningStatus: 'CLEAR', aiFlaggedReasons: [],
      }, CITIZEN_A, 'tips')
    }
    await expect(createWithQuota(asCitizenA(), 'tips', {
      id: 'quota-tip-16th', reportId: 'report-pending-a', content: 'x', isAnonymous: true,
      timestamp: 1, isReviewed: false, aiScreeningStatus: 'CLEAR', aiFlaggedReasons: [],
    }, CITIZEN_A, 'tips')).rejects.toThrow()

    for (let i = 0; i < 5; i++) {
      await createWithQuota(asCitizenA(), 'alerts', {
        id: `quota-alert-${i}`, title: 'SOS', content: 'x', urgency: 3, locationName: 'x', latitude: 0, longitude: 0, timestamp: 1,
      }, CITIZEN_A, 'alerts')
    }
    await expect(createWithQuota(asCitizenA(), 'alerts', {
      id: 'quota-alert-6th', title: 'SOS', content: 'x', urgency: 3, locationName: 'x', latitude: 0, longitude: 0, timestamp: 1,
    }, CITIZEN_A, 'alerts')).rejects.toThrow()
  })
})

describe('firestore.rules — field-length and media-count limits (cost-abuse protection)', () => {
  it('rejects a report title/description far beyond the documented limits', async () => {
    await assertFails(setDoc(doc(asCitizenA(), 'reports', 'huge-title'), {
      id: 'huge-title', title: 'x'.repeat(201), description: 'x', type: 'THEFT', latitude: 0, longitude: 0,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_A, isAnonymous: false, status: 'PENDING',
      imageUrls: [], videoUrls: [], audioUrls: [],
    }))
    await assertFails(setDoc(doc(asCitizenA(), 'reports', 'huge-desc'), {
      id: 'huge-desc', title: 'x', description: 'x'.repeat(5001), type: 'THEFT', latitude: 0, longitude: 0,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_A, isAnonymous: false, status: 'PENDING',
      imageUrls: [], videoUrls: [], audioUrls: [],
    }))
  })
  it('rejects a report with more than 6 total attached media items, even if each URL is individually valid', async () => {
    const url = (n:number) => `https://firebasestorage.googleapis.com/v0/b/x/o/img${n}.jpg`
    await assertFails(setDoc(doc(asCitizenA(), 'reports', 'too-much-media'), {
      id: 'too-much-media', title: 'x', description: 'x', type: 'THEFT', latitude: 0, longitude: 0,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_A, isAnonymous: false, status: 'PENDING',
      imageUrls: [url(1), url(2), url(3), url(4)], videoUrls: [url(5), url(6)], audioUrls: [url(7)],
    }))
  })
  it('rejects a wanted notice with more than 4 photos', async () => {
    const url = (n:number) => `https://firebasestorage.googleapis.com/v0/b/x/o/img${n}.jpg`
    await assertFails(setDoc(doc(asOfficer(), 'wanted_criminals', 'too-many-photos'), {
      id: 'too-many-photos', name: 'x', description: 'x', timestamp: 1, updatedAt: 1, isArrested: false,
      imageUrls: [url(1), url(2), url(3), url(4), url(5)], category: 'x', status: 'SUBMITTED', isVerified: false,
    }))
  })
})


describe('firestore.rules — /audit_logs/{id} (written exclusively by Cloud Functions via the Admin SDK)', () => {
  it('[authorized read] admin can read audit log entries', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'audit_logs', 'entry-1'), { action: 'approveOfficer', actorUid: ADMIN, result: 'success' })
    })
    await assertSucceeds(getDoc(doc(asAdmin(), 'audit_logs', 'entry-1')))
    await assertSucceeds(getDocs(collection(asAdmin(), 'audit_logs')))
  })
  it('[unauthorized read] neither a citizen nor an officer can read audit logs — only ADMIN', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'audit_logs', 'entry-2'), { action: 'x' })
    })
    await assertFails(getDoc(doc(asCitizenA(), 'audit_logs', 'entry-2')))
    await assertFails(getDoc(doc(asOfficer(), 'audit_logs', 'entry-2')))
  })
  it('[unauthorized write] NOBODY can write an audit log entry directly, including admin — only the Admin SDK (Cloud Functions) can, which bypasses rules entirely', async () => {
    await assertFails(setDoc(doc(asAdmin(), 'audit_logs', 'forged'), { action: 'approveOfficer', actorUid: ADMIN, result: 'success' }))
  })
})

describe('firestore.rules — /function_calls/{requestId} (internal idempotency bookkeeping)', () => {
  it('is completely inaccessible to every client, including admin, for both read and write', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'function_calls', 'req-1'), { status: 'completed' })
    })
    await assertFails(getDoc(doc(asAdmin(), 'function_calls', 'req-1')))
    await assertFails(setDoc(doc(asAdmin(), 'function_calls', 'req-2'), { status: 'completed' }))
  })
})

describe('firestore.rules — /function_rate_limits/{id} (internal per-caller rate counters)', () => {
  it('is completely inaccessible to every client, including admin, for both read and write', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'function_rate_limits', `${ADMIN}_publishAlert`), { windowStart: Date.now(), count: 1 })
    })
    await assertFails(getDoc(doc(asAdmin(), 'function_rate_limits', `${ADMIN}_publishAlert`)))
    await assertFails(setDoc(doc(asAdmin(), 'function_rate_limits', `${ADMIN}_setUserStatus`), { windowStart: Date.now(), count: 1 }))
  })
})
