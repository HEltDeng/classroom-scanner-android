package com.classroomassessment.scanner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ScannerColors = darkColorScheme(
    primary = Color(0xFFFFB454),
    onPrimary = Color(0xFF2A1700),
    secondary = Color(0xFF54D6C7),
    onSecondary = Color(0xFF00201D),
    tertiary = Color(0xFFA9C7FF),
    background = Color(0xFF080E1B),
    onBackground = Color(0xFFE9EEF8),
    surface = Color(0xFF101A2B),
    onSurface = Color(0xFFE9EEF8),
    surfaceVariant = Color(0xFF253247),
    onSurfaceVariant = Color(0xFFB8C3D6),
    error = Color(0xFFFFB4AB),
)

@Composable
fun ScannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ScannerColors,
        typography = Typography(),
        content = content,
    )
}
