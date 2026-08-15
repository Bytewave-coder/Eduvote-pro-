package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0B0F19)
val CardBackground = Color(0xFF151A27)
val ElevatedBackground = Color(0xFF1E2536)

val PrimaryBlue = Color(0xFF3B82F6)
val PrimaryBlueGlow = Color(0x663B82F6)
val PrimaryBlueLight = Color(0xFF93C5FD)
val PrimaryBlueDark = Color(0xFF1D4ED8)

val SuccessGreen = Color(0xFF10B981)
val SuccessGreenDark = Color(0xFF047857)
val WarningYellow = Color(0xFFF59E0B)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val BorderColor = Color(0x40334155)

val AccentPurple = Color(0xFF8B5CF6)
val AccentCyan = Color(0xFF06B6D4)

val GradientPrimary = Brush.horizontalGradient(
    colors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
)

val GradientSuccess = Brush.horizontalGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF059669))
)

val GradientSurfaceDark = Brush.verticalGradient(
    colors = listOf(Color(0xFF1A2133), Color(0xFF111522))
)

val GradientSurfaceLight = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))
)
