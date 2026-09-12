package com.example.ui.components

import com.example.BuildConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import coil.request.ImageRequest
import coil.request.CachePolicy

@Composable
fun SafeAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val processedModel by produceState<Any?>(initialValue = model, key1 = model) {
        value = withContext(Dispatchers.IO) {
            if (BuildConfig.DEBUG) android.util.Log.d("SafeImage", "Processing model: ${model?.toString()?.take(50)}")
            if (model is String) {
                if (model.startsWith("data:")) {
                    try {
                        // OOM FIX: Reject large base64 blobs (> 5MB)
                        if (model.length > 5 * 1024 * 1024 * 1.33) { // Rough base64 overhead check
                             if (BuildConfig.DEBUG) android.util.Log.e("SafeImage", "Base64 blob too large, skipping to avoid OOM.")
                             null
                        } else {
                            val cachedUrl = com.example.util.MediaStorageHelper.getMediaUriAndCacheIfNeeded(context, model)
                            if (cachedUrl.startsWith("file://")) {
                                val path = cachedUrl.removePrefix("file://")
                                java.io.File(path)
                            } else {
                                model
                            }
                        }
                    } catch (e: Throwable) {
                        model
                    }
                } else if (model.startsWith("file://")) {
                    try {
                        java.io.File(model.removePrefix("file://"))
                    } catch (e: Exception) {
                        android.net.Uri.parse(model)
                    }
                } else if (model.startsWith("http") || model.startsWith("content://")) {
                    android.net.Uri.parse(model)
                } else {
                    model
                }
            } else {
                model
            }
        }
    }

    val imageRequest = remember(processedModel) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e("FirebaseValidation", "===== VALIDATION STEP 4 =====")
            android.util.Log.e("FirebaseValidation", "Before Coil loads the image: request URL: $processedModel")
        }
        ImageRequest.Builder(context)
            .data(processedModel)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .listener(
                onStart = { request -> 
                    if (BuildConfig.DEBUG) android.util.Log.e("FirebaseValidation", "Coil onStart: ${request.data}")
                },
                onSuccess = { request, result -> 
                    if (BuildConfig.DEBUG) {
                        android.util.Log.e("FirebaseValidation", "Coil onSuccess: ${request.data}")
                        android.util.Log.e("FirebaseValidation", "Image decode result: Success (DataSource: ${result.dataSource})")
                    }
                },
                onError = { request, result -> 
                    if (BuildConfig.DEBUG) {
                        android.util.Log.e("FirebaseValidation", "Coil onError: ${request.data}")
                        android.util.Log.e("FirebaseValidation", "Coil exception: ${result.throwable.message}", result.throwable)
                    }
                }
            )
            .build()
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Image unavailable",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Image unavailable",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}
