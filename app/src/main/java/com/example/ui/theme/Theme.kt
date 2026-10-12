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
    onPrimary = Color.Black,
    primaryContainer = SchoolBlueDark,
    onPrimaryContainer = Color.White,
    secondary = PositiveGreen,
    onSecondary = Color.Black,
    tertiary = WarningAmber,
    background = AppBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = AppSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    error = IncidentRed,
    onError = Color.White
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
    darkTheme: Boolean = isSystemInDarkTheme(),
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
