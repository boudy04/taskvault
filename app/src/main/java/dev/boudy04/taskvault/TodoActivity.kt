/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.boudy04.taskvault

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import dev.boudy04.taskvault.settings.SettingsRepository
import dev.boudy04.taskvault.settings.ThemeMode
import dev.boudy04.taskvault.ui.theme.TaskVaultTheme
import dev.boudy04.taskvault.ui.theme.resolvesDark
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity for the todoapp
 */
@AndroidEntryPoint
class TodoActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
            // ponytail: activity-scoped state instead of a VM; single consumer, no test churn
            LaunchedEffect(Unit) {
                settingsRepository.themeMode.collect { themeMode = it }
            }
            val darkTheme = themeMode.resolvesDark(isSystemInDarkTheme())
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
                onDispose { }
            }
            val scope = rememberCoroutineScope()
            TaskVaultTheme(themeMode = themeMode) {
                TodoNavGraph(
                    themeMode = themeMode,
                    onCycleTheme = {
                        scope.launch {
                            settingsRepository.setThemeMode(
                                when (themeMode) {
                                    ThemeMode.DARK -> ThemeMode.LIGHT
                                    ThemeMode.LIGHT -> ThemeMode.SYSTEM
                                    ThemeMode.SYSTEM -> ThemeMode.DARK
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}
