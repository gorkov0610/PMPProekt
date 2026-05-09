package uklo.fikt.pmp.pmpproekt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}