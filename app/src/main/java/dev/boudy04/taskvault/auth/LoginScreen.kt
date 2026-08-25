package dev.boudy04.taskvault.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.ui.theme.PrimaryPillButton

/**
 * Two-input login: a Name field plus a password field that stays empty for
 * members and carries the workspace key for the administrator.
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    uiState.snackbarRes?.let { res ->
        val text = stringResource(res)
        LaunchedEffect(res, snackbarHostState) {
            snackbarHostState.showSnackbar(text)
            viewModel.snackbarShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(72.dp))
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text(stringResource(R.string.login_name_hint)) },
                singleLine = true,
                enabled = !uiState.busy,
                isError = uiState.nameError,
                supportingText = {
                    if (uiState.nameError) {
                        Text(stringResource(R.string.login_error_invalid_name))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::updatePassword,
                label = { Text(stringResource(R.string.login_password_hint)) },
                singleLine = true,
                enabled = !uiState.busy,
                isError = uiState.keyError,
                visualTransformation = PasswordVisualTransformation(),
                supportingText = {
                    Text(
                        text = if (uiState.keyError) {
                            stringResource(R.string.login_error_wrong_key)
                        } else {
                            stringResource(R.string.login_helper)
                        },
                        color = if (uiState.keyError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            PrimaryPillButton(
                onClick = viewModel::enter,
                enabled = !uiState.busy && (uiState.name.isNotBlank() || uiState.password.isNotBlank()),
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text(stringResource(R.string.login_enter))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
