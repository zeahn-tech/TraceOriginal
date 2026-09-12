package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.SessionManager
import com.example.data.api.*
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.example.data.model.*
import com.example.data.repository.TraceNetRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

class MainViewModel(private val repository: TraceNetRepository) : ViewModel() {
    private val auth: FirebaseAuth? by lazy {
        try {
            val context = com.example.TraceNetApplication.instance
            val apps = com.google.firebase.FirebaseApp.getApps(context)
            val app = if (apps.isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context)
            } else {
                apps[0]
            }
            if (app != null) {
                FirebaseAuth.getInstance(app)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private val storageManager = com.example.data.firebase.FirebaseStorageManager()
    
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            SessionManager.logout()
            repository.stopRealtimeSync()
        } else {
            viewModelScope.launch {
                // Try to fetch existing user profile from Room
                val localUser = repository.getUser(firebaseUser.uid).first()
                if (localUser != null) {
                    SessionManager.login(localUser)
                } else {
                    // Fallback to Firestore if not in Room
                    repository.fetchUserFromCloud(firebaseUser.uid)?.let { cloudUser ->
                        SessionManager.login(cloudUser)
                    }
                }
                repository.startRealtimeSync()
            }
        }
    }

    init {
        auth?.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth?.removeAuthStateListener(authStateListener)
    }
    
    suspend fun uploadImage(context: android.content.Context, uri: android.net.Uri, folder: String): String? {
        android.util.Log.d("MainViewModel", "uploadImage: attempting upload to Firebase Storage for $uri")
        return storageManager.uploadImage(context, uri, folder)
    }

    suspend fun uploadImage(uri: android.net.Uri, folder: String): String? {
        return uploadImage(com.example.TraceNetApplication.instance, uri, folder)
    }

    suspend fun uploadVideo(context: android.content.Context, uri: android.net.Uri, folder: String): String? {
        android.util.Log.d("MainViewModel", "uploadVideo: attempting upload to Firebase Storage for $uri")
        return storageManager.uploadVideo(context, uri, folder)
    }

    suspend fun uploadVideo(uri: android.net.Uri, folder: String): String? {
        return uploadVideo(com.example.TraceNetApplication.instance, uri, folder)
    }

    suspend fun uploadAudio(context: android.content.Context, uri: android.net.Uri, folder: String): String? {
        android.util.Log.d("MainViewModel", "uploadAudio: attempting upload to Firebase Storage for $uri")
        return storageManager.uploadAudio(context, uri, folder)
    }

    suspend fun uploadAudio(uri: android.net.Uri, folder: String): String? {
        return uploadAudio(com.example.TraceNetApplication.instance, uri, folder)
    }

    suspend fun uploadIdCard(context: android.content.Context, uri: android.net.Uri, userId: String): String? {
        android.util.Log.d("MainViewModel", "uploadIdCard: attempting upload to Firebase Storage for $uri")
        return storageManager.uploadIdCard(context, uri, userId)
    }

    suspend fun uploadIdCard(uri: android.net.Uri, userId: String): String? {
        return uploadIdCard(com.example.TraceNetApplication.instance, uri, userId)
    }
    
    val currentUser = SessionManager.currentUser
    val allReports = repository.allReports.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeReports = repository.activeReports.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val deletedReports = repository.deletedReports.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlerts = repository.allAlerts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingUsers = repository.pendingLawEnforcers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allWantedCriminals = repository.wantedCriminals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val wantedCriminals = repository.verifiedWantedCriminals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeWantedCriminals = repository.activeWantedCriminals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingWantedCriminals = repository.pendingWantedCriminals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val deletedWantedCriminals = repository.deletedWantedCriminals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTips = repository.allTips.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val deletedTips = repository.deletedTips.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allUsers = repository.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAuditLogs = repository.allAuditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val deletedAuditLogs = repository.deletedAuditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allHelpMessages = repository.allHelpMessages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _aiSensitivity = MutableStateFlow(0.7f)
    val aiSensitivity = _aiSensitivity.asStateFlow()

    private val _enabledCategories = MutableStateFlow<Set<com.example.data.model.ReportType>>(com.example.data.model.ReportType.entries.toSet())
    val enabledCategories = _enabledCategories.asStateFlow()

    private val _isTwoFactorEnabled = MutableStateFlow(SessionManager.getGlobalTwoFactorEnabled())
    val isTwoFactorEnabled = _isTwoFactorEnabled.asStateFlow()

    private val _passwordExpiry = MutableStateFlow(SessionManager.getGlobalPasswordExpiry())
    val passwordExpiry = _passwordExpiry.asStateFlow()

    fun updateSecuritySettings(twoFactorEnabled: Boolean, expiryDays: Int) {
        _isTwoFactorEnabled.value = twoFactorEnabled
        _passwordExpiry.value = expiryDays
        SessionManager.setGlobalTwoFactorEnabled(twoFactorEnabled)
        SessionManager.setGlobalPasswordExpiry(expiryDays)
        logAction("SECURITY_SETTINGS_UPDATED", "Updated password policy (expiry: ${expiryDays} days) and 2FA ($twoFactorEnabled)")
    }

    fun updateEnabledCategories(categories: Set<com.example.data.model.ReportType>) {
        _enabledCategories.value = categories
        logAction("CATEGORIES_CONFIG_UPDATED", "Updated enabled tip/report categories. Total active: ${categories.size}")
    }

    fun updateAiSensitivity(sensitivity: Float) {
        _aiSensitivity.value = sensitivity
        logAction("AI_CONFIG_UPDATED", "Adjusted screening sensitivity to ${(sensitivity * 100).toInt()}%")
    }

    val userTips = currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getTipsForUser(user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val fbAuth = auth
        if (fbAuth != null) {
            fbAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        onError(task.exception?.message ?: "Failed to send reset email")
                    }
                }
        } else {
            onError("Authentication service is unavailable.")
        }
    }

    fun logAction(action: String, description: String) {
        viewModelScope.launch {
            val user = SessionManager.currentUser.first()
            if (user != null) {
                repository.logAction(
                    AuditLog(
                        action = action,
                        description = description,
                        userId = user.id,
                        userName = user.name
                    )
                )
            }
        }
    }

    fun updateUserStatus(userId: String, status: String) {
        viewModelScope.launch {
            val userName = allUsers.value.find { it.id == userId }?.name ?: userId
            repository.updateUserStatus(userId, status)
            logAction("USER_STATUS_UPDATE", "Updated user $userName status to $status")
        }
    }

    fun signUp(
        user: User, 
        password: String, 
        idCardUri: android.net.Uri?,
        onProgress: (String) -> Unit,
        onResult: (Boolean, String?, User?) -> Unit
    ) {
        // SECURITY FIX: Remove hardcoded admin backdoor. 
        // All signups default to CITIZEN or their selected role, subject to approval if non-citizen.
        val finalUser = if (user.role == UserRole.ADMIN) {
             user.copy(role = UserRole.CITIZEN, isApproved = true)
        } else {
             user.copy(isApproved = user.role == UserRole.CITIZEN)
        }

        val fbAuth = auth
        if (fbAuth == null) {
            onResult(false, "Authentication service is unavailable.", null)
            return
        }

        fbAuth.createUserWithEmailAndPassword(finalUser.email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = fbAuth.currentUser
                    if (firebaseUser != null) {
                        viewModelScope.launch {
                            try {
                                var uploadedIdCardUrl = finalUser.idCardUrl
                                if (idCardUri != null) {
                                    onProgress("Uploading ID verification...")
                                    uploadedIdCardUrl = uploadIdCard(idCardUri, firebaseUser.uid)
                                }
                                
                                val userWithId = finalUser.copy(
                                    id = firebaseUser.uid,
                                    idCardUrl = uploadedIdCardUrl
                                )
                                
                                onProgress("Finalizing registration...")
                                repository.saveUser(userWithId)
                                
                                // Always login after signup for citizen screen access
                                SessionManager.login(userWithId)
                                onResult(true, null, userWithId)
                            } catch (e: Exception) {
                                onResult(false, "Profile setup failed: ${e.message}", null)
                            }
                        }
                    } else {
                        onResult(false, "Registration successful but user not found", null)
                    }
                } else {
                    onResult(false, task.exception?.message ?: "Registration failed", null)
                }
            }
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String?, User?) -> Unit) {
        val fbAuth = auth
        if (fbAuth == null) {
            onResult(false, "Authentication service is unavailable.", null)
            return
        }
        fbAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = fbAuth.currentUser
                    if (firebaseUser != null) {
                        viewModelScope.launch {
                            // 1. Try local cache first
                            var user = repository.getUser(firebaseUser.uid).first()
                            
                            // 2. If not in local, try cloud
                            if (user == null) {
                                user = repository.fetchUserFromCloud(firebaseUser.uid)
                            }
                            
                            if (user != null) {
                                login(user)
                                onResult(true, null, user)
                            } else {
                                onResult(false, "User profile not found. Please contact support.", null)
                            }
                        }
                    } else {
                        onResult(false, "Authentication successful but user session is invalid", null)
                    }
                } else {
                    onResult(false, task.exception?.message ?: "Invalid email or password", null)
                }
            }
    }

    fun signInWithGoogle(
        idToken: String, 
        selectedRole: UserRole? = null,
        badgeNumber: String? = null,
        contact: String? = null,
        address: String? = null,
        idCardUri: android.net.Uri? = null,
        onResult: (Boolean, String?, User?) -> Unit
    ) {
        val fbAuth = auth
        if (fbAuth == null) {
            onResult(false, "Authentication service is unavailable.", null)
            return
        }
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        fbAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = fbAuth.currentUser
                    if (firebaseUser != null) {
                        viewModelScope.launch {
                            // 1. Try local
                            var existingUser = repository.getUser(firebaseUser.uid).first()
                            
                            // 2. Try cloud fallback
                            if (existingUser == null) {
                                existingUser = repository.fetchUserFromCloud(firebaseUser.uid)
                            }
                            
                            if (existingUser == null) {
                                try {
                                    var idCardUrl: String? = null
                                    if (idCardUri != null) {
                                        idCardUrl = uploadIdCard(idCardUri, firebaseUser.uid)
                                    }

                                    // SECURITY FIX: Remove hardcoded admin backdoor.
                                    val userRole = if (selectedRole == UserRole.ADMIN) UserRole.CITIZEN else (selectedRole ?: UserRole.CITIZEN)
                                    val approved = userRole == UserRole.CITIZEN

                                    val user = User(
                                        id = firebaseUser.uid,
                                        name = firebaseUser.displayName ?: "Google User",
                                        email = firebaseUser.email ?: "",
                                        role = userRole,
                                        isApproved = approved,
                                        badgeNumber = badgeNumber,
                                        contact = contact,
                                        address = address,
                                        idCardUrl = idCardUrl,
                                        status = "ACTIVE"
                                    )
                                    repository.saveUser(user)
                                    // Always login to allow access to citizen screen
                                    SessionManager.login(user)
                                    onResult(true, null, user)
                                } catch (e: Exception) {
                                    onResult(false, "Profile setup failed: ${e.message}", null)
                                }
                            } else {
                                SessionManager.login(existingUser)
                                onResult(true, null, existingUser)
                            }
                        }
                    } else {
                        onResult(false, "User not found after sign in", null)
                    }
                } else {
                    onResult(false, task.exception?.message ?: "Authentication failed", null)
                }
            }
    }

    fun updatePassword(currentPassword: String, newPassword: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth?.currentUser
        val email = user?.email
        if (user != null && email != null) {
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            SessionManager.setPasswordLastChanged(user.uid, System.currentTimeMillis())
                            onResult(true, "Password updated successfully")
                        } else {
                            onResult(false, updateTask.exception?.message ?: "Failed to update password")
                        }
                    }
                } else {
                    onResult(false, "Authentication failed. Incorrect current password.")
                }
            }
        } else {
            onResult(false, "No authenticated user or authentication service unavailable")
        }
    }

    fun getPasswordLastChanged(userId: String): Long {
        return SessionManager.getPasswordLastChanged(userId)
    }

    fun setPasswordLastChanged(userId: String, timestamp: Long) {
        SessionManager.setPasswordLastChanged(userId, timestamp)
    }

    fun updateUserRole(userId: String, newRole: UserRole, isApproved: Boolean) {
        viewModelScope.launch {
            val userName = allUsers.value.find { it.id == userId }?.name ?: userId
            repository.updateUserRole(userId, newRole, isApproved)
            logAction("USER_ROLE_UPDATED", "Updated user $userName to $newRole (Approved: $isApproved)")
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            val userName = allUsers.value.find { it.id == userId }?.name ?: userId
            repository.deleteUser(userId)
            logAction("USER_DELETED", "Deleted user $userName")
        }
    }

    fun signOut() {
        auth?.signOut()
        SessionManager.logout()
    }

    fun login(user: User) {
        if (user.status == "SUSPENDED" || user.status == "BANNED") {
            // SECURITY FIX: Block login for SUSPENDED users as well as BANNED
            return
        }
        SessionManager.login(user)
        viewModelScope.launch {
            repository.saveUser(user)
        }
    }

    fun register(user: User) {
        viewModelScope.launch {
            repository.saveUser(user)
        }
    }

    suspend fun getExistingUser(email: String): User? {
        return repository.getUserByEmail(email).first() ?: repository.fetchUserByEmailFromCloud(email)
    }

    fun postCriminalReport(
        context: android.content.Context,
        name: String,
        description: String,
        lastSeen: String,
        reward: String?,
        category: String,
        county: String? = null,
        imageUris: List<android.net.Uri>,
        onProgress: (String) -> Unit,
        onResult: (Boolean) -> Unit
    ) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                onProgress("Processing and saving images...")
                val imageUrls = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    coroutineScope {
                        imageUris.map { uri ->
                            async {
                                try {
                                    val localSavedPath = try {
                                        com.example.util.MediaStorageHelper.saveMediaToInternalStorage(appContext, uri)
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainViewModel", "Error saving criminal image locally: ${e.message}")
                                        null
                                    }
                                    val localSavedUri = if (localSavedPath != null) android.net.Uri.parse(localSavedPath) else uri
                                    val cloudUrl = try {
                                        uploadImage(appContext, localSavedUri, "criminals")
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainViewModel", "Error uploading criminal image to cloud: ${e.message}")
                                        null
                                    }
                                    cloudUrl ?: throw IllegalStateException("Image upload failed: $uri")
                                } catch (e: Exception) {
                                    android.util.Log.e("MainViewModel", "Error processing criminal image URI $uri: ${e.message}")
                                    throw IllegalStateException("Unable to prepare criminal image: $uri", e)
                                }
                            }
                        }.awaitAll().filter { it.isNotBlank() }
                    }
                }
                
                onProgress("Broadcasting alert...")
                val criminal = WantedCriminal(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    lastSeen = lastSeen,
                    county = county,
                    reward = reward,
                    imageUrls = imageUrls,
                    category = category,
                    status = "SUBMITTED",
                    isVerified = false
                )
                repository.postWantedCriminal(criminal)
                logAction("CRIMINAL_POSTED", "Posted wanted criminal record for $name")
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun updateWantedCriminal(id: String, name: String, description: String, lastSeen: String, reward: String?) {
        viewModelScope.launch {
            repository.updateWantedCriminal(id, name, description, lastSeen, reward)
            logAction("CRIMINAL_UPDATED", "Updated record for wanted criminal: $name")
        }
    }

    fun softDeleteWantedCriminal(id: String) {
        viewModelScope.launch {
            val criminalName = allWantedCriminals.value.find { it.id == id }?.name ?: id
            repository.softDeleteWantedCriminal(id)
            logAction("CRIMINAL_SOFT_DELETE", "Moved wanted criminal record '$criminalName' to trash")
        }
    }

    fun verifyWantedCriminal(id: String, isVerified: Boolean) {
        viewModelScope.launch {
            val criminalName = allWantedCriminals.value.find { it.id == id }?.name ?: id
            val status = if (isVerified) "VERIFIED" else "DISMISSED"
            repository.updateWantedCriminalVerification(id, isVerified, status)
            logAction("CRIMINAL_VERIFICATION", "Verified wanted criminal $criminalName as $status")
        }
    }

    fun updateWantedCriminalStatus(id: String, status: String) {
        viewModelScope.launch {
            val criminalName = allWantedCriminals.value.find { it.id == id }?.name ?: id
            repository.updateWantedCriminalStatus(id, status)
            logAction("CRIMINAL_STATUS_UPDATE", "Updated criminal $criminalName status to $status")
        }
    }

    fun restoreWantedCriminal(id: String) {
        viewModelScope.launch {
            val criminalName = deletedWantedCriminals.value.find { it.id == id }?.name ?: id
            repository.restoreWantedCriminal(id)
            logAction("CRIMINAL_RESTORE", "Restored wanted criminal record '$criminalName' from trash")
        }
    }

    fun permanentlyDeleteWantedCriminal(id: String) {
        viewModelScope.launch {
            val criminalName = deletedWantedCriminals.value.find { it.id == id }?.name ?: id
            repository.permanentlyDeleteWantedCriminal(id)
            logAction("CRIMINAL_PERMANENT_DELETE", "Permanently deleted wanted criminal record '$criminalName'")
        }
    }

    fun submitReport(
        context: android.content.Context,
        title: String,
        description: String,
        type: ReportType,
        county: String?,
        latitude: Double,
        longitude: Double,
        reporterId: String,
        isAnonymous: Boolean,
        contactInfo: String?,
        imageUris: List<android.net.Uri>,
        videoUris: List<android.net.Uri>,
        audioUris: List<android.net.Uri>,
        incidentTimestamp: Long,
        onProgress: (String) -> Unit,
        onResult: (Boolean) -> Unit
    ) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                android.util.Log.d("MainViewModel", "Starting report submission with ${imageUris.size} images, ${videoUris.size} videos, ${audioUris.size} audio")
                onProgress("Processing and saving images...")
                val imageUrls = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    coroutineScope {
                        imageUris.map { uri ->
                            async {
                                try {
                                    val localSavedPath = try {
                                        com.example.util.MediaStorageHelper.saveMediaToInternalStorage(appContext, uri)
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainViewModel", "Error saving report image locally: ${e.message}")
                                        null
                                    }
                                    val localSavedUri = if (localSavedPath != null) android.net.Uri.parse(localSavedPath) else uri
                                    val cloudUrl = try {
                                        uploadImage(appContext, localSavedUri, "reports/images")
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainViewModel", "Error uploading report image to cloud: ${e.message}")
                                        null
                                    }
                                    cloudUrl ?: throw IllegalStateException("Image upload failed: $uri")
                                } catch (e: Exception) {
                                    android.util.Log.e("MainViewModel", "Error processing report image URI $uri: ${e.message}")
                                    throw IllegalStateException("Unable to prepare report image: $uri", e)
                                }
                            }
                        }.awaitAll().filter { it.isNotBlank() }
                    }
                }
                
                onProgress("Processing and saving videos...")
                val videoUrls = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    coroutineScope {
                        videoUris.map { uri ->
                            async {
                                try {
                                    val localSavedPath = try {
                                        com.example.util.MediaStorageHelper.saveMediaToInternalStorage(appContext, uri)
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainViewModel", "Error saving report video locally: ${e.message}")
                                        null
                                    }
                                    val localSavedUri = if (localSavedPath != null) android.net.Uri.parse(localSavedPath) else uri
                                    val cloudUrl = try {
                                        uploadVideo(appContext, localSavedUri, "reports/videos")
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainViewModel", "Error uploading report video to cloud: ${e.message}")
                                        null
                                    }
                                    cloudUrl ?: throw IllegalStateException("Video upload failed: $uri")
                                } catch (e: Exception) {
                                    android.util.Log.e("MainViewModel", "Error processing report video URI $uri: ${e.message}")
                                    throw IllegalStateException("Unable to prepare report video: $uri", e)
                                }
                            }
                        }.awaitAll().filter { it.isNotBlank() }
                    }
                }
                
                onProgress("Processing and saving audio...")
                val audioUrls = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    coroutineScope {
                        audioUris.map { uri ->
                            async {
                                try {
                                    val localSavedPath = try {
                                        com.example.util.MediaStorageHelper.saveMediaToInternalStorage(appContext, uri)
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainViewModel", "Error saving report audio locally: ${e.message}")
                                        null
                                    }
                                    val localSavedUri = if (localSavedPath != null) android.net.Uri.parse(localSavedPath) else uri
                                    val cloudUrl = try {
                                        uploadAudio(appContext, localSavedUri, "reports/audio")
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainViewModel", "Error uploading report audio to cloud: ${e.message}")
                                        null
                                    }
                                    cloudUrl ?: throw IllegalStateException("Audio upload failed: $uri")
                                } catch (e: Exception) {
                                    android.util.Log.e("MainViewModel", "Error processing report audio URI $uri: ${e.message}")
                                    throw IllegalStateException("Unable to prepare report audio: $uri", e)
                                }
                            }
                        }.awaitAll().filter { it.isNotBlank() }
                    }
                }
                
                android.util.Log.d("MainViewModel", "Media urls collected - Images: $imageUrls, Videos: $videoUrls, Audio: $audioUrls")
                
                onProgress("Saving report...")
                val report = Report(
                    title = title,
                    description = description,
                    type = type,
                    county = county,
                    latitude = latitude,
                    longitude = longitude,
                    reporterId = reporterId,
                    isAnonymous = isAnonymous,
                    contactInfo = contactInfo,
                    imageUrls = imageUrls,
                    videoUrls = videoUrls,
                    audioUrls = audioUrls,
                    incidentTimestamp = incidentTimestamp
                )
                repository.submitReport(report)
                screenReport(report)
                logAction("REPORT_SUBMISSION", "Submitted a ${type.name} report: '${report.title}'")
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun submitReport(report: Report) {
        viewModelScope.launch {
            repository.submitReport(report)
            screenReport(report)
            logAction("REPORT_SUBMISSION", "Submitted a ${report.type.name} report: '${report.title}'")
        }
    }

    private fun screenReport(report: Report) {
        viewModelScope.launch(Dispatchers.IO) {
            val prompt = """
                Analyze the following incident report for potential issues:
                Title: ${report.title}
                Description: ${report.description}
                Type: ${report.type}
                
                Identify if the report contains:
                1. Insults or abusive language.
                2. Direct or indirect threats.
                3. Hate speech.
                4. Obvious revenge language or malicious intent.
                5. Suspicious patterns or nonsense text.
                
                Provide your response in JSON format:
                {
                  "status": "CLEAN" or "FLAGGED",
                  "reasons": ["list", "of", "reasons", "if", "flagged"]
                }
            """.trimIndent()

            try {
                val responseText = GeminiClient.generateContent(prompt)
                
                if (responseText != null) {
                    val jsonString = if (responseText.contains("```json")) {
                        responseText.substringAfter("```json").substringBefore("```").trim()
                    } else if (responseText.contains("```")) {
                        responseText.substringAfter("```").substringBefore("```").trim()
                    } else {
                        responseText.trim()
                    }

                    val json = Json.parseToJsonElement(jsonString).jsonObject
                    val status = json["status"]?.jsonPrimitive?.content ?: "NOT_SCREENED"
                    val reasons = json["reasons"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    
                    val updatedReport = report.copy(
                        aiScreeningStatus = status,
                        aiFlaggedReasons = reasons,
                        status = if (status == "FLAGGED") "PENDING_REVIEW" else report.status
                    )
                    repository.updateReport(updatedReport)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val (status, reasons) = localScreenContent("${report.title} ${report.description}")
                val updatedReport = report.copy(
                    aiScreeningStatus = status,
                    aiFlaggedReasons = reasons,
                    status = if (status == "FLAGGED") "PENDING_REVIEW" else report.status
                )
                repository.updateReport(updatedReport)
            }
        }
    }

    fun updateReport(reportId: String, title: String, description: String) {
        viewModelScope.launch {
            repository.updateReport(reportId, title, description)
        }
    }

    fun softDeleteReport(reportId: String) {
        viewModelScope.launch {
            val reportTitle = allReports.value.find { it.id == reportId }?.title ?: reportId
            repository.softDeleteReport(reportId)
            logAction("REPORT_SOFT_DELETE", "Moved report '$reportTitle' to trash")
        }
    }

    fun restoreReport(reportId: String) {
        viewModelScope.launch {
            val reportTitle = deletedReports.value.find { it.id == reportId }?.title ?: reportId
            repository.restoreReport(reportId)
            logAction("REPORT_RESTORE", "Restored report '$reportTitle' from trash")
        }
    }

    fun permanentlyDeleteReport(reportId: String) {
        viewModelScope.launch {
            val reportTitle = deletedReports.value.find { it.id == reportId }?.title ?: reportId
            repository.permanentlyDeleteReport(reportId)
            logAction("REPORT_PERMANENT_DELETE", "Permanently deleted report '$reportTitle'")
        }
    }

    fun submitTip(tip: Tip) {
        viewModelScope.launch {
            repository.submitTip(tip)
            screenTip(tip)
            val tipPreview = tip.content.take(30)
            logAction("TIP_SUBMISSION", "Submitted a new tip: \"$tipPreview...\"")
        }
    }

    fun softDeleteTip(tipId: String) {
        viewModelScope.launch {
            val tipContent = allTips.value.find { it.id == tipId }?.content?.take(30) ?: tipId
            repository.softDeleteTip(tipId)
            logAction("TIP_SOFT_DELETE", "Moved tip \"$tipContent...\" to trash")
        }
    }

    fun restoreTip(tipId: String) {
        viewModelScope.launch {
            val tipContent = deletedTips.value.find { it.id == tipId }?.content?.take(30) ?: tipId
            repository.restoreTip(tipId)
            logAction("TIP_RESTORE", "Restored tip \"$tipContent...\" from trash")
        }
    }

    fun permanentlyDeleteTip(tipId: String) {
        viewModelScope.launch {
            val tipContent = deletedTips.value.find { it.id == tipId }?.content?.take(30) ?: tipId
            repository.permanentlyDeleteTip(tipId)
            logAction("TIP_PERMANENT_DELETE", "Permanently deleted tip \"$tipContent...\"")
        }
    }

    fun softDeleteMultipleTips(tipIds: List<String>) {
        viewModelScope.launch {
            tipIds.forEach { repository.softDeleteTip(it) }
            logAction("MULTI_TIP_SOFT_DELETE", "Moved ${tipIds.size} tips to trash")
        }
    }

    fun permanentlyDeleteMultipleTips(tipIds: List<String>) {
        viewModelScope.launch {
            tipIds.forEach { repository.permanentlyDeleteTip(it) }
            logAction("MULTI_TIP_PERMANENT_DELETE", "Permanently deleted ${tipIds.size} tips")
        }
    }

    private fun screenTip(tip: Tip) {
        viewModelScope.launch(Dispatchers.IO) {
            val prompt = """
                Analyze the following tip submitted for an incident report for potential issues:
                Tip Content: ${tip.content}
                
                Identify if the tip contains:
                1. Insults or abusive language.
                2. Direct or indirect threats.
                3. Hate speech.
                4. Obvious revenge language or malicious intent.
                5. Suspicious patterns or nonsense text.
                
                Provide your response in JSON format:
                {
                  "status": "CLEAN" or "FLAGGED",
                  "reasons": ["list", "of", "reasons", "if", "flagged"]
                }
            """.trimIndent()

            try {
                val responseText = GeminiClient.generateContent(prompt)
                
                if (responseText != null) {
                    val jsonString = if (responseText.contains("```json")) {
                        responseText.substringAfter("```json").substringBefore("```").trim()
                    } else if (responseText.contains("```")) {
                        responseText.substringAfter("```").substringBefore("```").trim()
                    } else {
                        responseText.trim()
                    }

                    val json = Json.parseToJsonElement(jsonString).jsonObject
                    val status = json["status"]?.jsonPrimitive?.content ?: "NOT_SCREENED"
                    val reasons = json["reasons"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    
                    val updatedTip = tip.copy(
                        aiScreeningStatus = status,
                        aiFlaggedReasons = reasons,
                        isReviewed = if (status == "FLAGGED") false else tip.isReviewed
                    )
                    repository.submitTip(updatedTip)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val (status, reasons) = localScreenContent(tip.content)
                val updatedTip = tip.copy(
                    aiScreeningStatus = status,
                    aiFlaggedReasons = reasons,
                    isReviewed = if (status == "FLAGGED") false else tip.isReviewed
                )
                repository.submitTip(updatedTip)
            }
        }
    }

    fun markTipAsReviewed(tipId: String) {
        viewModelScope.launch {
            val tipContent = allTips.value.find { it.id == tipId }?.content?.take(30) ?: tipId
            repository.markTipAsReviewed(tipId)
            logAction("TIP_REVIEWED", "Marked tip \"$tipContent...\" as reviewed")
        }
    }

    fun submitAlert(alert: Alert) {
        viewModelScope.launch {
            repository.submitAlert(alert)
            logAction("ALERT_SUBMITTED", "Submitted a new ${alert.urgency} alert: '${alert.title}'")
        }
    }

    fun deleteAlert(alertId: String) {
        viewModelScope.launch {
            val alertTitle = allAlerts.value.find { it.id == alertId }?.title ?: alertId
            repository.deleteAlert(alertId)
            logAction("ALERT_DELETED", "Deleted alert: '$alertTitle'")
        }
    }

    fun updateAlert(id: String, title: String, content: String, urgency: Int, locationName: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repository.updateAlert(id, title, content, urgency, locationName, latitude, longitude)
            logAction("ALERT_UPDATED", "Updated alert: '$title'")
        }
    }

    fun approveLawEnforcer(userId: String) {
        viewModelScope.launch {
            val userName = allUsers.value.find { it.id == userId }?.name ?: userId
            repository.approveLawEnforcer(userId)
            logAction("LAW_ENFORCER_APPROVED", "Approved law enforcer account: $userName")
        }
    }

    fun rejectLawEnforcer(userId: String) {
        viewModelScope.launch {
            val userName = allUsers.value.find { it.id == userId }?.name ?: userId
            repository.rejectLawEnforcer(userId)
            logAction("LAW_ENFORCER_REJECTED", "Rejected law enforcer account: $userName")
        }
    }

    fun updateReportStatus(reportId: String, status: String) {
        viewModelScope.launch {
            val report = repository.allReports.first().find { it.id == reportId }
            if (report != null) {
                val reportTitle = report.title
                repository.updateReportStatus(reportId, status)
                logAction("REPORT_STATUS_UPDATE", "Updated report '$reportTitle' status to $status")

                // Reputation Scoring Logic
                val reporterId = report.reporterId
                val reporter = repository.allUsers.first().find { it.id == reporterId }
                if (reporter != null) {
                    val scoreChange = when (status.uppercase()) {
                        "VERIFIED" -> 10
                        "FALSE_REPORT" -> -30
                        "DISMISSED" -> -5
                        else -> 0
                    }
                    
                    if (scoreChange != 0) {
                        val newScore = (reporter.reputationScore + scoreChange).coerceIn(0, 200)
                        val newStatus = if (newScore < 30) "SUSPENDED" else reporter.status
                        
                        repository.updateUser(reporter.copy(
                            reputationScore = newScore,
                            status = newStatus
                        ))
                        
                        logAction(
                            "REPUTATION_UPDATE", 
                            "Reporter ${reporter.name} score changed by $scoreChange to $newScore. Status: $newStatus"
                        )
                    }
                }
            }
        }
    }

    fun mergeReports(primaryReportId: String, duplicateReportId: String) {
        viewModelScope.launch {
            val primaryTitle = allReports.value.find { it.id == primaryReportId }?.title ?: primaryReportId
            val duplicateTitle = allReports.value.find { it.id == duplicateReportId }?.title ?: duplicateReportId
            // In a real app, this would involve complex logic to combine data.
            // For now, we'll just soft delete the duplicate and log it.
            repository.softDeleteReport(duplicateReportId)
            logAction("REPORTS_MERGED", "Merged report '$duplicateTitle' into '$primaryTitle'")
        }
    }

    fun assignInvestigator(reportId: String, investigatorName: String) {
        viewModelScope.launch {
            val reportTitle = allReports.value.find { it.id == reportId }?.title ?: reportId
            val report = repository.allReports.first().find { it.id == reportId }
            if (report != null) {
                repository.updateReport(report.copy(assignedInvestigator = investigatorName))
                logAction("INVESTIGATOR_ASSIGNED", "Assigned $investigatorName to report '$reportTitle'")
            }
        }
    }

    fun updateInternalNotes(reportId: String, notes: String) {
        viewModelScope.launch {
            val reportTitle = allReports.value.find { it.id == reportId }?.title ?: reportId
            val report = repository.allReports.first().find { it.id == reportId }
            if (report != null) {
                repository.updateReport(report.copy(internalNotes = notes))
                logAction("INTERNAL_NOTES_UPDATED", "Updated internal notes for report '$reportTitle'")
            }
        }
    }

    fun escalateReport(reportId: String, escalated: Boolean) {
        viewModelScope.launch {
            val reportTitle = allReports.value.find { it.id == reportId }?.title ?: reportId
            val report = repository.allReports.first().find { it.id == reportId }
            if (report != null) {
                repository.updateReport(report.copy(isEscalated = escalated))
                logAction(
                    if (escalated) "REPORT_ESCALATED" else "REPORT_DE_ESCALATED",
                    "${if (escalated) "Escalated" else "De-escalated"} report '$reportTitle'"
                )
            }
        }
    }

    suspend fun uploadImageBytes(bytes: ByteArray, folder: String): String? {
        return storageManager.uploadImageBytes(bytes, folder)
    }

    fun updateProfile(user: User) {
        viewModelScope.launch {
            repository.updateUser(user)
            SessionManager.login(user) // Refresh session with updated user data
            logAction("PROFILE_UPDATED", "User updated their profile information")
        }
    }

    fun deleteAccount(userId: String) {
        viewModelScope.launch {
            val userName = SessionManager.currentUser.first()?.name ?: userId
            repository.deleteUser(userId)
            logAction("ACCOUNT_DELETED", "User $userName deleted their own account")
            SessionManager.logout()
        }
    }

    fun softDeleteAuditLog(logId: String) {
        viewModelScope.launch {
            repository.softDeleteAuditLog(logId)
        }
    }

    fun restoreAuditLog(logId: String) {
        viewModelScope.launch {
            repository.restoreAuditLog(logId)
        }
    }

    fun permanentlyDeleteAuditLog(logId: String) {
        viewModelScope.launch {
            repository.permanentlyDeleteAuditLog(logId)
        }
    }

    fun softDeleteMultipleAuditLogs(logIds: List<String>) {
        viewModelScope.launch {
            logIds.forEach { repository.softDeleteAuditLog(it) }
        }
    }

    fun permanentlyDeleteMultipleAuditLogs(logIds: List<String>) {
        viewModelScope.launch {
            logIds.forEach { repository.permanentlyDeleteAuditLog(it) }
        }
    }

    private fun localScreenContent(text: String): Pair<String, List<String>> {
        val lowercaseText = text.lowercase()
        val flaggedReasons = mutableListOf<String>()

        val abusiveWords = listOf("fuck", "shit", "bitch", "asshole", "bastard", "idiot", "stupid", "dumb", "jerk", "crap", "cunt", "dick", "abuse", "abusive", "harass")
        val threatWords = listOf("kill", "die", "shoot", "attack", "bomb", "murder", "harm", "destroy", "burn", "assassinate", "stab", "threat")
        val hateSpeechWords = listOf("slur", "hate", "scum", "trash")

        val hasAbusive = abusiveWords.any { lowercaseText.contains(it) }
        val hasThreat = threatWords.any { lowercaseText.contains(it) }
        val hasHate = hateSpeechWords.any { lowercaseText.contains(it) }

        if (hasAbusive) {
            flaggedReasons.add("Abusive/Offensive Language")
        }
        if (hasThreat) {
            flaggedReasons.add("Direct/Indirect Threat")
        }
        if (hasHate) {
            flaggedReasons.add("Hate Speech")
        }

        // Check if too short or suspicious nonsense
        if (text.isNotBlank() && text.length < 5) {
            flaggedReasons.add("Suspiciously Short Content")
        }

        val status = if (flaggedReasons.isNotEmpty()) "FLAGGED" else "CLEAN"
        return Pair(status, flaggedReasons)
    }

    fun syncNow() {
        viewModelScope.launch {
            repository.syncLocalDatabaseToFirebase()
        }
    }

    fun submitHelpMessage(senderName: String, senderEmail: String, senderContact: String, message: String) {
        viewModelScope.launch {
            repository.submitHelpMessage(
                HelpMessage(
                    senderName = senderName,
                    senderEmail = senderEmail,
                    senderContact = senderContact,
                    message = message
                )
            )
            logAction("HELP_MESSAGE_SUBMITTED", "User $senderName submitted a support/help message")
        }
    }

    fun deleteHelpMessage(id: String) {
        viewModelScope.launch {
            val senderName = allHelpMessages.value.find { it.id == id }?.senderName ?: id
            repository.deleteHelpMessage(id)
            logAction("HELP_MESSAGE_DELETED", "Deleted help message from $senderName")
        }
    }

    fun markHelpMessageAsRead(id: String, isRead: Boolean) {
        viewModelScope.launch {
            repository.markHelpMessageAsRead(id, isRead)
        }
    }
}
