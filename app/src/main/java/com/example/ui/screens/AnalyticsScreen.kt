package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Report
import com.example.data.model.ReportType
import com.example.data.model.User
import com.example.data.model.WantedCriminal
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.text.style.TextOverflow

val ChartPaletteCrimeRates = listOf(
    Color(0xFF1E88E5), // Blue
    Color(0xFF00ACC1), // Cyan
    Color(0xFF0288D1), // Light Blue
    Color(0xFF0097A7), // Dark Cyan
    Color(0xFF1976D2), // Royal Blue
    Color(0xFF00838F), // Deep Cyan
    Color(0xFF039BE5), // Sky Blue
    Color(0xFF00B8D4), // Bright Teal
    Color(0xFF3F51B5), // Indigo
    Color(0xFF00E5FF)  // Bright Cyan
)

val ChartPaletteSafest = listOf(
    Color(0xFF10B981), // Emerald
    Color(0xFF00897B), // Teal
    Color(0xFF43A047), // Green
    Color(0xFF00BFA5), // Accent Teal
    Color(0xFF2E7D32), // Dark Green
    Color(0xFF7CB342), // Lime Green
    Color(0xFF059669), // Mint Green
    Color(0xFF0F766E), // Dark Teal
    Color(0xFF66BB6A), // Light Green
    Color(0xFF1DE9B6)  // Bright Mint
)

val ChartPaletteCrimeTypes = listOf(
    Color(0xFFFB8C00), // Orange
    Color(0xFFFFB300), // Amber
    Color(0xFFF57C00), // Dark Orange
    Color(0xFFFF8F00), // Deep Amber
    Color(0xFFE65100), // Burnt Orange
    Color(0xFFFFA000), // Gold
    Color(0xFFFF6D00), // Bright Orange
    Color(0xFFFFAB00), // Light Gold
    Color(0xFFEF6C00), // Medium Orange
    Color(0xFFFFD600)  // Yellow Gold
)

val ChartPaletteWantedCriminals = listOf(
    Color(0xFFE53935), // Red
    Color(0xFFD81B60), // Rose
    Color(0xFFC62828), // Dark Red
    Color(0xFFAD1457), // Deep Rose
    Color(0xFFB71C1C), // Crimson
    Color(0xFFE91E63), // Bright Pink
    Color(0xFFFF1744), // Bright Red
    Color(0xFFF44336), // Coral Red
    Color(0xFF880E4F), // Deep Wine
    Color(0xFFFF4081)  // Hot Pink
)

val ChartPaletteDistribution = listOf(
    Color(0xFF8E24AA), // Purple
    Color(0xFF3949AB), // Indigo
    Color(0xFF5E35B1), // Deep Purple
    Color(0xFF6A1B9A), // Dark Violet
    Color(0xFF4A148C)  // Plum
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val reports by viewModel.activeReports.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val criminals by viewModel.allWantedCriminals.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "NATIONAL ANALYTICS", 
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
                colors = TopAppBarDefaults.topAppBarColors(
                    
                    
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Summary Stats
            item {
                SummaryStatsRow(reports.size, users.size, criminals.size)
            }

            // Crime Rates by County (Bar Chart)
            item {
                ChartCard(title = "Crime Rates by County", icon = Icons.Default.BarChart) {
                    val countyData = reports.groupBy { it.county ?: "Unknown" }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(10)
                    
                    if (countyData.isNotEmpty()) {
                        CustomBarChart(
                            data = countyData.map { it.second.toFloat() },
                            labels = countyData.map { it.first },
                            modifier = Modifier.height(200.dp).fillMaxWidth()
                        )
                    } else {
                        EmptyChartMessage("No county data available")
                    }
                }
            }

            // Safest Counties (Lowest Crime Rates)
            item {
                ChartCard(
                    title = "Safest Counties", 
                    icon = Icons.Default.Security,
                    iconColor = MaterialTheme.colorScheme.primary
                ) {
                    val safestData = reports.groupBy { it.county ?: "Unknown" }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedBy { it.second } // Ascending order
                        .take(10)
                    
                    if (safestData.isNotEmpty()) {
                        SafestCountiesBarChart(
                            safestData = safestData,
                            modifier = Modifier.height(200.dp).fillMaxWidth()
                        )
                    } else {
                        EmptyChartMessage("No county data available")
                    }
                }
            }

            // Highest Crime by Type per County (New Chart)
            item {
                CountyCrimeTypeChart(reports = reports)
            }

            // Wanted Criminal Reports by Crime (New Chart)
            item {
                WantedCriminalReportsChart(criminals = criminals, reports = reports)
            }

            // Crime Status Overview (Pie Chart)
            item {
                ChartCard(title = "Crime Status Overview", icon = Icons.Default.PieChart) {
                    val statusCounts = reports.groupBy { it.status }
                        .mapValues { it.value.size }
                    
                    if (statusCounts.isNotEmpty()) {
                        CustomPieChart(
                            data = statusCounts.values.map { it.toFloat() },
                            labels = statusCounts.keys.toList(),
                            modifier = Modifier.height(200.dp).fillMaxWidth()
                        )
                    } else {
                        EmptyChartMessage("No status data available")
                    }
                }
            }

            // Crime Trend (Line Chart)
            item {
                ChartCard(title = "Monthly Crime Trends", icon = Icons.Default.ShowChart) {
                    if (reports.isNotEmpty()) {
                        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        val monthlyCounts = (0..11).map { month ->
                            reports.count { report ->
                                val rCal = Calendar.getInstance()
                                rCal.timeInMillis = report.timestamp
                                rCal.get(Calendar.MONTH) == month
                            }.toFloat()
                        }
                        CustomLineChart(
                            data = monthlyCounts,
                            labels = months,
                            modifier = Modifier.height(200.dp).fillMaxWidth()
                        )
                    } else {
                        EmptyChartMessage("No trend data available")
                    }
                }
            }

            // Peak Stats
            item {
                PeakStatsSection(reports)
            }

            // Crime by Type
            item {
                ChartCard(title = "Crime Distribution by Type", icon = Icons.Default.Category) {
                    CrimeTypeDistribution(reports)
                }
            }

            // Arrests & Resolution Stats
            item {
                ChartCard(title = "Operational Outcomes", icon = Icons.Default.Poll) {
                    OperationalStats(reports)
                }
            }
        }
    }
}

@Composable
fun SummaryStatsRow(reportCount: Int, userCount: Int, criminalCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Total Reports",
                value = reportCount.toString(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = Icons.Default.Description
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "TraceNet Users",
                value = userCount.toString(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                icon = Icons.Default.People
            )
        }
        StatCard(
            modifier = Modifier.fillMaxWidth(),
            title = "Wanted Criminals",
            value = criminalCount.toString(),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            icon = Icons.Default.Gavel
        )
    }
}

@Composable
fun PeakStatsSection(reports: List<Report>) {
    val dayFormat = SimpleDateFormat("EEEE", Locale.US)
    val monthFormat = SimpleDateFormat("MMMM", Locale.US)
    
    val peakDay = reports.groupBy { 
        dayFormat.format(Date(it.timestamp))
    }.maxByOrNull { it.value.size }?.key ?: "N/A"
    
    val peakMonth = reports.groupBy { 
        monthFormat.format(Date(it.timestamp))
    }.maxByOrNull { it.value.size }?.key ?: "N/A"
    
    val peakWeek = reports.groupBy { 
        val wCal = Calendar.getInstance()
        wCal.timeInMillis = it.timestamp
        wCal.get(Calendar.WEEK_OF_YEAR)
    }.maxByOrNull { it.value.size }?.let { "Week ${it.key}" } ?: "N/A"

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Incident Peaks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PeakCard(modifier = Modifier.weight(1f), title = "Peak Day", value = peakDay, icon = Icons.Default.Today)
            PeakCard(modifier = Modifier.weight(1f), title = "Peak Month", value = peakMonth, icon = Icons.Default.CalendarMonth)
        }
        PeakCard(modifier = Modifier.fillMaxWidth(), title = "Peak Week of Year", value = peakWeek, icon = Icons.Default.DateRange)
    }
}

@Composable
fun PeakCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.7f))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun ChartCard(
    title: String, 
    icon: ImageVector, 
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = titleColor)
            }
            content()
        }
    }
}

@Composable
fun CustomBarChart(data: List<Float>, labels: List<String>, modifier: Modifier = Modifier) {
    val maxVal = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { index, value ->
            val fraction = value / maxVal
            val color = ChartPaletteCrimeRates[index % ChartPaletteCrimeRates.size]
            val countStr = if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.weight(1f))
                
                // Value text on top of the bar (Data label)
                Text(
                    text = countStr,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height((130 * fraction).dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = 0.5f), color)
                            ),
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                        )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Label under the bar
                Text(
                    text = labels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SafestCountiesBarChart(
    safestData: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val maxVal = (safestData.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        safestData.forEachIndexed { index, (county, count) ->
            val fraction = count.toFloat() / maxVal.toFloat()
            // Use unique color from Safest palette for each bar
            val color = ChartPaletteSafest[index % ChartPaletteSafest.size]

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.weight(1f))
                
                // Value text on top of the bar
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height((130 * fraction).dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = 0.5f), color)
                            ),
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // County Label
                Text(
                    text = county,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    maxLines = 3,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CustomPieChart(data: List<Float>, labels: List<String>, modifier: Modifier = Modifier) {
    val colors = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFFC107), 
        Color(0xFFF44336), Color(0xFF9C27B0), Color(0xFF00BCD4)
    )
    val total = data.sum()
    
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (total > 0f) {
            Canvas(modifier = Modifier.size(150.dp).weight(1f)) {
                var startAngle = -90f
                data.forEachIndexed { index, value ->
                    val sweepAngle = (value / total) * 360f
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += sweepAngle
                }
            }
        } else {
            Box(modifier = Modifier.size(150.dp).weight(1f), contentAlignment = Alignment.Center) {
                Text("No data", style = MaterialTheme.typography.labelSmall)
            }
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { index, label ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(colors[index % colors.size], RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${label.lowercase().replace("_", " ").capitalize()}: ${data[index].toInt()}", 
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CustomLineChart(data: List<Float>, labels: List<String>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val maxVal = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)
    
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val stepX = size.width / (data.size - 1).coerceAtLeast(1)
            val points = data.mapIndexed { index, value ->
                Offset(index * stepX, size.height - (value / maxVal) * size.height)
            }
            
            val path = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
            }
            
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            
            // Fill under the line
            val fillPath = Path().apply {
                if (points.isNotEmpty()) {
                    addPath(path)
                    lineTo(points.last().x, size.height)
                    lineTo(points.first().x, size.height)
                    close()
                }
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent)
                )
            )
            
            // Points
            points.forEach { point ->
                drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = point)
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = point)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun CrimeTypeDistribution(reports: List<Report>) {
    val typeCounts = reports.groupBy { it.type }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(5)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        typeCounts.forEachIndexed { index, (type, count) ->
            val progress = count.toFloat() / (reports.size.takeIf { it > 0 } ?: 1)
            val barColor = ChartPaletteDistribution[index % ChartPaletteDistribution.size]
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            type.name.lowercase().replace("_", " ").capitalize(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            count.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = barColor,
                        trackColor = barColor.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChartMessage(message: String, color: Color = Color.Gray) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = color, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun OperationalStats(reports: List<Report>) {
    val statusCounts = reports.groupBy { it.status.uppercase() }
        .mapValues { it.value.size }

    val metrics = listOf(
        Triple("Arrested", statusCounts["ARRESTED"] ?: 0, Color(0xFF6366F1)),
        Triple("Found", statusCounts["FOUND"] ?: 0, Color(0xFF14B8A6)),
        Triple("Resolved", statusCounts["RESOLVED"] ?: 0, Color(0xFF10B981)),
        Triple("Active", statusCounts["ACTIVE"] ?: 0, MaterialTheme.colorScheme.tertiary),
        Triple("Under Investigation", (statusCounts["VERIFIED"] ?: 0) + (statusCounts["UNDER_INVESTIGATION"] ?: 0), Color(0xFF3B82F6)),
        Triple("Pending", statusCounts["PENDING"] ?: 0, Color(0xFFFBBF24))
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        metrics.forEach { (label, count, color) ->
            val progress = count.toFloat() / (reports.size.takeIf { it > 0 } ?: 1)
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(count.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = color,
                        trackColor = color.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountyCrimeTypeChart(reports: List<Report>) {
    val counties = listOf("All Counties", "Bomi", "Bong", "Gbarpolu", "Grand Bassa", "Grand Cape Mount", "Grand Gedeh", "Grand Kru", "Lofa", "Margibi", "Maryland", "Montserrado", "Nimba", "River Cess", "River Gee", "Sinoe")
    var expanded by remember { mutableStateOf(false) }
    var selectedCounty by remember { mutableStateOf(counties[0]) }

    ChartCard(title = "Highest Crime by Type (per County)", icon = Icons.Default.LocationOn) {
        Column {
            // Dropdown for county selection
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = selectedCounty,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filter by County") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .heightIn(max = 280.dp),
                    containerColor = Color(0xFFF8F9FA)
                ) {
                    counties.forEach { county ->
                        val isSelected = selectedCounty == county
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
                                        county,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) Color.White else Color(0xFF0D47A1),
                                        fontWeight = FontWeight.Medium
                                    ) 
                                },
                                onClick = {
                                    selectedCounty = county
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            val filteredReports = if (selectedCounty == "All Counties") reports else reports.filter { it.county == selectedCounty }
            
            val crimeTypeCounts = filteredReports.groupBy { it.type.name }.mapValues { it.value.size }
            val sortedData = crimeTypeCounts.toList().sortedByDescending { it.second }.take(10) // Top 10
            
            if (sortedData.isNotEmpty()) {
                val maxVal = (sortedData.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                
                Row(
                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    sortedData.forEachIndexed { index, (type, count) ->
                        val fraction = count.toFloat() / maxVal.toFloat()
                        val color = ChartPaletteCrimeTypes[index % ChartPaletteCrimeTypes.size]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height((130 * fraction).dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(color.copy(alpha = 0.5f), color)
                                        ),
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = type.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = color,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            } else {
                EmptyChartMessage("No data available for $selectedCounty")
            }
        }
    }
}

@Composable
fun WantedCriminalReportsChart(criminals: List<WantedCriminal>, reports: List<Report>) {
    val criminalReportList = remember(criminals, reports) {
        val nonDeletedCriminals = criminals.filter { !it.isDeleted }
        val nonDeletedReports = reports.filter { !it.isDeleted }

        // Group active wanted criminals by normalized name to prevent duplicate bars
        val groupedCriminals = nonDeletedCriminals.groupBy { it.name.trim().lowercase() }

        groupedCriminals.map { (normalizedName, criminalGroup) ->
            val displayName = criminalGroup.firstOrNull()?.name?.trim()?.ifBlank { "Unknown" } ?: "Unknown"

            // Match incident reports referencing this criminal name
            val matchedReports = if (normalizedName.isBlank()) emptyList() else nonDeletedReports.filter { report ->
                val titleLower = report.title.lowercase()
                val descLower = report.description.lowercase()

                if (titleLower.contains(normalizedName) || descLower.contains(normalizedName)) {
                    return@filter true
                }

                val nameParts = normalizedName.split(" ").filter { it.length > 2 }
                if (nameParts.isNotEmpty() && nameParts.all { part -> titleLower.contains(part) || descLower.contains(part) }) {
                    return@filter true
                }

                false
            }

            // Total crime / notoriety count = wanted criminal records + matched incident reports
            val totalCount = criminalGroup.size + matchedReports.size

            Pair(displayName, totalCount)
        }
        .sortedByDescending { it.second }
        .take(10)
    }

    ChartCard(title = "Wanted Criminal Report by Name per Crime", icon = Icons.Default.Gavel) {
        Column {
            if (criminalReportList.isNotEmpty()) {
                val maxVal = (criminalReportList.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    criminalReportList.forEachIndexed { index, (name, count) ->
                        val fraction = count.toFloat() / maxVal.toFloat()
                        val color = ChartPaletteWantedCriminals[index % ChartPaletteWantedCriminals.size]
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height((120 * fraction).dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(color.copy(alpha = 0.5f), color)
                                        ),
                                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = color,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 1.dp)
                            )
                        }
                    }
                }
            } else {
                EmptyChartMessage("No active wanted criminal reports available")
            }
        }
    }
}
