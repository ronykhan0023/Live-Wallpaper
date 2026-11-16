package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemeMode

private val AmoledDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2B1D54),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = SapphireBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF17284B),
    onSecondaryContainer = Color(0xFFD6E4FF),
    tertiary = CyanGlow,
    onTertiary = Color(0xFF001F26),
    background = ObsidianBlack,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ObsidianCardElevated,
    outline = OutlineDark,
    outlineVariant = Color(0xFF1E2238),
    error = RosePulse,
    onError = Color.White
)

private val DaylightColorScheme = lightColorScheme(
    primary = DeepViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = Color(0xFF0891B2),
    onTertiary = Color.White,
    background = DaylightBackground,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightCard,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = DaylightSurface,
    surfaceContainerLow = DaylightBackground,
    surfaceContainer = DaylightCard,
    surfaceContainerHigh = Color(0xFFE4E9F5),
    outline = OutlineLight,
    outlineVariant = Color(0xFFE5EAF5),
    error = Color(0xFFE11D48),
    onError = Color.White
)

val RonyShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

object RonyGradients {
    val PurpleBluePrimary = Brush.linearGradient(
        colors = listOf(ElectricViolet, SapphireBlue)
    )

    val PurpleCyanAccent = Brush.linearGradient(
        colors = listOf(ElectricViolet, SapphireBlue, CyanGlow)
    )

    val GlassBorderDark = Brush.linearGradient(
        colors = listOf(
            Color(0x558B5CF6),
            Color(0x333B82F6),
            Color(0x2206B6D4)
        )
    )

    val GlassBorderLight = Brush.linearGradient(
        colors = listOf(
            Color(0x336D28D9),
            Color(0x222563EB)
        )
    )
}

@Composable
fun RonyLiveWallpaperTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDark) AmoledDarkColorScheme else DaylightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RonyTypography,
        shapes = RonyShapes,
        content = content
    )
}
