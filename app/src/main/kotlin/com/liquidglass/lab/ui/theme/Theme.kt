package com.liquidglass.lab.ui.theme

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

private val LabDarkScheme = darkColorScheme(
    primary = Color(0xFF7CC4FF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBCC7DC),
    surface = Color(0xFF10131A),
    surfaceContainer = Color(0xFF171B24),
    surfaceContainerHigh = Color(0xFF1E2430),
    background = Color(0xFF0A0D12),
    onSurface = Color(0xFFE6EAF2)
)

private val LabLightScheme = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    surface = Color(0xFFF7F9FC),
    background = Color(0xFFF2F5FA),
    onSurface = Color(0xFF161920)
)

@Composable
fun LiquidGlassLabTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LabDarkScheme
        else -> LabLightScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
