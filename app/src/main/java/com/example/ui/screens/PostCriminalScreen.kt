package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.data.model.WantedCriminal
import com.example.ui.MainViewModel
import com.example.util.MediaStorageHelper
import java.util.UUID

@Composable
fun PostCriminalScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var lastSeen by remember { mutableStateOf("") }
    var reward by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Wanted person notices") }
    var categoryExpanded by remember { mutableStateOf(false) }
    val categories = listOf(
        "Missing person alerts",
        "Wanted person notices",
        "Stolen property alerts",
        "Witness requests",
        "Emergency warnings"
    )
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(4),
        onResult = { uris -> 
            selectedImageUris = (selectedImageUris + uris).take(4)
        }
    )

    Scaffold(
        
    ) { padding ->
        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) { } // Block clicks
                    .padding(padding)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            uploadStatus, 
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Please do not close the app",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

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
                        "ADMIN NODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "POST WANTED CRIMINAL DATA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.3).sp
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Criminal Name") },
                modifier = Modifier.fillMaxWidth(),
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Information/Crimes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
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
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = lastSeen,
                onValueChange = { lastSeen = it },
                label = { Text("Last Seen Location") },
                modifier = Modifier.fillMaxWidth(),
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
            
            Spacer(modifier = Modifier.height(16.dp))


            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = reward,
                onValueChange = { reward = it },
                label = { Text("Reward (Optional)") },
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        IconButton(onClick = { categoryExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    },
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
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f).heightIn(max = 280.dp),
                    containerColor = Color(0xFFF8F9FA)
                ) {
                    categories.forEach { cat ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE3F2FD)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        cat,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF0D47A1),
                                        fontWeight = FontWeight.Medium
                                    ) 
                                },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "CRIMINAL EVIDENCE / PHOTOS (UP TO 4)",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Surface(
                onClick = { 
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            ) {
                if (selectedImageUris.isEmpty()) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("SELECT IMAGES", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (uri in selectedImageUris) {
                            Box(
                                modifier = Modifier.size(108.dp)
                            ) {
                                com.example.ui.components.SafeAsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .align(Alignment.BottomStart)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(24.dp)
                                        .background(MaterialTheme.colorScheme.tertiary, shape = androidx.compose.foundation.shape.CircleShape)
                                        .clickable {
                                            selectedImageUris = selectedImageUris - uri
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Image",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        if (selectedImageUris.size < 4) {
                            Box(
                                modifier = Modifier.size(108.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .align(Alignment.BottomStart)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .clickable { 
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isUploading = true
                    viewModel.postCriminalReport(
                        context = context,
                        name = name,
                        description = description,
                        lastSeen = lastSeen,
                        reward = if (reward.isBlank()) null else reward,
                        category = category,
                        imageUris = selectedImageUris,
                        onProgress = { uploadStatus = it },
                        onResult = { success ->
                            isUploading = false
                            if (success) {
                                onBack()
                            } else {
                                android.widget.Toast.makeText(context, "Failed to broadcast alert", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                },
                enabled = name.isNotBlank() && description.isNotBlank() && !isUploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("BROADCAST ALERT", fontWeight = FontWeight.Bold)
            }
        }
    }
}
