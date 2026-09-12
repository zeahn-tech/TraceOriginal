package com.example.ui.screens

import com.example.ui.toTraceNetLiberiaAnnotatedString

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.HelpMessage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.MainViewModel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.util.MediaStorageHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var name by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var email by remember(currentUser) { mutableStateOf(currentUser?.email ?: "") }
    var address by remember(currentUser) { mutableStateOf(currentUser?.address ?: "") }
    var profileImageUrl by remember(currentUser) { mutableStateOf(currentUser?.profileImageUrl) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUpdating by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showRateUsDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsOfServiceDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { 
                selectedImageUri = it
                profileImageUrl = it.toString()
            }
        }
    )

    if (showHelpDialog) {
        HelpDialog(
            currentUser = currentUser,
            onDismiss = { showHelpDialog = false },
            onSubmitInAppMessage = { msg ->
                viewModel.submitHelpMessage(
                    senderName = currentUser?.name ?: "Anonymous Citizen",
                    senderEmail = currentUser?.email ?: "anonymous@tracenet.lib",
                    senderContact = currentUser?.contact ?: "0776391531",
                    message = msg
                )
                Toast.makeText(context, "Help request sent to administrators!", Toast.LENGTH_LONG).show()
            }
        )
    }

    if (showRateUsDialog) {
        RateUsDialog(
            onDismiss = { showRateUsDialog = false },
            onSubmitRating = { stars, feedback ->
                showRateUsDialog = false
                viewModel.logAction("APP_RATING", "User rated the app $stars stars. Feedback: $feedback")
                Toast.makeText(context, "Thank you for rating us $stars stars!", Toast.LENGTH_LONG).show()
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicyDialog = false })
    }

    if (showTermsOfServiceDialog) {
        TermsOfServiceDialog(onDismiss = { showTermsOfServiceDialog = false })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "PROFILE MANAGEMENT", 
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentUser?.role == com.example.data.model.UserRole.CITIZEN) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            IconButton(
                                onClick = { showHelpDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Help & Support",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = onNavigateToAbout,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "About TraceNet",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { showHelpDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Help & Support",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    
                )
            )
        },
        
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image Section
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.size(120.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    if (profileImageUrl != null) {
                        com.example.ui.components.SafeAsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxSize(),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Surface(
                    onClick = { 
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 4.dp
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Avatar",
                        modifier = Modifier.padding(8.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = currentUser?.role?.name ?: "USER",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Editable Fields
            ProfileField(
                label = "Full Name",
                value = name,
                onValueChange = { name = it },
                icon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileField(
                label = "Email Address (Identity)",
                value = email,
                onValueChange = { },
                icon = Icons.Default.Email,
                readOnly = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileField(
                label = "Physical Location / Address",
                value = address,
                onValueChange = { address = it },
                icon = Icons.Default.LocationOn
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Account Status Info (Read-only)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (currentUser?.isApproved == true) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (currentUser?.isApproved == true) Color(0xFF059669) else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Verification",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                if (currentUser?.isApproved == true) "Approved" else "Pending",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        (currentUser?.reputationScore ?: 0) >= 80 -> Color(0xFF10B981).copy(alpha = 0.1f)
                        (currentUser?.reputationScore ?: 0) >= 40 -> Color(0xFFFBBF24).copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = when {
                                (currentUser?.reputationScore ?: 0) >= 80 -> Color(0xFF059669)
                                (currentUser?.reputationScore ?: 0) >= 40 -> Color(0xFFD97706)
                                else -> Color(0xFFC8102E)
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Reputation",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                "${currentUser?.reputationScore ?: 0}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    scope.launch {
                        isUpdating = true
                        currentUser?.let {
                            var finalImageUrl = it.profileImageUrl
                            var imageUploadFailed = false
                            if (selectedImageUri != null) {
                                try {
                                    val uploadedUrl = viewModel.uploadImage(selectedImageUri!!, "profiles")
                                    if (uploadedUrl != null) {
                                        finalImageUrl = uploadedUrl
                                    } else {
                                        // Fallback to local save if cloud fails
                                        val localSavedPath = com.example.util.MediaStorageHelper.saveMediaToInternalStorage(context, selectedImageUri!!)
                                        if (localSavedPath != null) {
                                            finalImageUrl = Uri.parse(localSavedPath).toString()
                                            imageUploadFailed = true
                                        } else {
                                            // Manual fallback
                                            val inputStream = context.contentResolver.openInputStream(selectedImageUri!!)
                                            val bytes = inputStream?.readBytes()
                                            inputStream?.close()
                                            if (bytes != null) {
                                                val file = java.io.File(context.filesDir, "profile_${java.util.UUID.randomUUID()}.jpg")
                                                val outputStream = java.io.FileOutputStream(file)
                                                outputStream.write(bytes)
                                                outputStream.close()
                                                finalImageUrl = Uri.fromFile(file).toString()
                                                imageUploadFailed = true
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    imageUploadFailed = true
                                }
                            }
                            
                            val updatedUser = it.copy(
                                name = name, 
                                email = email,
                                address = address,
                                profileImageUrl = finalImageUrl
                            )
                            viewModel.updateProfile(updatedUser)
                            selectedImageUri = null // Reset selected image after update
                            
                            val message = if (imageUploadFailed && selectedImageUri != null) {
                                "Profile updated, but photo failed to upload to cloud (saved locally)."
                            } else if (imageUploadFailed) {
                                "Profile updated, but photo update failed."
                            } else {
                                "Profile updated successfully!"
                            }
                            snackbarHostState.showSnackbar(message)
                        }
                        isUpdating = false
                    }
                },
                enabled = !isUpdating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("SAVING...", fontWeight = FontWeight.Bold)
                } else {
                    Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var showChangePasswordDialog by remember { mutableStateOf(false) }
            if (showChangePasswordDialog) {
                ChangePasswordDialog(
                    onDismiss = { showChangePasswordDialog = false },
                    onConfirm = { oldPass, newPass ->
                        viewModel.updatePassword(oldPass, newPass) { success, error ->
                            if (success) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(error ?: "Password updated successfully!")
                                }
                                showChangePasswordDialog = false
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Error: $error")
                                }
                            }
                        }
                    }
                )
            }

            OutlinedButton(
                onClick = { showChangePasswordDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Password, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CHANGE PASSWORD", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showRateUsDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("RATE OUR APP", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("LOGOUT SESSION", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete My Account", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account permanently?") },
            text = { Text("This action cannot be undone. All your reports, tips, and personal data will be removed from the system. Are you absolutely sure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        currentUser?.let { viewModel.deleteAccount(it.id) }
                        onLogout()
                    }
                ) {
                    Text("DELETE FOREVER", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout Session?") },
            text = { Text("Are you sure you want to end your current session? You will need to re-authenticate.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("LOGOUT", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Current Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF212121)),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color(0xFF212121),
                        unfocusedTextColor = Color(0xFF212121),
                        focusedBorderColor = Color(0xFF1E88E5),
                        unfocusedBorderColor = Color(0xFFBDBDBD),
                        focusedLabelColor = Color(0xFF1E88E5),
                        unfocusedLabelColor = Color(0xFF757575)
                    )
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF212121)),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color(0xFF212121),
                        unfocusedTextColor = Color(0xFF212121),
                        focusedBorderColor = Color(0xFF1E88E5),
                        unfocusedBorderColor = Color(0xFFBDBDBD),
                        focusedLabelColor = Color(0xFF1E88E5),
                        unfocusedLabelColor = Color(0xFF757575)
                    )
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF212121)),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color(0xFF212121),
                        unfocusedTextColor = Color(0xFF212121),
                        focusedBorderColor = Color(0xFF1E88E5),
                        unfocusedBorderColor = Color(0xFFBDBDBD),
                        focusedLabelColor = Color(0xFF1E88E5),
                        unfocusedLabelColor = Color(0xFF757575)
                    )
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword != confirmPassword) {
                        error = "Passwords do not match"
                    } else if (newPassword.length < 6) {
                        error = "Password must be at least 6 characters"
                    } else if (oldPassword.isBlank()) {
                        error = "Current password is required"
                    } else {
                        onConfirm(oldPassword, newPassword)
                    }
                },
                enabled = oldPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("UPDATE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    readOnly: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            readOnly = readOnly,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color(0xFF212121),
                unfocusedTextColor = Color(0xFF212121),
                focusedBorderColor = Color(0xFF1E88E5),
                unfocusedBorderColor = Color(0xFFBDBDBD),
                focusedLabelColor = Color(0xFF1E88E5),
                unfocusedLabelColor = Color(0xFF757575)
            )
        )
    }
}

@Composable
fun HelpDialog(
    currentUser: User?,
    onDismiss: () -> Unit,
    onSubmitInAppMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    var inAppMessageMode by remember { mutableStateOf(false) }

    AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Help & Assistance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Get in touch with TraceNet Liberia administrators".toTraceNetLiberiaAnnotatedString(MaterialTheme.colorScheme.primary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!inAppMessageMode) {
                    // Contact Buttons
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:info.tracenetlib@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "TraceNet Liberia - Support Request")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(
contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Email: info.tracenetlib@gmail.com",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:0776391531")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(
contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Call Support: 0776391531",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { inAppMessageMode = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "In-App Message to Admin",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                } else {
                    // In-App message field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Write support message to admin",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("How can we assist you today?", color = Color(0xFF757575)) },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF212121)),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = Color(0xFF212121),
                                unfocusedTextColor = Color(0xFF212121),
                                focusedBorderColor = Color(0xFF1E88E5),
                                unfocusedBorderColor = Color(0xFFBDBDBD),
                                focusedLabelColor = Color(0xFF1E88E5),
                                unfocusedLabelColor = Color(0xFF757575)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { inAppMessageMode = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    if (messageText.isBlank()) {
                                        Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onSubmitInAppMessage(messageText)
                                        messageText = ""
                                        inAppMessageMode = false
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Send")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!inAppMessageMode) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun RateUsDialog(
    onDismiss: () -> Unit,
    onSubmitRating: (Int, String) -> Unit
) {
    var rating by remember { mutableStateOf(0) }
    var reviewText by remember { mutableStateOf("") }

    AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFEAB308).copy(alpha = 0.1f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFEAB308),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Rate TraceNet Liberia".toTraceNetLiberiaAnnotatedString(MaterialTheme.colorScheme.primary),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "We value your feedback to make Liberia safer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Star Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rate $i Star",
                            tint = if (i <= rating) Color(0xFFEAB308) else Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { rating = i }
                        )
                    }
                }

                if (rating > 0) {
                    val ratingText = when (rating) {
                        1 -> "Terrible 😟"
                        2 -> "Bad 🙁"
                        3 -> "Okay 😐"
                        4 -> "Good 🙂"
                        5 -> "Excellent! 😍"
                        else -> ""
                    }
                    Text(
                        text = ratingText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEAB308)
                    )
                }

                // Comment text field
                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    placeholder = { Text("Tell us more about your experience... (optional)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmitRating(rating, reviewText)
                },
                enabled = rating > 0,
                colors = ButtonDefaults.buttonColors(
contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SUBMIT", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.primary)
            }
        },
        
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TraceNet Liberia".toTraceNetLiberiaAnnotatedString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color(0xFF757575)
                )
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(scrollState)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Effective Date: July 8, 2026",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "1. Information We Collect\nTo help maintain community security, we process report content, anonymous or identified tip information, approximate location coordinates (for pinpointing incidents on the map), and basic profile details like name and contact info (if you choose to register).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )

                Text(
                    text = "2. How Information is Used\nYour safety reports and local crime feedback are processed to update public awareness feeds, generate live analytical statistics, and dispatch emergency safety signals. We do not sell or monetize any personal identifier.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )

                Text(
                    text = "3. Complete Confidentiality\nWhen submitting tips, your identity remains fully confidential and protected. Anonymous submissions are structurally scrubbed of user credentials prior to being recorded in the system database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )

                Text(
                    text = "4. Data Cooperation\nWe collaborate with certified local law enforcement bodies and safety organizations to address active security concerns. Selected alerts or reports are shared directly to coordinate swift and effective support.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )

                Text(
                    text = "5. Security & Protection\nWe enforce industry-standard structural security measures to preserve your personal details and restrict unauthorized file transfers or exposure of database entries.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("DISMISS", fontWeight = FontWeight.Bold)
            }
        },
        
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun TermsOfServiceDialog(onDismiss: () -> Unit) {
    AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TraceNet Liberia".toTraceNetLiberiaAnnotatedString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color(0xFF757575)
                )
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(scrollState)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Last Updated: July 8, 2026",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "1. Agreement to Terms\nBy accessing or creating an account within TraceNet Liberia, you accept and agree to follow these Terms of Service. If you disagree, please refrain from using our system.".toTraceNetLiberiaAnnotatedString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )

                Text(
                    text = "2. User Conduct & Integrity\nYou must verify that all incident reports, tips, and descriptions you upload are accurate, honest, and posted in good faith. Submitting false reports or fabricating security emergencies is strictly illegal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )

                Text(
                    text = "3. Platform Limitations\nTraceNet Liberia serves as a supportive public awareness and crowd-sourced reporting tool. It does not replace national emergency dispatch services (such as police, fire, or medical response). Always contact local authorities immediately in high-risk scenarios.".toTraceNetLiberiaAnnotatedString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )

                Text(
                    text = "4. Account Registration & Safety\nUsers are responsible for preserving password secrecy and are liable for any posts, messages, or activities registered under their credential logs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )

                Text(
                    text = "5. Policy Modifications\nWe reserve the right to revise or adjust these terms to comply with regulatory updates or system improvements. Continued use of TraceNet Liberia constitutes your approval of amended terms.".toTraceNetLiberiaAnnotatedString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ACCEPT", fontWeight = FontWeight.Bold)
            }
        },
        
        shape = RoundedCornerShape(24.dp)
    )
}
