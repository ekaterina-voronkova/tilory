package com.voronkova.tilory.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * "Tilory colours" (#25), taken from the owner's watercolour on the icon: paper cream, flower blue, leaf green,
 * ink navy and mustard splashes. Dark mode uses the painting's navy shadows as the background.
 */
val TiloryLight = lightColorScheme(
    primary = Color(0xFF1E88D8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFE6F8),
    onPrimaryContainer = Color(0xFF0B2F4F),
    secondary = Color(0xFF4E9A3A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9EDCF),
    onSecondaryContainer = Color(0xFF173A10),
    tertiary = Color(0xFFB59A00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E7A6),
    onTertiaryContainer = Color(0xFF3A3100),
    background = Color(0xFFF6EFE6),
    onBackground = Color(0xFF1F2A44),
    surface = Color(0xFFF6EFE6),
    onSurface = Color(0xFF1F2A44),
    surfaceVariant = Color(0xFFE8DFD3),
    onSurfaceVariant = Color(0xFF4A5468),
    surfaceContainerLowest = Color(0xFFFFFBF6),
    surfaceContainerLow = Color(0xFFF9F2EA),
    surfaceContainer = Color(0xFFF1E9DE),
    surfaceContainerHigh = Color(0xFFEBE2D6),
    surfaceContainerHighest = Color(0xFFE5DCCF),
    surfaceBright = Color(0xFFFFFBF6),
    surfaceDim = Color(0xFFE2D9CC),
    outline = Color(0xFF8A8478),
    outlineVariant = Color(0xFFD5CCBF),
)

val TiloryDark = darkColorScheme(
    primary = Color(0xFF6FBDF5),
    onPrimary = Color(0xFF00315A),
    primaryContainer = Color(0xFF1D5C93),
    onPrimaryContainer = Color(0xFFD3E8FB),
    secondary = Color(0xFF8FD07A),
    onSecondary = Color(0xFF12380A),
    secondaryContainer = Color(0xFF2F5E24),
    onSecondaryContainer = Color(0xFFD2F0C6),
    tertiary = Color(0xFFE3CB4F),
    onTertiary = Color(0xFF3A3100),
    tertiaryContainer = Color(0xFF5A4C00),
    onTertiaryContainer = Color(0xFFF7E9A3),
    background = Color(0xFF121A2B),
    onBackground = Color(0xFFE6ECF5),
    surface = Color(0xFF121A2B),
    onSurface = Color(0xFFE6ECF5),
    surfaceVariant = Color(0xFF2A3550),
    onSurfaceVariant = Color(0xFFB9C3D6),
    surfaceContainerLowest = Color(0xFF0D1422),
    surfaceContainerLow = Color(0xFF172036),
    surfaceContainer = Color(0xFF1B253C),
    surfaceContainerHigh = Color(0xFF222D47),
    surfaceContainerHighest = Color(0xFF2A3652),
    surfaceBright = Color(0xFF323E5A),
    surfaceDim = Color(0xFF121A2B),
    outline = Color(0xFF8792A8),
    outlineVariant = Color(0xFF3A4560),
)
