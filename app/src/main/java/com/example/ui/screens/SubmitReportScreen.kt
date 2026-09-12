package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.example.data.model.Report
import com.example.data.model.ReportType
import com.example.ui.MainViewModel
import com.example.util.MediaStorageHelper
import com.google.android.gms.location.LocationServices
import java.util.UUID
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import android.annotation.SuppressLint

@Composable
fun SubmitReportScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ReportType.SUSPICIOUS_ACTIVITY) }
    var selectedCounty by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var countyDropdownExpanded by remember { mutableStateOf(false) }
    var isAnonymous by remember { mutableStateOf(false) }
    var contactInfo by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var selectedVideoUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var selectedAudioUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var incidentTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var hasDeclared by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val counties = listOf(
        "Bomi", "Bong", "Gbarpolu", "Grand Bassa", "Grand Cape Mount", 
        "Grand Gedeh", "Grand Kru", "Lofa", "Margibi", "Maryland", 
        "Montserrado", "Nimba", "River Cess", "River Gee", "Sinoe"
    )
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(4),
        onResult = { uris -> 
            selectedImageUris = selectedImageUris + uris
        }
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                selectedVideoUris = selectedVideoUris + it
            }
        }
    )

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                selectedAudioUris = selectedAudioUris + it
            }
        }
    )

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Scaffold(
        
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding() // Ensures layout adjusts for keyboard
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
                        "REPORT NODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "INCIDENT REPORT",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            val userReputation = currentUser?.reputationScore ?: 100
            if (userReputation < 50) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            "Low Reputation Warning: Your reports will be under stricter scrutiny due to past inaccuracies. Repetitive false reporting will lead to account suspension.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Surface(
                color = Color(0xFF1E88E5),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Submitting a report does not establish guilt. Every report is reviewed and verified before any action is taken. Deliberately false or malicious reports may result in account suspension and, where applicable, referral to the appropriate authorities.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Short Title") },
                modifier = Modifier.fillMaxWidth(),
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Detailed Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
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
            
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "INCIDENT DATE & TIME",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Surface(
                onClick = { /* In a real app, show date/time picker dialog */ },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(Date(incidentTimestamp)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "GPS LOCATION",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                if (latitude == 0.0) "No location captured" else "Location Captured",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (latitude != 0.0) {
                                Text(
                                    "Lat: ${String.format("%.4f", latitude)}, Lng: ${String.format("%.4f", longitude)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    
                    fun fetchLocation() {
                        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        
                        if (hasFine || hasCoarse) {
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { loc ->
                                    if (loc != null) {
                                        latitude = loc.latitude
                                        longitude = loc.longitude
                                    } else {
                                        android.widget.Toast.makeText(context, "Location unavailable. Please ensure GPS is on.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    android.widget.Toast.makeText(context, "Location error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            android.widget.Toast.makeText(context, "Location permission required to capture GPS coordinates.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }

                    TextButton(onClick = { fetchLocation() }) {
                        Text("GET GPS")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "ATTACH EVIDENCE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Photo Button
                Surface(
                    onClick = { 
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("PHOTO", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Video Button
                Surface(
                    onClick = { 
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("VIDEO", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Audio Button
                Surface(
                    onClick = { 
                        audioPickerLauncher.launch("audio/*")
                    },
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF10B981))
                        Text("AUDIO", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (selectedImageUris.isNotEmpty() || selectedVideoUris.isNotEmpty() || selectedAudioUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    selectedImageUris.forEach { uri ->
                        Box(
                            modifier = Modifier.size(68.dp)
                        ) {
                            com.example.ui.components.SafeAsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .align(Alignment.BottomStart)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
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
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    selectedVideoUris.forEach { uri ->
                        Box(
                            modifier = Modifier.size(68.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(60.dp)
                                    .align(Alignment.BottomStart),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(16.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .background(MaterialTheme.colorScheme.tertiary, shape = androidx.compose.foundation.shape.CircleShape)
                                    .clickable {
                                        selectedVideoUris = selectedVideoUris - uri
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    selectedAudioUris.forEach { uri ->
                        Box(
                            modifier = Modifier.size(68.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(60.dp)
                                    .align(Alignment.BottomStart),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.padding(16.dp), tint = Color(0xFF10B981))
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .background(MaterialTheme.colorScheme.tertiary, shape = androidx.compose.foundation.shape.CircleShape)
                                    .clickable {
                                        selectedAudioUris = selectedAudioUris - uri
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Audio",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "SELECT CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { dropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = type.name.replace("_AND_", " & ").replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown Arrow",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 280.dp),
                    containerColor = Color(0xFFF8F9FA)
                ) {
                    ReportType.entries.forEach { reportType ->
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
                                        reportType.name.replace("_AND_", " & ").replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF0D47A1),
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    type = reportType
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "SELECT COUNTY",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { countyDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedCounty.isEmpty()) "Select County" else selectedCounty,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedCounty.isEmpty()) Color.Gray else Color(0xFF212121)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown Arrow",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                DropdownMenu(
                    expanded = countyDropdownExpanded,
                    onDismissRequest = { countyDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 280.dp),
                    containerColor = Color(0xFFF8F9FA)
                ) {
                    counties.forEach { county ->
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
                                        county,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF0D47A1),
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    selectedCounty = county
                                    countyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                onClick = { isAnonymous = !isAnonymous }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAnonymous,
                        onCheckedChange = { isAnonymous = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text("Submit as Anonymous Node", style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (!isAnonymous) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = contactInfo,
                    onValueChange = { contactInfo = it },
                    label = { Text("Contact Information (Phone or Email)") },
                    placeholder = { Text("How can law enforcers reach you?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
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
                Text(
                    "This information will be used by law enforcers to contact you for prompt action or clarification.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(
                color = Color(0xFF1E88E5),
                shape = RoundedCornerShape(16.dp),
                onClick = { hasDeclared = !hasDeclared },
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = hasDeclared,
                        onCheckedChange = { hasDeclared = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.White,
                            uncheckedColor = Color.White,
                            checkmarkColor = Color(0xFF1E88E5)
                        )
                    )
                    Text(
                        "I declare that the information I am providing is true to the best of my knowledge. I understand that knowingly submitting false or malicious reports may result in my account being suspended or permanently banned.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (!hasDeclared) return@Button
                    isLoading = true
                    viewModel.submitReport(
                        context = context,
                        title = title,
                        description = description,
                        type = type,
                        county = selectedCounty.ifEmpty { null },
                        latitude = latitude,
                        longitude = longitude,
                        reporterId = currentUser?.id ?: "unknown",
                        isAnonymous = isAnonymous,
                        contactInfo = if (isAnonymous) null else contactInfo.ifBlank { null },
                        imageUris = selectedImageUris,
                        videoUris = selectedVideoUris,
                        audioUris = selectedAudioUris,
                        incidentTimestamp = incidentTimestamp,
                        onProgress = { loadingMessage = it }
                    ) { success ->
                        isLoading = false
                        if (success) {
                            showSuccessDialog = true
                        } else {
                            android.widget.Toast.makeText(context, "Submission failed. Please check your connection and try again.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = title.isNotBlank() && description.isNotBlank() && hasDeclared && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
contentColor = MaterialTheme.colorScheme.onTertiary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(loadingMessage, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("SUBMIT TO TRACENET", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showSuccessDialog) {
        val matchingContact = remember(selectedCounty) {
            val list = try {
                com.example.data.model.VerifiedContacts.list
            } catch (e: Exception) {
                emptyList()
            }
            val matched = list.firstOrNull {
                it.county.equals(selectedCounty, ignoreCase = true) && it.category == "Police"
            }
            matched ?: list.firstOrNull { it.category == "Police" }
        }

        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Report Submitted Successfully", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Thank you. Your report has been received and is awaiting review.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (matchingContact != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            "Need immediate assistance?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Text(
                            "You can contact the relevant office directly from this screen:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        
                        Text(
                            matchingContact.officeName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF60A5FA)
                        )
                        
                        // 📞 Call Station Button
                        val phoneIsAvailable = !matchingContact.phoneNumber.isNullOrBlank() && matchingContact.phoneNumber != "N/A"
                        Button(
                            enabled = phoneIsAvailable,
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${matchingContact.phoneNumber}"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Cannot place call", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x201E88E5), // Transparent App Blue
                                contentColor = Color(0xFF1565C0),   // Deeper App Blue
                                disabledContainerColor = Color(0xFFF1F5F9),
                                disabledContentColor = Color(0xFF94A3B8)
                            ),
                            border = BorderStroke(1.dp, if (phoneIsAvailable) Color(0x401E88E5) else Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Phone, 
                                contentDescription = "Call", 
                                tint = if (phoneIsAvailable) Color(0xFF1565C0) else Color(0xFF94A3B8), 
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val shortName = matchingContact.officeName
                                .replace("Liberia National Police (LNP) ", "")
                                .replace("County Command", "Police")
                            Text(
                                "Call $shortName", 
                                fontWeight = FontWeight.Bold, 
                                style = MaterialTheme.typography.labelMedium, 
                                fontSize = 12.sp, 
                                color = if (phoneIsAvailable) Color(0xFF1565C0) else Color(0xFF94A3B8), 
                                maxLines = 1, 
                                softWrap = false, 
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        // 💬 WhatsApp Message Button
                        val waNum = matchingContact.whatsAppNumber
                        val waIsAvailable = !waNum.isNullOrBlank() && waNum != "N/A"
                        val cleanNum = waNum?.replace(" ", "")?.replace("+", "") ?: ""
                        Button(
                            enabled = waIsAvailable,
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/$cleanNum"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Cannot open WhatsApp", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x2016A34A),
                                contentColor = Color(0xFF15803D),
                                disabledContainerColor = Color(0xFFF1F5F9),
                                disabledContentColor = Color(0xFF94A3B8)
                            ),
                            border = BorderStroke(1.dp, if (waIsAvailable) Color(0x4016A34A) else Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Send, 
                                contentDescription = "WhatsApp", 
                                tint = if (waIsAvailable) Color(0xFF15803D) else Color(0xFF94A3B8), 
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Message on WhatsApp", 
                                fontWeight = FontWeight.Bold, 
                                style = MaterialTheme.typography.labelMedium, 
                                fontSize = 12.sp, 
                                color = if (waIsAvailable) Color(0xFF15803D) else Color(0xFF94A3B8), 
                                maxLines = 1, 
                                softWrap = false, 
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    showSuccessDialog = false
                    onBack()
                }) {
                    Text("OK, GOT IT", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
