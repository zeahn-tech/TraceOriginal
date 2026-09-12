package com.example.ui.screens

import androidx.compose.ui.text.buildAnnotatedString

import androidx.compose.ui.text.withStyle

import androidx.compose.ui.text.SpanStyle

import androidx.compose.ui.unit.sp


import androidx.compose.foundation.Image

import androidx.compose.foundation.background

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.*

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Info

import androidx.compose.ui.platform.LocalContext

import android.content.Context

import com.example.R

import com.example.data.model.User

import com.example.data.model.UserRole

import com.example.ui.MainViewModel

import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.first

import com.google.android.gms.auth.api.signin.GoogleSignIn

import com.google.android.gms.auth.api.signin.GoogleSignInOptions

import com.google.android.gms.common.api.ApiException

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts


import androidx.compose.ui.graphics.Brush

@Composable
fun SignInScreen(
    viewModel: MainViewModel,
    onSignInSuccess: (User) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToPublicViewing: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("user_credentials", Context.MODE_PRIVATE) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.CITIZEN) }
    var loginError by remember { mutableStateOf<String?>(null) }
    
    // Forgot Password States
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotStep by remember { mutableStateOf(1) } // 1: Verify, 2: Enter code, 3: Change password
    var forgotEmail by remember { mutableStateOf("") }
    var forgotFullName by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf("") }
    var enteredCode by remember { mutableStateOf("") }
    var isSendingCode by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var verificationError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

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
                    "SIGN IN",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    stringResource(R.string.app_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF212121)),
                shape = RoundedCornerShape(12.dp),
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
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF212121)),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
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

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { 
                        forgotStep = 1
                        forgotEmail = email.trim()
                        forgotFullName = ""
                        newPassword = ""
                        confirmNewPassword = ""
                        generatedCode = ""
                        enteredCode = ""
                        isSendingCode = false
                        verificationError = null
                        showForgotPasswordDialog = true 
                    },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                ) {
                    Text(
                        "Forgot Password?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
                    label = { Text("Access Level") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF212121)),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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
                    shape = RoundedCornerShape(12.dp),
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
                        if (email.isBlank() || password.isBlank()) {
                            loginError = "Please enter your credentials"
                            return@launch
                        }

                        viewModel.signIn(email.trim(), password) { success, error, signedInUser ->
                            if (success && signedInUser != null) {
                                scope.launch {
                                    if (signedInUser.role != role) {
                                        loginError = "Role mismatch for this account"
                                        return@launch
                                    }

                                    if (role == UserRole.LAW_ENFORCER && !signedInUser.isApproved) {
                                        loginError = "Awaiting Admin Approval"
                                        return@launch
                                    }

                                    onSignInSuccess(signedInUser)
                                }
                            } else {
                                loginError = error ?: "Invalid Credentials"
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White)
            ) {
                Text("SIGN IN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onNavigateToSignUp) {
                Text(
                    "Don't have an account? Sign Up",
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
            
            var isGoogleSigningIn by remember { mutableStateOf(false) }

            val googleSignInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account?.idToken
                    if (idToken != null) {
                        viewModel.signInWithGoogle(idToken) { success, error, user ->
                            isGoogleSigningIn = false
                            if (success && user != null) {
                                onSignInSuccess(user)
                            } else {
                                loginError = error ?: "Google Sign-In failed"
                            }
                        }
                    } else {
                        isGoogleSigningIn = false
                        loginError = "Failed to retrieve Google ID Token"
                    }
                } catch (e: ApiException) {
                    isGoogleSigningIn = false
                    loginError = "Google Sign-In error: ${e.statusCode}"
                }
            }
            
            Surface(
                onClick = {
                    isGoogleSigningIn = true
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    val signInClient = GoogleSignIn.getClient(context, gso)
                    
                    // Force account selection picker by signing out first
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
                    if (isGoogleSigningIn) {
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
                            "SIGN IN WITH GOOGLE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onNavigateToPublicViewing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PUBLIC VIEWING PORTAL", color = Color.White)
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

    if (showForgotPasswordDialog) {
        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { 
                if (!isSendingCode) showForgotPasswordDialog = false 
            },
            
            title = {
                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    if (isSendingCode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Sending secure reset link...",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Text(
                            text = "Enter your registered email address. We will send you a secure link to reset your password.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        OutlinedTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (verificationError != null && !isSendingCode) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = verificationError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                if (!isSendingCode) {
                    Button(
                        onClick = {
                            if (forgotEmail.isBlank()) {
                                verificationError = "Please enter your email address."
                                return@Button
                            }
                            isSendingCode = true
                            verificationError = null
                            viewModel.sendPasswordResetEmail(
                                email = forgotEmail.trim(),
                                onSuccess = {
                                    isSendingCode = false
                                    showForgotPasswordDialog = false
                                    loginError = "Success! Check your email for the reset link."
                                },
                                onError = { error ->
                                    isSendingCode = false
                                    verificationError = error
                                }
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
contentColor = Color.White
                        )
                    ) {
                        Text("Send Link")
                    }
                }
            },
            dismissButton = {
                if (!isSendingCode) {
                    TextButton(
                        onClick = { showForgotPasswordDialog = false }
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        )
    }
}
