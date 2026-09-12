package com.example.ui.screens

import androidx.compose.ui.text.buildAnnotatedString

import androidx.compose.ui.text.withStyle

import androidx.compose.ui.text.SpanStyle

import androidx.compose.ui.unit.sp


import androidx.compose.foundation.layout.*

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp

import androidx.compose.ui.platform.LocalContext

import android.content.Context

import com.example.auth.SessionManager

import com.example.data.model.User

import com.example.data.model.UserRole

import com.example.ui.MainViewModel


import androidx.compose.foundation.Image

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.res.painterResource

import com.example.R


import androidx.compose.foundation.background

import androidx.compose.ui.draw.clip

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.ui.graphics.Color

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.PickVisualMediaRequest

import androidx.activity.result.contract.ActivityResultContracts

import com.google.android.gms.auth.api.signin.GoogleSignIn

import com.google.android.gms.auth.api.signin.GoogleSignInOptions

import com.google.android.gms.common.api.ApiException

import androidx.compose.foundation.clickable

import coil.compose.AsyncImage

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.AddAPhoto

import kotlinx.coroutines.launch


import androidx.compose.ui.graphics.Brush

@Composable
fun SignUpScreen(
    viewModel: MainViewModel,
    onSignUpSuccess: (User) -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("user_credentials", Context.MODE_PRIVATE) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.CITIZEN) }
    var loginError by remember { mutableStateOf<String?>(null) }
    
    var badgeNumber by remember { mutableStateOf("") }
    var idCardUrl by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedIdCardUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("") }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsOfServiceDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedIdCardUri = uri
                idCardUrl = uri.toString()
            }
        }
    )

    Scaffold(
        
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF42A5F5),
                            Color(0xFF2196F3),
                            Color(0xFF1E88E5)
                        )
                    )
                )
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_tracenet_logo_1783258216661),
                contentDescription = "TraceNet Logo",
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Black)) {
                            append("TRACENET")
                        }
                        append(" ")
                        withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Black, shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black, blurRadius = 4f))) {
                            append("LIBERIA")
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "NODE REGISTRATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    "SIGN UP",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF212121)),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color(0xFF212121),
                    unfocusedTextColor = Color(0xFF212121),
                    focusedBorderColor = Color(0xFF1E88E5),
                    unfocusedBorderColor = Color(0xFFBDBDBD),
                    focusedLabelColor = Color(0xFF1E88E5),
                    unfocusedLabelColor = Color(0xFF757575),
                    cursorColor = Color(0xFF1E88E5)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF212121)),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color(0xFF212121),
                    unfocusedTextColor = Color(0xFF212121),
                    focusedBorderColor = Color(0xFF1E88E5),
                    unfocusedBorderColor = Color(0xFFBDBDBD),
                    focusedLabelColor = Color(0xFF1E88E5),
                    unfocusedLabelColor = Color(0xFF757575),
                    cursorColor = Color(0xFF1E88E5)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Create Password") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF212121)),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color(0xFF212121),
                    unfocusedTextColor = Color(0xFF212121),
                    focusedBorderColor = Color(0xFF1E88E5),
                    unfocusedBorderColor = Color(0xFFBDBDBD),
                    focusedLabelColor = Color(0xFF1E88E5),
                    unfocusedLabelColor = Color(0xFF757575),
                    cursorColor = Color(0xFF1E88E5)
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            var expanded by remember { mutableStateOf(false) }
            val roles = listOf(UserRole.CITIZEN, UserRole.LAW_ENFORCER, UserRole.ADMIN)

            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = when(role) {
                        UserRole.CITIZEN -> "Citizen Access"
                        UserRole.LAW_ENFORCER -> "Law Enforcer Access"
                        UserRole.ADMIN -> "Administrator Access"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Access Level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .heightIn(max = 280.dp),
                    containerColor = Color(0xFFF8F9FA)
                ) {
                    roles.forEach { selectionOption ->
                        val isSelected = role == selectionOption
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF1E88E5) else Color(0xFFE3F2FD)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when(selectionOption) {
                                            UserRole.CITIZEN -> "Citizen"
                                            UserRole.LAW_ENFORCER -> "Law Enforcer"
                                            UserRole.ADMIN -> "Administrator"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) Color.White else Color(0xFF0D47A1),
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    role = selectionOption
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Physical Location / Address") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF212121)),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color(0xFF212121),
                    unfocusedTextColor = Color(0xFF212121),
                    focusedBorderColor = Color(0xFF1E88E5),
                    unfocusedBorderColor = Color(0xFFBDBDBD),
                    focusedLabelColor = Color(0xFF1E88E5),
                    unfocusedLabelColor = Color(0xFF757575),
                    cursorColor = Color(0xFF1E88E5)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (role == UserRole.LAW_ENFORCER) {
                Text(
                    "ENFORCER VERIFICATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = badgeNumber,
                    onValueChange = { badgeNumber = it },
                    label = { Text("Badge Number") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF212121)),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color(0xFF212121),
                        unfocusedTextColor = Color(0xFF212121),
                        focusedBorderColor = Color(0xFF1E88E5),
                        unfocusedBorderColor = Color(0xFFBDBDBD),
                        focusedLabelColor = Color(0xFF1E88E5),
                        unfocusedLabelColor = Color(0xFF757575),
                        cursorColor = Color(0xFF1E88E5)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Number") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF212121)),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color(0xFF212121),
                        unfocusedTextColor = Color(0xFF212121),
                        focusedBorderColor = Color(0xFF1E88E5),
                        unfocusedBorderColor = Color(0xFFBDBDBD),
                        focusedLabelColor = Color(0xFF1E88E5),
                        unfocusedLabelColor = Color(0xFF757575),
                        cursorColor = Color(0xFF1E88E5)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clickable {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        if (idCardUrl.isEmpty()) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (idCardUrl.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap to upload ID Card Image",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        com.example.ui.components.SafeAsyncImage(
                            model = idCardUrl,
                            contentDescription = "Selected ID Card",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            if (loginError != null) {
                Text(
                    loginError!!,
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedButton(
                onClick = {
                    scope.launch {
                        if (name.isBlank() || email.isBlank() || password.isBlank()) {
                            loginError = "Please fill in all details"
                            return@launch
                        }

                        if (role == UserRole.LAW_ENFORCER && (badgeNumber.isBlank() || contact.isBlank() || address.isBlank())) {
                            loginError = "All verification fields are mandatory for Enforcers"
                            return@launch
                        }

                        isLoading = true
                        loadingMessage = "Initializing deployment..."

                        val newUser = User(
                            id = "", // Will be set by Firebase UID in ViewModel
                            name = name,
                            email = email.trim(),
                            role = role,
                            isApproved = role != UserRole.LAW_ENFORCER,
                            badgeNumber = if (role == UserRole.LAW_ENFORCER) badgeNumber else null,
                            contact = if (role == UserRole.LAW_ENFORCER) contact else null,
                            address = address.ifBlank { null }
                        )

                        viewModel.signUp(
                            user = newUser, 
                            password = password,
                            idCardUri = selectedIdCardUri,
                            onProgress = { loadingMessage = it }
                        ) { success, error, createdUser ->
                            isLoading = false
                            if (success && createdUser != null) {
                                if (role == UserRole.LAW_ENFORCER) {
                                    loginError = "Deployment request submitted. Stand by for verification."
                                } else {
                                    onSignUpSuccess(createdUser)
                                }
                            } else {
                                loginError = error ?: "Registration failed"
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(loadingMessage, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("SIGN UP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By signing up, you agree to our ",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = "Terms",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showTermsOfServiceDialog = true }
                )
                Text(
                    text = " & ",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showPrivacyPolicyDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(onClick = onNavigateToSignIn) {
                Text(
                    "Already have an account? Sign In",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                Text(
                    "OR CONTINUE WITH",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            var isGoogleSigningUp by remember { mutableStateOf(false) }

            val googleSignInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account?.idToken
                    if (idToken != null) {
                        viewModel.signInWithGoogle(
                            idToken = idToken, 
                            selectedRole = role,
                            badgeNumber = if (role == UserRole.LAW_ENFORCER) badgeNumber else null,
                            contact = if (role == UserRole.LAW_ENFORCER) contact else null,
                            address = address.ifBlank { null },
                            idCardUri = selectedIdCardUri
                        ) { success, error, user ->
                            isGoogleSigningUp = false
                            if (success && user != null) {
                                if (user.role == UserRole.LAW_ENFORCER && !user.isApproved) {
                                    loginError = "Deployment request submitted. Stand by for verification."
                                } else {
                                    onSignUpSuccess(user)
                                }
                            } else {
                                loginError = error ?: "Google Sign-Up failed"
                            }
                        }
                    } else {
                        isGoogleSigningUp = false
                        loginError = "Failed to retrieve Google ID Token"
                    }
                } catch (e: ApiException) {
                    isGoogleSigningUp = false
                    loginError = "Google Sign-Up error: ${e.statusCode}"
                }
            }

            Surface(
                onClick = {
                    if (role == UserRole.LAW_ENFORCER && (badgeNumber.isBlank() || contact.isBlank() || address.isBlank() || selectedIdCardUri == null)) {
                        loginError = "Please fill in enforcer details before Google Sign-Up"
                        return@Surface
                    }
                    isGoogleSigningUp = true
                    // Force account selection picker by signing out first
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    val signInClient = GoogleSignIn.getClient(context, gso)
                    
                    signInClient.signOut().addOnCompleteListener {
                        googleSignInLauncher.launch(signInClient.signInIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                contentColor = Color.Black
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isGoogleSigningUp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "SIGN UP WITH GOOGLE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "POWERED BY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Zeahn's Tech",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicyDialog = false })
    }
    if (showTermsOfServiceDialog) {
        TermsOfServiceDialog(onDismiss = { showTermsOfServiceDialog = false })
    }
}

