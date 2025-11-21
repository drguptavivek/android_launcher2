package edu.aiims.surveylauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

private val DarkColorScheme = darkColorScheme(
    primary = SkyPrimary,
    onPrimary = SkyOnPrimary,
    primaryContainer = SkyContainer,
    onPrimaryContainer = SkyOnContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberOnContainer,
    background = NavyBase,
    onBackground = SkyOnContainer,
    surface = NavySurface,
    onSurface = SkyOnContainer,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = SkyOnContainer.copy(alpha = 0.78f),
    outline = SkyOnContainer.copy(alpha = 0.36f),
    error = ErrorRed,
    onError = ErrorOnRed,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorOnContainer
)

private val SurveyTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.SemiBold),
    displayMedium = TextStyle(fontWeight = FontWeight.SemiBold),
    displaySmall = TextStyle(fontWeight = FontWeight.SemiBold),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold)
)

@Composable
fun SurveyLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = SurveyTypography,
        shapes = Shapes(),
        content = content
    )
}
