package es.chefcore.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat



private val LightColorScheme = lightColorScheme(
    primary = VerdeChefCore,
    secondary = GrisSecundario,
    tertiary = VerdeChefCore,
    background = GrisFondo,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = GrisTexto,
    onSurface = GrisTexto,
    error = RojoCoste
)

private val DarkColorScheme = darkColorScheme(
    primary = VerdeChefCore,
    secondary = GrisSecundario,
    tertiary = VerdeChefCore,
    background = Color(0xFF121212), // Gris muy oscuro para modo noche
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE5E7EB), // Textos claros en modo noche
    onSurface = Color(0xFFE5E7EB),
    error = Color(0xFFEF4444)
)

@Composable
fun ChefCoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // IMPORTANTE: Lo pasamos a 'false' por defecto para que Android
    // no cambie tu verde corporativo por los colores del sistema del usuario.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Configuración para que la barra de estado superior (hora, batería)
    // se adapte a los colores de la app
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}