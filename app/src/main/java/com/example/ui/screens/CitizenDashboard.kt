package com.example.ui.screens

import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import com.example.ui.components.LiberiaFlag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.example.data.model.Report
import com.example.data.model.WantedCriminal
import com.example.ui.MainViewModel
import androidx.compose.foundation.lazy.LazyRow
import coil.compose.AsyncImage
import com.example.ui.components.SafeAsyncImage
import com.example.ui.components.DeleteConfirmationDialog
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable

import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.R

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import com.example.data.model.Tip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle

import com.example.data.model.UserRole
import com.example.ui.components.DashboardCarousel
import com.example.ui.components.VideoPlayer
import com.example.ui.components.AudioPlayer

@Composable
fun CitizenDashboard(
    viewModel: MainViewModel,
    onNavigateToMap: (Double?, Double?) -> Unit,
    onNavigateToSOS: () -> Unit,
    onNavigateToSubmitReport: () -> Unit,
    onNavigateToSubmitTip: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToPublicViewing: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToEmergencyContacts: () -> Unit,
    onNavigateToLawEnforcer: () -> Unit = {}
) {
    val reports by viewModel.activeReports.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser) {
        if (currentUser?.isApproved == true && (currentUser?.role == UserRole.LAW_ENFORCER || currentUser?.role == UserRole.ADMIN)) {
            onNavigateToLawEnforcer()
        }
    }
    val userReports = remember(reports, currentUser) {
        reports.filter { it.reporterId == currentUser?.id }
    }
    val alerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    val criminals by viewModel.wantedCriminals.collectAsStateWithLifecycle()
    val userTips by viewModel.userTips.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(0) } // 0 = Home, 1 = Alerts
    var dismissedTipNotificationIds by remember { mutableStateOf(setOf<String>()) }
    var selectedAlertsSubTab by remember { mutableStateOf(0) } // 0 = Public Alerts, 1 = My Secure Tips
    var showSOSConfirmationDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    if (showShareDialog) {
        ShareAppDialog(onDismiss = { showShareDialog = false })
    }

    Scaffold(
        
        topBar = {
            Column {
                if (currentUser?.role == UserRole.LAW_ENFORCER && !currentUser!!.isApproved) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Account pending verification. You will be redirected to Enforcer Dashboard once approved.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
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
                        .padding(horizontal = 10.dp, vertical = 20.dp)
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
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
                            style = MaterialTheme.typography.headlineMedium,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    IconButton(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share App",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    stringResource(R.string.app_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    },
    bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E88E5),
                contentColor = Color.White,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { 
                        BadgedBox(
                            badge = {
                                val reviewedNotDismissedCount = userTips.count { it.isReviewed && !dismissedTipNotificationIds.contains(it.id) }
                                if (reviewedNotDismissedCount > 0) {
                                    Badge(containerColor = Color.Red) { Text("$reviewedNotDismissedCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    },
                    label = { Text("Alerts", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showSOSConfirmationDialog = true },
                    icon = { Icon(Icons.Default.Warning, contentDescription = "SOS") },
                    label = { Text("SOS", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = Color.White.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigateToMap(null, null) },
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text("Map", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToProfile,
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Profile", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToSubmitReport,
                containerColor = Color(0xFF1E88E5),
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Report", style = MaterialTheme.typography.labelSmall)
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
            if (currentTab == 0) {
                item {
                    AnalyticsCard(onClick = onNavigateToAnalytics)
                }
                item {
                    Surface(
                        onClick = onNavigateToPublicViewing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
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
                                Text("Official alerts and verified incidents", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item {
                    Surface(
                        onClick = onNavigateToEmergencyContacts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3B82F6).copy(alpha = 0.6f)),
                        contentColor = Color.White
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Red)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Emergency Directory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Red)
                                    Text("Verified law enforcement & rescue contacts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Red)
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 12.dp, end = 12.dp)
                                    .background(Color.Red, shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("OFFICIAL", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                item {
                    Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                        Text(
                            "FEATURED INSIGHTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 12.dp)) {
                                DashboardCarousel()
                            }
                        }
                    }
                }
                val activeNotifications = userTips.filter { it.isReviewed && !dismissedTipNotificationIds.contains(it.id) }
                activeNotifications.forEach { tip ->
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            color = Color.Red.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notification",
                                    tint = Color.Red,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "SECURE TIP REVIEWED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Thanks, your secure tip has been reviewed, and the appropriate team will get in touch with you soon",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tip ref: #${tip.id.take(8).uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        dismissedTipNotificationIds = dismissedTipNotificationIds + tip.id
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = "Active Alerts",
                            value = "${alerts.size}",
                            valueColor = Color.Red,
                            subValue = "+${alerts.filter { it.urgency >= 2 }.size} major",
                            subValueColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Watch Level",
                            value = if (alerts.any { it.urgency == 3 }) "High" else "Low",
                            subValue = "Normal",
                            valueColor = if (alerts.any { it.urgency == 3 }) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    AnonymousTipCard(onClick = onNavigateToSubmitTip)
                }

                if (userReports.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("You haven't submitted any reports yet", color = Color.Gray)
                        }
                    }
                } else {
                    items(userReports, key = { it.id }) { report ->
                        ReportCard(
                            report = report,
                            onDelete = { viewModel.softDeleteReport(report.id) },
                            onEdit = { title, desc -> viewModel.updateReport(report.id, title, desc) },
                            onNavigateToMap = { lat, lng -> onNavigateToMap(lat, lng) }
                        )
                    }
                }
            } else {
                // Alerts tab
                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            "ACTIVE BROADCASTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Emergency & Safety Alerts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    }
                }

                item {
                    TabRow(
                        selectedTabIndex = selectedAlertsSubTab,
                        
                        divider = {},
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Tab(
                            selected = selectedAlertsSubTab == 0,
                            onClick = { selectedAlertsSubTab = 0 },
                            text = { Text("Public Alerts") },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = Color.Gray
                        )
                        Tab(
                            selected = selectedAlertsSubTab == 1,
                            onClick = { selectedAlertsSubTab = 1 },
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("My Secure Tips")
                                    val reviewedNotDismissedCount = userTips.count { it.isReviewed && !dismissedTipNotificationIds.contains(it.id) }
                                    if (reviewedNotDismissedCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "$reviewedNotDismissedCount",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = Color.Gray
                        )
                    }
                }

                if (selectedAlertsSubTab == 0) {
                    if (alerts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No current emergency broadcasts", color = Color.Gray)
                            }
                        }
                    } else {
                        items(alerts, key = { it.id }) { alert ->
                            AlertCard(alert = alert)
                        }
                    }
                } else {
                    val activeNotifications = userTips.filter { it.isReviewed && !dismissedTipNotificationIds.contains(it.id) }
                    
                    if (activeNotifications.isNotEmpty()) {
                        items(activeNotifications, key = { it.id }) { tip ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                color = Color.Red.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notification",
                                        tint = Color.Red,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "SECURE TIP REVIEWED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Red,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Thanks, your secure tip has been reviewed, and the appropriate team will get in touch with you soon",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tip ref: #${tip.id.take(8).uppercase()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            dismissedTipNotificationIds = dismissedTipNotificationIds + tip.id
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (userTips.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No secure tips sent yet", color = Color.Gray)
                                }
                            }
                        }
                    } else {
                        items(userTips, key = { it.id }) { tip ->
                            CitizenTipStatusCard(tip = tip)
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showSOSConfirmationDialog) {
        AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



            onDismissRequest = { showSOSConfirmationDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Red
                    )
                    Text(
                        text = "Activate SOS Alert?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to broadcast an emergency distress signal? This will immediately alert active nodes and response units.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSOSConfirmationDialog = false
                        onNavigateToSOS()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    )
                ) {
                    Text("Broadcast SOS", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSOSConfirmationDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            
            tonalElevation = 6.dp
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    subValue: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
    subValueColor: Color = Color.Gray
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor)
                Spacer(modifier = Modifier.width(6.dp))
                Text(subValue, style = MaterialTheme.typography.labelSmall, color = subValueColor, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
fun AnalyticsCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp),
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
                Text("View crime rates and statistics nationwide", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AnonymousTipCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1E88E5),
        shape = RoundedCornerShape(32.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Submit Anonymous Tip", style = MaterialTheme.typography.titleLarge, fontSize = 14.sp, color = Color.White)
                Text(
                    "Help local authorities maintain community safety",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun CriminalCard(criminal: WantedCriminal, onTipClick: () -> Unit) {
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
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            criminal.name.uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFFF3B30),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Last seen: ${criminal.lastSeen}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            criminal.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.Black
                        )
                    }
                }
            
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
            
            if (criminal.reward != null) {
                Spacer(modifier = Modifier.height(12.dp))
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
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onTipClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Text("SUBMIT ANONYMOUS TIP")
            }
        }
    }
}
}

@Composable
fun AlertCard(alert: com.example.data.model.Alert) {
    val (urgencyText, urgencyColor, urgencyIcon) = when (alert.urgency) {
        3 -> Triple("CRITICAL", Color(0xFFEF4444), Icons.Default.Warning)
        2 -> Triple("WARNING", Color(0xFFF59E0B), Icons.Default.Warning)
        else -> Triple("INFO", Color(0xFF38BDF8), Icons.Default.Info)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, urgencyColor.copy(alpha = 0.3f))
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
        }
    }
}

@Composable
fun ReportCard(
    report: Report,
    onDelete: () -> Unit,
    onEdit: (String, String) -> Unit,
    onNavigateToMap: (Double, Double) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            onDismissRequest = { showDeleteConfirm = false },
            onConfirm = onDelete,
            title = "Delete Report?",
            text = "Are you sure you want to delete this report? This will move it to the trash."
        )
    }

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
                Text(report.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    report.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF374151)
                )
            
            if (report.latitude != 0.0 || report.longitude != 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
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
            
            if (report.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                if (report.imageUrls.size == 1) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        SafeAsyncImage(
                            model = report.imageUrls[0],
                            contentDescription = "Report Image",
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
                                    contentDescription = "Report Image",
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
                    color = MaterialTheme.colorScheme.onSurface,
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
                                    .aspectRatio(0.8f),
                                isLazy = true
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
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    report.audioUrls.forEach { url ->
                        AudioPlayer(audioUrl = url, isLazy = true)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Status:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusBadge(status = report.status)
            }

            // Report Response Link - Instant Immediate Assistance Contact
            val context = androidx.compose.ui.platform.LocalContext.current
            val matchingContact = remember(report.county) {
                val list = try {
                    com.example.data.model.VerifiedContacts.list
                } catch(e: Exception) {
                    emptyList()
                }
                val matched = list.firstOrNull {
                    it.county.equals(report.county, ignoreCase = true) && it.category == "Police"
                }
                matched ?: list.firstOrNull { it.category == "Police" }
            }

            if (matchingContact != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "Need immediate assistance?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Call Button
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
                        modifier = Modifier.weight(1.2f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone, 
                            contentDescription = "Call", 
                            tint = if (phoneIsAvailable) Color(0xFF1565C0) else Color(0xFF94A3B8), 
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val cleanOfficeName = matchingContact.officeName
                            .replace("Liberia National Police (LNP) ", "")
                            .replace("County Command", "Police")
                        Text(
                            "Call $cleanOfficeName", 
                            style = MaterialTheme.typography.labelSmall, 
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (phoneIsAvailable) Color(0xFF1565C0) else Color(0xFF94A3B8),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // WhatsApp Button
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
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Send, 
                            contentDescription = "WhatsApp", 
                            tint = if (waIsAvailable) Color(0xFF15803D) else Color(0xFF94A3B8), 
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "WhatsApp", 
                            style = MaterialTheme.typography.labelSmall, 
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (waIsAvailable) Color(0xFF15803D) else Color(0xFF94A3B8),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
}

@Composable
fun StatusBadge(status: String) {
    val (backgroundColor, contentColor) = when (status.uppercase()) {
        "PENDING" -> Pair(Color(0xFFFBBF24).copy(alpha = 0.1f), Color(0xFFD97706))
        "ACTIVE" -> Pair(Color(0xFFEF4444).copy(alpha = 0.1f), Color(0xFFC8102E))
        "VERIFIED" -> Pair(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), MaterialTheme.colorScheme.primary)
        "RESOLVED" -> Pair(Color(0xFF10B981).copy(alpha = 0.1f), Color(0xFF059669))
        "ARRESTED" -> Pair(Color(0xFF6366F1).copy(alpha = 0.1f), Color(0xFF4F46E5))
        "FOUND" -> Pair(Color(0xFF14B8A6).copy(alpha = 0.1f), Color(0xFF0D9488))
        "CLOSED" -> Pair(Color(0xFF64748B).copy(alpha = 0.1f), Color(0xFF475569))
        else -> Pair(Color.Gray.copy(alpha = 0.1f), Color.Gray)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
fun CitizenTipStatusCard(tip: Tip) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3B82F6).copy(alpha = 0.6f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart).padding(end = 80.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                (if (tip.isReviewed) Color(0xFF10B981) else Color(0xFFF59E0B)).copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (tip.isReviewed) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (tip.isReviewed) Color(0xFF059669) else Color(0xFFD97706)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (tip.isReviewed) "TIP REVIEWED" else "PENDING REVIEW",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (tip.isReviewed) Color(0xFF059669) else Color(0xFFD97706),
                        fontSize = 13.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Tip Content:",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tip.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (tip.aiScreeningStatus != "NOT_SCREENED") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = if (tip.aiScreeningStatus == "FLAGGED") Color(0xFFEF4444).copy(alpha = 0.1f) else Color(0xFF10B981).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (tip.aiScreeningStatus == "FLAGGED") Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = "AI Screen",
                        tint = if (tip.aiScreeningStatus == "FLAGGED") Color(0xFFEF4444) else Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (tip.aiScreeningStatus == "FLAGGED") "AI FLAGGED: ${tip.aiFlaggedReasons.joinToString(", ")}" else "AI CLEAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tip.aiScreeningStatus == "FLAGGED") Color(0xFFEF4444) else Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (tip.isReviewed) Color(0xFF10B981) else Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (tip.isReviewed) "Reviewed & Action Taken" else "Pending Review",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (tip.isReviewed) Color(0xFF10B981) else Color(0xFFF59E0B),
                        fontWeight = FontWeight.Medium
                    )
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (tip.isReviewed) {
                    "Thanks, your secure tip has been reviewed, and the appropriate team will get in touch with you soon"
                } else {
                    "Your transmission is securely queued in TraceNet Liberia's command network."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (tip.isReviewed) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }
    }
}
