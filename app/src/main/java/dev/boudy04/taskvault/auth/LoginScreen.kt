package dev.boudy04.taskvault.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.ui.theme.PrimaryPillButton

/**
 * Identity picker shown while no session exists. One tap per member row; the
 * administrator row expands a masked workspace-key field.
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
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(72.dp))
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(28.dp))

            uiState.members.forEach { member ->
                IdentityRow(
                    label = member.username,
                    leading = { InitialBadge(member.username) },
                    enabled = !uiState.busy,
                    onClick = { viewModel.loginAs(member) },
                )
            }
            if (uiState.membersFailed && uiState.members.isEmpty()) {
                TextButton(onClick = viewModel::loadMembers) {
                    Text(stringResource(R.string.login_error_offline))
                }
            }

            Spacer(Modifier.height(8.dp))
            if (uiState.adminExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.workspaceKey,
                        onValueChange = viewModel::updateWorkspaceKey,
                        label = { Text(stringResource(R.string.login_workspace_key_hint)) },
                        singleLine = true,
                        enabled = !uiState.busy,
                        isError = uiState.keyError,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (uiState.keyError) {
                        Text(
                            text = stringResource(R.string.login_error_wrong_key),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    PrimaryPillButton(
                        onClick = viewModel::verifyAdmin,
                        enabled = !uiState.busy && uiState.workspaceKey.isNotBlank(),
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text(stringResource(R.string.login_verify))
                    }
                }
            } else {
                IdentityRow(
                    label = stringResource(R.string.login_administrator),
                    leading = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    enabled = !uiState.busy,
                    onClick = { viewModel.toggleAdmin(true) },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** 56dp tappable identity row: leading badge/icon + label. */
@Composable
private fun IdentityRow(
    label: String,
    leading: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            leading()
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

// ponytail: same muted palette as TasksScreen assignee badges; shared theme would need a theme module
private val initialColors = listOf(
    Color(0xFF7986CB), Color(0xFF4DB6AC), Color(0xFFE57373),
    Color(0xFFFFB74D), Color(0xFF9575CD), Color(0xFF81C784),
)

@Composable
private fun InitialBadge(username: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .background(initialColors[Math.abs(username.hashCode()) % initialColors.size], CircleShape),
    ) {
        Text(
            text = username.take(1).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = Color.White,
        )
    }
}
