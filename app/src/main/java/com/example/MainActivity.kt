package com.example
import kotlinx.coroutines.launch

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.auth.SessionManager
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.TraceNetTheme
import com.example.util.NotificationHelper
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class MainActivity : androidx.fragment.app.FragmentActivity() {
 // Use FragmentActivity for BiometricPrompt
  private val deepLinkFlow = kotlinx.coroutines.flow.MutableSharedFlow<android.content.Intent>(extraBufferCapacity = 1)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    NotificationHelper.createNotificationChannel(this)
    
    intent?.let { deepLinkFlow.tryEmit(it) }
    
    val repository = (application as TraceNetApplication).repository
    
    setContent {
      TraceNetTheme {
        val viewModel: MainViewModel = viewModel(
            factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository) as T
                }
            }
        )
        MainScreen(viewModel, deepLinkFlow)
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    deepLinkFlow.tryEmit(intent)
  }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    deepLinkFlow: kotlinx.coroutines.flow.SharedFlow<android.content.Intent>
) {
    val navController = rememberNavController()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Item 9: Two-step runtime permission flow for background location
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val geofenceHelper = com.example.util.GeofenceHelper(context)
            // Item 10: Use real coordinates (Liberia National Police HQ as sample)
            geofenceHelper.addGeofence(
                id = "SafeZone_HQ",
                latitude = 6.3006, 
                longitude = -10.7969,
                radius = 1000f
            )
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocationGranted || coarseLocationGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                val geofenceHelper = com.example.util.GeofenceHelper(context)
                geofenceHelper.addGeofence(
                    id = "SafeZone_HQ",
                    latitude = 6.3006,
                    longitude = -10.7969,
                    radius = 1000f
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // Check background location separately if foreground is already granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                val geofenceHelper = com.example.util.GeofenceHelper(context)
                geofenceHelper.addGeofence(
                    id = "SafeZone_HQ",
                    latitude = 6.3006,
                    longitude = -10.7969,
                    radius = 1000f
                )
            }
        }
    }

    // Handle deep link intents
    LaunchedEffect(deepLinkFlow) {
        deepLinkFlow.collect { intent ->
            val navigateTo = intent.getStringExtra("navigate_to")
            if (navigateTo == "map") {
                val lat = intent.getDoubleExtra("latitude", Double.NaN)
                val lng = intent.getDoubleExtra("longitude", Double.NaN)
                if (!lat.isNaN() && !lng.isNaN()) {
                    navController.navigate(Screen.MapScreen(latitude = lat, longitude = lng))
                }
            }
        }
    }

    // Real-time notification observer
    val allAlerts by viewModel.allAlerts.collectAsState()
    var lastAlertId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(allAlerts) {
        if (allAlerts.isNotEmpty()) {
            val latestAlert = allAlerts.first()
            if (lastAlertId == null) {
                lastAlertId = latestAlert.id
            } else if (latestAlert.id != lastAlertId) {
                lastAlertId = latestAlert.id
                
                try {
                    val isSOS = latestAlert.title.contains("SOS", ignoreCase = true)
                    val notificationContent = if (isSOS) {
                        "${latestAlert.content}\nLocation: ${latestAlert.locationName} (${latestAlert.latitude}, ${latestAlert.longitude})"
                    } else {
                        latestAlert.content
                    }
                    
                    NotificationHelper.showNotification(
                        context,
                        title = if (isSOS) "🚨 EMERGENCY: ${latestAlert.title}" else "New TraceNet Alert: ${latestAlert.title}",
                        content = notificationContent,
                        latitude = if (isSOS) latestAlert.latitude else null,
                        longitude = if (isSOS) latestAlert.longitude else null
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Notification error", e)
                }
            }
        }
    }

    val allReports by viewModel.allReports.collectAsState()
    var initialReportCount by remember { mutableStateOf(-1) }

    LaunchedEffect(allReports) {
        if (initialReportCount == -1) {
            initialReportCount = allReports.size
        } else if (allReports.size > initialReportCount) {
            val newReport = allReports.lastOrNull()
            if (newReport != null) {
                NotificationHelper.showNotification(
                    context,
                    title = "New Incident Reported: ${newReport.title}",
                    content = newReport.description
                )
            }
            initialReportCount = allReports.size
        }
    }

    val passwordExpiry by viewModel.passwordExpiry.collectAsState()
    var isPasswordExpired by remember { mutableStateOf(false) }

    // Item 5: Event-driven password expiry check (Replace infinite busy-poll)
    LaunchedEffect(currentUser, passwordExpiry) {
        if (currentUser != null) {
            val lastChanged = viewModel.getPasswordLastChanged(currentUser!!.id)
            val ageMs = System.currentTimeMillis() - lastChanged
            val ageDays = ageMs / (24 * 60 * 60 * 1000L)
            isPasswordExpired = ageDays >= passwordExpiry
        } else {
            isPasswordExpired = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash
        ) {
            composable<Screen.Splash> {
                SplashScreen(
                    onTimeout = {
                        val destination = if (currentUser == null) Screen.Welcome else {
                            if (currentUser?.isApproved == false && currentUser?.role == UserRole.LAW_ENFORCER) {
                                Screen.CitizenDashboard
                            } else {
                                when (currentUser?.role) {
                                    UserRole.CITIZEN -> Screen.CitizenDashboard
                                    UserRole.LAW_ENFORCER -> Screen.LawEnforcerDashboard
                                    UserRole.ADMIN -> Screen.AdminDashboard
                                    else -> Screen.SignIn
                                }
                            }
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                )
            }
            composable<Screen.Welcome> {
                WelcomeScreen(
                    onNavigateToSignIn = { navController.navigate(Screen.SignIn) },
                    onNavigateToSignUp = { navController.navigate(Screen.Login) },
                    onNavigateToPublicViewing = { navController.navigate(Screen.PublicViewing) }
                )
            }
            composable<Screen.SignIn> {
                SignInScreen(
                    viewModel = viewModel,
                    onSignInSuccess = { user ->
                        val destination = if (!user.isApproved && user.role == UserRole.LAW_ENFORCER) {
                            Screen.CitizenDashboard
                        } else {
                            when (user.role) {
                                UserRole.CITIZEN -> Screen.CitizenDashboard
                                UserRole.LAW_ENFORCER -> Screen.LawEnforcerDashboard
                                UserRole.ADMIN -> Screen.AdminDashboard
                            }
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.SignIn) { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = {
                        navController.navigate(Screen.Login)
                    },
                    onNavigateToPublicViewing = {
                        navController.navigate(Screen.PublicViewing)
                    }
                )
            }
            composable<Screen.PublicViewing> {
                PublicViewingScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToSubmitTip = { navController.navigate(Screen.SubmitTip) },
                    onNavigateToAnalytics = { navController.navigate(Screen.Analytics) },
                    onNavigateToMap = { lat, lng ->
                        navController.navigate(Screen.MapScreen(latitude = lat, longitude = lng))
                    }
                )
            }
            composable<Screen.Login> {
                SignUpScreen(
                    viewModel = viewModel,
                    onSignUpSuccess = { user ->
                        val destination = if (!user.isApproved && user.role == UserRole.LAW_ENFORCER) {
                            Screen.CitizenDashboard
                        } else {
                            when (user.role) {
                                UserRole.CITIZEN -> Screen.CitizenDashboard
                                UserRole.LAW_ENFORCER -> Screen.LawEnforcerDashboard
                                UserRole.ADMIN -> Screen.AdminDashboard
                            }
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.Login) { inclusive = true }
                        }
                    },
                    onNavigateToSignIn = {
                        navController.popBackStack()
                    }
                )
            }
            composable<Screen.CitizenDashboard> {
                // Item 3: Role Guard for CitizenDashboard
                if (currentUser != null && (currentUser?.role == UserRole.CITIZEN || currentUser?.role == UserRole.LAW_ENFORCER || currentUser?.role == UserRole.ADMIN)) {
                    CitizenDashboard(
                        viewModel = viewModel,
                        onNavigateToMap = { lat, lng -> navController.navigate(Screen.MapScreen(latitude = lat, longitude = lng)) },
                        onNavigateToSOS = { navController.navigate(Screen.SOSScreen) },
                        onNavigateToSubmitReport = { navController.navigate(Screen.SubmitReport) },
                        onNavigateToSubmitTip = { navController.navigate(Screen.SubmitTip) },
                        onNavigateToProfile = { navController.navigate(Screen.Profile) },
                        onNavigateToAnalytics = { navController.navigate(Screen.Analytics) },
                        onNavigateToPublicViewing = { navController.navigate(Screen.PublicViewing) },
                        onNavigateToAbout = { navController.navigate(Screen.About) },
                        onNavigateToEmergencyContacts = { navController.navigate(Screen.EmergencyContacts) },
                        onNavigateToLawEnforcer = {
                            val dest = if (currentUser?.role == UserRole.ADMIN) Screen.AdminDashboard else Screen.LawEnforcerDashboard
                            navController.navigate(dest) {
                                popUpTo(Screen.CitizenDashboard) { inclusive = true }
                            }
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.SignIn) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
            composable<Screen.LawEnforcerDashboard> {
                // Item 3: Role Guard for LawEnforcerDashboard
                if (currentUser != null && (currentUser?.role == UserRole.LAW_ENFORCER || currentUser?.role == UserRole.ADMIN)) {
                    LawEnforcerDashboard(
                        viewModel = viewModel,
                        onNavigateToPostCriminal = { navController.navigate(Screen.PostCriminal) },
                        onNavigateToProfile = { navController.navigate(Screen.Profile) },
                        onNavigateToAnalytics = { navController.navigate(Screen.Analytics) },
                        onNavigateToPublicViewing = { navController.navigate(Screen.PublicViewing) },
                        onNavigateToAbout = { navController.navigate(Screen.About) },
                        onNavigateToMap = { lat, lng -> navController.navigate(Screen.MapScreen(latitude = lat, longitude = lng)) },
                        onNavigateToEmergencyContacts = { navController.navigate(Screen.EmergencyContacts) },
                        onNavigateToCitizenDashboard = {
                            navController.navigate(Screen.CitizenDashboard) {
                                popUpTo(Screen.LawEnforcerDashboard) { inclusive = true }
                            }
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.SignIn) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
        composable<Screen.AdminDashboard> {
            if (currentUser?.role == UserRole.ADMIN) {
                AdminDashboard(
                    viewModel = viewModel,
                    onNavigateToPostCriminal = { navController.navigate(Screen.PostCriminal) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile) },
                    onNavigateToAnalytics = { navController.navigate(Screen.Analytics) },
                    onNavigateToPublicViewing = { navController.navigate(Screen.PublicViewing) },
                    onNavigateToAbout = { navController.navigate(Screen.About) },
                    onNavigateToMap = { lat, lng -> navController.navigate(Screen.MapScreen(latitude = lat, longitude = lng)) },
                    onNavigateToEmergencyContacts = { navController.navigate(Screen.EmergencyContacts) },
                    onNavigateToCitizenDashboard = {
                        navController.navigate(Screen.CitizenDashboard) {
                            popUpTo(Screen.AdminDashboard) { inclusive = true }
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.SignIn) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        composable<Screen.PostCriminal> {
            PostCriminalScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.MapScreen> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.MapScreen>()
            MapScreen(
                viewModel = viewModel,
                latitude = route.latitude,
                longitude = route.longitude,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.SOSScreen> {
            SOSScreen(
                viewModel = viewModel,
                onDismiss = { navController.popBackStack() },
                onNavigateToContacts = { navController.navigate(Screen.EmergencyContacts) }
            )
        }
        composable<Screen.SubmitReport> {
            SubmitReportScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable<Screen.SubmitTip> {
            SubmitTipScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable<Screen.Profile> {
            ProfileScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    viewModel.signOut()
                    navController.navigate(Screen.SignIn) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToAbout = { navController.navigate(Screen.About) }
            )
        }
        composable<Screen.Analytics> {
            AnalyticsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.About> {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable<Screen.EmergencyContacts> {
            EmergencyContactsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }

    if (isPasswordExpired && currentUser != null) {
        PasswordExpiredOverlay(
            viewModel = viewModel,
            userId = currentUser!!.id,
            userEmail = currentUser!!.email,
            passwordExpiry = passwordExpiry,
            onPasswordUpdated = {
                isPasswordExpired = false
            },
            onLogout = {
                viewModel.signOut()
                navController.navigate(Screen.SignIn) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
}
}

@Composable
fun PasswordExpiredOverlay(
    viewModel: MainViewModel,
    userId: String,
    userEmail: String,
    passwordExpiry: Int,
    onPasswordUpdated: () -> Unit,
    onLogout: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    var isUpdating by remember { mutableStateOf(false) }
    var isSendingReset by remember { mutableStateOf(false) }
    var showResetSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                
                Text(
                    text = "Password Expired",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Your password has expired (Policy: every $passwordExpiry days). For security and compliance, you must update your password before you can proceed.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(
                                imageVector = if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (currentPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (newPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )
                }
                
                if (successMessage != null) {
                    Text(
                        text = successMessage!!,
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                            errorMessage = "Please fill in all fields"
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            errorMessage = "New passwords do not match"
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            errorMessage = "Password must be at least 6 characters"
                            return@Button
                        }
                        
                        isUpdating = true
                        errorMessage = null
                        
                        viewModel.updatePassword(currentPassword, newPassword) { success, error ->
                            isUpdating = false
                            if (success) {
                                successMessage = "Password changed successfully! Resuming session..."
                                android.widget.Toast.makeText(context, "Password updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                onPasswordUpdated()
                            } else {
                                errorMessage = error ?: "Failed to update password"
                            }
                        }
                    },
                    enabled = !isUpdating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("UPDATE & UNLOCK", fontWeight = FontWeight.Bold)
                    }
                }
                
                TextButton(
                    onClick = {
                        isSendingReset = true
                        errorMessage = null
                        viewModel.sendPasswordResetEmail(
                            email = userEmail,
                            onSuccess = {
                                isSendingReset = false
                                showResetSuccessDialog = true
                            },
                            onError = { error ->
                                isSendingReset = false
                                errorMessage = error
                            }
                        )
                    },
                    enabled = !isUpdating && !isSendingReset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSendingReset) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Forgot password? Reset via Email", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                
                TextButton(
                    onClick = onLogout,
                    enabled = !isUpdating && !isSendingReset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel & Log Out", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showResetSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* Don't dismiss without action */ },
            confirmButton = {
                Button(
                    onClick = {
                        showResetSuccessDialog = false
                        onLogout()
                    }
                ) {
                    Text("OK")
                }
            },
            title = { Text("Reset Link Sent") },
            text = { Text("A password reset link has been sent to your registered email address:\n\n$userEmail\n\nYou will now be logged out to complete the password reset process.") }
        )
    }
}
