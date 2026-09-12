import * as admin from 'firebase-admin';

admin.initializeApp();

export { approveOfficer } from './functions/approveOfficer';
export { setUserRole } from './functions/setUserRole';
export { setUserStatus } from './functions/setUserStatus';
export { verifyWantedNotice } from './functions/verifyWantedNotice';
export { publishAlert } from './functions/publishAlert';
export { transitionReportStatus } from './functions/transitionReportStatus';
