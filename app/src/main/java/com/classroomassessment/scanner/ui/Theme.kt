package com.classroomassessment.scanner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ScannerColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6F5F1),
    onPrimaryContainer = Color(0xFF0A5D57),
    secondary = Color(0xFF0A5D57),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFB57924),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F8F7),
    onBackground = Color(0xFF18333B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18333B),
    surfaceVariant = Color(0xFFE6F5F1),
    onSurfaceVariant = Color(0xFF6B8387),
    outline = Color(0xFFE2ECE9),
    error = Color(0xFFA54837),
    onError = Color(0xFFFFFFFF),
)

@Composable
fun ScannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ScannerColors,
        typography = Typography(),
        content = content,
    )
}
