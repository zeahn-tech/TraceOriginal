package com.example.ui.screens

import com.example.ui.toTraceNetLiberiaAnnotatedString

import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.SafeAsyncImage
import com.example.ui.components.LiberiaFlag
import com.example.ui.components.VideoPlayer
import com.example.ui.components.AudioPlayer
import com.example.data.model.Alert
import com.example.data.model.Report
import com.example.data.model.ReportType
import com.example.ui.MainViewModel
import com.example.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class PublicAlertSlide(
    val category: String,
    val title: String,
    val description: String,
    val imageResId: Int,
    val color: Color
)

@Composable
fun PublicSlidingCarousel(modifier: Modifier = Modifier) {
    val items = remember {
        listOf(
            PublicAlertSlide(
                category = "CRIME SCENE INVESTIGATION",
                title = "Evidence Cordon & Preservation",
                description = "Secured cordons established around high-priority incident perimeters to safeguard physical forensics and guide forensic mapping.",
                imageResId = R.drawable.img_crime_scene_1783453712511,
                color = Color(0xFFFF3B30)
            ),
            PublicAlertSlide(
                category = "HOMICIDE PREVENTION",
                title = "Forensic Scanning Sweeps",
                description = "Advanced 3D laser scanners deployed in active forensic incident investigations to gather evidence and trace suspects.",
                imageResId = R.drawable.img_forensic_investigation_1783448806634,
                color = Color(0xFFFF3B30)
            ),
            PublicAlertSlide(
                category = "THEFT DETECTION",
                title = "AI Smart City CCTV",
                description = "Real-time surveillance cameras tracking night-time burglary movements and vehicle theft in residential clusters.",
                imageResId = R.drawable.img_theft_monitoring_1783448820563,
                color = Color(0xFF3B82F6)
            ),
            PublicAlertSlide(
                category = "VICTIM CRITICAL SUPPORT",
                title = "Emergency Help Kiosks Active",
                description = "Instant support pillars and emergency safety response corridors established in high-priority public areas.",
                imageResId = R.drawable.img_emergency_response_1783448835242,
                color = Color(0xFFEC4899)
            ),
            PublicAlertSlide(
                category = "ARMED ROBBERY WARNING",
                title = "Rapid Enforcer Patrolling",
                description = "High-tech rapid interceptors on high-alert monitoring near transit stops, financial institutions, and business avenues.",
                imageResId = R.drawable.img_robbery_intercept_1783448848720,
                color = Color(0xFFF59E0B)
            ),
            PublicAlertSlide(
                category = "KIDNAPPING RESPONSE",
                title = "Tactical Search & Rescue Swarm",
                description = "Overhead drone scanning grids and emergency response teams active in child protection search-and-rescue sweeps.",
                imageResId = R.drawable.img_tactical_rescue_1783448861647,
                color = Color(0xFF8B5CF6)
            ),
            PublicAlertSlide(
                category = "CYBERCRIME PREVENTION",
                title = "Yahoo Boy & Phishing Tracker",
                description = "Tracing active internet fraud rings, credential harvesting, business email compromise, and wire transfer scams.",
                imageResId = R.drawable.img_cybercrime_tracker_1783448875037,
                color = Color(0xFF10B981)
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000L)
            val nextPage = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            pageSpacing = 12.dp
        ) { page ->
            CarouselSlideCard(item = items[page])
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Pager indicator
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(items.size) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(width = if (active) 16.dp else 6.dp, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                )
            }
        }
    }
}

@Composable
fun CarouselSlideCard(item: PublicAlertSlide) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = item.imageResId),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x1A1E40AF), // Subtle blue tint to keep top of image bright and recognizable
                                Color(0xAA0B1E43)  // Elegant blue/navy tint at the bottom for text readability
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Surface(
                    color = item.color,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = item.category,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicViewingScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToSubmitTip: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToMap: (Double, Double) -> Unit
) {
    val allReports by viewModel.allReports.collectAsState()
    val allAlerts by viewModel.allAlerts.collectAsState()
    val criminals by viewModel.wantedCriminals.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState(null)
    val enabledCategories by viewModel.enabledCategories.collectAsState()
    val isUserLoggedIn = currentUser != null

    // Verified: Status is VERIFIED
    val verifiedReports = allReports.filter { it.status == "VERIFIED" && !it.isDeleted }
    
    // Filter alerts (Safety advisories, Emergency notices, Community announcements)
    // In this app, Alerts are already official.
    val officialAlerts = allAlerts

    var showShareDialog by remember { mutableStateOf(false) }

    if (showShareDialog) {
        ShareAppDialog(onDismiss = { showShareDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        LiberiaFlag(modifier = Modifier.padding(bottom = 2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Black)) {
                                        append("TRACENET")
                                    }
                                    append(" ")
                                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f))) {
                                        append("LIBERIA")
                                    }
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                letterSpacing = (-0.5).sp
                            )
                        }
                        Text(
                            "Verified Safety Information", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share App",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Liberia National Police Logo Card
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .width(350.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_liberia_police_logo_1784771614492),
                                contentDescription = "Liberia National Police Logo",
                                modifier = Modifier
                                    .size(90.dp),
                                contentScale = ContentScale.Fit
                            )
                            val fullDescription = stringResource(R.string.app_description)
                            val annotatedDescription = buildAnnotatedString {
                                val target = "TraceNet"
                                if (fullDescription.startsWith(target)) {
                                    withStyle(SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                                        append(target)
                                    }
                                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                        append(fullDescription.substring(target.length))
                                    }
                                } else {
                                    val index = fullDescription.indexOf(target)
                                    if (index == -1) {
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                            append(fullDescription)
                                        }
                                    } else {
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                            append(fullDescription.substring(0, index))
                                        }
                                        withStyle(SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                                            append(target)
                                        }
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                            append(fullDescription.substring(index + target.length))
                                        }
                                    }
                                }
                            }
                            Text(
                                text = annotatedDescription,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary
                    ),
                    onClick = onNavigateToAnalytics
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "NATIONAL ANALYTICS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "View live statistics, crime trends, and response metrics.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "View Analytics",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Text(
                    "Global Safety Watch",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                PublicSlidingCarousel(modifier = Modifier.fillMaxWidth())
            }

            if (officialAlerts.isNotEmpty()) {
                item {
                    Text(
                        "Official Announcements",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(officialAlerts.sortedByDescending { it.timestamp }, key = { it.id }) { alert ->
                    AlertItem(alert)
                }
            }

            if (criminals.isNotEmpty()) {
                items(criminals, key = { it.id }) { criminal ->
                    WantedPersonItem(criminal, isUserLoggedIn, enabledCategories, onNavigateToSubmitTip)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "INCIDENT & MISSING PERSON",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            if (verifiedReports.isEmpty()) {
                item {
                    EmptyState(message = "No verified reports available.")
                }
            } else {
                items(verifiedReports.sortedByDescending { it.timestamp }, key = { it.id }) { report ->
                    VerifiedReportItem(report, isUserLoggedIn, enabledCategories, onNavigateToSubmitTip, onNavigateToMap)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                DisclaimerSection()
            }
        }
    }
}

@Composable
fun AlertItem(alert: Alert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E88E5).copy(alpha = 0.12f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFF1E88E5).copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (alert.urgency >= 2) Icons.Default.Warning else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (alert.urgency >= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    alert.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                alert.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    alert.locationName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(alert.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun FlashingGreenIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier.size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .alpha(alpha * 0.4f)
                .background(Color(0xFF22C55E), shape = CircleShape)
        )
        // Inner solid dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color(0xFF22C55E), shape = CircleShape)
        )
    }
}

@Composable
fun VerifiedReportItem(
    report: Report,
    isUserLoggedIn: Boolean,
    enabledCategories: Set<com.example.data.model.ReportType>,
    onNavigateToSubmitTip: () -> Unit,
    onNavigateToMap: (Double, Double) -> Unit
) {
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
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlashingGreenIndicator()
                    Text(
                        text = "ACTIVE & VERIFIED INCIDENT REPORT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF10B981)
                    )
                }

                Text(
                    report.title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                report.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF374151),
                maxLines = 5
            )

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
            
            if (report.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                if (report.imageUrls.size == 1) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        SafeAsyncImage(
                            model = report.imageUrls[0],
                            contentDescription = "Photo Evidence",
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
                                    contentDescription = "Photo Evidence",
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Surface(
                        color = when(report.status) {
                            "RESOLVED", "VERIFIED" -> Color(0xFF10B981).copy(alpha = 0.15f)
                            "UNDER_INVESTIGATION" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = when(report.status) {
                                "RESOLVED", "VERIFIED" -> Color(0xFF10B981).copy(alpha = 0.5f)
                                else -> Color.Transparent
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                report.status,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when(report.status) {
                                    "RESOLVED", "VERIFIED" -> Color(0xFF10B981)
                                    "UNDER_INVESTIGATION" -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date Reported",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(report.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
                
                if (isUserLoggedIn) {
                    val isCategoryEnabled = enabledCategories.contains(report.type)
                    Button(
                        onClick = onNavigateToSubmitTip,
                        enabled = isCategoryEnabled,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCategoryEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.3f),
                            contentColor = if (isCategoryEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                            disabledContainerColor = Color.LightGray.copy(alpha = 0.3f),
                            disabledContentColor = Color.Gray
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCategoryEnabled) "SUBMIT TIP" else "TIPS CLOSED", 
                            style = MaterialTheme.typography.labelSmall, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun WantedPersonItem(
    criminal: com.example.data.model.WantedCriminal,
    isUserLoggedIn: Boolean,
    enabledCategories: Set<com.example.data.model.ReportType>,
    onNavigateToSubmitTip: () -> Unit
) {
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
                if (currentStatus == "ACTIVE" || currentStatus == "VERIFIED") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlashingGreenIndicator()
                        Text(
                            text = "ACTIVE & VERIFIED WANTED NOTICE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFF10B981)
                        )
                    }
                }

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
                    
                    Surface(
                        color = if (currentStatus == "ACTIVE" || currentStatus == "VERIFIED") Color(0xFF10B981).copy(alpha = 0.15f) else statusColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (currentStatus == "ACTIVE" || currentStatus == "VERIFIED") Color(0xFF10B981).copy(alpha = 0.5f) else statusColor.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentStatus == "ACTIVE") "ACTIVE & VERIFIED" else currentStatus,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (currentStatus == "ACTIVE" || currentStatus == "VERIFIED") Color(0xFF10B981) else statusColor
                            )
                        }
                    }
                }
                
                if (isUserLoggedIn) {
                    val isCategoryEnabled = enabledCategories.contains(com.example.data.model.ReportType.WANTED_INDIVIDUAL)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onNavigateToSubmitTip,
                        enabled = isCategoryEnabled,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isCategoryEnabled) Color(0xFF1E88E5) else Color.Gray,
                            disabledContentColor = Color.Gray
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp, 
                            if (isCategoryEnabled) Color(0xFF1E88E5) else Color.LightGray
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCategoryEnabled) "SUBMIT TIP" else "TIPS CLOSED", 
                            style = MaterialTheme.typography.bodyMedium, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun DisclaimerSection() {
    Surface(
        color = Color(0xFFFDE8E8),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "This portal displays only verified incidents and official government notices. Unverified reports and names of accused individuals are withheld to protect privacy and ensure accuracy.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF374151),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ShareAppDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
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
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Share TraceNet Liberia".toTraceNetLiberiaAnnotatedString(MaterialTheme.colorScheme.onSurface),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Spread the word and help keep our community safe",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Facebook button
                    SharePlatformButton(
                        modifier = Modifier.weight(1f),
                        name = "FACEBOOK",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF1877F2), shape = RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "f",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                                    )
                                )
                            }
                        },
                        onClick = {
                            shareApp(context, "FACEBOOK")
                        }
                    )

                    // WhatsApp button
                    SharePlatformButton(
                        modifier = Modifier.weight(1f),
                        name = "WHATSAPP",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF25D366), shape = RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        onClick = {
                            shareApp(context, "WHATSAPP")
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Instagram button
                    SharePlatformButton(
                        modifier = Modifier.weight(1f),
                        name = "INSTAGRAM",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF833AB4),
                                                Color(0xFFFD1D1D),
                                                Color(0xFFF77737)
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        onClick = {
                            shareApp(context, "INSTAGRAM")
                        }
                    )

                    // YouTube button
                    SharePlatformButton(
                        modifier = Modifier.weight(1f),
                        name = "YOUTUBE",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFFF0000), shape = RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        onClick = {
                            shareApp(context, "YOUTUBE")
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Email button
                    SharePlatformButton(
                        modifier = Modifier.weight(1f),
                        name = "EMAIL",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFEA4335), shape = RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        onClick = {
                            shareApp(context, "EMAIL")
                        }
                    )

                    // Messenger button
                    SharePlatformButton(
                        modifier = Modifier.weight(1f),
                        name = "MESSENGER",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF0084FF), shape = RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        onClick = {
                            shareApp(context, "MESSENGER")
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        },
        
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun SharePlatformButton(
    modifier: Modifier,
    name: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun shareApp(context: Context, platform: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        val shareMessage = """
            🚨 Join me on TRACENET LIBERIA to report and track local security incidents, view live crime statistics, and help make Liberia safer!
            
            Download the app now and be part of the community safety network.
        """.trimIndent()
        putExtra(Intent.EXTRA_TEXT, shareMessage)
        putExtra(Intent.EXTRA_SUBJECT, "TraceNet Liberia")
    }

    if (platform.uppercase() == "EMAIL") {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "TraceNet Liberia App")
            putExtra(Intent.EXTRA_TEXT, """
                🚨 Join me on TRACENET LIBERIA to report and track local security incidents, view live crime statistics, and help make Liberia safer!
                
                Download the app now and be part of the community safety network.
            """.trimIndent())
        }
        try {
            context.startActivity(emailIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "No email client found. Opening general sharing...", Toast.LENGTH_SHORT).show()
            val chooser = Intent.createChooser(shareIntent, "Share App")
            context.startActivity(chooser)
        }
    } else {
        val targetPackage = when (platform.uppercase()) {
            "FACEBOOK" -> "com.facebook.katana"
            "WHATSAPP" -> "com.whatsapp"
            "INSTAGRAM" -> "com.instagram.android"
            "YOUTUBE" -> "com.google.android.youtube"
            "MESSENGER" -> "com.facebook.orca"
            else -> null
        }

        if (targetPackage != null) {
            shareIntent.setPackage(targetPackage)
            try {
                context.startActivity(shareIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "$platform app not found. Opening general sharing...", Toast.LENGTH_SHORT).show()
                val chooser = Intent.createChooser(shareIntent, "Share with $platform")
                context.startActivity(chooser)
            }
        } else {
            val chooser = Intent.createChooser(shareIntent, "Share via")
            context.startActivity(chooser)
        }
    }
}
