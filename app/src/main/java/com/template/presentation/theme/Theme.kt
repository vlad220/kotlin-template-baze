package com.template.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = TaxiAmber,
    onPrimary = OnTaxiAmber,
    primaryContainer = TaxiAmberDim,
    onPrimaryContainer = TaxiAmberSoft,
    secondary = TaxiEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0F3D2C),
    onSecondaryContainer = Color(0xFFB6F0D4),
    tertiary = Color(0xFF7EB6FF),
    background = NightBg,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightMuted,
    outline = Color(0xFF3A4552),
    error = TaxiError,
    onError = Color.White,
    errorContainer = Color(0xFF4A1C1C),
    onErrorContainer = Color(0xFFFFC9C9)
)

private val LightColorScheme = lightColorScheme(
    primary = DayPrimary,
    onPrimary = OnTaxiAmber,
    primaryContainer = Color(0xFFFFE58A),
    onPrimaryContainer = OnTaxiAmber,
    secondary = TaxiEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4F5E6),
    onSecondaryContainer = Color(0xFF063322),
    tertiary = Color(0xFF2B6CB0),
    background = DayBg,
    onBackground = DayText,
    surface = DaySurface,
    onSurface = DayText,
    surfaceVariant = DaySurfaceHigh,
    onSurfaceVariant = DayMuted,
    outline = Color(0xFFD4CBB8),
    error = TaxiError,
    onError = Color.White,
    errorContainer = Color(0xFFFFE0E0),
    onErrorContainer = Color(0xFF5C1010)
)

private val TaxiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AppTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = TaxiTypography,
        shapes = TaxiShapes,
        content = content
    )
}
