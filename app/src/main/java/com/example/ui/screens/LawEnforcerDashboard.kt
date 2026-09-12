package com.example.ui.screens

import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.data.model.Tip
import com.example.ui.components.LiberiaFlag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyRow
import coil.compose.AsyncImage
import com.example.ui.components.SafeAsyncImage
import com.example.ui.components.DeleteConfirmationDialog
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.Report
import com.example.data.model.User
import com.example.data.model.WantedCriminal
import com.example.ui.components.DashboardCarousel
import com.example.ui.components.VideoPlayer
import com.example.ui.components.AudioPlayer
import com.example.ui.MainViewModel
import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment

@Composable
fun LawEnforcerDashboard(
    viewModel: MainViewModel, 
    onNavigateToPostCriminal: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToPublicViewing: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToMap: (Double?, Double?) -> Unit = { _, _ -> },
    onNavigateToEmergencyContacts: () -> Unit = {},
    onNavigateToCitizenDashboard: () -> Unit = {}
) {
    val reports by viewModel.activeReports.collectAsStateWithLifecycle()
    val criminals by viewModel.wantedCriminals.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allTips by viewModel.allTips.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Incidents", "Tips", "Wanted", "Contacts")

    var selectedTips by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        
        topBar = {
            Column(
                modifier = Modifier
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF42A5F5),
                                Color(0xFF2196F3),
                                Color(0xFF1E88E5)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        LiberiaFlag(modifier = Modifier.padding(bottom = 2.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Black)) {
                                    append("TRACENET")
                                }
                                append(" ")
                                withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Black)) {
                                    append("LIBERIA")
                                }
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            letterSpacing = (-0.5).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Profile Button (At Top)
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        if (currentUser?.profileImageUrl != null) {
                            SafeAsyncImage(
                                model = currentUser?.profileImageUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    stringResource(R.string.app_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (currentUser?.isApproved == true) {
                    Spacer(modifier = Modifier.height(24.dp))
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        edgePadding = 16.dp,
                        divider = {},
                        indicator = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val selected = selectedTab == index
                            Tab(
                                selected = selected,
                                onClick = { selectedTab = index },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .border(
                                        width = 1.dp,
                                        color = Color.Red,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        if (selected) Color.Red.copy(alpha = 0.2f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    ),
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Visible
                                    )
                                },
                                selectedContentColor = Color.White,
                                unselectedContentColor = Color.Gray
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentUser?.isApproved == true) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToPostCriminal,
                    containerColor = Color(0xFF1E88E5),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Post Criminal")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Post Criminal")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                    Text(
                        "ENFORCER BULLETINS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 12.dp)) {
                            DashboardCarousel()
                        }
                    }
                }
            }

            item {
                Surface(
                    onClick = onNavigateToAnalytics,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("National Analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("View statistics and trends nationwide", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                Surface(
                    onClick = onNavigateToPublicViewing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Public Viewing Portal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Verify what the public can see", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (currentUser?.isApproved == false) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                    ) {
                        Text(
                            "Account Pending Approval. Please wait for an Admin to verify your credentials.",
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        if (reports.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                    Text("No reports", color = Color.Gray)
                                }
                            }
                        }
                        items(reports, key = { it.id }) { report ->
                            IncidentManagementCard(
                                report = report,
                                onUpdateStatus = { status -> viewModel.updateReportStatus(report.id, status) },
                                onDelete = { viewModel.softDeleteReport(report.id) },
                                onMerge = { otherId -> viewModel.mergeReports(report.id, otherId) },
                                onAssignInvestigator = { name -> viewModel.assignInvestigator(report.id, name) },
                                onUpdateNotes = { notes -> viewModel.updateInternalNotes(report.id, notes) },
                                onEscalate = { escalated -> viewModel.escalateReport(report.id, escalated) },
                                allReports = reports,
                                allUsers = allUsers,
                                onNavigateToMap = { lat, lng -> onNavigateToMap(lat, lng) }
                            )
                        }
                    }
                    1 -> {
                        if (allTips.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = selectedTips.size == allTips.size && allTips.isNotEmpty(),
                                            onCheckedChange = { checked ->
                                                selectedTips = if (checked) allTips.map { it.id }.toSet() else emptySet()
                                            }
                                        )
                                        Text("Select All", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (selectedTips.isNotEmpty()) {
                                        Button(
                                            onClick = {
                                                viewModel.softDeleteMultipleTips(selectedTips.toList())
                                                selectedTips = emptySet()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Delete (${selectedTips.size})")
                                        }
                                    }
                                }
                            }
                        }
                        if (allTips.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                    Text("No anonymous tips received", color = Color.Gray)
                                }
                            }
                        }
                        items(allTips, key = { it.id }) { tip ->
                            EnforcerTipCard(
                                tip = tip,
                                onReviewClick = { viewModel.markTipAsReviewed(tip.id) },
                                selected = selectedTips.contains(tip.id),
                                onSelectedChange = { selected ->
                                    selectedTips = if (selected) selectedTips + tip.id else selectedTips - tip.id
                                },
                                onDelete = { viewModel.softDeleteTip(tip.id) }
                            )
                        }
                    }
                    2 -> {
                        if (criminals.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                    Text("No wanted criminals listed", color = Color.Gray)
                                }
                            }
                        }
                        items(criminals, key = { it.id }) { criminal ->
                            EnforcerCriminalCard(
                                criminal = criminal,
                                onUpdateStatus = { newStatus ->
                                    viewModel.updateWantedCriminalStatus(criminal.id, newStatus)
                                }
                            )
                        }
                    }
                    3 -> { // Contacts
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1E88E5).copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Emergency Contacts Management", style = MaterialTheme.typography.titleLarge, color = Color(0xFF1E88E5), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Manage, verify, and toggle visibility of emergency response units across Liberia.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF424242))
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = onNavigateToEmergencyContacts,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Open Contacts Manager")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun IncidentManagementCard(
    report: Report,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit,
    onMerge: (String) -> Unit,
    onAssignInvestigator: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onEscalate: (Boolean) -> Unit,
    allReports: List<Report>,
    allUsers: List<User>,
    onNavigateToMap: (Double, Double) -> Unit
) {
    var showMergeDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            onDismissRequest = { showDeleteConfirm = false },
            onConfirm = onDelete,
            title = "Delete Incident?",
            text = "Are you sure you want to delete this incident report? This will move it to the trash."
        )
    }

    val reporter = allUsers.find { it.id == report.reporterId }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${report.type.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }} • ${report.county ?: "Unknown County"}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
        ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        report.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (report.isEscalated) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "ESCALATED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            val isAnonymousReport = report.isAnonymous || report.reporterId.equals("anonymous", ignoreCase = true) || report.reporterId.equals("unknown", ignoreCase = true)
            Text(
                text = "Source: ${if (isAnonymousReport) "Concern Citizen" else (reporter?.name ?: report.reporterId)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            if (!report.isAnonymous) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showContactDialog = true }
                        .padding(top = 4.dp, bottom = 4.dp, end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Contact Info",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    val contactText = if (report.contactInfo != null) "Contact: ${report.contactInfo}" else "Contact Reporter"
                    Text(
                        text = contactText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (report.latitude != 0.0 || report.longitude != 0.0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToMap(report.latitude, report.longitude) }
                        .padding(top = 4.dp, bottom = 4.dp, end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "GPS Coordinates",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "GPS Coordinates: ${String.format(Locale.US, "%.5f", report.latitude)}, ${String.format(Locale.US, "%.5f", report.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (report.aiScreeningStatus == "FLAGGED") {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GppBad,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "AI FLAGGED FOR REVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                letterSpacing = 1.sp
                            )
                            if (report.aiFlaggedReasons.isNotEmpty()) {
                                Text(
                                    text = report.aiFlaggedReasons.joinToString(", "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            } else if (report.aiScreeningStatus == "CLEAN") {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "AI CLEAN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            if (report.assignedInvestigator != null) {
                Text(
                    "Assigned: ${report.assignedInvestigator}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                report.description,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = Color(0xFF374151)
            )


            if (!report.internalNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = Color(0xFFFFFBEB), // Soft warm amber background
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)), // Golden amber border
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Notes Icon",
                            tint = Color(0xFFD97706), // Accent amber icon
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "INTERNAL NOTES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309), // Dark amber label
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report.internalNotes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1F2937), // Highly legible dark text
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            if (report.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                if (report.imageUrls.size == 1) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        SafeAsyncImage(
                            model = report.imageUrls[0],
                            contentDescription = "Incident Evidence",
                            modifier = Modifier
                                .width(300.dp)
                                .aspectRatio(0.8f)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Row(
                            modifier = Modifier
                                .wrapContentWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            report.imageUrls.forEach { url ->
                                SafeAsyncImage(
                                    model = url,
                                    contentDescription = "Incident Evidence",
                                    modifier = Modifier
                                        .width(300.dp)
                                        .aspectRatio(0.8f)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
            
            if (report.videoUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "VIDEO EVIDENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        report.videoUrls.forEach { url ->
                            VideoPlayer(
                                videoUrl = url,
                                modifier = Modifier
                                    .width(300.dp)
                                    .aspectRatio(0.8f)
                            )
                        }
                    }
                }
            }

            if (report.audioUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "AUDIO EVIDENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    report.audioUrls.forEach { url ->
                        AudioPlayer(audioUrl = url)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var expanded by remember { mutableStateOf(false) }
            val statuses = listOf("SUBMITTED", "UNDER_REVIEW", "VERIFIED", "DISMISSED", "FALSE_REPORT")

            val (backgroundColor, contentColor) = when (report.status.uppercase()) {
                "SUBMITTED" -> Pair(Color(0xFFFBBF24).copy(alpha = 0.15f), Color(0xFFFBBF24))
                "UNDER_REVIEW" -> Pair(Color(0xFF3B82F6).copy(alpha = 0.15f), Color(0xFF3B82F6))
                "VERIFIED" -> Pair(Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF10B981))
                "DISMISSED" -> Pair(Color(0xFF6B7280).copy(alpha = 0.15f), Color(0xFF6B7280))
                "FALSE_REPORT" -> Pair(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), MaterialTheme.colorScheme.error)
                else -> Pair(Color.Gray.copy(alpha = 0.15f), Color.Gray)
            }

            // Repositioned Status Selector & Reporter Trust Score
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                    Surface(
                        onClick = { expanded = true },
                        color = backgroundColor,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.3f)),
                        modifier = Modifier.widthIn(min = 160.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = report.status.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = contentColor)
                        }
                    }
                    DropdownMenu(
                        expanded = expanded, 
                        onDismissRequest = { expanded = false }, 
                        modifier = Modifier.heightIn(max = 280.dp),
                        containerColor = Color(0xFFF8F9FA)
                    ) {
                        statuses.forEach { st ->
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
                                            st, 
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF0D47A1),
                                            fontWeight = FontWeight.Medium
                                        ) 
                                    },
                                    onClick = { onUpdateStatus(st); expanded = false }
                                )
                            }
                        }
                    }
                }

                if (reporter != null) {
                    Surface(
                        color = when {
                            reporter.reputationScore >= 80 -> Color(0xFF10B981).copy(alpha = 0.1f)
                            reporter.reputationScore >= 40 -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, when {
                            reporter.reputationScore >= 80 -> Color(0xFF10B981).copy(alpha = 0.2f)
                            reporter.reputationScore >= 40 -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        }),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "Reporter Trust: ${reporter.reputationScore}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                reporter.reputationScore >= 80 -> Color(0xFF10B981)
                                reporter.reputationScore >= 40 -> Color(0xFFF59E0B)
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Spacer(modifier = Modifier.width(12.dp)) // Shifts icons to the right so status button is "slightly to the left"
                        IconButton(onClick = { showNotesDialog = true }) { Icon(Icons.Default.NoteAdd, "Notes", tint = Color.Gray) }
                        IconButton(onClick = { showAssignDialog = true }) { Icon(Icons.Default.AssignmentInd, "Assign", tint = Color.Gray) }
                        IconButton(onClick = { showMergeDialog = true }) { Icon(Icons.Default.MergeType, "Merge", tint = Color.Gray) }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                        IconButton(onClick = { onEscalate(!report.isEscalated) }) {
                            Icon(
                                imageVector = if (report.isEscalated) Icons.Default.Flag else Icons.Default.OutlinedFlag,
                                contentDescription = "Escalate",
                                tint = if (report.isEscalated) MaterialTheme.colorScheme.error else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
    }

    if (showMergeDialog) {
        MergeReportDialog(
            currentReport = report,
            allReports = allReports,
            onDismiss = { showMergeDialog = false },
            onMerge = { otherId -> onMerge(otherId); showMergeDialog = false }
        )
    }

    if (showAssignDialog) {
        SimpleInputDialog(
            title = "Assign Investigator",
            label = "Investigator Name",
            onDismiss = { showAssignDialog = false },
            onConfirm = { name -> onAssignInvestigator(name); showAssignDialog = false }
        )
    }

    if (showNotesDialog) {
        SimpleInputDialog(
            title = "Internal Notes",
            label = "Notes",
            initialValue = report.internalNotes ?: "",
            onDismiss = { showNotesDialog = false },
            onConfirm = { notes -> onUpdateNotes(notes); showNotesDialog = false }
        )
    }

    if (showContactDialog) {
        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { showContactDialog = false },
            title = { Text("Contact Reporter") },
            text = { 
                Column {
                    Text("Reporter: ${report.reporterId}")
                    if (report.contactInfo != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Contact Info:", fontWeight = FontWeight.Bold)
                        Text(report.contactInfo, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No specific contact info provided by reporter.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            },
            confirmButton = { Button(onClick = { showContactDialog = false }) { Text("OK") } }
        )
    }
}

@Composable
fun SimpleInputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
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
        },
        confirmButton = { Button(onClick = { onConfirm(value) }) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun MergeReportDialog(
    currentReport: Report,
    allReports: List<Report>,
    onDismiss: () -> Unit,
    onMerge: (String) -> Unit
) {
    val potentialDuplicates = allReports.filter { it.id != currentReport.id && it.status != "CLOSED" }
    AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



        onDismissRequest = onDismiss,
        title = { Text("Merge Reports") },
        text = {
            if (potentialDuplicates.isEmpty()) {
                Text("No potential duplicate reports found.")
            } else {
                Column {
                    Text("Select a report to merge with this one. This report will be marked as duplicate.")
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(potentialDuplicates, key = { it.id }) { report ->
                            Surface(
                                onClick = { onMerge(report.id) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(report.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(report.description, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EnforcerCriminalCard(
    criminal: WantedCriminal,
    onUpdateStatus: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val statuses = listOf("ACTIVE", "ARRESTED", "CAPTURED", "COLD CASE", "SUBMITTED", "VERIFIED", "DISMISSED")
    
    val currentStatus = (criminal.status ?: "SUBMITTED").uppercase()
    val statusColor = when (currentStatus) {
        "ACTIVE" -> Color(0xFF3B82F6) // Blue
        "VERIFIED" -> Color(0xFF10B981) // Emerald Green
        "ARRESTED", "CAPTURED" -> Color(0xFF10B981) // Emerald Green
        "SUBMITTED" -> Color(0xFFFBBF24) // Yellow
        "COLD CASE" -> Color(0xFF6B7280) // Gray
        else -> MaterialTheme.colorScheme.error // Red/Dismissed
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = (criminal.category ?: "Wanted person notices").uppercase(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            (criminal.name ?: "Unknown").uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Last seen: ${criminal.lastSeen ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                criminal.description,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            if (criminal.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        criminal.imageUrls.forEach { url ->
                            SafeAsyncImage(
                                model = url,
                                contentDescription = "Criminal Evidence",
                                modifier = Modifier
                                    .width(350.dp)
                                    .aspectRatio(0.8f)
                                    .clip(RoundedCornerShape(24.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (criminal.reward != null) {
                    Surface(
                        color = Color(0xFFFBBF24).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.2f))
                    ) {
                        Text(
                            "REWARD: ${criminal.reward}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                Box {
                    Surface(
                        onClick = { expanded = true },
                        color = statusColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentStatus,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                            Icon(
                                Icons.Default.ArrowDropDown, 
                                contentDescription = "Change Status", 
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.heightIn(max = 280.dp),
                        containerColor = Color(0xFFF8F9FA)
                    ) {
                        statuses.forEach { st ->
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
                                            st, 
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF0D47A1)
                                        ) 
                                    },
                                    onClick = { 
                                        onUpdateStatus(st)
                                        expanded = false 
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun EnforcerTipCard(
    tip: Tip,
    onReviewClick: () -> Unit,
    selected: Boolean = false,
    onSelectedChange: (Boolean) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart).padding(end = 80.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = onSelectedChange
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (tip.isAnonymous) "ANONYMOUS TIP" else "USER TIP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = tip.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (!tip.isAnonymous && !tip.submitterId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Submitter ID: ${tip.submitterId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (tip.isReviewed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF10B981), RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reviewed & Citizen Notified",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "New Tip (Pending Review)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = java.text.DateFormat.getDateTimeInstance(
                            java.text.DateFormat.SHORT, 
                            java.text.DateFormat.SHORT
                        ).format(java.util.Date(tip.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                if (!tip.isReviewed) {
                    Button(
                        onClick = onReviewClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
contentColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Mark as Reviewed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
