package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TraceNetAccent
import com.example.ui.theme.TraceNetError
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EmergencyContact
import com.example.data.model.UserRole
import com.example.data.model.VerifiedContacts
import com.example.ui.MainViewModel
import com.google.android.gms.location.LocationServices

// Haversine formula to compute distance in km
fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Earth's radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

// Helpers for SharedPreferences Persistence
fun saveContactsToPrefs(context: Context, contacts: List<EmergencyContact>) {
    val prefs = context.getSharedPreferences("emergency_contacts_prefs", Context.MODE_PRIVATE)
    try {
        val json = kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(EmergencyContact.serializer()),
            contacts
        )
        prefs.edit().putString("contacts_json", json).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadContactsFromPrefs(context: Context): List<EmergencyContact> {
    val prefs = context.getSharedPreferences("emergency_contacts_prefs", Context.MODE_PRIVATE)
    val json = prefs.getString("contacts_json", null)
    if (json != null) {
        try {
            return kotlinx.serialization.json.Json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(EmergencyContact.serializer()),
                json
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return VerifiedContacts.list
}

// Map Category name to Icon and Color Theme
fun getCategoryIconAndColor(category: String): Pair<ImageVector, Color> {
    return when (category) {
        "Police" -> Pair(Icons.Default.LocalPolice, Color(0xFF3B82F6)) // Blue
        "Traffic" -> Pair(Icons.Default.Traffic, Color(0xFFF59E0B)) // Amber/Orange
        "Criminal Investigation" -> Pair(Icons.Default.Security, Color(0xFF8B5CF6)) // Purple
        "Women & Children" -> Pair(Icons.Default.Face, Color(0xFFEC4899)) // Pink/Rose
        "Emergency Response" -> Pair(Icons.Default.Warning, TraceNetError) // Red
        "Fire Service" -> Pair(Icons.Default.LocalFireDepartment, Color(0xFFF97316)) // Dark Orange
        "Ambulance" -> Pair(Icons.Default.LocalHospital, Color(0xFF10B981)) // Emerald Green
        else -> Pair(Icons.Default.Shield, Color(0xFF64748B)) // Slate Grey for other
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    // Loaded contacts state
    var contactsList by remember { mutableStateOf(loadContactsFromPrefs(context)) }

    // Role check
    val canManageContacts = currentUser?.role == UserRole.LAW_ENFORCER || currentUser?.role == UserRole.ADMIN

    // Filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedCounty by remember { mutableStateOf("All Counties") }
    var showCountyDropdown by remember { mutableStateOf(false) }

    // Proximity state
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLon by remember { mutableStateOf<Double?>(null) }
    var sortByDistance by remember { mutableStateOf(false) }

    // Dialog state
    var showAddEditDialog by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<EmergencyContact?>(null) }

    // Categories and Counties lists
    val categories = listOf("All", "Police", "Traffic", "Criminal Investigation", "Women & Children", "Emergency Response", "Fire Service", "Ambulance", "Immigration")
    val counties = listOf(
        "All Counties", "Bomi", "Bong", "Gbarpolu", "Grand Bassa", "Grand Cape Mount",
        "Grand Gedeh", "Grand Kru", "Lofa", "Margibi", "Maryland", "Montserrado",
        "Nimba", "River Cess", "River Gee", "Sinoe"
    )

    // Location client
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Helper check for location permission
    fun isLocationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    var hasPermission by remember { mutableStateOf(isLocationGranted()) }

    // Launcher for requesting permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasPermission = fineGranted || coarseGranted
        if (hasPermission) {
            Toast.makeText(context, "Location Access Granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Location Access Denied. Cannot calculate proximity.", Toast.LENGTH_LONG).show()
        }
    }

    // Trigger location fetch if permission granted
    @SuppressLint("MissingPermission")
    fun requestUserLocation() {
        if (hasPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    userLat = loc.latitude
                    userLon = loc.longitude
                } else {
                    // Fallback to Monrovia center if location is null but permission is granted (to test proximity)
                    userLat = 6.3005
                    userLon = -10.7969
                }
            }.addOnFailureListener {
                // Default center fallback
                userLat = 6.3005
                userLon = -10.7969
            }
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            requestUserLocation()
        }
    }

    // Filter and Sort contacts dynamically based on categories, county, search, and proximity
    val processedContacts = remember(contactsList, searchQuery, selectedCategory, selectedCounty, userLat, userLon, sortByDistance, canManageContacts) {
        val filtered = contactsList.filter { contact ->
            // Filter out disabled/outdated contacts for normal citizens
            if (!canManageContacts && contact.isDisabled) return@filter false

            // Category matches
            val categoryMatches = when (selectedCategory) {
                "All" -> true
                else -> contact.category.equals(selectedCategory, ignoreCase = true) ||
                        (selectedCategory == "Police" && contact.agencyName.contains("Police", ignoreCase = true) && !contact.category.contains("Traffic", ignoreCase = true))
            }

            // County matches
            val countyMatches = if (selectedCounty == "All Counties") true else contact.county.equals(selectedCounty, ignoreCase = true)

            // Search query matches (Station, County, City, Unit, Officer Name)
            val searchMatches = searchQuery.isBlank() || 
                                contact.agencyName.contains(searchQuery, ignoreCase = true) ||
                                contact.officeName.contains(searchQuery, ignoreCase = true) ||
                                contact.county.contains(searchQuery, ignoreCase = true) ||
                                contact.districtCity.contains(searchQuery, ignoreCase = true) ||
                                (contact.contactPerson?.contains(searchQuery, ignoreCase = true) ?: false) ||
                                contact.position.contains(searchQuery, ignoreCase = true) ||
                                contact.category.contains(searchQuery, ignoreCase = true)

            categoryMatches && countyMatches && searchMatches
        }

        // Sort by Proximity if requested and user coordinates available
        if (sortByDistance && userLat != null && userLon != null) {
            filtered.sortedBy { contact ->
                if (contact.latitude != null && contact.longitude != null) {
                    calculateDistanceKm(userLat!!, userLon!!, contact.latitude, contact.longitude)
                } else {
                    Double.MAX_VALUE
                }
            }
        } else {
            filtered
        }
    }

    // Helper for finding the closest police stations specifically
    val nearestPoliceStations = remember(contactsList, userLat, userLon, canManageContacts) {
        if (userLat != null && userLon != null) {
            contactsList
                .filter { (it.category.equals("Police", ignoreCase = true) || it.agencyName.contains("Police", ignoreCase = true)) && it.latitude != null && it.longitude != null && (canManageContacts || !it.isDisabled) }
                .sortedBy { calculateDistanceKm(userLat!!, userLon!!, it.latitude!!, it.longitude!!) }
                .take(3)
        } else {
            emptyList()
        }
    }

    Scaffold(
         // Light Slate Background
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Verified Contacts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Official Directory of Law Enforcement",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475569)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (canManageContacts) {
                ExtendedFloatingActionButton(
                    onClick = {
                        contactToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(42.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Contact", modifier = Modifier.size(18.dp)) },
                    text = { Text("Add Contact", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth(0.70f)
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
                placeholder = { 
                    Text(
                        "Search station, county, unit...", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    ) 
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Clear, 
                                contentDescription = "Clear", 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF212121),
                    unfocusedTextColor = Color(0xFF212121),
                    focusedBorderColor = Color(0xFF1E88E5),
                    unfocusedBorderColor = Color(0xFFBDBDBD),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedLabelColor = Color(0xFF1E88E5),
                    unfocusedLabelColor = Color(0xFF757575)
                )
            )

            // Location Permission / Active GPS Banner
            if (!hasPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "GPS",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Find Nearest Stations",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Enable location to view exact station distances in real-time.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Enable", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (userLat != null && userLon != null) {
                // GPS Active row showing proximity control
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFD1FAE5), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "GPS Active",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "GPS Tracking Active",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                        }

                        // Upper Right Corner: Toggle switch on left, "Sort by Proximity" text on upper right
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Switch(
                                checked = sortByDistance,
                                onCheckedChange = { sortByDistance = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF10B981),
                                    checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.scale(0.75f)
                            )
                            Text(
                                "Sort by Proximity",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF065F46),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        "Distances calculated from current location",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF047857),
                        modifier = Modifier.padding(start = 24.dp, top = 2.dp)
                    )
                }
            }

            // Category & County Filters Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // County selector button
                Box {
                    Surface(
                        onClick = { showCountyDropdown = true },
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(selectedCounty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = showCountyDropdown,
                        onDismissRequest = { showCountyDropdown = false },
                        modifier = Modifier.heightIn(max = 280.dp),
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
                                        showCountyDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Show count of contacts
                Text(
                    text = "${processedContacts.size} contacts",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Categories horizontal scroll list
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    val (catIcon, catColor) = getCategoryIconAndColor(category)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (category != "All") {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else catColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(category, fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = catColor,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            selectedBorderColor = catColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main List Layout
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Section 1: "Nearest Police Stations" (Only if GPS has user coordinates)
                if (hasPermission && userLat != null && userLon != null && searchQuery.isBlank() && selectedCategory == "All" && selectedCounty == "All Counties") {
                    item {
                        Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = "Nearest",
                                    tint = TraceNetError
                                )
                                Text(
                                    "NEAREST POLICE STATIONS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = TraceNetError,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                "Quickest response stations closest to you",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    items(nearestPoliceStations, key = { "near_${it.id}" }) { contact ->
                        ContactItemCard(
                            contact = contact,
                            context = context,
                            userLat = userLat,
                            userLon = userLon,
                            canManage = canManageContacts,
                            onEdit = {
                                contactToEdit = contact
                                showAddEditDialog = true
                            },
                            onDelete = {
                                contactsList = contactsList.filter { it.id != contact.id }
                                saveContactsToPrefs(context, contactsList)
                                Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        Text(
                            "ALL VERIFIED DIRECTORY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                // Section 2: Main list or filtered list
                if (processedContacts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color(0xFF475569)
                                )
                                Text(
                                    "No verified contacts found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    "Try revising your search criteria or filters",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                } else {
                    items(processedContacts, key = { it.id }) { contact ->
                        ContactItemCard(
                            contact = contact,
                            context = context,
                            userLat = userLat,
                            userLon = userLon,
                            canManage = canManageContacts,
                            onEdit = {
                                contactToEdit = contact
                                showAddEditDialog = true
                            },
                            onDelete = {
                                contactsList = contactsList.filter { it.id != contact.id }
                                saveContactsToPrefs(context, contactsList)
                                Toast.makeText(context, "Contact removed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditContactDialog(
            contact = contactToEdit,
            onDismiss = { showAddEditDialog = false },
            defaultLatitude = userLat,
            defaultLongitude = userLon,
            onSave = { savedContact ->
                if (contactToEdit == null) {
                    // Create new
                    contactsList = contactsList + savedContact
                } else {
                    // Edit existing
                    contactsList = contactsList.map { if (it.id == savedContact.id) savedContact else it }
                }
                saveContactsToPrefs(context, contactsList)
                showAddEditDialog = false
                Toast.makeText(context, "Directory updated successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ContactItemCard(
    contact: EmergencyContact,
    context: Context,
    userLat: Double?,
    userLon: Double?,
    canManage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (catIcon, catColor) = getCategoryIconAndColor(contact.category)

    // Distance state
    val distanceText = remember(contact, userLat, userLon) {
        if (userLat != null && userLon != null && contact.latitude != null && contact.longitude != null) {
            val dist = calculateDistanceKm(userLat, userLon, contact.latitude, contact.longitude)
            String.format("%.1f km", dist)
        } else {
            null
        }
    }

    // Status Indicator parameters
    val statusColor = when (contact.status) {
        "Available" -> Color(0xFF10B981) // Green
        "Busy" -> Color(0xFFF59E0B) // Amber
        else -> TraceNetError // Red
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFDADCE0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Category label & Edit/Delete actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category & Status Badges Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Chip / Pill
                    Box(
                        modifier = Modifier
                            .background(catColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, catColor.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = contact.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = catColor,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Disabled/Outdated Badge
                    if (contact.isDisabled) {
                        Box(
                            modifier = Modifier
                                .background(TraceNetError.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                .border(1.5.dp, TraceNetError.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "DISABLED / OUTDATED",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Edit/Delete Controls for Administrators/Enforcers
                if (canManage) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TraceNetError, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Office / Agency Title and Availability hours
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.agencyName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = contact.officeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Row for Availability and Proximity Badges
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Availability Hours Badge
                    Box(
                        modifier = Modifier
                            .background(
                                if (contact.emergencyAvailability == "24/7" || contact.emergencyAvailability == "24 Hours") TraceNetError.copy(alpha = 0.15f)
                                else Color(0xFFF59E0B).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (contact.emergencyAvailability == "24/7" || contact.emergencyAvailability == "24 Hours") TraceNetError else Color(0xFFF59E0B),
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = contact.emergencyAvailability,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (contact.emergencyAvailability == "24/7" || contact.emergencyAvailability == "24 Hours") Color(0xFFD32F2F) else Color(0xFFD97706),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Distance Badge
                    if (distanceText != null) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsRun,
                                    contentDescription = "Distance",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = distanceText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Verified + Status Indicators Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ✅ Verified Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (contact.isVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = if (contact.isVerified) "Verified" else "Verification Pending",
                        tint = if (contact.isVerified) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (contact.isVerified) "Verified by TraceNet" else "Pending Verification",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (contact.isVerified) Color(0xFF059669) else Color(0xFFD97706),
                        fontWeight = FontWeight.Bold
                    )
                }

                // 🟢/🟡/🔴 Availability Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = contact.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Details section
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Location info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${contact.districtCity}, ${contact.county} County",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Contact Person & Position if available
                if (contact.contactPerson != null && contact.contactPerson.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Contact Person", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${contact.contactPerson} (${contact.position})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = "Role", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Text(
                            text = contact.position,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Address
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Address", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(
                        text = contact.officeAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Office Hours
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Hours", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Hours: ${contact.officeHours}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Track Last Update Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Update, contentDescription = "Last Updated", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    val lastUpdatedFormatted = remember(contact.lastUpdated) {
                        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(contact.lastUpdated))
                    }
                    Text(
                        text = "Last updated: $lastUpdatedFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Action Buttons Row (Strictly 48dp touch targets)
            val phoneIsAvailable = !contact.phoneNumber.isNullOrBlank() && 
                contact.phoneNumber.trim() != "N/A" && 
                contact.phoneNumber.trim() != "None"

            val waIsAvailable = !contact.whatsAppNumber.isNullOrBlank() && 
                contact.whatsAppNumber.trim() != "N/A" && 
                contact.whatsAppNumber.trim() != "None"

            val emailIsAvailable = !contact.email.isNullOrBlank() && 
                contact.email.trim() != "N/A" && 
                contact.email.trim() != "None"

            val addressIsAvailable = !contact.officeAddress.isNullOrBlank() && 
                contact.officeAddress.trim() != "N/A" && 
                contact.officeAddress.trim() != "None"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 📞 Call Now Button
                Button(
                    enabled = phoneIsAvailable,
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot place call", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x201E88E5), // Transparent App Blue
                        contentColor = Color(0xFF1565C0),   // Deeper App Blue for Text & Icon
                        disabledContainerColor = Color(0xFFF1F5F9), // Dimmed Gray
                        disabledContentColor = Color(0xFF94A3B8)   // Dimmed Slate Text/Icon
                    ),
                    border = BorderStroke(1.dp, if (phoneIsAvailable) Color(0x401E88E5) else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Phone, 
                        contentDescription = "Call", 
                        tint = if (phoneIsAvailable) Color(0xFF1565C0) else Color(0xFF94A3B8), 
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        "Call Now", 
                        style = MaterialTheme.typography.labelMedium, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold,
                        color = if (phoneIsAvailable) Color(0xFF1565C0) else Color(0xFF94A3B8),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 💬 WhatsApp Button
                Button(
                    enabled = waIsAvailable,
                    onClick = {
                        try {
                            val cleanNum = contact.whatsAppNumber!!.replace(" ", "").replace("+", "")
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanNum"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open WhatsApp", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x2016A34A), // Transparent WhatsApp Green
                        contentColor = Color(0xFF15803D),   // Deeper Green for Text & Icon
                        disabledContainerColor = Color(0xFFF1F5F9), // Dimmed Gray
                        disabledContentColor = Color(0xFF94A3B8)   // Dimmed Slate Text/Icon
                    ),
                    border = BorderStroke(1.dp, if (waIsAvailable) Color(0x4016A34A) else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Send, 
                        contentDescription = "WhatsApp", 
                        tint = if (waIsAvailable) Color(0xFF15803D) else Color(0xFF94A3B8), 
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        "WhatsApp", 
                        style = MaterialTheme.typography.labelMedium, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold,
                        color = if (waIsAvailable) Color(0xFF15803D) else Color(0xFF94A3B8),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 📧 Email Button
                Button(
                    enabled = emailIsAvailable,
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot draft email", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x201E88E5), // Transparent App Blue
                        contentColor = Color(0xFF1565C0),   // Deeper App Blue for Text & Icon
                        disabledContainerColor = Color(0xFFF1F5F9), // Dimmed Gray
                        disabledContentColor = Color(0xFF94A3B8)   // Dimmed Slate Text/Icon
                    ),
                    border = BorderStroke(1.dp, if (emailIsAvailable) Color(0x401E88E5) else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Email, 
                        contentDescription = "Email", 
                        tint = if (emailIsAvailable) Color(0xFF1565C0) else Color(0xFF94A3B8), 
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        "Email", 
                        style = MaterialTheme.typography.labelMedium, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold,
                        color = if (emailIsAvailable) Color(0xFF1565C0) else Color(0xFF94A3B8),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 🗺️ Directions Button (Map intent)
                IconButton(
                    enabled = addressIsAvailable,
                    onClick = {
                        try {
                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(contact.officeAddress)}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open map", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (addressIsAvailable) Color(0x201E88E5) else Color(0xFFF1F5F9), 
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp, 
                            if (addressIsAvailable) Color(0x401E88E5) else Color(0xFFE2E8F0), 
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = "Directions",
                        tint = if (addressIsAvailable) Color(0xFF1565C0) else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactDialog(
    contact: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (EmergencyContact) -> Unit,
    defaultLatitude: Double? = null,
    defaultLongitude: Double? = null
) {
    var agencyName by remember { mutableStateOf(contact?.agencyName ?: "") }
    var officeName by remember { mutableStateOf(contact?.officeName ?: "") }
    var county by remember { mutableStateOf(contact?.county ?: "Montserrado") }
    var districtCity by remember { mutableStateOf(contact?.districtCity ?: "") }
    var contactPerson by remember { mutableStateOf(contact?.contactPerson ?: "") }
    var position by remember { mutableStateOf(contact?.position ?: "") }
    var phoneNumber by remember { mutableStateOf(contact?.phoneNumber ?: "") }
    var whatsAppNumber by remember { mutableStateOf(contact?.whatsAppNumber ?: "") }
    var email by remember { mutableStateOf(contact?.email ?: "") }
    var officeAddress by remember { mutableStateOf(contact?.officeAddress ?: "") }
    var officeHours by remember { mutableStateOf(contact?.officeHours ?: "08:00 AM - 05:00 PM") }
    var emergencyAvailability by remember { mutableStateOf(contact?.emergencyAvailability ?: "24/7") }
    var category by remember { mutableStateOf(contact?.category ?: "Police") }
    var status by remember { mutableStateOf(contact?.status ?: "Available") }
    var latitudeStr by remember { mutableStateOf(contact?.latitude?.toString() ?: defaultLatitude?.toString() ?: "") }
    var longitudeStr by remember { mutableStateOf(contact?.longitude?.toString() ?: defaultLongitude?.toString() ?: "") }
    var isVerified by remember { mutableStateOf(contact?.isVerified ?: true) }
    var isDisabled by remember { mutableStateOf(contact?.isDisabled ?: false) }

    var errorText by remember { mutableStateOf("") }

    val counties = listOf(
        "Bomi", "Bong", "Gbarpolu", "Grand Bassa", "Grand Cape Mount",
        "Grand Gedeh", "Grand Kru", "Lofa", "Margibi", "Maryland", "Montserrado",
        "Nimba", "River Cess", "River Gee", "Sinoe"
    )
    val categories = listOf("Police", "Traffic", "Criminal Investigation", "Women & Children", "Emergency Response", "Fire Service", "Ambulance", "Immigration")
    val statuses = listOf("Available", "Busy", "Offline")
    val availabilities = listOf("24/7", "Office Hours", "Emergency Only")

    var countyExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var availabilityExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        val dialogTextFieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF212121),
            unfocusedTextColor = Color(0xFF212121),
            focusedBorderColor = Color(0xFF1E88E5),
            unfocusedBorderColor = Color(0xFFBDBDBD),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedLabelColor = Color(0xFF1E88E5),
            unfocusedLabelColor = Color(0xFF757575)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface, // Clean white dialog background
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Title
                Text(
                    text = if (contact == null) "Add Verified Contact" else "Edit Verified Contact",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (errorText.isNotBlank()) {
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TraceNetError,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Agency Name
                    OutlinedTextField(
                        value = agencyName,
                        onValueChange = { agencyName = it },
                        label = { Text("Agency Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors
                    )

                    // Office / Division Name
                    OutlinedTextField(
                        value = officeName,
                        onValueChange = { officeName = it },
                        label = { Text("Office/Division/Station Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors
                    )

                    // County Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = county,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("County") },
                            trailingIcon = {
                                IconButton(onClick = { countyExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = dialogTextFieldColors
                        )
                        DropdownMenu(
                            expanded = countyExpanded,
                            onDismissRequest = { countyExpanded = false },
                            modifier = Modifier.heightIn(max = 280.dp),
                            containerColor = Color(0xFFF8F9FA)
                        ) {
                            counties.forEach { c ->
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
                                                c, 
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF0D47A1),
                                                fontWeight = FontWeight.Medium
                                            ) 
                                        },
                                        onClick = {
                                            county = c
                                            countyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // District / City
                    OutlinedTextField(
                        value = districtCity,
                        onValueChange = { districtCity = it },
                        label = { Text("District/City") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors
                    )

                    // Category Dropdown with Icons & Colors indication
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                IconButton(onClick = { categoryExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = dialogTextFieldColors
                        )
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.heightIn(max = 280.dp),
                            containerColor = Color(0xFFF8F9FA)
                        ) {
                            categories.forEach { cat ->
                                val (icon, col) = getCategoryIconAndColor(cat)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE3F2FD)
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(icon, contentDescription = null, tint = col) },
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

                    // Contact Person (Optional)
                    OutlinedTextField(
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        label = { Text("Contact Officer Name (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors
                    )

                    // Position / Title
                    OutlinedTextField(
                        value = position,
                        onValueChange = { position = it },
                        label = { Text("Officer Position/Role") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors
                    )

                    // Phone Number
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors
                    )

                    // WhatsApp Number (Optional)
                    OutlinedTextField(
                        value = whatsAppNumber,
                        onValueChange = { whatsAppNumber = it },
                        label = { Text("WhatsApp Number (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors
                    )

                    // Email Address
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors
                    )

                    // Physical Office Address
                    OutlinedTextField(
                        value = officeAddress,
                        onValueChange = { officeAddress = it },
                        label = { Text("Office Address") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors
                    )

                    // Office Hours
                    OutlinedTextField(
                        value = officeHours,
                        onValueChange = { officeHours = it },
                        label = { Text("Office Hours") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors
                    )

                    // Availability Mode Dropdown ("24/7", "Office Hours", etc)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = emergencyAvailability,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Emergency Availability Status") },
                            trailingIcon = {
                                IconButton(onClick = { availabilityExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = dialogTextFieldColors
                        )
                        DropdownMenu(
                            expanded = availabilityExpanded,
                            onDismissRequest = { availabilityExpanded = false },
                            modifier = Modifier.heightIn(max = 280.dp),
                            containerColor = Color(0xFFF8F9FA)
                        ) {
                            availabilities.forEach { av ->
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
                                                av, 
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF0D47A1),
                                                fontWeight = FontWeight.Medium
                                            ) 
                                        },
                                        onClick = {
                                            emergencyAvailability = av
                                            availabilityExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Live Status Dropdown (🟢 Available, 🟡 Busy, 🔴 Offline)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = status,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Live Availability Indicator") },
                            trailingIcon = {
                                IconButton(onClick = { statusExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = dialogTextFieldColors
                        )
                        DropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false },
                            modifier = Modifier.heightIn(max = 280.dp),
                            containerColor = Color(0xFFF8F9FA)
                        ) {
                            statuses.forEach { st ->
                                val stColor = when (st) {
                                    "Available" -> Color(0xFF10B981)
                                    "Busy" -> Color(0xFFF59E0B)
                                    else -> TraceNetError
                                }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE3F2FD)
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = { Box(modifier = Modifier.size(8.dp).background(stColor, CircleShape)) },
                                        text = { 
                                            Text(
                                                st, 
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF0D47A1),
                                                fontWeight = FontWeight.Medium
                                            ) 
                                        },
                                        onClick = {
                                            status = st
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Latitude
                    OutlinedTextField(
                        value = latitudeStr,
                        onValueChange = { latitudeStr = it },
                        label = { Text("Latitude (e.g. 6.301)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors
                    )

                    // Longitude
                    OutlinedTextField(
                        value = longitudeStr,
                        onValueChange = { longitudeStr = it },
                        label = { Text("Longitude (e.g. -10.796)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors
                    )

                    // Verification Status Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Verified Contact", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Indicates official endorsement by TraceNet", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = isVerified,
                            onCheckedChange = { isVerified = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Disabled/Outdated Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Disable Contact", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Hides from citizens (outdated/inactive)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = isDisabled,
                            onCheckedChange = { isDisabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TraceNetError,
                                checkedTrackColor = TraceNetError.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // Action Buttons Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (agencyName.isBlank() || officeName.isBlank() || districtCity.isBlank() || phoneNumber.isBlank() || email.isBlank() || officeAddress.isBlank()) {
                                errorText = "Please fill in all required fields."
                                return@Button
                            }

                            val latParsed = latitudeStr.toDoubleOrNull()
                            val lonParsed = longitudeStr.toDoubleOrNull()

                            val saved = EmergencyContact(
                                id = contact?.id ?: "contact_${System.currentTimeMillis()}",
                                agencyName = agencyName,
                                officeName = officeName,
                                county = county,
                                districtCity = districtCity,
                                contactPerson = contactPerson.ifBlank { null },
                                position = position,
                                phoneNumber = phoneNumber,
                                whatsAppNumber = whatsAppNumber.ifBlank { null },
                                email = email,
                                officeAddress = officeAddress,
                                officeHours = officeHours,
                                emergencyAvailability = emergencyAvailability,
                                latitude = latParsed,
                                longitude = lonParsed,
                                category = category,
                                status = status,
                                isVerified = isVerified,
                                isDisabled = isDisabled,
                                lastUpdated = System.currentTimeMillis()
                            )
                            onSave(saved)
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors()
) {
                        Text("Save Details", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
