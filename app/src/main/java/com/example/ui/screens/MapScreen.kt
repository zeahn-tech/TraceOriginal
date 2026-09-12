package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MainViewModel,
    latitude: Double? = null,
    longitude: Double? = null,
    onBack: () -> Unit
) {
    val reports by viewModel.allReports.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incident Map", color = Color(0xFF1E88E5)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (BuildConfig.MAPS_API_KEY.isEmpty() || BuildConfig.MAPS_API_KEY == "PLACEHOLDER") {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Google Maps API Key Missing",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please configure your MAPS_API_KEY in the AI Studio Secrets Panel to view the map.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@Scaffold
        }

        val hasTarget = latitude != null && longitude != null
        val defaultLocation = if (hasTarget) LatLng(latitude!!, longitude!!) else LatLng(0.0, 0.0)
        val initialZoom = if (hasTarget) 15f else 2f
        
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(defaultLocation, initialZoom)
        }

        LaunchedEffect(latitude, longitude) {
            if (latitude != null && longitude != null) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(latitude, longitude),
                        15f
                    )
                )
            }
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize().padding(padding),
            cameraPositionState = cameraPositionState
        ) {
            reports.forEach { report ->
                Marker(
                    state = MarkerState(position = LatLng(report.latitude, report.longitude)),
                    title = report.title,
                    snippet = report.description
                )
            }
            if (latitude != null && longitude != null) {
                Marker(
                    state = MarkerState(position = LatLng(latitude, longitude)),
                    title = "🚨 ACTIVE SOS EMERGENCY",
                    snippet = "Exact distress coordinates: ($latitude, $longitude)",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                    )
                )
            }
        }
    }
}
