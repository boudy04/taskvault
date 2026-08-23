package dev.boudy04.taskvault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
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

// One variable TTF (wght 100-900, default instance = Thin). FontVariation.Settings
// pins the axis per declared weight; without it every style renders Thin.
// API < 26 ignores variation settings and falls back to the default instance.
@OptIn(ExperimentalTextApi::class)
private val Outfit = FontFamily(
    Font(
        R.font.outfit_regular,
        FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.outfit_medium,
        FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.outfit_semibold,
        FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.outfit_semibold,
        FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

// Portfolio display face for headings; single static instance, no variation axis.
private val InstrumentSerif = FontFamily(Font(R.font.instrument_serif_regular))

// fontChoice: "app" = Instrument Serif headings + Outfit body, "system" = platform default everywhere.
private fun outfitTypography(fontFamilyChoice: String): Typography {
    val t = Typography()
    val system = fontFamilyChoice == SYSTEM_FONT_FAMILY
    val headingFont = if (system) FontFamily.Default else InstrumentSerif
    val bodyFont = if (system) FontFamily.Default else Outfit
    return Typography(
        displayLarge = t.displayLarge.copy(fontFamily = headingFont, fontWeight = FontWeight.Bold),
        displayMedium = t.displayMedium.copy(fontFamily = headingFont, fontWeight = FontWeight.Bold),
        displaySmall = t.displaySmall.copy(fontFamily = headingFont, fontWeight = FontWeight.Bold),
        headlineLarge = t.headlineLarge.copy(fontFamily = headingFont, fontWeight = FontWeight.Bold),
        headlineMedium = t.headlineMedium.copy(fontFamily = headingFont, fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontFamily = headingFont, fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontFamily = headingFont, fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontFamily = headingFont, fontWeight = FontWeight.Bold),
        titleSmall = t.titleSmall.copy(fontFamily = headingFont, fontWeight = FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.copy(fontFamily = bodyFont, fontWeight = FontWeight.Medium),
        bodyMedium = t.bodyMedium.copy(fontFamily = bodyFont, fontWeight = FontWeight.Medium),
        bodySmall = t.bodySmall.copy(fontFamily = bodyFont, fontWeight = FontWeight.Medium),
        labelLarge = t.labelLarge.copy(fontFamily = bodyFont, fontWeight = FontWeight.SemiBold),
        labelMedium = t.labelMedium.copy(fontFamily = bodyFont, fontWeight = FontWeight.Medium),
        labelSmall = t.labelSmall.copy(fontFamily = bodyFont, fontWeight = FontWeight.Medium),
    )
}

const val SYSTEM_FONT_FAMILY = "system"
const val APP_FONT_FAMILY = "app"

@Composable
fun TaskVaultTheme(
    themeMode: ThemeMode,
    fontChoice: String = APP_FONT_FAMILY,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (themeMode.resolvesDark(isSystemInDarkTheme())) DarkColors else LightColors,
        typography = outfitTypography(fontChoice),
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
