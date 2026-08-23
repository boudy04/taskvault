package dev.boudy04.taskvault.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.settings.ThemeMode

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    secondaryContainer = SecondaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
)

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
)

fun ThemeMode.resolvesDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

private val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
)

private fun outfitTypography(): Typography {
    val t = Typography()
    return Typography(
        displayLarge = t.displayLarge.copy(fontFamily = Outfit),
        displayMedium = t.displayMedium.copy(fontFamily = Outfit),
        displaySmall = t.displaySmall.copy(fontFamily = Outfit),
        headlineLarge = t.headlineLarge.copy(fontFamily = Outfit),
        headlineMedium = t.headlineMedium.copy(fontFamily = Outfit),
        headlineSmall = t.headlineSmall.copy(fontFamily = Outfit),
        titleLarge = t.titleLarge.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.copy(fontFamily = Outfit),
        bodyMedium = t.bodyMedium.copy(fontFamily = Outfit),
        bodySmall = t.bodySmall.copy(fontFamily = Outfit),
        labelLarge = t.labelLarge.copy(fontFamily = Outfit),
        labelMedium = t.labelMedium.copy(fontFamily = Outfit),
        labelSmall = t.labelSmall.copy(fontFamily = Outfit),
    )
}

@Composable
fun TaskVaultTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (themeMode.resolvesDark(isSystemInDarkTheme())) DarkColors else LightColors,
        typography = outfitTypography(),
        content = content,
    )
}

// Shared minimalist pill styling: solid primary for primary CTAs, quiet tonal+outline otherwise.
@Composable
fun PrimaryPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(onClick = onClick, modifier = modifier, shape = CircleShape, content = content)
}

@Composable
fun QuietPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        content = content,
    )
}
