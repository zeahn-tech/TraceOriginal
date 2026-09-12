package com.example.ui.components

import com.example.BuildConfig
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Resolves a media source based on priority:
 * 1. Permanent Firebase download URL or HTTPS stream
 * 2. Cached Base64 URI
 * 3. Local file URI (verified to exist on the current device)
 * 4. Controlled error state for missing local paths
 */
fun resolveMediaSource(context: android.content.Context, url: String): String {
    if (url.isBlank()) {
        if (BuildConfig.DEBUG) android.util.Log.e("MediaPlayers", "❌ [resolveMediaSource] Blank URL provided")
        return "error://empty_url"
    }
    
    if (BuildConfig.DEBUG) android.util.Log.d("MediaPlayers", "🔍 [resolveMediaSource] Resolving media source for URL: $url")
    
    // Priority 1: Permanent Firebase download URL or web stream
    if (url.startsWith("http://") || url.startsWith("https://")) {
        if (BuildConfig.DEBUG) android.util.Log.d("MediaPlayers", "✅ [resolveMediaSource] Selected source: Firebase Cloud/Web URL -> $url")
        return url
    }
    
    // Priority 2: Base64 URI (cached to local file)
    if (url.startsWith("data:")) {
        val cached = com.example.util.MediaStorageHelper.getMediaUriAndCacheIfNeeded(context, url)
        if (BuildConfig.DEBUG) android.util.Log.d("MediaPlayers", "✅ [resolveMediaSource] Selected source: Cached Base64 URI -> $cached")
        return cached
    }
    
    // Priority 3: Local file URI (check if exists and is readable on THIS device)
    if (url.startsWith("file://") || url.startsWith("content://") || url.startsWith("/")) {
        val cleanUrl = if (url.startsWith("/")) "file://$url" else url
        val uri = android.net.Uri.parse(cleanUrl)
        val fileExists = try {
            if (uri.scheme == "file") {
                File(uri.path ?: "").exists()
            } else {
                context.contentResolver.openInputStream(uri)?.use { true } ?: false
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("MediaPlayers", "❌ [resolveMediaSource] Error checking file existence for $url: ${e.message}")
            false
        }
        
        if (fileExists) {
            if (BuildConfig.DEBUG) android.util.Log.d("MediaPlayers", "✅ [resolveMediaSource] Selected source: Local file URI (exists and readable) -> $cleanUrl")
            return cleanUrl
        } else {
            if (BuildConfig.DEBUG) android.util.Log.e("MediaPlayers", "❌ [resolveMediaSource] Local file URI does not exist or is not readable on this device: $url")
            return "error://file_not_found"
        }
    }
    
    // Fallback
    if (BuildConfig.DEBUG) android.util.Log.w("MediaPlayers", "⚠️ [resolveMediaSource] Unknown URL format. Attempting direct playback for: $url")
    return url
}

@OptIn(UnstableApi::class)
private fun buildConfiguredExoPlayer(context: android.content.Context, processedUrl: String): ExoPlayer {
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("TraceNet-Android/1.0")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(15000)
        
    val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
    val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
        .apply {
            val uri = android.net.Uri.parse(processedUrl)
            if (BuildConfig.DEBUG) {
                android.util.Log.e("FirebaseValidation", "===== VALIDATION STEP 5 =====")
                android.util.Log.e("FirebaseValidation", "Before ExoPlayer.prepare(): MediaItem URI: $uri")
            }
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    isLazy: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var playbackError by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }
    
    // Lazy state
    var isInitialized by remember { mutableStateOf(!isLazy) }

    var resolvedUrl by remember(videoUrl, retryTrigger) { mutableStateOf<String?>(null) }
    
    LaunchedEffect(videoUrl, retryTrigger) {
        resolvedUrl = withContext(Dispatchers.IO) { resolveMediaSource(context, videoUrl) }
    }
    
    LaunchedEffect(resolvedUrl) {
        val url = resolvedUrl ?: return@LaunchedEffect
        if (url == "error://empty_url") {
            playbackError = "No valid video URL provided."
        } else if (url == "error://file_not_found") {
            playbackError = "Video file not found."
        } else {
            playbackError = null
        }
    }
    
    val exoPlayer = remember(resolvedUrl, isInitialized, retryTrigger) {
        if (!isInitialized || resolvedUrl == null || resolvedUrl!!.startsWith("error://")) {
            null
        } else {
            try {
                buildConfiguredExoPlayer(context, resolvedUrl!!)
            } catch (e: Exception) {
                playbackError = "Failed to initialize video player: ${e.message}"
                null
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackError = "Unable to play video: ${error.message}"
                if (BuildConfig.DEBUG) android.util.Log.e("VideoPlayer", "❌ [ExoPlayer Event] Playback error: ${error.message}", error)
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
        if (!isInitialized) {
            // Lazy Thumbnail/Play Button
            IconButton(onClick = { isInitialized = true }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play Video", tint = Color.White, modifier = Modifier.size(48.dp))
            }
        } else if (playbackError != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = playbackError!!,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                IconButton(onClick = { retryTrigger++ }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White)
                }
            }
        } else if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun AudioPlayer(
    audioUrl: String,
    modifier: Modifier = Modifier,
    isLazy: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var playbackError by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }
    
    // Lazy state
    var isInitialized by remember { mutableStateOf(!isLazy) }
    
    var resolvedUrl by remember(audioUrl, retryTrigger) { mutableStateOf<String?>(null) }
    
    LaunchedEffect(audioUrl, retryTrigger) {
        resolvedUrl = withContext(Dispatchers.IO) { resolveMediaSource(context, audioUrl) }
    }
    
    LaunchedEffect(resolvedUrl) {
        val url = resolvedUrl ?: return@LaunchedEffect
        if (url == "error://empty_url") {
            playbackError = "No audio URL provided."
        } else if (url == "error://file_not_found") {
            playbackError = "Audio file not found."
        } else {
            playbackError = null
        }
    }
    
    val exoPlayer = remember(resolvedUrl, isInitialized, retryTrigger) {
        if (!isInitialized || resolvedUrl == null || resolvedUrl!!.startsWith("error://")) {
            null
        } else {
            try {
                buildConfiguredExoPlayer(context, resolvedUrl!!)
            } catch (e: Exception) {
                playbackError = "Failed to initialize audio player: ${e.message}"
                null
            }
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0L) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer?.duration ?: 0L
                    playbackError = null
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackError = "Audio playback error: ${error.message}"
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    LaunchedEffect(isPlaying, exoPlayer) {
        while (isPlaying && exoPlayer != null) {
            progress = if (duration > 0) exoPlayer.currentPosition.toFloat() / duration else 0f
            kotlinx.coroutines.delay(500)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        if (!isInitialized) {
            IconButton(onClick = { isInitialized = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play Audio")
            }
        } else if (playbackError != null) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(playbackError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = { retryTrigger++ }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = MaterialTheme.colorScheme.error)
                }
            }
        } else if (exoPlayer != null) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatTime(exoPlayer.currentPosition),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            formatTime(duration),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

