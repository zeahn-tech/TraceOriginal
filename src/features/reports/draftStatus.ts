import { MAX_RETRY_ATTEMPTS, type ReportDraft } from '../../services'

// Internal state machine has 7 states (see services.ts); the two "media done,
// Firestore write in flight/queued" states (UPLOADED, REPORT_PENDING) both
// read as "Syncing" to the person using the app — everything else maps 1:1
// onto the Pending / Uploading / Syncing / Failed / Conflict / Synced model.
export function draftStatusLabel(status:ReportDraft['status']):string{
  switch(status){
    case 'LOCAL_PENDING': return 'PENDING'
    case 'UPLOADING': return 'UPLOADING'
    case 'UPLOADED': case 'REPORT_PENDING': return 'SYNCING'
    case 'FAILED': return 'FAILED'
    case 'CONFLICT': return 'CONFLICT'
    case 'SYNCED': return 'SYNCED'
  }
}
export function draftStatusDetail(d:ReportDraft):string{
  switch(d.status){
    case 'LOCAL_PENDING': return 'Saved on this device — will upload once you\u2019re back online.'
    case 'UPLOADING': return 'Uploading attached media now.'
    case 'UPLOADED': case 'REPORT_PENDING': return 'Media uploaded — finishing sync.'
    case 'FAILED': return d.attempts>=MAX_RETRY_ATTEMPTS ? `Could not sync after ${MAX_RETRY_ATTEMPTS} attempts. Discard and resubmit as a new report.` : 'Could not sync. Your report is safe on this device.'
    case 'CONFLICT': return 'A conflicting record already exists. Review before retrying.'
    case 'SYNCED': return 'Synced.'
  }
}
