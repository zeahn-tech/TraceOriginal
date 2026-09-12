package com.example.data.firebase

import com.example.BuildConfig
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.example.data.local.TraceNetDao
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.firstOrNull

import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus

class FirebaseSyncManager(
    private val context: Context,
    private val dao: TraceNetDao
) {
    private val TAG = "FirebaseSyncManager"
    private var firestore: FirebaseFirestore? = null
    private val managerScope = CoroutineScope(Dispatchers.IO) + SupervisorJob()

    init {
        try {
            val apps = FirebaseApp.getApps(context)
            val app = if (apps.isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                apps[0]
            }
            
            if (app != null) {
                firestore = FirebaseFirestore.getInstance(app)
                if (BuildConfig.DEBUG) Log.d(TAG, "Firebase successfully initialized for TraceNet (App: ${app.name})")
            } else {
                Log.e(TAG, "FirebaseApp initialization returned null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed critically: ${e.message}. Falling back to Room database only.", e)
            firestore = null
        }
    }

    val isFirebaseAvailable: Boolean
        get() = firestore != null

    // ------------------------------------------------------------------------
    // Entity Mappers
    // ------------------------------------------------------------------------

    private fun User.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "email" to email,
        "role" to role.name,
        "isApproved" to isApproved,
        "profileImageUrl" to profileImageUrl,
        "biometricEnabled" to biometricEnabled,
        "badgeNumber" to badgeNumber,
        "idCardUrl" to idCardUrl,
        "contact" to contact,
        "address" to address,
        "status" to status,
        "reputationScore" to reputationScore
    )

    private fun mapToUser(map: Map<String, Any?>, fallbackId: String = ""): User = User(
        id = map["id"] as? String ?: fallbackId,
        name = map["name"] as? String ?: "",
        email = map["email"] as? String ?: "",
        role = try { UserRole.valueOf(map["role"] as? String ?: UserRole.CITIZEN.name) } catch (e: Exception) { UserRole.CITIZEN },
        isApproved = map["isApproved"] as? Boolean ?: false,
        profileImageUrl = map["profileImageUrl"] as? String,
        biometricEnabled = map["biometricEnabled"] as? Boolean ?: false,
        badgeNumber = map["badgeNumber"] as? String,
        idCardUrl = map["idCardUrl"] as? String,
        contact = map["contact"] as? String,
        address = map["address"] as? String,
        status = map["status"] as? String ?: "ACTIVE",
        reputationScore = (map["reputationScore"] as? Number)?.toInt() ?: 100
    )

    private fun Report.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "type" to type.name,
        "county" to county,
        "latitude" to latitude,
        "longitude" to longitude,
        "timestamp" to timestamp,
        "reporterId" to reporterId,
        "isAnonymous" to isAnonymous,
        "status" to status,
        "imageUrls" to imageUrls,
        "videoUrls" to videoUrls,
        "audioUrls" to audioUrls,
        "incidentTimestamp" to incidentTimestamp,
        "aiScreeningStatus" to aiScreeningStatus,
        "aiFlaggedReasons" to aiFlaggedReasons,
        "internalNotes" to internalNotes,
        "assignedInvestigator" to assignedInvestigator,
        "isEscalated" to isEscalated,
        "contactInfo" to contactInfo,
        "isDeleted" to isDeleted
    )

    private fun mapToReport(map: Map<String, Any?>, fallbackId: String = ""): Report {
        Log.d(TAG, "Parsing report $fallbackId. imageUrls in map: ${map["imageUrls"]}")
        return Report(
            id = map["id"] as? String ?: fallbackId,
            title = map["title"] as? String ?: "",
            description = map["description"] as? String ?: "",
            type = try { ReportType.valueOf(map["type"] as? String ?: ReportType.SUSPICIOUS_ACTIVITY.name) } catch (e: Exception) { ReportType.SUSPICIOUS_ACTIVITY },
            county = map["county"] as? String,
            latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
            longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
            timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            reporterId = map["reporterId"] as? String ?: "",
            isAnonymous = map["isAnonymous"] as? Boolean ?: false,
            status = map["status"] as? String ?: "PENDING",
            imageUrls = (map["imageUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            videoUrls = (map["videoUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            audioUrls = (map["audioUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            incidentTimestamp = (map["incidentTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            aiScreeningStatus = map["aiScreeningStatus"] as? String ?: "NOT_SCREENED",
            aiFlaggedReasons = (map["aiFlaggedReasons"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            internalNotes = map["internalNotes"] as? String,
            assignedInvestigator = map["assignedInvestigator"] as? String,
            isEscalated = map["isEscalated"] as? Boolean ?: false,
            contactInfo = map["contactInfo"] as? String,
            isDeleted = map["isDeleted"] as? Boolean ?: false
        ).also {
            Log.d(TAG, "Successfully mapped report ${it.id}. ImageUrls count: ${it.imageUrls.size}")
        }
    }

    private fun Tip.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "reportId" to reportId,
        "content" to content,
        "isAnonymous" to isAnonymous,
        "timestamp" to timestamp,
        "submitterId" to submitterId,
        "isReviewed" to isReviewed,
        "reviewTimestamp" to reviewTimestamp,
        "aiScreeningStatus" to aiScreeningStatus,
        "aiFlaggedReasons" to aiFlaggedReasons
    )

    private fun mapToTip(map: Map<String, Any?>, fallbackId: String = ""): Tip = Tip(
        id = map["id"] as? String ?: fallbackId,
        reportId = map["reportId"] as? String ?: "",
        content = map["content"] as? String ?: "",
        isAnonymous = map["isAnonymous"] as? Boolean ?: true,
        timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        submitterId = map["submitterId"] as? String,
        isReviewed = map["isReviewed"] as? Boolean ?: false,
        reviewTimestamp = (map["reviewTimestamp"] as? Number)?.toLong(),
        aiScreeningStatus = map["aiScreeningStatus"] as? String ?: "NOT_SCREENED",
        aiFlaggedReasons = (map["aiFlaggedReasons"] as? List<*>)?.map { it.toString() } ?: emptyList()
    )

    private fun Alert.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "content" to content,
        "urgency" to urgency,
        "locationName" to locationName,
        "county" to county,
        "latitude" to latitude,
        "longitude" to longitude,
        "timestamp" to timestamp
    )

    private fun mapToAlert(map: Map<String, Any?>, fallbackId: String = ""): Alert = Alert(
        id = map["id"] as? String ?: fallbackId,
        title = map["title"] as? String ?: "",
        content = map["content"] as? String ?: "",
        urgency = (map["urgency"] as? Number)?.toInt() ?: 1,
        locationName = map["locationName"] as? String ?: "",
        county = map["county"] as? String,
        latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
        longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
        timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )

    private fun WantedCriminal.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "lastSeen" to lastSeen,
        "county" to county,
        "reward" to reward,
        "imageUrls" to imageUrls,
        "isArrested" to isArrested,
        "timestamp" to timestamp,
        "isDeleted" to isDeleted,
        "category" to category,
        "status" to status,
        "isVerified" to isVerified
    )

    private fun mapToWantedCriminal(map: Map<String, Any?>, fallbackId: String = ""): WantedCriminal {
        Log.d(TAG, "Parsing criminal $fallbackId. imageUrls in map: ${map["imageUrls"]}")
        return WantedCriminal(
            id = map["id"] as? String ?: fallbackId,
            name = map["name"] as? String ?: "",
            description = map["description"] as? String ?: "",
            lastSeen = map["lastSeen"] as? String ?: "",
            county = map["county"] as? String,
            reward = map["reward"] as? String,
            imageUrls = (map["imageUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            isArrested = map["isArrested"] as? Boolean ?: false,
            timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            isDeleted = map["isDeleted"] as? Boolean ?: false,
            category = map["category"] as? String ?: "Wanted person notices",
            status = map["status"] as? String ?: "SUBMITTED",
            isVerified = map["isVerified"] as? Boolean ?: false
        ).also {
            Log.d(TAG, "Successfully mapped criminal ${it.id}. ImageUrls count: ${it.imageUrls.size}")
        }
    }

    private fun AuditLog.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "action" to action,
        "description" to description,
        "userId" to userId,
        "userName" to userName,
        "timestamp" to timestamp
    )

    private fun mapToAuditLog(map: Map<String, Any?>, fallbackId: String = ""): AuditLog = AuditLog(
        id = map["id"] as? String ?: fallbackId,
        action = map["action"] as? String ?: "",
        description = map["description"] as? String ?: "",
        userId = map["userId"] as? String ?: "",
        userName = map["userName"] as? String ?: "",
        timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )

    private fun HelpMessage.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "senderName" to senderName,
        "senderEmail" to senderEmail,
        "senderContact" to senderContact,
        "message" to message,
        "timestamp" to timestamp,
        "isRead" to isRead
    )

    private fun mapToHelpMessage(map: Map<String, Any?>, fallbackId: String = ""): HelpMessage = HelpMessage(
        id = map["id"] as? String ?: fallbackId,
        senderName = map["senderName"] as? String ?: "",
        senderEmail = map["senderEmail"] as? String ?: "",
        senderContact = map["senderContact"] as? String ?: "",
        message = map["message"] as? String ?: "",
        timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        isRead = map["isRead"] as? Boolean ?: false
    )

    // ------------------------------------------------------------------------
    // Write Sync Methods (Room -> Firestore)
    // ------------------------------------------------------------------------

    private fun isLocalUri(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.startsWith("file://") || 
               url.startsWith("content://") || 
               url.startsWith("data:") || 
               url.startsWith("/") ||
               url.contains("/data/user/") ||
               url.contains("/storage/emulated/")
    }

    private suspend fun uploadLocalMediaAndGetFirebaseUrl(localUriStr: String, type: String): String {
        if (!isLocalUri(localUriStr)) {
            return localUriStr
        }
        
        Log.d(TAG, "Uploading local media path: $localUriStr of type $type")
        val storageManager = FirebaseStorageManager()
        val uri = if (localUriStr.startsWith("/")) {
            android.net.Uri.fromFile(java.io.File(localUriStr))
        } else {
            android.net.Uri.parse(localUriStr)
        }
        
        return try {
            val uploadedUrl = when (type.lowercase()) {
                "image" -> {
                    if (localUriStr.startsWith("data:")) {
                        val commaIndex = localUriStr.indexOf(",")
                        if (commaIndex != -1) {
                            val base64Data = localUriStr.substring(commaIndex + 1)
                            // OOM FIX: Reject large base64 blobs (> 5MB)
                            if (base64Data.length > 5 * 1024 * 1024 * 1.33) {
                                Log.e(TAG, "Base64 image too large for upload.")
                                localUriStr
                            } else {
                                val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                                storageManager.uploadImageBytes(bytes, "reports/images") ?: localUriStr
                            }
                        } else {
                            null
                        }
                    } else {
                        storageManager.uploadImage(context, uri, "reports/images")
                    }
                }
                "video" -> storageManager.uploadVideo(context, uri, "reports/videos")
                "audio" -> storageManager.uploadAudio(context, uri, "reports/audio")
                else -> null
            }
            if (uploadedUrl != null) {
                Log.d(TAG, "Successfully uploaded local media $localUriStr to cloud: $uploadedUrl")
                uploadedUrl
            } else {
                Log.w(TAG, "Failed to upload local media $localUriStr. Returning original local URI for retry.")
                localUriStr
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading local media $localUriStr: ${e.message}", e)
            localUriStr
        }
    }

    private suspend fun processAndUploadReportMedia(report: Report): Report {
        var modified = false
        
        val newImageUrls = report.imageUrls.map { url ->
            val updated = uploadLocalMediaAndGetFirebaseUrl(url, "image")
            if (updated != url) modified = true
            updated
        }
        
        val newVideoUrls = report.videoUrls.map { url ->
            val updated = uploadLocalMediaAndGetFirebaseUrl(url, "video")
            if (updated != url) modified = true
            updated
        }
        
        val newAudioUrls = report.audioUrls.map { url ->
            val updated = uploadLocalMediaAndGetFirebaseUrl(url, "audio")
            if (updated != url) modified = true
            updated
        }
        
        return if (modified) {
            val r = report.copy(
                imageUrls = newImageUrls,
                videoUrls = newVideoUrls,
                audioUrls = newAudioUrls
            )
            dao.insertReport(r)
            Log.d(TAG, "Updated local Room report with permanent cloud URLs: ${r.id}")
            r
        } else {
            report
        }
    }

    private suspend fun processAndUploadWantedCriminalMedia(criminal: WantedCriminal): WantedCriminal {
        var modified = false
        val newImageUrls = criminal.imageUrls.map { url ->
            val updated = uploadLocalMediaAndGetFirebaseUrl(url, "image")
            if (updated != url) modified = true
            updated
        }
        return if (modified) {
            val c = criminal.copy(imageUrls = newImageUrls)
            dao.insertWantedCriminal(c)
            Log.d(TAG, "Updated local Room wanted criminal with permanent cloud URLs: ${c.id}")
            c
        } else {
            criminal
        }
    }

    private suspend fun processAndUploadUserMedia(user: User): User {
        var modified = false
        val newProfileImageUrl = user.profileImageUrl?.let { url ->
            val updated = uploadLocalMediaAndGetFirebaseUrl(url, "image")
            if (updated != url) modified = true
            updated
        }
        val newIdCardUrl = user.idCardUrl?.let { url ->
            val updated = uploadLocalMediaAndGetFirebaseUrl(url, "image")
            if (updated != url) modified = true
            updated
        }
        return if (modified) {
            val u = user.copy(profileImageUrl = newProfileImageUrl, idCardUrl = newIdCardUrl)
            dao.insertUser(u)
            Log.d(TAG, "Updated local Room user with permanent cloud URLs: ${u.id}")
            u
        } else {
            user
        }
    }

    suspend fun syncUser(user: User) {
        val db = firestore ?: return
        try {
            val cleanUser = processAndUploadUserMedia(user)
            // DATA INTEGRITY FIX: Skip Firestore sync if media failed to upload (local URIs still present)
            if (isLocalUri(cleanUser.profileImageUrl) || isLocalUri(cleanUser.idCardUrl)) {
                Log.w(TAG, "User ${cleanUser.id} has pending media uploads. Postponing Firestore sync.")
                return
            }
            db.collection("users").document(cleanUser.id).set(cleanUser.toMap()).await()
            Log.d(TAG, "Successfully synced user to Firestore: ${cleanUser.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user: ${e.message}")
        }
    }

    suspend fun deleteUser(userId: String) {
        val db = firestore ?: return
        try {
            db.collection("users").document(userId).delete().await()
            Log.d(TAG, "Successfully deleted user from Firestore: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user: ${e.message}")
        }
    }

    suspend fun syncReport(report: Report) {
        val db = firestore ?: return
        try {
            val cleanReport = processAndUploadReportMedia(report)
            // DATA INTEGRITY FIX: Skip Firestore sync if media failed to upload (local URIs still present)
            val hasLocalMedia = cleanReport.imageUrls.any { isLocalUri(it) } || 
                               cleanReport.videoUrls.any { isLocalUri(it) } || 
                               cleanReport.audioUrls.any { isLocalUri(it) }
            
            if (hasLocalMedia) {
                Log.w(TAG, "Report ${cleanReport.id} has pending media uploads. Postponing Firestore sync.")
                return
            }
            
            db.collection("reports").document(cleanReport.id).set(cleanReport.toMap()).await()
            Log.d(TAG, "Successfully synced report to Firestore: ${cleanReport.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing report: ${e.message}")
        }
    }

    suspend fun deleteReport(reportId: String) {
        val db = firestore ?: return
        try {
            db.collection("reports").document(reportId).delete().await()
            Log.d(TAG, "Successfully deleted report from Firestore: $reportId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting report: ${e.message}")
        }
    }

    suspend fun syncTip(tip: Tip) {
        val db = firestore ?: return
        try {
            db.collection("tips").document(tip.id).set(tip.toMap()).await()
            Log.d(TAG, "Successfully synced tip to Firestore: ${tip.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing tip: ${e.message}")
        }
    }

    suspend fun deleteTip(tipId: String) {
        val db = firestore ?: return
        try {
            db.collection("tips").document(tipId).delete().await()
            Log.d(TAG, "Successfully deleted tip from Firestore: $tipId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting tip: ${e.message}")
        }
    }

    suspend fun syncAlert(alert: Alert) {
        val db = firestore ?: return
        try {
            db.collection("alerts").document(alert.id).set(alert.toMap()).await()
            Log.d(TAG, "Successfully synced alert to Firestore: ${alert.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing alert: ${e.message}")
        }
    }

    suspend fun deleteAlert(alertId: String) {
        val db = firestore ?: return
        try {
            db.collection("alerts").document(alertId).delete().await()
            Log.d(TAG, "Successfully deleted alert from Firestore: $alertId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting alert: ${e.message}")
        }
    }

    suspend fun syncWantedCriminal(criminal: WantedCriminal) {
        val db = firestore ?: return
        try {
            val cleanCriminal = processAndUploadWantedCriminalMedia(criminal)
            // DATA INTEGRITY FIX: Skip Firestore sync if media failed to upload (local URIs still present)
            if (cleanCriminal.imageUrls.any { isLocalUri(it) }) {
                Log.w(TAG, "Criminal ${cleanCriminal.id} has pending media uploads. Postponing Firestore sync.")
                return
            }
            db.collection("wanted_criminals").document(cleanCriminal.id).set(cleanCriminal.toMap()).await()
            Log.d(TAG, "Successfully synced wanted criminal to Firestore: ${cleanCriminal.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing wanted criminal: ${e.message}")
        }
    }

    suspend fun deleteWantedCriminal(criminalId: String) {
        val db = firestore ?: return
        try {
            db.collection("wanted_criminals").document(criminalId).delete().await()
            Log.d(TAG, "Successfully deleted wanted criminal from Firestore: $criminalId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting wanted criminal: ${e.message}")
        }
    }

    suspend fun syncAuditLog(log: AuditLog) {
        val db = firestore ?: return
        try {
            db.collection("audit_logs").document(log.id).set(log.toMap()).await()
            Log.d(TAG, "Successfully synced audit log to Firestore: ${log.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing audit log: ${e.message}")
        }
    }

    suspend fun deleteAuditLog(logId: String) {
        val db = firestore ?: return
        try {
            db.collection("audit_logs").document(logId).delete().await()
            Log.d(TAG, "Successfully deleted audit log from Firestore: $logId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting audit log: ${e.message}")
        }
    }

    suspend fun syncHelpMessage(message: HelpMessage) {
        val db = firestore ?: return
        try {
            db.collection("help_messages").document(message.id).set(message.toMap()).await()
            Log.d(TAG, "Successfully synced help message to Firestore: ${message.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing help message: ${e.message}")
        }
    }

    suspend fun deleteHelpMessage(id: String) {
        val db = firestore ?: return
        try {
            db.collection("help_messages").document(id).delete().await()
            Log.d(TAG, "Successfully deleted help message from Firestore: $id")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting help message: ${e.message}")
        }
    }

    suspend fun fetchUser(userId: String): User? {
        val db = firestore ?: return null
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val data = doc.data
                if (data != null) mapToUser(data, doc.id) else null
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user from Firestore: ${e.message}")
            null
        }
    }

    suspend fun fetchUserByEmail(email: String): User? {
        val db = firestore ?: return null
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            val doc = snapshot.documents.firstOrNull()
            if (doc != null && doc.exists()) {
                val data = doc.data
                if (data != null) mapToUser(data, doc.id) else null
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user by email from Firestore: ${e.message}")
            null
        }
    }

    // ------------------------------------------------------------------------
    // Real-time Cloud Listeners (Firestore -> Room)
    // ------------------------------------------------------------------------

    fun syncLocalDatabaseToFirebase() {
        val db = firestore ?: return
        managerScope.launch {
            try {
                Log.d(TAG, "Starting one-time upload of existing local Room DB records to Cloud Firestore...")
                
                // Sync users
                val users = dao.getAllUsers().firstOrNull() ?: emptyList()
                for (user in users) {
                    val cleanUser = processAndUploadUserMedia(user)
                    db.collection("users").document(cleanUser.id).set(cleanUser.toMap()).await()
                }

                // Sync reports
                val reports = dao.getAllReports().firstOrNull() ?: emptyList()
                for (report in reports) {
                    val cleanReport = processAndUploadReportMedia(report)
                    db.collection("reports").document(cleanReport.id).set(cleanReport.toMap()).await()
                }

                // Sync alerts
                val alerts = dao.getAllAlerts().firstOrNull() ?: emptyList()
                for (alert in alerts) {
                    db.collection("alerts").document(alert.id).set(alert.toMap()).await()
                }

                // Sync wanted criminals
                val criminals = dao.getAllWantedCriminals().firstOrNull() ?: emptyList()
                for (criminal in criminals) {
                    val cleanCriminal = processAndUploadWantedCriminalMedia(criminal)
                    db.collection("wanted_criminals").document(cleanCriminal.id).set(cleanCriminal.toMap()).await()
                }

                // Sync tips
                val tips = dao.getAllTips().firstOrNull() ?: emptyList()
                for (tip in tips) {
                    db.collection("tips").document(tip.id).set(tip.toMap()).await()
                }

                // Sync audit logs
                val logs = dao.getAllAuditLogs().firstOrNull() ?: emptyList()
                for (log in logs) {
                    db.collection("audit_logs").document(log.id).set(log.toMap()).await()
                }

                // Sync help messages
                val messages = dao.getAllHelpMessages().firstOrNull() ?: emptyList()
                for (message in messages) {
                    db.collection("help_messages").document(message.id).set(message.toMap()).await()
                }

                Log.d(TAG, "Finished uploading all local records to Cloud Firestore! Check your console now.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload existing local data to Cloud Firestore: ${e.message}", e)
            }
        }
    }

    private val activeRegistrations = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

    fun stopRealtimeSync() {
        synchronized(activeRegistrations) {
            for (reg in activeRegistrations) {
                try {
                    reg.remove()
                } catch (e: Exception) {
                    Log.e(TAG, "Error detaching snapshot listener: ${e.message}")
                }
            }
            activeRegistrations.clear()
        }
        Log.d(TAG, "Stopped Real-time Firestore sync and detached all listeners.")
    }

    fun startRealtimeSync() {
        val db = firestore ?: return
        
        // Ensure we stop and clear any existing listeners first to prevent duplicates/leaks
        stopRealtimeSync()
        
        Log.d(TAG, "Starting Real-time Bi-directional Firestore sync...")
        
        // Push any existing local database items to Cloud Firestore so the user immediately sees collections
        syncLocalDatabaseToFirebase()

        // 1. Listen to users collection
        val usersListener = db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Users snapshot listener error: ${error.message}")
                return@addSnapshotListener
            }
            snapshot?.let {
                if (it.metadata.hasPendingWrites()) return@addSnapshotListener
                managerScope.launch {
                    for (change in it.documentChanges) {
                        try {
                            val data = change.document.data
                            val docId = change.document.id
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                dao.deleteUser(docId)
                            } else {
                                val user = mapToUser(data, docId)
                                dao.insertUser(user)
                                // Item 12: Sync real-time user status/role changes with active session
                                com.example.auth.SessionManager.updateSessionUser(user)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping synced user: ${e.message}")
                        }
                    }
                }
            }
        }

        // 2. Listen to reports collection
        val reportsListener = db.collection("reports").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Reports snapshot listener error: ${error.message}")
                return@addSnapshotListener
            }
            snapshot?.let {
                if (it.metadata.hasPendingWrites()) return@addSnapshotListener
                managerScope.launch {
                    for (change in it.documentChanges) {
                        try {
                            val data = change.document.data
                            val docId = change.document.id
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                dao.permanentlyDeleteReport(docId)
                            } else {
                                Log.d(TAG, "📥 [Sync] Received report change for $docId from Firestore")
                                Log.d(TAG, "📥 [Sync] Raw Firestore data for $docId: $data")
                                val report = mapToReport(data, docId)
                                Log.d(TAG, "📥 [Sync] Mapped report $docId: imageUrls=${report.imageUrls}")
                                dao.insertReport(report)
                                Log.e("FirebaseValidation", "value stored in Room (imageUrls): ${report.imageUrls}")
                                Log.e("FirebaseValidation", "value stored in Room (videoUrls): ${report.videoUrls}")
                                Log.e("FirebaseValidation", "value stored in Room (audioUrls): ${report.audioUrls}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping synced report: ${e.message}")
                        }
                    }
                }
            }
        }

        // 3. Listen to tips collection
        val tipsListener = db.collection("tips").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Tips snapshot listener error: ${error.message}")
                return@addSnapshotListener
            }
            snapshot?.let {
                if (it.metadata.hasPendingWrites()) return@addSnapshotListener
                managerScope.launch {
                    for (change in it.documentChanges) {
                        try {
                            val data = change.document.data
                            val docId = change.document.id
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                dao.permanentlyDeleteTip(docId)
                            } else {
                                val tip = mapToTip(data, docId)
                                dao.insertTip(tip)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping synced tip: ${e.message}")
                        }
                    }
                }
            }
        }

        // 4. Listen to alerts collection
        val alertsListener = db.collection("alerts").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Alerts snapshot listener error: ${error.message}")
                return@addSnapshotListener
            }
            snapshot?.let {
                if (it.metadata.hasPendingWrites()) return@addSnapshotListener
                managerScope.launch {
                    val isInitialLoad = it.metadata.hasPendingWrites() || it.metadata.isFromCache
                    for (change in it.documentChanges) {
                        try {
                            val data = change.document.data
                            val docId = change.document.id
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                dao.deleteAlert(docId)
                            } else {
                                val alert = mapToAlert(data, docId)
                                dao.insertAlert(alert)
                                
                                // Only show notification for new alerts added to the cloud (not on initial cold start sync)
                                if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED && !it.metadata.isFromCache) {
                                    val isSOS = alert.title.contains("SOS", ignoreCase = true)
                                    com.example.util.NotificationHelper.showNotification(
                                        context,
                                        title = if (isSOS) "🚨 EMERGENCY: ${alert.title}" else "URGENT: ${alert.title}",
                                        content = if (isSOS) "${alert.content}\nLocation: ${alert.locationName} (${alert.latitude}, ${alert.longitude})" else alert.content,
                                        latitude = if (isSOS) alert.latitude else null,
                                        longitude = if (isSOS) alert.longitude else null
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping synced alert: ${e.message}")
                        }
                    }
                }
            }
        }

        // 5. Listen to wanted_criminals collection
        val criminalsListener = db.collection("wanted_criminals").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Criminals snapshot listener error: ${error.message}")
                return@addSnapshotListener
            }
            snapshot?.let {
                if (it.metadata.hasPendingWrites()) return@addSnapshotListener
                managerScope.launch {
                    for (change in it.documentChanges) {
                        try {
                            val data = change.document.data
                            val docId = change.document.id
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                dao.permanentlyDeleteWantedCriminal(docId)
                            } else {
                                Log.d(TAG, "📥 [Sync] Received wanted criminal change for $docId from Firestore")
                                val criminal = mapToWantedCriminal(data, docId)
                                Log.d(TAG, "📥 [Sync] Mapped criminal $docId: imageUrls=${criminal.imageUrls}")
                                dao.insertWantedCriminal(criminal)
                                
                                // Only show notification for new wanted criminals
                                if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED && !it.metadata.isFromCache) {
                                    com.example.util.NotificationHelper.showNotification(
                                        context,
                                        "WANTED: ${criminal.name}",
                                        "Last seen: ${criminal.lastSeen}. Info: ${criminal.description}"
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping synced wanted criminal: ${e.message}")
                        }
                    }
                }
            }
        }

        // 6. Listen to audit_logs collection
        val auditLogsListener = db.collection("audit_logs").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Audit logs snapshot listener error: ${error.message}")
                return@addSnapshotListener
            }
            snapshot?.let {
                if (it.metadata.hasPendingWrites()) return@addSnapshotListener
                managerScope.launch {
                    for (change in it.documentChanges) {
                        try {
                            val data = change.document.data
                            val docId = change.document.id
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                dao.permanentlyDeleteAuditLog(docId)
                            } else {
                                val log = mapToAuditLog(data, docId)
                                dao.insertAuditLog(log)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping synced audit log: ${e.message}")
                        }
                    }
                }
            }
        }

        // 7. Listen to help_messages collection
        val helpMessagesListener = db.collection("help_messages").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Help messages snapshot listener error: ${error.message}")
                return@addSnapshotListener
            }
            snapshot?.let {
                if (it.metadata.hasPendingWrites()) return@addSnapshotListener
                managerScope.launch {
                    for (change in it.documentChanges) {
                        try {
                            val data = change.document.data
                            val docId = change.document.id
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                dao.deleteHelpMessage(docId)
                            } else {
                                val msg = mapToHelpMessage(data, docId)
                                dao.insertHelpMessage(msg)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping synced help message: ${e.message}")
                        }
                    }
                }
            }
        }

        synchronized(activeRegistrations) {
            activeRegistrations.add(usersListener)
            activeRegistrations.add(reportsListener)
            activeRegistrations.add(tipsListener)
            activeRegistrations.add(alertsListener)
            activeRegistrations.add(criminalsListener)
            activeRegistrations.add(auditLogsListener)
            activeRegistrations.add(helpMessagesListener)
        }
    }
}
