package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.material3.lightColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = PrimaryBlueLight,
    tertiary = SuccessGreen,
    background = DarkBackground,
    surface = CardBackground,
    surfaceVariant = ElevatedBackground,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    error = Color(0xFFEF4444)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = PrimaryBlueDark,
    tertiary = SuccessGreen,
    background = Color(0xFFF1F5F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E8F0),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626)
)

@Composable
fun EduVoteTheme(
    darkTheme: Boolean = true,
    uiStyle: AppUiStyle = AppUiStyle.DEFAULT,
    content: @Composable () -> Unit
) {
    val colorScheme = when (uiStyle) {
        AppUiStyle.DEFAULT -> if (darkTheme) DarkColorScheme else LightColorScheme
        AppUiStyle.MINIMAL -> if (darkTheme) darkColorScheme(
            primary = Color(0xFF94A3B8),
            secondary = Color(0xFF64748B),
            tertiary = Color(0xFF38BDF8),
            background = Color(0xFF090D16),
            surface = Color(0xFF111827),
            surfaceVariant = Color(0xFF1F2937),
            onPrimary = Color.Black,
            onSecondary = Color.White,
            onBackground = Color(0xFFF9FAFB),
            onSurface = Color(0xFFF9FAFB),
            onSurfaceVariant = Color(0xFF9CA3AF),
            outline = Color(0xFF374151)
        ) else lightColorScheme(
            primary = Color(0xFF475569),
            secondary = Color(0xFF64748B),
            tertiary = Color(0xFF0284C7),
            background = Color(0xFFF9FAFB),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF3F4F6),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF111827),
            onSurface = Color(0xFF1F2937),
            onSurfaceVariant = Color(0xFF4B5563),
            outline = Color(0xFFE5E7EB)
        )
        AppUiStyle.VIBRANT -> if (darkTheme) darkColorScheme(
            primary = Color(0xFFEC4899),
            secondary = Color(0xFFA855F7),
            tertiary = Color(0xFF3B82F6),
            background = Color(0xFF1E102F),
            surface = Color(0xFF2A1745),
            surfaceVariant = Color(0xFF3B1E5F),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFFFDF4FF),
            onSurface = Color(0xFFFDF4FF),
            onSurfaceVariant = Color(0xFFE9D5FF),
            outline = Color(0xFF5B21B6)
        ) else lightColorScheme(
            primary = Color(0xFFD946EF),
            secondary = Color(0xFFA855F7),
            tertiary = Color(0xFF2563EB),
            background = Color(0xFFFAF5FF),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF3E8FF),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF2E1065),
            onSurface = Color(0xFF3B0764),
            onSurfaceVariant = Color(0xFF6B21A8),
            outline = Color(0xFFE9D5FF)
        )
        AppUiStyle.CLASSIC -> if (darkTheme) darkColorScheme(
            primary = Color(0xFF10B981),
            secondary = Color(0xFF059669),
            tertiary = Color(0xFFF59E0B),
            background = Color(0xFF062C1E),
            surface = Color(0xFF0B3C2A),
            surfaceVariant = Color(0xFF124E38),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFFECFDF5),
            onSurface = Color(0xFFECFDF5),
            onSurfaceVariant = Color(0xFFA7F3D0),
            outline = Color(0xFF047857)
        ) else lightColorScheme(
            primary = Color(0xFF059669),
            secondary = Color(0xFF10B981),
            tertiary = Color(0xFFD97706),
            background = Color(0xFFF0FDF4),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFDCFCE7),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF064E3B),
            onSurface = Color(0xFF065F46),
            onSurfaceVariant = Color(0xFF047857),
            outline = Color(0xFFA7F3D0)
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
