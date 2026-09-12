package com.example.data.repository

import com.example.data.local.TraceNetDao
import com.example.data.model.*
import com.example.data.firebase.FirebaseSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus

class TraceNetRepository(
    private val dao: TraceNetDao,
    private val syncManager: FirebaseSyncManager? = null
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO) + SupervisorJob()

    val allReports: Flow<List<Report>> = dao.getAllReports()
    val activeReports: Flow<List<Report>> = dao.getActiveReports()
    val deletedReports: Flow<List<Report>> = dao.getDeletedReports()

    val allAlerts: Flow<List<Alert>> = dao.getAllAlerts()
    val pendingLawEnforcers: Flow<List<User>> = dao.getPendingLawEnforcers()
    val wantedCriminals: Flow<List<WantedCriminal>> = dao.getAllWantedCriminals()
    val activeWantedCriminals: Flow<List<WantedCriminal>> = dao.getActiveWantedCriminals()
    val verifiedWantedCriminals: Flow<List<WantedCriminal>> = dao.getVerifiedWantedCriminals()
    val pendingWantedCriminals: Flow<List<WantedCriminal>> = dao.getPendingWantedCriminals()
    val deletedWantedCriminals: Flow<List<WantedCriminal>> = dao.getDeletedWantedCriminals()

    val allTips: Flow<List<Tip>> = dao.getAllTips()
    val deletedTips: Flow<List<Tip>> = dao.getDeletedTips()
    val allUsers: Flow<List<User>> = dao.getAllUsers()
    val allAuditLogs: Flow<List<AuditLog>> = dao.getAllAuditLogs()
    val deletedAuditLogs: Flow<List<AuditLog>> = dao.getDeletedAuditLogs()
    val allHelpMessages: Flow<List<HelpMessage>> = dao.getAllHelpMessages()

    fun getUser(userId: String): Flow<User?> = dao.getUserById(userId)
    
    suspend fun fetchUserFromCloud(userId: String): User? {
        return syncManager?.fetchUser(userId)?.also {
            dao.insertUser(it) // Cache locally
        }
    }

    fun getUserByEmail(email: String): Flow<User?> = dao.getUserByEmail(email)

    suspend fun fetchUserByEmailFromCloud(email: String): User? {
        return syncManager?.fetchUserByEmail(email)?.also {
            dao.insertUser(it) // Cache locally
        }
    }

    suspend fun updateUserStatus(userId: String, status: String) {
        dao.updateUserStatus(userId, status)
        repositoryScope.launch {
            try {
                dao.getUserByIdOnce(userId)?.let {
                    syncManager?.syncUser(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing user status: ${e.message}")
            }
        }
    }

    suspend fun logAction(action: AuditLog) {
        dao.insertAuditLog(action)
        syncManager?.syncAuditLog(action)
    }
    
    suspend fun saveUser(user: User) {
        dao.insertUser(user)
        syncManager?.syncUser(user)
    }
    
    suspend fun submitReport(report: Report) {
        dao.insertReport(report)
        syncManager?.syncReport(report)
    }
    
    suspend fun updateReport(report: Report) {
        dao.updateReport(report)
        syncManager?.syncReport(report)
    }

    suspend fun updateReport(reportId: String, title: String, description: String) {
        dao.updateReport(reportId, title, description)
        repositoryScope.launch {
            try {
                dao.getReportByIdOnce(reportId)?.let {
                    syncManager?.syncReport(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing report: ${e.message}")
            }
        }
    }

    suspend fun softDeleteReport(reportId: String) {
        dao.softDeleteReport(reportId)
        repositoryScope.launch {
            try {
                dao.getReportByIdOnce(reportId)?.let {
                    syncManager?.syncReport(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing soft delete report: ${e.message}")
            }
        }
    }

    suspend fun restoreReport(reportId: String) {
        dao.restoreReport(reportId)
        repositoryScope.launch {
            try {
                dao.getReportByIdOnce(reportId)?.let {
                    syncManager?.syncReport(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing restore report: ${e.message}")
            }
        }
    }

    suspend fun permanentlyDeleteReport(reportId: String) {
        dao.permanentlyDeleteReport(reportId)
        syncManager?.deleteReport(reportId)
    }
    
    suspend fun submitTip(tip: Tip) {
        dao.insertTip(tip)
        syncManager?.syncTip(tip)
    }

    suspend fun softDeleteTip(tipId: String) {
        dao.softDeleteTip(tipId)
        repositoryScope.launch {
            try {
                dao.getTipByIdOnce(tipId)?.let {
                    syncManager?.syncTip(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing soft delete tip: ${e.message}")
            }
        }
    }

    suspend fun restoreTip(tipId: String) {
        dao.restoreTip(tipId)
        repositoryScope.launch {
            try {
                dao.getTipByIdOnce(tipId)?.let {
                    syncManager?.syncTip(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing restore tip: ${e.message}")
            }
        }
    }

    suspend fun permanentlyDeleteTip(tipId: String) {
        dao.permanentlyDeleteTip(tipId)
        syncManager?.deleteTip(tipId)
    }
    
    suspend fun submitAlert(alert: Alert) {
        dao.insertAlert(alert)
        syncManager?.syncAlert(alert)
    }

    suspend fun deleteAlert(alertId: String) {
        dao.deleteAlert(alertId)
        syncManager?.deleteAlert(alertId)
    }

    suspend fun updateAlert(id: String, title: String, content: String, urgency: Int, locationName: String, latitude: Double, longitude: Double) {
        dao.updateAlert(id, title, content, urgency, locationName, latitude, longitude)
        repositoryScope.launch {
            try {
                dao.getAlertByIdOnce(id)?.let {
                    syncManager?.syncAlert(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing alert: ${e.message}")
            }
        }
    }

    suspend fun postWantedCriminal(criminal: WantedCriminal) {
        dao.insertWantedCriminal(criminal)
        syncManager?.syncWantedCriminal(criminal)
    }

    suspend fun updateWantedCriminal(id: String, name: String, description: String, lastSeen: String, reward: String?) {
        dao.updateWantedCriminal(id, name, description, lastSeen, reward)
        repositoryScope.launch {
            try {
                dao.getWantedCriminalByIdOnce(id)?.let {
                    syncManager?.syncWantedCriminal(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing criminal update: ${e.message}")
            }
        }
    }

    suspend fun updateWantedCriminalStatus(id: String, status: String) {
        dao.updateWantedCriminalStatus(id, status)
        repositoryScope.launch {
            try {
                dao.getWantedCriminalByIdOnce(id)?.let {
                    syncManager?.syncWantedCriminal(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing criminal status: ${e.message}")
            }
        }
    }

    suspend fun updateWantedCriminalVerification(id: String, isVerified: Boolean, status: String) {
        dao.updateWantedCriminalVerification(id, isVerified, status)
        repositoryScope.launch {
            try {
                dao.getWantedCriminalByIdOnce(id)?.let {
                    syncManager?.syncWantedCriminal(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing criminal verification: ${e.message}")
            }
        }
    }

    suspend fun softDeleteWantedCriminal(id: String) {
        dao.softDeleteWantedCriminal(id)
        repositoryScope.launch {
            try {
                dao.getWantedCriminalByIdOnce(id)?.let {
                    syncManager?.syncWantedCriminal(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing criminal delete: ${e.message}")
            }
        }
    }

    suspend fun restoreWantedCriminal(id: String) {
        dao.restoreWantedCriminal(id)
        repositoryScope.launch {
            try {
                dao.getWantedCriminalByIdOnce(id)?.let {
                    syncManager?.syncWantedCriminal(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing criminal restore: ${e.message}")
            }
        }
    }

    suspend fun permanentlyDeleteWantedCriminal(id: String) {
        dao.permanentlyDeleteWantedCriminal(id)
        syncManager?.deleteWantedCriminal(id)
    }
    
    suspend fun updateReportStatus(reportId: String, status: String) {
        dao.updateReportStatus(reportId, status)
        repositoryScope.launch {
            try {
                dao.getReportByIdOnce(reportId)?.let {
                    syncManager?.syncReport(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing report status: ${e.message}")
            }
        }
    }
    
    suspend fun approveLawEnforcer(userId: String) {
        dao.approveLawEnforcer(userId)
        repositoryScope.launch {
            try {
                dao.getUserByIdOnce(userId)?.let {
                    syncManager?.syncUser(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing approval: ${e.message}")
            }
        }
    }

    suspend fun rejectLawEnforcer(userId: String) {
        dao.rejectLawEnforcer(userId)
        syncManager?.deleteUser(userId)
    }

    suspend fun updateUserRole(userId: String, role: UserRole, isApproved: Boolean) {
        dao.updateUserRole(userId, role, isApproved)
        repositoryScope.launch {
            try {
                dao.getUserByIdOnce(userId)?.let {
                    syncManager?.syncUser(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing role update: ${e.message}")
            }
        }
    }

    suspend fun deleteUser(userId: String) {
        dao.deleteUser(userId)
        syncManager?.deleteUser(userId)
    }

    suspend fun updateUser(user: User) {
        dao.insertUser(user)
        syncManager?.syncUser(user)
    }

    fun getTips(reportId: String): Flow<List<Tip>> = dao.getTipsForReport(reportId)

    fun getTipsForUser(userId: String): Flow<List<Tip>> = dao.getTipsForUser(userId)

    suspend fun markTipAsReviewed(tipId: String) {
        dao.markTipAsReviewed(tipId)
        repositoryScope.launch {
            try {
                dao.getTipByIdOnce(tipId)?.let {
                    syncManager?.syncTip(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing reviewed tip: ${e.message}")
            }
        }
    }

    suspend fun softDeleteAuditLog(logId: String) {
        dao.softDeleteAuditLog(logId)
        repositoryScope.launch {
            try {
                dao.getAuditLogByIdOnce(logId)?.let {
                    syncManager?.syncAuditLog(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing soft delete log: ${e.message}")
            }
        }
    }

    suspend fun restoreAuditLog(logId: String) {
        dao.restoreAuditLog(logId)
        repositoryScope.launch {
            try {
                dao.getAuditLogByIdOnce(logId)?.let {
                    syncManager?.syncAuditLog(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing restore log: ${e.message}")
            }
        }
    }

    suspend fun permanentlyDeleteAuditLog(logId: String) {
        dao.permanentlyDeleteAuditLog(logId)
        syncManager?.deleteAuditLog(logId)
    }

    suspend fun submitHelpMessage(message: HelpMessage) {
        dao.insertHelpMessage(message)
        syncManager?.syncHelpMessage(message)
    }

    suspend fun deleteHelpMessage(id: String) {
        dao.deleteHelpMessage(id)
        syncManager?.deleteHelpMessage(id)
    }

    suspend fun markHelpMessageAsRead(id: String, isRead: Boolean) {
        dao.updateHelpMessageReadStatus(id, isRead)
        repositoryScope.launch {
            try {
                dao.getHelpMessageByIdOnce(id)?.let {
                    syncManager?.syncHelpMessage(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceNetRepository", "Error syncing help message read status: ${e.message}")
            }
        }
    }

    fun startRealtimeSync() {
        syncManager?.startRealtimeSync()
    }

    fun stopRealtimeSync() {
        syncManager?.stopRealtimeSync()
    }

    fun syncLocalDatabaseToFirebase() {
        syncManager?.syncLocalDatabaseToFirebase()
    }
}
