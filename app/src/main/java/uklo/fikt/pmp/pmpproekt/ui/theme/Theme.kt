package uklo.fikt.pmp.pmpproekt.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    secondary = SlateSecondary,
    tertiary = AmberAccent,
    background = Color(0xFF121212)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    secondary = SlateSecondary,
    tertiary = AmberAccent,
    background = BackgroundGray,
    onPrimary = Color.White
)

@Composable
fun PMPProektTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = EmeraldPrimary.toArgb() // Твојата зелена боја
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}