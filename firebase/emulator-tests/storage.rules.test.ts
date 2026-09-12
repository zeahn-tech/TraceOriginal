/**
 * Firebase Storage security rules emulator tests.
 *
 * REQUIRES the real Firestore + Storage emulators to be running (this
 * connects to live emulators — it is not a mock of any kind), since
 * storage.rules cross-references Firestore via firestore.get()/exists().
 * Run via:
 *
 *   firebase emulators:exec --project tracenet-emulator-test \
 *     --only firestore,storage "npx vitest run firebase/emulator-tests/storage.rules.test.ts"
 *
 * or use the convenience script: npm run test:security
 *
 * See FIREBASE_SECURITY_IMPLEMENTATION.md for why these could not be
 * executed in the environment that authored this suite, and for exact
 * instructions to run them yourself.
 */
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { afterAll, beforeAll, beforeEach, describe, it } from 'vitest'
import { assertFails, assertSucceeds, initializeTestEnvironment, type RulesTestEnvironment } from '@firebase/rules-unit-testing'
import { doc, setDoc } from 'firebase/firestore'
import { deleteObject, getBytes, ref, uploadBytes } from 'firebase/storage'

const PROJECT_ID = 'tracenet-emulator-test'

let testEnv: RulesTestEnvironment

const CITIZEN_A = 'citizen-a'
const CITIZEN_B = 'citizen-b'
const OFFICER_APPROVED = 'officer-approved'
const ADMIN = 'admin-1'

const smallJpeg = () => new Uint8Array(1024) // content-type is what the rules actually check; bytes are arbitrary
const bigJpeg = () => new Uint8Array(16 * 1024 * 1024 + 1) // 1 byte over the 15 MiB image limit

async function seed() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore()
    await setDoc(doc(db, 'users', CITIZEN_A), { id: CITIZEN_A, name: 'Citizen A', email: 'a@x.com', role: 'CITIZEN', isApproved: true })
    await setDoc(doc(db, 'users', CITIZEN_B), { id: CITIZEN_B, name: 'Citizen B', email: 'b@x.com', role: 'CITIZEN', isApproved: true })
    await setDoc(doc(db, 'users', OFFICER_APPROVED), { id: OFFICER_APPROVED, name: 'Officer', email: 'o@x.com', role: 'LAW_ENFORCER', isApproved: true })
    await setDoc(doc(db, 'users', ADMIN), { id: ADMIN, name: 'Admin', email: 'admin@x.com', role: 'ADMIN', isApproved: true })

    await setDoc(doc(db, 'reports', 'report-pending'), {
      id: 'report-pending', title: 'x', description: 'x', type: 'THEFT', latitude: 0, longitude: 0,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_A, isAnonymous: false, status: 'PENDING',
      imageUrls: [], videoUrls: [], audioUrls: [],
    })
    await setDoc(doc(db, 'reports', 'report-verified'), {
      id: 'report-verified', title: 'x', description: 'x', type: 'THEFT', latitude: 0, longitude: 0,
      timestamp: 1, updatedAt: 1, reporterId: CITIZEN_B, isAnonymous: false, status: 'VERIFIED',
      imageUrls: [], videoUrls: [], audioUrls: [],
    })
    await setDoc(doc(db, 'wanted_criminals', 'wanted-verified'), {
      id: 'wanted-verified', name: 'x', description: 'x', timestamp: 1, updatedAt: 1, isArrested: false,
      imageUrls: [], category: 'x', status: 'VERIFIED', isVerified: true,
    })
    await setDoc(doc(db, 'wanted_criminals', 'wanted-unverified'), {
      id: 'wanted-unverified', name: 'x', description: 'x', timestamp: 1, updatedAt: 1, isArrested: false,
      imageUrls: [], category: 'x', status: 'SUBMITTED', isVerified: false,
    })
  })
}

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: { rules: readFileSync(resolve(__dirname, '../firestore.rules'), 'utf8'), host: 'localhost', port: 8080 },
    storage: { rules: readFileSync(resolve(__dirname, '../storage.rules'), 'utf8'), host: 'localhost', port: 9199 },
  })
})
afterAll(async () => testEnv.cleanup())
beforeEach(async () => { await testEnv.clearFirestore(); await testEnv.clearStorage(); await seed() })

const asCitizenA = () => testEnv.authenticatedContext(CITIZEN_A).storage()
const asCitizenB = () => testEnv.authenticatedContext(CITIZEN_B).storage()
const asOfficer = () => testEnv.authenticatedContext(OFFICER_APPROVED).storage()
const asAdmin = () => testEnv.authenticatedContext(ADMIN).storage()
const anon = () => testEnv.unauthenticatedContext().storage()

describe('storage.rules — report evidence: reports/{reportId}/{uid}/{kind}/{file}', () => {
  it('[authorized write] the report\'s owner can upload evidence under their own uid segment', async () => {
    await assertSucceeds(uploadBytes(
      ref(asCitizenA(), 'reports/report-pending/citizen-a/image/scene.jpg'),
      smallJpeg(), { contentType: 'image/jpeg' },
    ))
  })
  it('[Storage path violation / ownership violation] a different citizen cannot upload into someone else\'s uid segment of the SAME report path', async () => {
    await assertFails(uploadBytes(
      ref(asCitizenB(), 'reports/report-pending/citizen-a/image/planted.jpg'),
      smallJpeg(), { contentType: 'image/jpeg' },
    ))
  })
  it('[unauthorized write] a signed-out visitor cannot upload evidence anywhere', async () => {
    await assertFails(uploadBytes(
      ref(anon(), 'reports/report-pending/citizen-a/image/scene.jpg'),
      smallJpeg(), { contentType: 'image/jpeg' },
    ))
  })
  it('[large-file protection] an oversized image is rejected even from the legitimate owner', async () => {
    await assertFails(uploadBytes(
      ref(asCitizenA(), 'reports/report-pending/citizen-a/image/huge.jpg'),
      bigJpeg(), { contentType: 'image/jpeg' },
    ))
  })
  it('[file-type validation] a disallowed content type is rejected even from the legitimate owner', async () => {
    await assertFails(uploadBytes(
      ref(asCitizenA(), 'reports/report-pending/citizen-a/image/malware.exe'),
      smallJpeg(), { contentType: 'application/x-msdownload' },
    ))
  })
  it('[authorized write] an approved officer may also attach evidence directly', async () => {
    await assertSucceeds(uploadBytes(
      ref(asOfficer(), 'reports/report-pending/citizen-a/image/field-evidence.jpg'),
      smallJpeg(), { contentType: 'image/jpeg' },
    ))
  })
  it('[authorized read] the owner can read their own evidence even while the report is still PENDING', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => uploadBytes(ref(ctx.storage(), 'reports/report-pending/citizen-a/image/scene.jpg'), smallJpeg(), { contentType: 'image/jpeg' }))
    await assertSucceeds(getBytes(ref(asCitizenA(), 'reports/report-pending/citizen-a/image/scene.jpg')))
  })
  it('[unauthorized read] another citizen cannot read evidence for a report that is still PENDING (not yet public)', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => uploadBytes(ref(ctx.storage(), 'reports/report-pending/citizen-a/image/scene.jpg'), smallJpeg(), { contentType: 'image/jpeg' }))
    await assertFails(getBytes(ref(asCitizenB(), 'reports/report-pending/citizen-a/image/scene.jpg')))
  })
  it('[authorized read] anyone can read evidence attached to a VERIFIED report', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => uploadBytes(ref(ctx.storage(), 'reports/report-verified/citizen-b/image/scene.jpg'), smallJpeg(), { contentType: 'image/jpeg' }))
    await assertSucceeds(getBytes(ref(anon(), 'reports/report-verified/citizen-b/image/scene.jpg')))
  })
  it('[authorized read] staff can read evidence for any report regardless of status', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => uploadBytes(ref(ctx.storage(), 'reports/report-pending/citizen-a/image/scene.jpg'), smallJpeg(), { contentType: 'image/jpeg' }))
    await assertSucceeds(getBytes(ref(asOfficer(), 'reports/report-pending/citizen-a/image/scene.jpg')))
  })
})

describe('storage.rules — wanted-notice photos: criminals/{id}/{kind}/{file}', () => {
  it('[authorized write] an approved officer can upload a wanted-notice photo', async () => {
    await assertSucceeds(uploadBytes(
      ref(asOfficer(), 'criminals/wanted-unverified/image/photo.jpg'),
      smallJpeg(), { contentType: 'image/jpeg' },
    ))
  })
  it('[unauthorized write / privilege escalation] a citizen cannot upload a wanted-notice photo', async () => {
    await assertFails(uploadBytes(
      ref(asCitizenA(), 'criminals/wanted-unverified/image/photo.jpg'),
      smallJpeg(), { contentType: 'image/jpeg' },
    ))
  })
  it('[authorized read] anyone can read a photo for a VERIFIED wanted notice', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => uploadBytes(ref(ctx.storage(), 'criminals/wanted-verified/image/photo.jpg'), smallJpeg(), { contentType: 'image/jpeg' }))
    await assertSucceeds(getBytes(ref(anon(), 'criminals/wanted-verified/image/photo.jpg')))
  })
  it('[unauthorized read] a citizen cannot read a photo for an UNVERIFIED wanted notice', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => uploadBytes(ref(ctx.storage(), 'criminals/wanted-unverified/image/photo.jpg'), smallJpeg(), { contentType: 'image/jpeg' }))
    await assertFails(getBytes(ref(asCitizenA(), 'criminals/wanted-unverified/image/photo.jpg')))
  })
})

describe('storage.rules — identity documents: id_cards/{uid}/{file} (private media)', () => {
  it('[authorized write] a citizen can upload their own id card', async () => {
    await assertSucceeds(uploadBytes(
      ref(asCitizenA(), 'id_cards/citizen-a/national-id.jpg'),
      smallJpeg(), { contentType: 'image/jpeg' },
    ))
  })
  it('[Storage path violation / ownership violation] a citizen cannot upload into another user\'s id_cards path', async () => {
    await assertFails(uploadBytes(
      ref(asCitizenB(), 'id_cards/citizen-a/planted-id.jpg'),
      smallJpeg(), { contentType: 'image/jpeg' },
    ))
  })
  it('[authorized read] the owner can read back their own id card', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => uploadBytes(ref(ctx.storage(), 'id_cards/citizen-a/national-id.jpg'), smallJpeg(), { contentType: 'image/jpeg' }))
    await assertSucceeds(getBytes(ref(asCitizenA(), 'id_cards/citizen-a/national-id.jpg')))
  })
  it('[private media / unauthorized read] another CITIZEN cannot read someone else\'s id card', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => uploadBytes(ref(ctx.storage(), 'id_cards/citizen-a/national-id.jpg'), smallJpeg(), { contentType: 'image/jpeg' }))
    await assertFails(getBytes(ref(asCitizenB(), 'id_cards/citizen-a/national-id.jpg')))
  })
  it('[private media / unauthorized read] even an approved OFFICER cannot read another user\'s id card — only ADMIN is explicitly authorized', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => uploadBytes(ref(ctx.storage(), 'id_cards/citizen-a/national-id.jpg'), smallJpeg(), { contentType: 'image/jpeg' }))
    await assertFails(getBytes(ref(asOfficer(), 'id_cards/citizen-a/national-id.jpg')))
  })
  it('[authorized read] admin CAN read a user\'s id card, for the legitimate purpose of officer-approval review', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => uploadBytes(ref(ctx.storage(), 'id_cards/citizen-a/national-id.jpg'), smallJpeg(), { contentType: 'image/jpeg' }))
    await assertSucceeds(getBytes(ref(asAdmin(), 'id_cards/citizen-a/national-id.jpg')))
  })
  it('[unauthorized write] a signed-out visitor cannot upload or read any id card', async () => {
    await assertFails(uploadBytes(ref(anon(), 'id_cards/citizen-a/x.jpg'), smallJpeg(), { contentType: 'image/jpeg' }))
    await testEnv.withSecurityRulesDisabled(async (ctx) => uploadBytes(ref(ctx.storage(), 'id_cards/citizen-a/national-id.jpg'), smallJpeg(), { contentType: 'image/jpeg' }))
    await assertFails(getBytes(ref(anon(), 'id_cards/citizen-a/national-id.jpg')))
  })
  it('[ownership violation] another citizen cannot delete a user\'s id card, but the owner and admin can', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => uploadBytes(ref(ctx.storage(), 'id_cards/citizen-a/national-id.jpg'), smallJpeg(), { contentType: 'image/jpeg' }))
    await assertFails(deleteObject(ref(asCitizenB(), 'id_cards/citizen-a/national-id.jpg')))
    await assertSucceeds(deleteObject(ref(asCitizenA(), 'id_cards/citizen-a/national-id.jpg')))
  })
})

describe('storage.rules — default deny', () => {
  it('an unmatched path is inaccessible to everyone, including admin', async () => {
    await assertFails(uploadBytes(ref(asAdmin(), 'unexpected/path/file.jpg'), smallJpeg(), { contentType: 'image/jpeg' }))
  })
})
