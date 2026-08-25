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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.boudy04.taskvault.auth.LoginScreen
import dev.boudy04.taskvault.settings.SettingsRepository
import dev.boudy04.taskvault.settings.ThemeMode
import dev.boudy04.taskvault.ui.theme.APP_FONT_FAMILY
import dev.boudy04.taskvault.ui.theme.BackgroundDark
import dev.boudy04.taskvault.ui.theme.OnBackgroundDark
import dev.boudy04.taskvault.ui.theme.PrimaryPillButton
import dev.boudy04.taskvault.ui.theme.TaskVaultTheme
import dev.boudy04.taskvault.ui.theme.resolvesDark
import dev.boudy04.taskvault.util.allowedAuthenticatorsCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            var fontChoice by remember { mutableStateOf(APP_FONT_FAMILY) }
            // ponytail: activity-scoped state instead of a VM; single consumer, no test churn
            LaunchedEffect(Unit) {
                settingsRepository.themeMode.collect { themeMode = it }
            }
            LaunchedEffect(Unit) {
                settingsRepository.fontFamily.collect { fontChoice = it }
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
            // Session gating runs before the lock gate: no identity -> picker.
            var signedIn by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) {
                settingsRepository.session.collect { session ->
                    signedIn = session.token.isNotBlank()
                }
            }
            TaskVaultTheme(themeMode = themeMode, fontChoice = fontChoice) {
                when (signedIn) {
                    null -> Unit
                    false -> LoginScreen()
                    true -> MainContent(
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
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MainContent(themeMode: ThemeMode, onCycleTheme: () -> Unit) {
    val todoViewModel: TodoViewModel = hiltViewModel()
    // Lock decision lives in the ViewModel: it survives configuration changes
    // (rotation, uiMode) but dies with the process, so an authenticated unlock
    // holds for the rest of the process lifetime exactly as specified.
    val locked by todoViewModel.lockState.collectAsStateWithLifecycle()
    when (locked) {
        false -> TodoNavGraph(
            themeMode = themeMode,
            onCycleTheme = onCycleTheme,
        )
        true -> LockGate(onUnlocked = todoViewModel::unlock)
        null -> Unit
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
                .setAllowedAuthenticators(allowedAuthenticatorsCompat())
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

/**
 * Holds the cold-start lock decision for [TodoActivity]. ViewModels survive configuration
 * changes (rotation, uiMode) but die with the process, so an authenticated unlock holds for
 * the rest of the process lifetime exactly as specified.
 */
@HiltViewModel
class TodoViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** null = still checking, true = gated, false = unlocked for this process. */
    private val _lockState = MutableStateFlow<Boolean?>(null)
    val lockState: StateFlow<Boolean?> = _lockState.asStateFlow()

    init {
        viewModelScope.launch {
            // Read once per process; later changes to the setting only apply on next cold start.
            _lockState.value = settingsRepository.appLock.first()
        }
    }

    fun unlock() {
        _lockState.value = false
    }
}
