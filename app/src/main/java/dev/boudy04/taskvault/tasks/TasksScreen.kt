/*
 * Copyright 2022 The Android Open Source Project
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

package dev.boudy04.taskvault.tasks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.TodoTheme
import dev.boudy04.taskvault.settings.ThemeMode
import dev.boudy04.taskvault.data.Task
import dev.boudy04.taskvault.data.TaskStatus
import dev.boudy04.taskvault.data.TaskPriority
import dev.boudy04.taskvault.tasks.TasksFilterType.ACTIVE_TASKS
import dev.boudy04.taskvault.tasks.TasksFilterType.ALL_TASKS
import dev.boudy04.taskvault.tasks.TasksFilterType.COMPLETED_TASKS
import dev.boudy04.taskvault.util.DueDates
import dev.boudy04.taskvault.util.LoadingContent
import dev.boudy04.taskvault.util.TasksTopAppBar

@Composable
fun TasksScreen(
    @StringRes userMessage: Int,
    onAddTask: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onUserMessageDisplayed: () -> Unit,
    openDrawer: () -> Unit,
    onSettingsClick: () -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onCycleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    RequestNotificationsIfNeeded()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TasksTopAppBar(
                openDrawer = openDrawer,
                onFilterAllTasks = { viewModel.setFiltering(ALL_TASKS) },
                onFilterActiveTasks = { viewModel.setFiltering(ACTIVE_TASKS) },
                onFilterCompletedTasks = { viewModel.setFiltering(COMPLETED_TASKS) },
                onClearCompletedTasks = { viewModel.clearCompletedTasks() },
                onRefresh = { viewModel.refresh() },
                onSettingsClick = onSettingsClick,
                themeMode = themeMode,
                onToggleTheme = onCycleTheme
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = onAddTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, stringResource(id = R.string.add_task))
            }
        }
    ) { paddingValues ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        TasksContent(
            loading = uiState.isLoading,
            tasks = uiState.items,
            pendingSyncIds = uiState.pendingSyncIds,
            currentFilteringLabel = uiState.filteringUiInfo.currentFilteringLabel,
            noTasksLabel = uiState.filteringUiInfo.noTasksLabel,
            noTasksIconRes = uiState.filteringUiInfo.noTaskIconRes,
            onRefresh = viewModel::refresh,
            onTaskClick = onTaskClick,
            onTaskCheckedChange = viewModel::completeTask,
            modifier = Modifier.padding(paddingValues)
        )

        // Check for user messages to display on the screen
        uiState.userMessage?.let { message ->
            val snackbarText = stringResource(message)
            LaunchedEffect(snackbarHostState, viewModel, message, snackbarText) {
                snackbarHostState.showSnackbar(snackbarText)
                viewModel.snackbarMessageShown()
            }
        }

        // Check if there's a userMessage to show to the user
        val currentOnUserMessageDisplayed by rememberUpdatedState(onUserMessageDisplayed)
        LaunchedEffect(userMessage) {
            if (userMessage != 0) {
                viewModel.showEditResultMessage(userMessage)
                currentOnUserMessageDisplayed()
            }
        }
    }
}

@Composable
private fun TasksContent(
    loading: Boolean,
    tasks: List<Task>,
    pendingSyncIds: Set<String>,
    @StringRes currentFilteringLabel: Int,
    @StringRes noTasksLabel: Int,
    @DrawableRes noTasksIconRes: Int,
    onRefresh: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onTaskCheckedChange: (Task, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LoadingContent(
        loading = loading,
        empty = tasks.isEmpty() && !loading,
        emptyContent = { TasksEmptyContent(noTasksLabel, noTasksIconRes, modifier) },
        onRefresh = onRefresh
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(id = R.dimen.horizontal_margin))
        ) {
            Text(
                text = stringResource(currentFilteringLabel),
                modifier = Modifier.padding(
                    horizontal = dimensionResource(id = R.dimen.list_item_padding),
                    vertical = dimensionResource(id = R.dimen.vertical_margin)
                ),
                style = MaterialTheme.typography.headlineSmall
            )
            LazyColumn {
                items(
                    items = tasks,
                    key = { it.id },
                    contentType = { "task" }
                ) { task ->
                    TaskItem(
                        task = task,
                        isUnsynced = task.id in pendingSyncIds,
                        onTaskClick = onTaskClick,
                        onCheckedChange = { onTaskCheckedChange(task, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskItem(
    task: Task,
    isUnsynced: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTaskClick: (Task) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 12.dp,
                )
                .clickable { onTaskClick(task) }
        ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = onCheckedChange
        )
        PriorityChip(priority = task.priority)
        Text(
            text = task.titleForList,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .weight(1f)
                .padding(start = dimensionResource(id = R.dimen.horizontal_margin)),
            textDecoration = if (task.isCompleted) {
                TextDecoration.LineThrough
            } else {
                null
            }
        )
        task.dueAt?.let { iso ->
            // ponytail: cached per (dueAt, active); overdue staleness within a minute is acceptable
            val (overdue, dueText) = remember(iso, task.isActive) {
                val isOverdue = runCatching {
                    java.time.Instant.parse(iso).isBefore(java.time.Instant.now()) && task.isActive
                }.getOrDefault(false)
                isOverdue to DueDates.format(iso).orEmpty()
            }
            val dueColor = if (overdue) Color(0xFFB3261E) else MaterialTheme.colorScheme.onSurfaceVariant
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = dueColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = dueText,
                    style = MaterialTheme.typography.labelSmall,
                    color = dueColor,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        if (isUnsynced) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = stringResource(R.string.cd_sync_pending),
                tint = Color(0xFFFFB300),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(16.dp)
                    .semantics { contentDescription = "Waiting to sync" }
            )
        } else {
            Icon(
                Icons.Filled.CloudDone,
                contentDescription = stringResource(R.string.cd_sync_synced),
                tint = Color(0xFF38693C).copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 8.dp).size(16.dp)
            )
        }
    }
    }
}

private val priorityContainerColors = mapOf(
    TaskPriority.HIGH to Color(0xFFB3261E),
    TaskPriority.MEDIUM to Color(0xFF7D5700),
    TaskPriority.LOW to Color(0xFF38693C)
)

@Composable
private fun PriorityChip(priority: TaskPriority) {
    val label = stringResource(
        when (priority) {
            TaskPriority.HIGH -> R.string.priority_high
            TaskPriority.MEDIUM -> R.string.priority_medium
            TaskPriority.LOW -> R.string.priority_low
        }
    )
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = priorityContainerColors.getValue(priority),
        contentColor = Color.White
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TasksEmptyContent(
    @StringRes noTasksLabel: Int,
    @DrawableRes noTasksIconRes: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = noTasksIconRes),
            contentDescription = stringResource(R.string.no_tasks_image_content_description),
            modifier = Modifier.size(96.dp)
        )
        Text(stringResource(id = noTasksLabel))
    }
}

@Preview
@Composable
private fun TasksContentPreview() {
    MaterialTheme {
        Surface {
            TasksContent(
                loading = false,
                tasks = listOf(
                    Task(
                        title = "Title 1",
                        description = "Description 1",
                        id = "ID 1"
                    ),
                    Task(
                        title = "Title 2",
                        description = "Description 2",
                        status = TaskStatus.DONE,
                        id = "ID 2"
                    ),
                    Task(
                        title = "Title 3",
                        description = "Description 3",
                        status = TaskStatus.DONE,
                        id = "ID 3"
                    ),
                    Task(
                        title = "Title 4",
                        description = "Description 4",
                        id = "ID 4"
                    ),
                    Task(
                        title = "Title 5",
                        description = "Description 5",
                        status = TaskStatus.DONE,
                        id = "ID 5"
                    ),
                ),
                currentFilteringLabel = R.string.label_all,
                noTasksLabel = R.string.no_tasks_all,
                noTasksIconRes = R.drawable.logo_no_fill,
                onRefresh = { },
                onTaskClick = { },
                onTaskCheckedChange = { _, _ -> },
                pendingSyncIds = setOf("ID 1")
            )
        }
    }
}

@Preview
@Composable
private fun TasksContentEmptyPreview() {
    MaterialTheme {
        Surface {
            TasksContent(
                loading = false,
                tasks = emptyList(),
                currentFilteringLabel = R.string.label_all,
                noTasksLabel = R.string.no_tasks_all,
                noTasksIconRes = R.drawable.logo_no_fill,
                onRefresh = { },
                onTaskClick = { },
                onTaskCheckedChange = { _, _ -> },
                pendingSyncIds = emptySet()
            )
        }
    }
}

@Preview
@Composable
private fun TasksEmptyContentPreview() {
    TodoTheme {
        Surface {
            TasksEmptyContent(
                noTasksLabel = R.string.no_tasks_all,
                noTasksIconRes = R.drawable.logo_no_fill
            )
        }
    }
}

@Preview
@Composable
private fun TaskItemPreview() {
    MaterialTheme {
        Surface {
            TaskItem(
                task = Task(
                    title = "Title",
                    description = "Description",
                    id = "ID"
                ),
                onTaskClick = { },
                onCheckedChange = { },
                isUnsynced = false
            )
        }
    }
}

@Preview
@Composable
private fun TaskItemCompletedPreview() {
    MaterialTheme {
        Surface {
            TaskItem(
                task = Task(
                    title = "Title",
                    description = "Description",
                    status = TaskStatus.DONE,
                    id = "ID"
                ),
                onTaskClick = { },
                onCheckedChange = { },
                isUnsynced = true
            )
        }
    }
}

@Composable
private fun RequestNotificationsIfNeeded() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* denial degrades silently: in-app dot remains */ }
    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
