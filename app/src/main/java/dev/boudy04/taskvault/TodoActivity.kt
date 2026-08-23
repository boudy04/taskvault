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
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.boudy04.taskvault.settings.SettingsRepository
import dev.boudy04.taskvault.settings.ThemeMode
import dev.boudy04.taskvault.ui.theme.BackgroundDark
import dev.boudy04.taskvault.ui.theme.OnBackgroundDark
import dev.boudy04.taskvault.ui.theme.PrimaryPillButton
import dev.boudy04.taskvault.ui.theme.TaskVaultTheme
import dev.boudy04.taskvault.ui.theme.resolvesDark
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity for the todoapp.
 *
 * Extends [FragmentActivity] because BiometricPrompt requires it; Compose setContent works
 * unchanged since FragmentActivity is an androidx ComponentActivity.
 */
@AndroidEntryPoint
class TodoActivity : FragmentActivity() {

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
                // Cold-start-only gate: unlocked state lives in activity state, so it holds for
                // the rest of the process lifetime once authentication succeeds.
                var locked by remember { mutableStateOf<Boolean?>(null) }
                LaunchedEffect(Unit) {
                    locked = settingsRepository.appLock.first()
                }
                // null = still checking; render nothing so content never flashes and no
                // prompt is launched before we know the lock is enabled.
                when (locked) {
                    false -> TodoNavGraph(
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
                    true -> LockGate(onUnlocked = { locked = false })
                    null -> Unit
                }
            }
        }
    }
}

// ponytail: auto-relock-on-background is the documented upgrade path, intentionally omitted
@Composable
private fun LockGate(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var showRetry by remember { mutableStateOf(false) }

    fun authenticate() {
        val activity = context as? FragmentActivity ?: return
        showRetry = false
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    showRetry = true
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.unlock_title))
                .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                .build()
        )
    }

    LaunchedEffect(Unit) { authenticate() }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundDark),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = OnBackgroundDark,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.unlock_title), color = OnBackgroundDark)
        if (showRetry) {
            Spacer(modifier = Modifier.height(24.dp))
            PrimaryPillButton(onClick = ::authenticate) {
                Text(stringResource(R.string.unlock_retry))
            }
        }
    }
}
