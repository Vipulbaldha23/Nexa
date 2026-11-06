package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexaColorScheme = darkColorScheme(
    primary = NexaCyan,
    onPrimary = Color(0xFF04101A),
    primaryContainer = Color(0xFF00363A),
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = NexaViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF38104D),
    onSecondaryContainer = Color(0xFFF3E5F5),
    tertiary = NexaNeonBlue,
    onTertiary = Color.White,
    background = NexaBackground,
    onBackground = TextPrimary,
    surface = NexaSurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF131926),
    onSurfaceVariant = TextSecondary,
    outline = Color(0x3364748B),
    outlineVariant = Color(0x1F64748B)
)

@Composable
fun MyApplicationTheme(
    amoledMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val effectiveScheme = if (amoledMode) {
        NexaColorScheme.copy(
            background = NexaBackgroundAMOLED,
            surface = Color(0xFF090C12)
        )
    } else {
        NexaColorScheme
    }

    MaterialTheme(
        colorScheme = effectiveScheme,
        typography = Typography,
        content = content
    )
}
