package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val TraceNetLightColorScheme =
    lightColorScheme(
        primary = TraceNetPrimary,
        onPrimary = Color.White,
        secondary = TraceNetSecondary,
        onSecondary = Color.White,
        tertiary = TraceNetAccent,
        onTertiary = Color.White,
        background = TraceNetBackground,
        onBackground = TraceNetText,
        surface = TraceNetSurface,
        onSurface = TraceNetText,
        surfaceVariant = Color(0xFFE3F2FD),
        onSurfaceVariant = TraceNetTextDim,
        error = TraceNetError,
        onError = Color.White
    )

private val TraceNetDarkColorScheme =
    darkColorScheme(
        primary = TraceNetPrimary,
        onPrimary = Color.White,
        secondary = TraceNetSecondary,
        onSecondary = Color.White,
        tertiary = TraceNetAccent,
        onTertiary = Color.White,
        background = TraceNetBackground,
        onBackground = TraceNetText,
        surface = TraceNetSurface,
        onSurface = TraceNetText,
        surfaceVariant = Color(0xFFE3F2FD),
        onSurfaceVariant = TraceNetTextDim,
        error = TraceNetError,
        onError = Color.White
    )

@Composable
fun TraceNetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) TraceNetDarkColorScheme else TraceNetLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
