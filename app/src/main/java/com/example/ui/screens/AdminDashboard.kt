package com.example.ui.screens

import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowForward
import com.example.ui.components.LiberiaFlag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import coil.compose.AsyncImage
import com.example.ui.components.SafeAsyncImage
import com.example.ui.components.DeleteConfirmationDialog
import com.example.data.model.Report
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.WantedCriminal
import com.example.data.model.Tip
import com.example.data.model.Alert
import com.example.data.model.AuditLog
import com.example.data.model.HelpMessage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.ui.components.DashboardCarousel
import com.example.ui.components.VideoPlayer
import com.example.ui.components.AudioPlayer
import com.example.ui.MainViewModel
import com.example.R

@Composable
fun AdminDashboard(
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
    val pendingUsers by viewModel.pendingUsers.collectAsStateWithLifecycle()
    val pendingCriminals by viewModel.pendingWantedCriminals.collectAsStateWithLifecycle()
    val activeReports by viewModel.activeReports.collectAsStateWithLifecycle()
    val deletedReports by viewModel.deletedReports.collectAsStateWithLifecycle()
    val activeWantedCriminals by viewModel.activeWantedCriminals.collectAsStateWithLifecycle()
    val deletedWantedCriminals by viewModel.deletedWantedCriminals.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allTips by viewModel.allTips.collectAsStateWithLifecycle()
    val allAlerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allAuditLogs by viewModel.allAuditLogs.collectAsStateWithLifecycle()
    val allHelpMessages by viewModel.allHelpMessages.collectAsStateWithLifecycle()
    val deletedTips by viewModel.deletedTips.collectAsStateWithLifecycle()
    val deletedAuditLogs by viewModel.deletedAuditLogs.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Approvals", "Reports", "Tips", "Wanted", "Alerts", "Users", "Contacts", "Stats", "Logs", "Settings", "Trash", "Support")
    var showSubmitAlertForm by remember { mutableStateOf(false) }

    var selectedTips by remember { mutableStateOf(setOf<String>()) }
    var selectedLogs by remember { mutableStateOf(setOf<String>()) }

    if (showSubmitAlertForm) {
        SubmitAlertForm(
            viewModel = viewModel,
            onBack = { showSubmitAlertForm = false }
        )
    } else {
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
                                    withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Black, shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black, blurRadius = 4f))) {
                                        append("LIBERIA")
                                    }
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Profile Button (At Top)
                        IconButton(
                            onClick = onNavigateToProfile,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            if (currentUser?.profileImageUrl != null) {
                                SafeAsyncImage(
                                    model = currentUser?.profileImageUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        stringResource(R.string.app_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1E88E5),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Public Alert Button
                    Button(
                        onClick = { showSubmitAlertForm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1E88E5)),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.height(48.dp).weight(1f)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Public Alert",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Post Criminal Button
                    Button(
                        onClick = onNavigateToPostCriminal,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1E88E5)),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.height(48.dp).weight(1f)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Post Criminal",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        floatingActionButton = { },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        edgePadding = 0.dp,
                        indicator = {},
                        divider = {}
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
                                        color = if (selected) Color(0xFF2196F3).copy(alpha = 0.8f) else Color(0xFF2196F3).copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        if (selected) Color(0xFF2196F3).copy(alpha = 0.85f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    ),
                                text = {
                                    val badgeCount = when (title) {
                                        "Approvals" -> pendingUsers.size + pendingCriminals.size
                                        "Reports" -> activeReports.count { it.status.uppercase() == "PENDING" }
                                        "Tips" -> allTips.count { !it.isReviewed }
                                        "Wanted" -> pendingCriminals.size
                                        "Support" -> allHelpMessages.count { !it.isRead }
                                        else -> 0
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Visible,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (badgeCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = Color.Red,
                                                        shape = CircleShape
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = badgeCount.toString(),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                },
                                selectedContentColor = Color.White,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        "SYSTEM NOTICES",
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
                            Text("Real-time crime statistics & trends", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text("View verified data from public perspective", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            when (selectedTab) {
                0 -> {
                    if (pendingUsers.isEmpty() && pendingCriminals.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No pending approvals",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    if (pendingUsers.isNotEmpty()) {
                        item {
                            Text(
                                "PENDING ENFORCERS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(pendingUsers, key = { it.id }) { user ->
                            UserApprovalCard(
                                user = user, 
                                onApprove = { viewModel.approveLawEnforcer(user.id) },
                                onReject = { viewModel.rejectLawEnforcer(user.id) }
                            )
                        }
                    }
                    if (pendingCriminals.isNotEmpty()) {
                        item {
                            Text(
                                "PENDING WANTED/ALERTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(pendingCriminals, key = { it.id }) { criminal ->
                            CriminalApprovalCard(
                                criminal = criminal,
                                onApprove = { viewModel.verifyWantedCriminal(criminal.id, true) },
                                onReject = { viewModel.verifyWantedCriminal(criminal.id, false) }
                            )
                        }
                    }
                }
                1 -> {
                    if (activeReports.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Text("No active reports", color = Color.Gray)
                            }
                        }
                    }
                    items(activeReports, key = { it.id }) { report ->
                        AdminReportCard(
                            report = report,
                            allUsers = allUsers,
                            onUpdateStatus = { newStatus -> viewModel.updateReportStatus(report.id, newStatus) },
                            onDelete = { viewModel.softDeleteReport(report.id) },
                            isDeleted = false,
                            onRestore = {},
                            onPermanentDelete = {},
                            onEdit = { title, desc -> viewModel.updateReport(report.id, title, desc) },
                            onNavigateToMap = { lat, lng -> onNavigateToMap(lat, lng) }
                        )
                    }
                }
                2 -> {
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
                                Text("No anonymous tips", color = Color.Gray)
                            }
                        }
                    }
                    items(allTips, key = { it.id }) { tip ->
                        AdminTipCard(
                            tip = tip,
                            allUsers = allUsers,
                            onReview = { viewModel.markTipAsReviewed(tip.id) },
                            selected = selectedTips.contains(tip.id),
                            onSelectedChange = { selected ->
                                selectedTips = if (selected) selectedTips + tip.id else selectedTips - tip.id
                            },
                            onDelete = { viewModel.softDeleteTip(tip.id) }
                        )
                    }
                }
                3 -> {
                    if (activeWantedCriminals.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Text("No active wanted criminals", color = Color.Gray)
                            }
                        }
                    }
                    items(activeWantedCriminals, key = { it.id }) { criminal ->
                        AdminWantedCriminalCard(
                            criminal = criminal,
                            onDelete = { viewModel.softDeleteWantedCriminal(criminal.id) },
                            isDeleted = false,
                            onRestore = {},
                            onPermanentDelete = {},
                            onEdit = { name, desc, lastSeen, reward -> viewModel.updateWantedCriminal(criminal.id, name, desc, lastSeen, reward) }
                        )
                    }
                }
                4 -> {
                    if (allAlerts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Text("No public alerts broadcasted", color = Color.Gray)
                            }
                        }
                    }
                    items(allAlerts, key = { it.id }) { alert ->
                        AdminAlertCard(
                            alert = alert,
                            onEdit = { title, content, urgency, locationName, lat, lng ->
                                viewModel.updateAlert(alert.id, title, content, urgency, locationName, lat, lng)
                            },
                            onDelete = {
                                viewModel.deleteAlert(alert.id)
                            }
                        )
                    }
                }
                5 -> { // Users
                    if (allUsers.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Text("No users found", color = Color.Gray)
                            }
                        }
                    }
                    items(allUsers, key = { it.id }) { user ->
                        AdminUserCard(
                            user = user,
                            onUpdateStatus = { status -> viewModel.updateUserStatus(user.id, status) },
                            onDelete = { viewModel.deleteUser(user.id) },
                            onUpdateRole = { role -> viewModel.updateUserRole(user.id, role, true) }
                        )
                    }
                }
                6 -> { // Contacts
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
                                    modifier = Modifier.fillMaxWidth(),
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
                7 -> { // Stats
                    item {
                        AdminStatsSection(
                            reports = activeReports,
                            tips = allTips,
                            wanted = activeWantedCriminals
                        )
                    }
                }
                8 -> { // Logs
                    if (allAuditLogs.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selectedLogs.size == allAuditLogs.size && allAuditLogs.isNotEmpty(),
                                        onCheckedChange = { checked ->
                                            selectedLogs = if (checked) allAuditLogs.map { it.id }.toSet() else emptySet()
                                        }
                                    )
                                    Text("Select All", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (selectedLogs.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            viewModel.softDeleteMultipleAuditLogs(selectedLogs.toList())
                                            selectedLogs = emptySet()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete (${selectedLogs.size})")
                                    }
                                }
                            }
                        }
                    }
                    if (allAuditLogs.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Text("No audit logs found", color = Color.Gray)
                            }
                        }
                    }
                    items(allAuditLogs, key = { it.id }) { log ->
                        AuditLogItem(
                            log = log,
                            selected = selectedLogs.contains(log.id),
                            onSelectedChange = { selected ->
                                selectedLogs = if (selected) selectedLogs + log.id else selectedLogs - log.id
                            },
                            onDelete = { viewModel.softDeleteAuditLog(log.id) }
                        )
                    }
                }
                9 -> { // Settings
                    item {
                        AdminSettingsSection(viewModel)
                    }
                }
                10 -> { // Trash
                    if (deletedReports.isEmpty() && deletedWantedCriminals.isEmpty() && deletedTips.isEmpty() && deletedAuditLogs.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Text("No recently deleted items", color = Color.Gray)
                            }
                        }
                    }
                    if (deletedReports.isNotEmpty()) {
                        item { Text("Deleted Reports", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp)) }
                        items(deletedReports, key = { it.id }) { report ->
                            AdminReportCard(
                                report = report,
                                allUsers = allUsers,
                                onUpdateStatus = {},
                                onDelete = {},
                                isDeleted = true,
                                onRestore = { viewModel.restoreReport(report.id) },
                                onPermanentDelete = { viewModel.permanentlyDeleteReport(report.id) },
                                onEdit = { _, _ -> },
                                onNavigateToMap = { lat, lng -> onNavigateToMap(lat, lng) }
                            )
                        }
                    }
                    if (deletedWantedCriminals.isNotEmpty()) {
                        item { Text("Deleted Wanted Criminals", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp)) }
                        items(deletedWantedCriminals, key = { it.id }) { criminal ->
                            AdminWantedCriminalCard(
                                criminal = criminal,
                                onDelete = {},
                                isDeleted = true,
                                onRestore = { viewModel.restoreWantedCriminal(criminal.id) },
                                onPermanentDelete = { viewModel.permanentlyDeleteWantedCriminal(criminal.id) },
                                onEdit = { _, _, _, _ -> }
                            )
                        }
                    }
                    if (deletedTips.isNotEmpty()) {
                        item { Text("Deleted Tips", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp)) }
                        items(deletedTips, key = { it.id }) { tip ->
                            AdminTipCard(
                                tip = tip,
                                allUsers = allUsers,
                                onReview = {},
                                isDeleted = true,
                                onRestore = { viewModel.restoreTip(tip.id) },
                                onPermanentDelete = { viewModel.permanentlyDeleteTip(tip.id) }
                            )
                        }
                    }
                    if (deletedAuditLogs.isNotEmpty()) {
                        item { Text("Deleted Audit Logs", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp)) }
                        items(deletedAuditLogs, key = { it.id }) { log ->
                            AuditLogItem(
                                log = log,
                                isDeleted = true,
                                onRestore = { viewModel.restoreAuditLog(log.id) },
                                onPermanentDelete = { viewModel.permanentlyDeleteAuditLog(log.id) }
                            )
                        }
                    }
                }
                11 -> { // Support / Help Requests
                    if (allHelpMessages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SupportAgent,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        "No support or help requests found",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                "CITIZEN HELP & ASSISTANCE REQUESTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(allHelpMessages, key = { it.id }) { msg ->
                            HelpMessageCard(
                                msg = msg,
                                onToggleRead = { viewModel.markHelpMessageAsRead(msg.id, !msg.isRead) },
                                onDelete = { viewModel.deleteHelpMessage(msg.id) }
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
    }
}

@Composable
fun AdminReportCard(
    report: Report,
    allUsers: List<User>,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit,
    isDeleted: Boolean,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
    onEdit: (String, String) -> Unit,
    onNavigateToMap: (Double, Double) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPermanentDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            onDismissRequest = { showDeleteConfirm = false },
            onConfirm = onDelete,
            title = "Move to Trash?",
            text = "Are you sure you want to move this report to the trash? It can be restored later from the 'Deleted' tab."
        )
    }

    if (showPermanentDeleteConfirm) {
        DeleteConfirmationDialog(
            onDismissRequest = { showPermanentDeleteConfirm = false },
            onConfirm = onPermanentDelete,
            title = "Permanently Delete?",
            text = "This action is irreversible. All data associated with this report will be permanently purged from the system."
        )
    }
    val reporter = allUsers.find { it.id == report.reporterId }
    val isCitizenReport = reporter?.role == UserRole.CITIZEN

    if (showEditDialog) {
        var editedTitle by remember { mutableStateOf(report.title) }
        var editedDesc by remember { mutableStateOf(report.description) }

        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Report") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Title") },
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
                    OutlinedTextField(
                        value = editedDesc,
                        onValueChange = { editedDesc = it },
                        label = { Text("Description") },
                        minLines = 3,
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
                }
            },
            confirmButton = {
                Button(onClick = {
                    onEdit(editedTitle, editedDesc)
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
            
            if (!report.isAnonymous && report.contactInfo != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { }
                        .padding(top = 4.dp, bottom = 4.dp, end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Contact Info",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Contact: ${report.contactInfo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
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
                                text = "AI FLAGGED",
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
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                report.description,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = Color(0xFF374151)
            )
            
            if (report.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                if (report.imageUrls.size == 1) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        SafeAsyncImage(
                            model = report.imageUrls[0],
                            contentDescription = "Evidence",
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
                                    contentDescription = "Evidence",
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

            Spacer(modifier = Modifier.height(24.dp))
            
            if (isDeleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onRestore) {
                        Text("Restore", color = Color(0xFF10B981))
                    }
                    Button(
                        onClick = { showPermanentDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFEE2E2),
                            contentColor = Color(0xFFB71C1C)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Delete forever",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    var expanded by remember { mutableStateOf(false) }
                    val statuses = listOf("PENDING", "ACTIVE", "VERIFIED", "RESOLVED", "ARRESTED", "FOUND", "CLOSED")
                    
                    val (backgroundColor, contentColor) = when (report.status.uppercase()) {
                        "PENDING" -> Pair(Color(0xFFFBBF24).copy(alpha = 0.15f), Color(0xFFFBBF24))
                        "ACTIVE" -> Pair(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), MaterialTheme.colorScheme.tertiary)
                        "VERIFIED" -> Pair(Color(0xFF3B82F6).copy(alpha = 0.15f), Color(0xFF3B82F6))
                        "RESOLVED" -> Pair(Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF10B981))
                        "ARRESTED" -> Pair(Color(0xFF6366F1).copy(alpha = 0.15f), Color(0xFF6366F1))
                        "FOUND" -> Pair(Color(0xFF14B8A6).copy(alpha = 0.15f), Color(0xFF14B8A6))
                        "CLOSED" -> Pair(Color(0xFF6B7280).copy(alpha = 0.15f), Color(0xFF6B7280))
                        else -> Pair(Color.Gray.copy(alpha = 0.15f), Color.Gray)
                    }

                    Box {
                        Surface(
                            onClick = { expanded = true },
                            color = backgroundColor,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = report.status.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Update Status",
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            statuses.forEach { status ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            status,
                                            fontWeight = FontWeight.Bold,
                                            color = when (status) {
                                                "PENDING" -> Color(0xFFFBBF24)
                                                "ACTIVE" -> MaterialTheme.colorScheme.tertiary
                                                "VERIFIED" -> Color(0xFF3B82F6)
                                                "RESOLVED" -> Color(0xFF10B981)
                                                "ARRESTED" -> Color(0xFF6366F1)
                                                "FOUND" -> Color(0xFF14B8A6)
                                                "CLOSED" -> Color(0xFF6B7280)
                                                else -> Color.Gray
                                            }
                                        )
                                    },
                                    onClick = {
                                        onUpdateStatus(status)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isCitizenReport) {
                            IconButton(onClick = { showEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun UserApprovalCard(user: User, onApprove: () -> Unit, onReject: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDADCE0)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(user.name, style = MaterialTheme.typography.titleLarge, fontSize = 18.sp)
                    Text(user.email, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F9FF), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VerificationRow(label = "Badge #", value = user.badgeNumber ?: "N/A")
                VerificationRow(label = "Contact", value = user.contact ?: "N/A")
                VerificationRow(label = "Address", value = user.address ?: "N/A")
                
                user.idCardUrl?.let { url ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ID Card Image:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        SafeAsyncImage(
                            model = url,
                            contentDescription = "Enforcer ID Card",
                            modifier = Modifier
                                .width(280.dp)
                                .aspectRatio(0.8f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                } ?: run {
                    VerificationRow(label = "ID Doc Ref", value = "No Image Uploaded")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reject", fontWeight = FontWeight.SemiBold)
                }
                
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Approve", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun CriminalApprovalCard(
    criminal: WantedCriminal,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
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
                            color = Color(0xFFFF3B30),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Last seen: ${criminal.lastSeen ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            criminal.description ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            
            Spacer(modifier = Modifier.height(12.dp))
            if (criminal.reward != null) {
                VerificationRow(label = "Reward", value = criminal.reward)
            }

            if (criminal.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LazyRow(
                        modifier = Modifier.wrapContentWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(criminal.imageUrls) { url ->
                            SafeAsyncImage(
                                model = url,
                                contentDescription = "Evidence",
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

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reject", fontWeight = FontWeight.SemiBold)
                }
                
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Approve", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
}

@Composable
fun VerificationRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun AdminWantedCriminalCard(
    criminal: WantedCriminal,
    onDelete: () -> Unit,
    isDeleted: Boolean,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
    onEdit: (String, String, String, String?) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPermanentDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            onDismissRequest = { showDeleteConfirm = false },
            onConfirm = onDelete,
            title = "Move to Trash?",
            text = "Are you sure you want to move this wanted person record to the trash? It can be restored later."
        )
    }

    if (showPermanentDeleteConfirm) {
        DeleteConfirmationDialog(
            onDismissRequest = { showPermanentDeleteConfirm = false },
            onConfirm = onPermanentDelete,
            title = "Permanently Delete?",
            text = "Are you sure? This will permanently remove this wanted person record from the database."
        )
    }

    if (showEditDialog) {
        var editedName by remember { mutableStateOf(criminal.name) }
        var editedDesc by remember { mutableStateOf(criminal.description) }
        var editedLastSeen by remember { mutableStateOf(criminal.lastSeen) }
        var editedReward by remember { mutableStateOf(criminal.reward ?: "") }

        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Wanted Criminal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Name") }
                    )
                    OutlinedTextField(
                        value = editedDesc,
                        onValueChange = { editedDesc = it },
                        label = { Text("Description") },
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = editedLastSeen,
                        onValueChange = { editedLastSeen = it },
                        label = { Text("Last Seen") }
                    )
                    OutlinedTextField(
                        value = editedReward,
                        onValueChange = { editedReward = it },
                        label = { Text("Reward") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onEdit(editedName, editedDesc, editedLastSeen, editedReward.ifBlank { null })
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
                            color = Color(0xFFFF3B30),
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
                
                Surface(
                    color = when((criminal.status ?: "SUBMITTED").uppercase()) {
                        "VERIFIED" -> Color(0xFF10B981).copy(alpha = 0.1f)
                        "SUBMITTED" -> Color(0xFFFBBF24).copy(alpha = 0.1f)
                        else -> Color(0xFFEF4444).copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        (criminal.status ?: "SUBMITTED").uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when((criminal.status ?: "SUBMITTED").uppercase()) {
                            "VERIFIED" -> Color(0xFF10B981)
                            "SUBMITTED" -> Color(0xFFFBBF24)
                            else -> Color(0xFFEF4444)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (isDeleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onRestore) {
                        Text("Restore", color = Color(0xFF10B981))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showPermanentDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFEE2E2),
                            contentColor = Color(0xFFB71C1C)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Delete forever",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.tertiary)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
}

@Composable
fun AdminTipCard(
    tip: Tip,
    allUsers: List<User>,
    onReview: () -> Unit,
    selected: Boolean = false,
    onSelectedChange: (Boolean) -> Unit = {},
    onDelete: () -> Unit = {},
    isDeleted: Boolean = false,
    onRestore: () -> Unit = {},
    onPermanentDelete: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDADCE0)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart).padding(end = 80.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isDeleted) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = onSelectedChange
                        )
                    }
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
                    if (!isDeleted) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            
            if (tip.aiScreeningStatus != "NOT_SCREENED") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = if (tip.aiScreeningStatus == "FLAGGED") MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else Color(0xFF10B981).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (tip.aiScreeningStatus == "FLAGGED") Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = "AI Screen",
                        tint = if (tip.aiScreeningStatus == "FLAGGED") MaterialTheme.colorScheme.error else Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (tip.aiScreeningStatus == "FLAGGED") "AI FLAGGED: ${tip.aiFlaggedReasons.joinToString(", ")}" else "AI CLEAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tip.aiScreeningStatus == "FLAGGED") MaterialTheme.colorScheme.error else Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
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

            Divider(color = Color(0xFFDADCE0), thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDeleted) {
                    Button(
                        onClick = onRestore,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f), contentColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore")
                    }
                    Button(
                        onClick = onPermanentDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Forever")
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        if (tip.isReviewed) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF10B981), RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reviewed & Notified",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "New Tip",
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

                    Button(
                        onClick = onReview,
                        enabled = !tip.isReviewed,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "Review",
                                style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

@Composable
fun SubmitAlertForm(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf(2) } // default Medium
    var locationName by remember { mutableStateOf("") }
    var selectedCounty by remember { mutableStateOf("") }
    var countyDropdownExpanded by remember { mutableStateOf(false) }
    var latitudeStr by remember { mutableStateOf("37.7749") }
    
    val counties = listOf(
        "Bomi", "Bong", "Gbarpolu", "Grand Bassa", "Grand Cape Mount", 
        "Grand Gedeh", "Grand Kru", "Lofa", "Margibi", "Maryland", 
        "Montserrado", "Nimba", "River Cess", "River Gee", "Sinoe"
    )
    var longitudeStr by remember { mutableStateOf("-122.4194") }
    
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

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
                    text = "Alert Broadcasted",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "The public emergency broadcast has been successfully encrypted and transmitted to the active civilian nodes on the network.",
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

    if (showErrorDialog) {
        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { showErrorDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Validation Error",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("Retry", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
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
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column {
                    Text(
                        "ALERT NODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "EMERGENCY BROADCAST",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1E88E5),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Alert Title") },
                placeholder = { Text("e.g. Chemical Leak Alert, Security Curfew") },
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
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = { Text("Affected Area / Location") },
                placeholder = { Text("e.g. Sector 7, Downtown Area") },
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
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
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
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .heightIn(max = 280.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = latitudeStr,
                    onValueChange = { latitudeStr = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.weight(1f),
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
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = longitudeStr,
                    onValueChange = { longitudeStr = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.weight(1f),
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
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "URGENCY THREAT LEVEL",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp),
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    1 to "Low",
                    2 to "Medium",
                    3 to "High"
                ).forEach { (level, name) ->
                    val isSelected = urgency == level
                    val cardColor = if (isSelected) {
                        when (level) {
                            1 -> Color(0xFF10B981).copy(alpha = 0.15f)
                            2 -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        }
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                    val borderColor = if (isSelected) {
                        when (level) {
                            1 -> Color(0xFF10B981)
                            2 -> Color(0xFFF59E0B)
                            else -> MaterialTheme.colorScheme.error
                        }
                    } else {
                        Color(0xFFDADCE0)
                    }
                    val textColor = if (isSelected) {
                        when (level) {
                            1 -> Color(0xFF10B981)
                            2 -> Color(0xFFF59E0B)
                            else -> MaterialTheme.colorScheme.error
                        }
                    } else {
                        Color.Gray
                    }

                    Surface(
                        onClick = { urgency = level },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        color = cardColor,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = name.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Broadcast Message Content") },
                placeholder = { Text("Enter detailed public warnings, safety protocols, and emergency guidelines...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
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

            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Please enter an alert title."
                        showErrorDialog = true
                    } else if (locationName.isBlank()) {
                        errorMessage = "Please specify the affected area or location."
                        showErrorDialog = true
                    } else if (content.isBlank()) {
                        errorMessage = "Please enter the broadcast message content."
                        showErrorDialog = true
                    } else {
                        val lat = latitudeStr.toDoubleOrNull()
                        val lon = longitudeStr.toDoubleOrNull()
                        if (lat == null || lon == null) {
                            errorMessage = "Please enter valid coordinate values for Latitude and Longitude."
                            showErrorDialog = true
                        } else {
                            val alert = Alert(
                                id = java.util.UUID.randomUUID().toString(),
                                title = title,
                                content = content,
                                urgency = urgency,
                                locationName = locationName,
                                county = selectedCounty.ifEmpty { null },
                                latitude = lat,
                                longitude = lon,
                                timestamp = System.currentTimeMillis()
                            )
                            viewModel.submitAlert(alert)
                            showSuccessDialog = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Publish",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

@Composable
fun AdminAlertCard(
    alert: Alert,
    onEdit: (String, String, Int, String, Double, Double) -> Unit,
    onDelete: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            onConfirm = onDelete,
            title = "Delete Public Alert?",
            text = "Are you sure you want to delete this alert? This action is permanent and will remove the alert from all citizen dashboards immediately."
        )
    }

    if (showEditDialog) {
        var editedTitle by remember { mutableStateOf(alert.title) }
        var editedContent by remember { mutableStateOf(alert.content) }
        var editedUrgency by remember { mutableStateOf(alert.urgency) }
        var editedLocationName by remember { mutableStateOf(alert.locationName) }
        var editedLatStr by remember { mutableStateOf(alert.latitude.toString()) }
        var editedLngStr by remember { mutableStateOf(alert.longitude.toString()) }

        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { showEditDialog = false },
            title = { 
                Text(
                    text = "Edit Public Alert", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Alert Title") },
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
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editedContent,
                        onValueChange = { editedContent = it },
                        label = { Text("Content/Instructions") },
                        minLines = 3,
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
                    
                    Text(
                        text = "Urgency Level", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(1 to "Low", 2 to "Medium", 3 to "High").forEach { (level, name) ->
                            val isSelected = editedUrgency == level
                            val chipBgColor = if (isSelected) {
                                when (level) {
                                    3 -> MaterialTheme.colorScheme.tertiary
                                    2 -> Color(0xFFF59E0B)
                                    else -> Color(0xFF38BDF8)
                                }
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                            val chipContentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(chipBgColor)
                                    .clickable { editedUrgency = level }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = chipContentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editedLocationName,
                        onValueChange = { editedLocationName = it },
                        label = { Text("Location Name") },
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
                        ),
                        singleLine = true
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editedLatStr,
                            onValueChange = { editedLatStr = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
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
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editedLngStr,
                            onValueChange = { editedLngStr = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
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
                            ),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = editedLatStr.toDoubleOrNull() ?: alert.latitude
                        val lng = editedLngStr.toDoubleOrNull() ?: alert.longitude
                        onEdit(editedTitle, editedContent, editedUrgency, editedLocationName, lat, lng)
                        showEditDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            },
            
            shape = RoundedCornerShape(12.dp)
        )
    }

    val (urgencyText, urgencyColor, urgencyIcon) = when (alert.urgency) {
        3 -> Triple("CRITICAL", MaterialTheme.colorScheme.tertiary, Icons.Default.Warning)
        2 -> Triple("WARNING", Color(0xFFF59E0B), Icons.Default.Warning)
        else -> Triple("INFO", Color(0xFF38BDF8), Icons.Default.Info)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDADCE0)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = urgencyColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, urgencyColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = urgencyIcon,
                            contentDescription = null,
                            tint = urgencyColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = urgencyText,
                            style = MaterialTheme.typography.labelSmall,
                            color = urgencyColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                val relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                    alert.timestamp,
                    System.currentTimeMillis(),
                    android.text.format.DateUtils.MINUTE_IN_MILLIS
                ).toString()
                Text(
                    text = relativeTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = alert.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = urgencyColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${alert.locationName} • ${alert.county ?: "Unknown County"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = urgencyColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Alert", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Alert", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminUserCard(
    user: User, 
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit,
    onUpdateRole: (UserRole) -> Unit
) {
    var showStatusDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    if (showDeleteConfirm) {
        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to permanently delete this user node?") },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete()
                    showDeleteConfirm = false 
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRoleDialog) {
        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { showRoleDialog = false },
            title = { Text("Change User Role") },
            text = {
                Column {
                    UserRole.entries.forEach { role ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onUpdateRole(role)
                                    showRoleDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = user.role == role, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(role.name)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDADCE0)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Profile (Left) and Status (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.profileImageUrl != null) {
                        SafeAsyncImage(
                            model = user.profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Status Badge (Top Right)
                Surface(
                    onClick = { showStatusDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    color = when(user.status) {
                        "ACTIVE" -> Color(0xFF10B981).copy(alpha = 0.1f)
                        "SUSPENDED" -> Color(0xFFFBBF24).copy(alpha = 0.1f)
                        "BANNED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else -> Color.Gray.copy(alpha = 0.1f)
                    },
                    contentColor = when(user.status) {
                        "ACTIVE" -> Color(0xFF059669)
                        "SUSPENDED" -> Color(0xFFD97706)
                        "BANNED" -> Color(0xFFC8102E)
                        else -> Color.Gray
                    }
                ) {
                    Text(
                        user.status, 
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Middle: Name and Email
            Column {
                Text(
                    user.name, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    user.email, 
                    style = MaterialTheme.typography.labelMedium, 
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom Row: Role (Left), Trust Score (Center), Delete (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Role Badge
                Surface(
                    onClick = { showRoleDialog = true },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        user.role.name, 
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Trust Score Badge
                Surface(
                    color = when {
                        user.reputationScore >= 80 -> Color(0xFF10B981).copy(alpha = 0.1f)
                        user.reputationScore >= 40 -> Color(0xFFFBBF24).copy(alpha = 0.1f)
                        else -> Color(0xFFEF4444).copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
                ) {
                    Text(
                        "TRUST SCORE: ${user.reputationScore}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = when {
                            user.reputationScore >= 80 -> Color(0xFF059669)
                            user.reputationScore >= 40 -> Color(0xFFD97706)
                            else -> Color(0xFFC8102E)
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Delete Button
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = "Delete", 
                        tint = Color.Red.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
    
    if (showStatusDialog) {
        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { showStatusDialog = false },
            title = { Text("Update User Status") },
            text = { Text("Select a new status for ${user.name}.") },
            confirmButton = {
                Column {
                    TextButton(onClick = { onUpdateStatus("ACTIVE"); showStatusDialog = false }) { Text("ACTIVE") }
                    TextButton(onClick = { onUpdateStatus("SUSPENDED"); showStatusDialog = false }) { Text("SUSPENDED") }
                    TextButton(onClick = { onUpdateStatus("BANNED"); showStatusDialog = false }) { Text("BANNED") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatusDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AdminStatsSection(reports: List<Report>, tips: List<Tip>, wanted: List<WantedCriminal>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Reporting Statistics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox(label = "Total Reports", count = reports.size.toString(), color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            StatBox(label = "Total Tips", count = tips.size.toString(), color = Color(0xFF14B8A6), modifier = Modifier.weight(1f))
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox(label = "Wanted Persons", count = wanted.size.toString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            val flaggedCount = reports.count { it.aiScreeningStatus == "FLAGGED" } + tips.count { it.aiScreeningStatus == "FLAGGED" }
            StatBox(label = "AI Flagged", count = flaggedCount.toString(), color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Reports by Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val statusCounts = reports.groupBy { it.status }.mapValues { it.value.size }
                if (statusCounts.isEmpty()) {
                    Text(
                        "No report status data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    statusCounts.forEach { (status, count) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(status, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = if (reports.isNotEmpty()) count.toFloat() / reports.size else 0f,
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun AuditLogItem(
    log: AuditLog,
    selected: Boolean = false,
    onSelectedChange: (Boolean) -> Unit = {},
    onDelete: () -> Unit = {},
    isDeleted: Boolean = false,
    onRestore: () -> Unit = {},
    onPermanentDelete: () -> Unit = {}
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            onDismissRequest = { showDeleteConfirm = false },
            onConfirm = onDelete,
            title = "Delete Audit Log?",
            text = "Are you sure you want to delete this specific audit log entry? This will move it to the trash."
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (!isDeleted) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = onSelectedChange,
                            modifier = Modifier.size(24.dp).padding(end = 8.dp)
                        )
                    }
                    Text(
                        log.action, 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                if (isDeleted) {
                    Row {
                        IconButton(onClick = onRestore, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore", modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onPermanentDelete, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete Forever", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Log",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(log.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(log.userName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AdminSettingsSection(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showAiConfigDialog by remember { mutableStateOf(false) }
    var showMaintenanceDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("System Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        
        SettingItem(
            title = "App Categories", 
            icon = Icons.Default.Category, 
            description = "Manage report and incident categories",
            onClick = { showCategoryDialog = true }
        )
        SettingItem(
            title = "Security Settings", 
            icon = Icons.Default.Security, 
            description = "Configure 2FA and password policies",
            onClick = { showSecurityDialog = true }
        )
        SettingItem(
            title = "AI Configuration", 
            icon = Icons.Default.SmartToy, 
            description = "Adjust AI screening sensitivity",
            onClick = { showAiConfigDialog = true }
        )
        SettingItem(
            title = "Cloud Synchronization", 
            icon = Icons.Default.Sync, 
            description = "Force bi-directional database sync",
            onClick = { 
                viewModel.syncNow()
                android.widget.Toast.makeText(context, "Synchronization started...", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
        SettingItem(
            title = "System Maintenance", 
            icon = Icons.Default.Settings, 
            description = "Perform database backups and cleanup",
            onClick = { showMaintenanceDialog = true }
        )
    }

    if (showCategoryDialog) {
        val currentEnabledCategories by viewModel.enabledCategories.collectAsState()
        var selectedCategories by remember { mutableStateOf(currentEnabledCategories) }
        val context = LocalContext.current

        AlertDialog(
            modifier = Modifier.fillMaxWidth(0.95f).widthIn(max = 420.dp),
            containerColor = Color(0xFFEEEEEE),
            titleContentColor = Color(0xFF212121),
            textContentColor = Color(0xFF212121),
            onDismissRequest = { showCategoryDialog = false },
            title = { 
                Column {
                    Text("App Categories Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Select categories enabled for viewing and submitting tips", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { selectedCategories = com.example.data.model.ReportType.entries.toSet() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Select All", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = { selectedCategories = emptySet() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Deselect All", fontSize = 12.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            com.example.data.model.ReportType.entries.forEach { type ->
                                val isSelected = selectedCategories.contains(type)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCategories = if (isSelected) {
                                                selectedCategories - type
                                            } else {
                                                selectedCategories + type
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedCategories = if (checked == true) {
                                                selectedCategories + type
                                            } else {
                                                selectedCategories - type
                                            }
                                        }
                                    )
                                }
                                if (type != com.example.data.model.ReportType.entries.last()) {
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Configuration Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Text("Active Categories: ${selectedCategories.size} of ${com.example.data.model.ReportType.entries.size}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = { 
                Button(onClick = { 
                    viewModel.updateEnabledCategories(selectedCategories)
                    android.widget.Toast.makeText(context, "Successfully updated settings for ${selectedCategories.size} categories", android.widget.Toast.LENGTH_SHORT).show()
                    showCategoryDialog = false 
                }) { Text("Save") } 
            },
            dismissButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("Cancel") } }
        )
    }

    if (showSecurityDialog) {
        val currentTwoFactor by viewModel.isTwoFactorEnabled.collectAsStateWithLifecycle()
        val currentExpiry by viewModel.passwordExpiry.collectAsStateWithLifecycle()
        var isTwoFactorEnabled by remember(currentTwoFactor) { mutableStateOf(currentTwoFactor) }
        var passwordExpiry by remember(currentExpiry) { mutableStateOf(currentExpiry) }
        val context = LocalContext.current
        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { showSecurityDialog = false },
            title = { Text("Security Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Mandatory 2FA")
                        Switch(checked = isTwoFactorEnabled, onCheckedChange = { isTwoFactorEnabled = it })
                    }
                    Column {
                        Text("Password Expiry (Days): $passwordExpiry")
                        Slider(value = passwordExpiry.toFloat(), onValueChange = { passwordExpiry = it.toInt() }, valueRange = 30f..365f)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)
                    Text("Test Password Expiry (Simulation)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Simulate when your password was last changed to test the automatic lockout flow:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    var simAgeDays by remember { mutableStateOf(0) }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Age: $simAgeDays days ago", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.2f))
                        Slider(
                            value = simAgeDays.toFloat(),
                            onValueChange = { simAgeDays = it.toInt() },
                            valueRange = 0f..365f,
                            modifier = Modifier.weight(2f)
                        )
                    }
                    Button(
                        onClick = {
                            currentUser?.let { user ->
                                val targetTime = System.currentTimeMillis() - (simAgeDays * 24 * 60 * 60 * 1000L)
                                viewModel.setPasswordLastChanged(user.id, targetTime)
                                android.widget.Toast.makeText(context, "Simulated password last changed to $simAgeDays days ago successfully!", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Simulation Age", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            confirmButton = { 
                Button(onClick = { 
                    viewModel.updateSecuritySettings(isTwoFactorEnabled, passwordExpiry)
                    android.widget.Toast.makeText(context, "Security settings saved successfully (2FA: $isTwoFactorEnabled, Expiry: ${passwordExpiry}d)", android.widget.Toast.LENGTH_SHORT).show()
                    showSecurityDialog = false 
                }) { Text("Save Changes") } 
            },
            dismissButton = { TextButton(onClick = { showSecurityDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAiConfigDialog) {
        val currentSensitivity by viewModel.aiSensitivity.collectAsStateWithLifecycle()
        var sensitivity by remember(currentSensitivity) { mutableStateOf(currentSensitivity) }
        val context = LocalContext.current
        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { showAiConfigDialog = false },
            title = { Text("AI Configuration") },
            text = {
                Column {
                    Text("Screening Sensitivity: ${(sensitivity * 100).toInt()}%")
                    Slider(value = sensitivity, onValueChange = { sensitivity = it })
                    Text(
                        "Higher sensitivity may result in more false positives but ensures higher safety screening.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = { 
                Button(onClick = { 
                    viewModel.updateAiSensitivity(sensitivity)
                    viewModel.logAction("AI_CONFIG_UPDATED", "Updated screening sensitivity to ${(sensitivity * 100).toInt()}%")
                    android.widget.Toast.makeText(context, "AI configuration updated successfully (${(sensitivity * 100).toInt()}%)", android.widget.Toast.LENGTH_SHORT).show()
                    showAiConfigDialog = false 
                }) { Text("Apply") } 
            },
            dismissButton = { TextButton(onClick = { showAiConfigDialog = false }) { Text("Cancel") } }
        )
    }

    if (showMaintenanceDialog) {
        var maintenanceInProgress by remember { mutableStateOf(false) }
        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { if (!maintenanceInProgress) showMaintenanceDialog = false },
            title = { Text("System Maintenance") },
            text = {
                if (maintenanceInProgress) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Maintenance in progress...")
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { 
                                maintenanceInProgress = true
                                // Simulate task
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Backup, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Full Database Backup")
                        }
                        Button(
                            onClick = { 
                                viewModel.logAction("LOGS_CLEANUP", "Cleaned up old audit logs")
                                showMaintenanceDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors()
) {
                            Icon(Icons.Default.DeleteSweep, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cleanup Old Logs")
                        }
                    }
                }
            },
            confirmButton = { 
                if (!maintenanceInProgress) {
                    TextButton(onClick = { showMaintenanceDialog = false }) { Text("Close") }
                }
            }
        )
        
        if (maintenanceInProgress) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                viewModel.logAction("DATABASE_BACKUP", "Completed full system backup")
                maintenanceInProgress = false
                showMaintenanceDialog = false
            }
        }
    }
}

@Composable
fun SettingItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun HelpMessageCard(
    msg: HelpMessage,
    onToggleRead: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val timeString = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", msg.timestamp).toString()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (msg.isRead) Color(0xFFF5F9FF) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (msg.isRead) Color(0xFFDADCE0) else Color(0xFF1E88E5)
        ),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                             .size(36.dp)
                             .background(
                                 if (msg.isRead) Color(0xFFE3F2FD) else Color(0xFFFFEBEE),
                                 CircleShape
                             ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (msg.isRead) Icons.Default.MarkEmailRead else Icons.Default.MarkEmailUnread,
                            contentDescription = null,
                            tint = if (msg.isRead) Color(0xFF1E88E5) else Color(0xFFD32F2F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = msg.senderName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        Text(
                            text = msg.senderEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF616161)
                        )
                    }
                }
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF616161)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Message
            Text(
                text = msg.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF212121),
                modifier = Modifier.padding(start = 4.dp)
            )

            if (msg.senderContact.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Contact: ${msg.senderContact}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFFDADCE0))
            Spacer(modifier = Modifier.height(8.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Email Reply action
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${msg.senderEmail}")
                                putExtra(Intent.EXTRA_SUBJECT, "TraceNet Admin Support Reply")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = "Reply", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }

                    // Call action
                    if (msg.senderContact.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${msg.senderContact}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Mark Read Toggle
                    TextButton(
                        onClick = onToggleRead,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (msg.isRead) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (msg.isRead) "Mark Unread" else "Mark Read",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Message", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
