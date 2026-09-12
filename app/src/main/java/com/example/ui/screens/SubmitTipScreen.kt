package com.example.ui.screens

import com.example.ui.toTraceNetLiberiaAnnotatedString

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Tip
import com.example.ui.MainViewModel
import java.util.UUID

@Composable
fun SubmitTipScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var content by remember { mutableStateOf("") }
    val currentUser by viewModel.currentUser.collectAsState()
    var showSuccessDialog by remember { mutableStateOf(false) }

    val enabledCategories by viewModel.enabledCategories.collectAsState()
    var selectedCategory by remember(enabledCategories) { mutableStateOf(enabledCategories.firstOrNull() ?: com.example.data.model.ReportType.SUSPICIOUS_ACTIVITY) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    if (showSuccessDialog) {
        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = {
                showSuccessDialog = false
                onBack()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Tip Securely Sent",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Your anonymous tip has been encrypted and securely routed to TraceNet Liberia's command node. You will receive an in-app update here once the operators review your information.".toTraceNetLiberiaAnnotatedString(MaterialTheme.colorScheme.onSurface),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onBack()
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column {
                    Text(
                        "TIP NODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "SUBMIT ANONYMOUS TIP",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF1E88E5),
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            Text(
                "Your identity is protected. Tips are encrypted and routed through secure nodes.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                "Select Case Category",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Surface(
                    onClick = { categoryDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedCategory.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            val isSelectedCategoryActive = enabledCategories.contains(selectedCategory)
                            if (!isSelectedCategoryActive) {
                                Text(
                                    "Inactive: Tips are currently disabled for this category",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text(
                                    "Active: Accepting tips",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                        Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                DropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f).heightIn(max = 300.dp),
                    containerColor = Color(0xFFF8F9FA)
                ) {
                    com.example.data.model.ReportType.entries.forEach { type ->
                        val isTypeActive = enabledCategories.contains(type)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isTypeActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isTypeActive) Color.Black else Color.Gray
                                    )
                                    if (isTypeActive) {
                                        Badge(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32)) {
                                            Text("Active", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                        }
                                    } else {
                                        Badge(containerColor = Color(0xFFFFEBEE), contentColor = Color(0xFFC62828)) {
                                            Text("Inactive", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                        }
                                    }
                                }
                            },
                            onClick = {
                                selectedCategory = type
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Enter Tip Details") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                shape = RoundedCornerShape(16.dp),
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
            
            Spacer(modifier = Modifier.height(32.dp))

            val isSelectedCategoryActive = enabledCategories.contains(selectedCategory)
            Button(
                onClick = {
                    if (content.isNotBlank() && isSelectedCategoryActive) {
                        val tip = Tip(
                            id = UUID.randomUUID().toString(),
                            reportId = "category_${selectedCategory.name}", // Associated with category
                            content = content,
                            isAnonymous = true,
                            submitterId = currentUser?.id ?: "anonymous"
                        )
                        viewModel.submitTip(tip)
                        showSuccessDialog = true
                    }
                },
                enabled = content.isNotBlank() && isSelectedCategoryActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelectedCategoryActive) Color(0xFF1E88E5) else Color.Gray,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = if (isSelectedCategoryActive) "ENCRYPT & SEND TIP" else "TIPS INACTIVE FOR THIS CATEGORY",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
