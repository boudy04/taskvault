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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.boudy04.taskvault.ui.theme.PrimaryPillButton
import dev.boudy04.taskvault.ui.theme.QuietPillButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.TodoTheme
import dev.boudy04.taskvault.util.AddEditTaskTopAppBar

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

        SettingsContent(
            baseUrl = uiState.baseUrl,
            token = uiState.token,
            onBaseUrlChanged = viewModel::updateBaseUrl,
            onTokenChanged = viewModel::updateToken,
            onSave = viewModel::saveConfig,
            onTestConnection = viewModel::testConnection,
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
    onBaseUrlChanged: (String) -> Unit,
    onTokenChanged: (String) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.list_item_padding))
        ) {
            PrimaryPillButton(onClick = onSave) {
                Text(stringResource(id = R.string.settings_save))
            }
            QuietPillButton(onClick = onTestConnection) {
                Text(stringResource(id = R.string.settings_test_connection))
            }
        }
    }
}

@Preview
@Composable
private fun SettingsContentPreview() {
    TodoTheme {
        SettingsContent(
            baseUrl = "https://example.com",
            token = "token",
            onBaseUrlChanged = { },
            onTokenChanged = { },
            onSave = { },
            onTestConnection = { }
        )
    }
}
