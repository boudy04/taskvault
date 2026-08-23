package dev.boudy04.taskvault

import androidx.compose.runtime.Composable
import dev.boudy04.taskvault.settings.ThemeMode
import dev.boudy04.taskvault.ui.theme.TaskVaultTheme

// Retained for previews and tests; production entry point is TaskVaultTheme.
@Composable
fun TodoTheme(content: @Composable () -> Unit) {
    TaskVaultTheme(themeMode = ThemeMode.SYSTEM, content = content)
}
