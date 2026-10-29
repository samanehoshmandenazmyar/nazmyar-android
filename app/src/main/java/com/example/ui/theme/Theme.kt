package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = SchoolBluePrimaryDark,
    onPrimary = Color(0xFF0B1120),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = PositiveGreen,
    onSecondary = Color(0xFF0B1120),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = WarningAmber,
    onTertiary = Color(0xFF0B1120),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFDE68A),
    background = AppBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = AppSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF475569),
    error = IncidentRed,
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA)
)

private val LightColorScheme = lightColorScheme(
    primary = SchoolBluePrimary,
    onPrimary = Color.White,
    primaryContainer = SchoolBlueContainer,
    onPrimaryContainer = OnSchoolBlueContainer,
    secondary = PositiveGreen,
    onSecondary = Color.White,
    secondaryContainer = PositiveGreenContainer,
    onSecondaryContainer = OnPositiveGreenContainer,
    tertiary = WarningAmber,
    onTertiary = Color.White,
    tertiaryContainer = WarningAmberContainer,
    onTertiaryContainer = OnWarningAmberContainer,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = AppSurface,
    onSurface = TextPrimary,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = AppOutline,
    error = IncidentRed,
    onError = Color.White,
    errorContainer = IncidentRedContainer,
    onErrorContainer = OnIncidentRedContainer
)

@Composable
fun NezamYarTheme(
    darkTheme: Boolean = com.example.core.theme.isAppInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides Typography.bodyLarge
            ) {
                content()
            }
        }
    }
}
