/*
 * Copyright 2026 The Android Open Source Project
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

package dev.boudy04.taskvault.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.boudy04.taskvault.ui.theme.PrimaryPillButton
import dev.boudy04.taskvault.ui.theme.APP_FONT_FAMILY
import dev.boudy04.taskvault.ui.theme.SYSTEM_FONT_FAMILY
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.TodoTheme
import dev.boudy04.taskvault.util.AddEditTaskTopAppBar
import dev.boudy04.taskvault.util.allowedAuthenticatorsCompat
import timber.log.Timber

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AddEditTaskTopAppBar(R.string.settings_title, onBack) }
    ) { paddingValues ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val teamViewModel: TeamViewModel = hiltViewModel()
        val teamState by teamViewModel.uiState.collectAsStateWithLifecycle()

        SettingsContent(
            baseUrl = uiState.baseUrl,
            token = uiState.token,
            appLock = uiState.appLock,
            fontFamily = uiState.fontFamily,
            onBaseUrlChanged = viewModel::updateBaseUrl,
            onTokenChanged = viewModel::updateToken,
            onAppLockChanged = viewModel::setAppLock,
            onFontFamilyChanged = viewModel::setFontFamily,
            onSave = viewModel::saveConfig,
            teamContent = {
                TeamSection(
                    state = teamState,
                    onNewNameChanged = teamViewModel::updateNewName,
                    onAdd = teamViewModel::addMember,
                    onRemove = teamViewModel::removeMember
                )
            },
            modifier = Modifier.padding(paddingValues)
        )

        uiState.resultText?.let { resultText ->
            LaunchedEffect(snackbarHostState, viewModel, resultText) {
                snackbarHostState.showSnackbar(resultText)
                viewModel.resultMessageShown()
            }
        }
    }
}

@Composable
private fun SettingsContent(
    baseUrl: String,
    token: String,
    appLock: Boolean,
    fontFamily: String,
    onBaseUrlChanged: (String) -> Unit,
    onTokenChanged: (String) -> Unit,
    onAppLockChanged: (Boolean) -> Unit,
    onFontFamilyChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    teamContent: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* denied is fine: connectivity notification silently no-ops, dot badge remains */ }

    // Ask once per Save press on API 33+; saving itself is never blocked by it.
    val onSaveWithPermission = {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        onSave()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(all = dimensionResource(id = R.dimen.horizontal_margin))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChanged,
            label = { Text(stringResource(id = R.string.server_url_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChanged,
            label = { Text(stringResource(id = R.string.auth_token_hint)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.app_lock),
                style = MaterialTheme.typography.titleMedium
            )
            Switch(
                checked = appLock,
                // Enabling only after a successful biometric/credential verify, so users can
                // never lock themselves out without a working authenticator.
                onCheckedChange = { checked ->
                    if (checked) {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            activity.verifyThen {
                                onAppLockChanged(true)
                            }
                        } else {
                            // Never silently dead-end users on an unexpected host context.
                            Timber.w("Host context is not a FragmentActivity; enabling App lock without verification")
                            onAppLockChanged(true)
                        }
                    } else {
                        onAppLockChanged(false)
                    }
                }
            )
        }
        Text(
            text = stringResource(id = R.string.font_section_title),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = fontFamily == APP_FONT_FAMILY,
                onClick = { onFontFamilyChanged(APP_FONT_FAMILY) },
                label = { Text(stringResource(id = R.string.font_family_taskvault)) }
            )
            FilterChip(
                selected = fontFamily == SYSTEM_FONT_FAMILY,
                onClick = { onFontFamilyChanged(SYSTEM_FONT_FAMILY) },
                label = { Text(stringResource(id = R.string.font_family_system)) }
            )
        }
        PrimaryPillButton(
            onClick = onSaveWithPermission,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(stringResource(id = R.string.settings_save))
        }
        teamContent()
    }
}

// The API does not expose an owner flag yet, so the owner row is recognized by
// its username until then.
const val OWNER_USERNAME = "boudy04"

@Composable
private fun TeamSection(
    state: TeamUiState,
    onNewNameChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.team_section_title),
            style = MaterialTheme.typography.titleMedium
        )
        state.members.forEach { member ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(member.username)
                if (member.username != OWNER_USERNAME) {
                    IconButton(onClick = { onRemove(member.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(id = R.string.cd_remove_member)
                        )
                    }
                }
            }
        }
        state.errorRes?.let { errorRes ->
            Text(
                text = stringResource(id = errorRes),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.newName,
                onValueChange = onNewNameChanged,
                label = { Text(stringResource(id = R.string.team_add_hint)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            PrimaryPillButton(onClick = onAdd) {
                Text(stringResource(id = R.string.team_add))
            }
        }
    }
}

private fun FragmentActivity.verifyThen(onSuccess: () -> Unit) {
    val prompt = BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(this),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_lock_verify))
            .setAllowedAuthenticators(allowedAuthenticatorsCompat())
            .build()
    )
}

@Preview
@Composable
private fun SettingsContentPreview() {
    TodoTheme {
        SettingsContent(
            baseUrl = "https://example.com",
            token = "token",
            appLock = false,
            fontFamily = APP_FONT_FAMILY,
            onBaseUrlChanged = { },
            onTokenChanged = { },
            onAppLockChanged = { },
            onFontFamilyChanged = { },
            onSave = { }
        )
    }
}
