package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.data.model.Alert
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer

import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator

class SOSSirenPlayer(private val context: android.content.Context) {
    private var audioTrack: android.media.AudioTrack? = null
    private var isPlaying = false
    private var thread: Thread? = null

    private var originalVolumes = mutableMapOf<Int, Int>()

    fun start() {
        if (isPlaying) return
        isPlaying = true
        
        try {
            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            
            // Store original volumes
            val streams = listOf(
                android.media.AudioManager.STREAM_ALARM,
                android.media.AudioManager.STREAM_RING,
                android.media.AudioManager.STREAM_MUSIC
            )
            streams.forEach { stream ->
                originalVolumes[stream] = audioManager.getStreamVolume(stream)
            }

            // Force alarm, ring, and music streams to maximum volume for maximum sound level
            val maxAlarmVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxAlarmVol, 0)
            
            val maxRingVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_RING)
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_RING, maxRingVol, 0)

            val maxMusicVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, maxMusicVol, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        thread = Thread {
            val sampleRate = 44100
            val minBufferSize = android.media.AudioTrack.getMinBufferSize(
                sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )
            
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
                
            val audioFormat = android.media.AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                .build()

            try {
                audioTrack = android.media.AudioTrack(
                    audioAttributes,
                    audioFormat,
                    if (minBufferSize > 0) minBufferSize * 2 else 8192,
                    android.media.AudioTrack.MODE_STREAM,
                    0
                )
                audioTrack?.play()
            } catch (e: Exception) {
                e.printStackTrace()
                return@Thread
            }

            val bufferSize = 2048
            val buffer = ShortArray(bufferSize)
            var angle1 = 0.0
            var angle2 = 0.0
            var sampleCount = 0L

            while (isPlaying) {
                for (i in 0 until bufferSize) {
                    val time = sampleCount.toDouble() / sampleRate
                    
                    // High-frequency modulating sweeps to sound EXACTLY like an active emergency vehicle Yelp/Wail siren
                    // Sweep 1: Classic Wail (modulates between 600Hz and 1300Hz every 1.4s)
                    val wailCycle = 1.4
                    val phaseWail = (time % wailCycle) / wailCycle
                    val modWail = if (phaseWail < 0.5) phaseWail * 2.0 else (1.0 - phaseWail) * 2.0
                    val freq1 = 600.0 + (modWail * 700.0)
                    
                    // Sweep 2: Classic fast-alert Yelp (modulates between 800Hz and 1600Hz every 0.3s)
                    val yelpCycle = 0.3
                    val phaseYelp = (time % yelpCycle) / yelpCycle
                    val modYelp = if (phaseYelp < 0.5) phaseYelp * 2.0 else (1.0 - phaseYelp) * 2.0
                    val freq2 = 800.0 + (modYelp * 800.0)
                    
                    angle1 += 2.0 * java.lang.Math.PI * freq1 / sampleRate
                    if (angle1 > 2.0 * java.lang.Math.PI) angle1 -= 2.0 * java.lang.Math.PI
                    
                    angle2 += 2.0 * java.lang.Math.PI * freq2 / sampleRate
                    if (angle2 > 2.0 * java.lang.Math.PI) angle2 -= 2.0 * java.lang.Math.PI
                    
                    // Synthesize overlapping components: sine-wave + square-wave elements for a harsh, realistic, piercing sound
                    val val1 = java.lang.Math.sin(angle1)
                    val val2 = if (java.lang.Math.sin(angle2) > 0) 0.6 else -0.6
                    
                    val combined = (val1 * 0.5) + (val2 * 0.5)
                    
                    buffer[i] = (combined * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                    sampleCount++
                }
                
                try {
                    audioTrack?.write(buffer, 0, bufferSize)
                } catch (e: Exception) {
                    break
                }
            }
            
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            audioTrack = null
        }
        thread?.start()
    }

    fun stop() {
        isPlaying = false
        try {
            audioTrack?.stop()
            
            // Restore original volumes
            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            originalVolumes.forEach { (stream, volume) ->
                try {
                    audioManager.setStreamVolume(stream, volume, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            originalVolumes.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun SOSScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onNavigateToContacts: () -> Unit = {}
) {
    val context = LocalContext.current
    var transmitCount by remember { mutableStateOf(1) }
    val sirenPlayer = remember { SOSSirenPlayer(context) }
    val fusedLocationClient = remember { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context) }

    DisposableEffect(Unit) {
        onDispose {
            sirenPlayer.stop()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                val lat = loc?.latitude ?: 37.7749
                val lng = loc?.longitude ?: -122.4194
                val sosAlert = Alert(
                    title = "SOS ACTIVE: Citizen Distress Signal",
                    content = "Distress signal broadcast from mobile node. Immediate response dispatch requested.",
                    urgency = 3, // High Urgency
                    locationName = "Transmitting Citizen Location",
                    latitude = lat,
                    longitude = lng
                )
                viewModel.submitAlert(sosAlert)
                Toast.makeText(context, "🚨 SOS Emergency Signal Transmitted!", Toast.LENGTH_LONG).show()
                sirenPlayer.start()
            }.addOnFailureListener {
                val sosAlert = Alert(
                    title = "SOS ACTIVE: Citizen Distress Signal",
                    content = "Distress signal broadcast from mobile node. Immediate response dispatch requested.",
                    urgency = 3, // High Urgency
                    locationName = "Transmitting Citizen Location",
                    latitude = 37.7749,
                    longitude = -122.4194
                )
                viewModel.submitAlert(sosAlert)
                Toast.makeText(context, "🚨 SOS Emergency Signal Transmitted!", Toast.LENGTH_LONG).show()
                sirenPlayer.start()
            }
        } catch (e: SecurityException) {
            val sosAlert = Alert(
                title = "SOS ACTIVE: Citizen Distress Signal",
                content = "Distress signal broadcast from mobile node. Immediate response dispatch requested.",
                urgency = 3, // High Urgency
                locationName = "Transmitting Citizen Location",
                latitude = 37.7749,
                longitude = -122.4194
            )
            viewModel.submitAlert(sosAlert)
            Toast.makeText(context, "🚨 SOS Emergency Signal Transmitted!", Toast.LENGTH_LONG).show()
            sirenPlayer.start()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        IconButton(
            onClick = {
                sirenPlayer.stop()
                onDismiss()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .safeDrawingPadding()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "EMERGENCY NODE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                "SOS ACTIVE",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.tertiary
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Box(
                modifier = Modifier
                    .size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing outer ring
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .background(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                            CircleShape
                        )
                )

                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.tertiary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(180.dp)
                )
                Surface(
                    modifier = Modifier
                        .size(140.dp)
                        .clickable {
                            transmitCount++
                            try {
                                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                    val lat = loc?.latitude ?: 37.7749
                                    val lng = loc?.longitude ?: -122.4194
                                    val sosAlert = Alert(
                                        title = "SOS RE-TRANSMITTED: Citizen Distress",
                                        content = "Subsequent distress signal broadcast received from active citizen (Sequence: #$transmitCount).",
                                        urgency = 3,
                                        locationName = "Transmitting Citizen Location",
                                        latitude = lat,
                                        longitude = lng
                                    )
                                    viewModel.submitAlert(sosAlert)
                                    Toast.makeText(context, "🚨 SOS Re-transmitted! Sequence #$transmitCount", Toast.LENGTH_SHORT).show()
                                    sirenPlayer.stop()
                                    sirenPlayer.start()
                                }
                            } catch (e: SecurityException) {
                                val sosAlert = Alert(
                                    title = "SOS RE-TRANSMITTED: Citizen Distress",
                                    content = "Subsequent distress signal broadcast received from active citizen (Sequence: #$transmitCount).",
                                    urgency = 3,
                                    locationName = "Transmitting Citizen Location",
                                    latitude = 37.7749,
                                    longitude = -122.4194
                                )
                                viewModel.submitAlert(sosAlert)
                                Toast.makeText(context, "🚨 SOS Re-transmitted! Sequence #$transmitCount", Toast.LENGTH_SHORT).show()
                                sirenPlayer.stop()
                                sirenPlayer.start()
                            }
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    shadowElevation = 20.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "HELP",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onTertiary,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Text(
                "SIGNAL TRANSMITTED",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Notifying closest active nodes and emergency response units.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = {
                    sirenPlayer.stop()
                    onNavigateToContacts()
                },
                colors = ButtonDefaults.buttonColors(
contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CALL EMERGENCY CONTACTS", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = {
                    sirenPlayer.stop()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
contentColor = MaterialTheme.colorScheme.tertiary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
            ) {
                Text("CANCEL SOS", fontWeight = FontWeight.Bold)
            }
        }
    }
}
