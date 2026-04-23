package es.chefcore.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ChefCoreColors {
    val PrimaryGreen = Color(0xFF2E7D32)
    val PrimaryGreenDark = Color(0xFF1B5E20)
    val PrimaryGreenLight = Color(0xFF66BB6A)

    val AccentYellow = Color(0xFFFBC02D)
    val AccentYellowDark = Color(0xFFF57F17)

    val ErrorRed = Color(0xFFC62828)
    val ErrorRedLight = Color(0xFFEF5350)

    val TextDark = Color(0xFF37474F)
    val TextMedium = Color(0xFF78909C)
    val TextLight = Color(0xFFB0BEC5)

    val BackgroundLight = Color(0xFFF5F5F5)
    val BackgroundWhite = Color(0xFFFFFFFF)

    val SurfaceGray = Color(0xFFEEEEEE)
}

private val LightColorScheme = lightColorScheme(
    primary = ChefCoreColors.PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = ChefCoreColors.PrimaryGreenLight,
    onPrimaryContainer = ChefCoreColors.PrimaryGreenDark,
    secondary = ChefCoreColors.AccentYellow,
    onSecondary = ChefCoreColors.TextDark,
    secondaryContainer = Color(0xFFFFF9C4),
    onSecondaryContainer = ChefCoreColors.AccentYellowDark,
    tertiary = ChefCoreColors.ErrorRed,
    onTertiary = Color.White,
    tertiaryContainer = ChefCoreColors.ErrorRedLight,
    onTertiaryContainer = Color.White,
    error = ChefCoreColors.ErrorRed,
    onError = Color.White,
    errorContainer = ChefCoreColors.ErrorRedLight,
    onErrorContainer = Color.White,
    background = ChefCoreColors.BackgroundLight,
    onBackground = ChefCoreColors.TextDark,
    surface = ChefCoreColors.BackgroundWhite,
    onSurface = ChefCoreColors.TextDark,
    surfaceVariant = ChefCoreColors.SurfaceGray,
    onSurfaceVariant = ChefCoreColors.TextMedium,
    outline = ChefCoreColors.TextLight,
    outlineVariant = ChefCoreColors.SurfaceGray,
    scrim = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = ChefCoreColors.PrimaryGreenLight,
    onPrimary = ChefCoreColors.PrimaryGreenDark,
    primaryContainer = ChefCoreColors.PrimaryGreen,
    onPrimaryContainer = ChefCoreColors.PrimaryGreenLight,
    secondary = ChefCoreColors.AccentYellow,
    onSecondary = ChefCoreColors.TextDark,
    secondaryContainer = ChefCoreColors.AccentYellowDark,
    onSecondaryContainer = ChefCoreColors.AccentYellow,
    tertiary = ChefCoreColors.ErrorRedLight,
    onTertiary = ChefCoreColors.ErrorRed,
    tertiaryContainer = ChefCoreColors.ErrorRed,
    onTertiaryContainer = ChefCoreColors.ErrorRedLight,
    error = ChefCoreColors.ErrorRedLight,
    onError = ChefCoreColors.ErrorRed,
    errorContainer = ChefCoreColors.ErrorRed,
    onErrorContainer = ChefCoreColors.ErrorRedLight,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = ChefCoreColors.TextLight,
    outlineVariant = Color(0xFF3C3C3C),
    scrim = Color.Black
)

@Composable
fun ChefCoreTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
