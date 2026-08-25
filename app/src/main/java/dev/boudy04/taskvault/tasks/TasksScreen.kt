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

@file:OptIn(ExperimentalMaterial3Api::class)

package dev.boudy04.taskvault.tasks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.data.Task
import dev.boudy04.taskvault.settings.ThemeMode
import dev.boudy04.taskvault.tasks.TasksFilterType.ACTIVE_TASKS
import dev.boudy04.taskvault.tasks.TasksFilterType.ALL_TASKS
import dev.boudy04.taskvault.tasks.TasksFilterType.COMPLETED_TASKS
import dev.boudy04.taskvault.util.DueDates
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
            personalItems = uiState.personalItems,
            teamItems = uiState.items,
            pendingSyncIds = uiState.pendingSyncIds,
            availableGroups = uiState.availableGroups,
            selectedGroup = uiState.selectedGroup,
            onSelectGroup = viewModel::selectGroup,
            members = uiState.members,
            personFilter = uiState.personFilter,
            onSelectPerson = viewModel::selectPerson,
            statusFilter = uiState.statusFilter,
            onSelectStatus = viewModel::setFiltering,
            sort = uiState.sort,
            onSelectSort = viewModel::setSort,
            searchQuery = uiState.searchQuery,
            onSearchQueryChanged = viewModel::setSearchQuery,
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
    personalItems: List<Task>,
    teamItems: List<Task>,
    pendingSyncIds: Set<String>,
    availableGroups: List<String>,
    selectedGroup: String?,
    onSelectGroup: (String?) -> Unit,
    members: List<dev.boudy04.taskvault.data.source.network.MemberDto>,
    personFilter: Int?,
    onSelectPerson: (Int?) -> Unit,
    statusFilter: TasksFilterType,
    onSelectStatus: (TasksFilterType) -> Unit,
    sort: TasksSort,
    onSelectSort: (TasksSort) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onTaskCheckedChange: (Task, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPersonal by rememberSaveable { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            val segmentedColors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 4.dp)
            ) {
                SegmentedButton(
                    selected = showPersonal,
                    onClick = { showPersonal = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = segmentedColors
                ) {
                    Text(stringResource(R.string.section_personal))
                }
                SegmentedButton(
                    selected = !showPersonal,
                    onClick = { showPersonal = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = segmentedColors
                ) {
                    Text(stringResource(R.string.section_team))
                }
            }

            FilterBar(
                availableGroups = availableGroups,
                selectedGroup = selectedGroup,
                onSelectGroup = onSelectGroup,
                members = members,
                personFilter = personFilter,
                onSelectPerson = onSelectPerson,
                statusFilter = statusFilter,
                onSelectStatus = onSelectStatus,
                sort = sort,
                onSelectSort = onSelectSort,
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
            )

            if (showPersonal) {
                if (personalItems.isEmpty()) {
                    SectionEmpty(
                        headline = stringResource(R.string.empty_title_clear),
                        subtext = stringResource(R.string.empty_personal)
                    )
                } else {
                    TaskList(
                        tasks = personalItems,
                        members = members,
                        pendingSyncIds = pendingSyncIds,
                        onTaskClick = onTaskClick,
                        onTaskCheckedChange = onTaskCheckedChange
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.team_explainer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                if (teamItems.isEmpty()) {
                    SectionEmpty(
                        headline = stringResource(R.string.empty_title_nothing),
                        subtext = stringResource(R.string.empty_team)
                    )
                } else {
                    TaskList(
                        tasks = teamItems,
                        members = members,
                        pendingSyncIds = pendingSyncIds,
                        onTaskClick = onTaskClick,
                        onTaskCheckedChange = onTaskCheckedChange
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionEmpty(headline: String, subtext: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskList(
    tasks: List<Task>,
    members: List<dev.boudy04.taskvault.data.source.network.MemberDto>,
    pendingSyncIds: Set<String>,
    onTaskClick: (Task) -> Unit,
    onTaskCheckedChange: (Task, Boolean) -> Unit
) {
    LazyColumn {
        items(
            items = tasks,
            key = { it.id },
            contentType = { "task" }
        ) { task ->
            TaskItem(
                task = task,
                members = members,
                isUnsynced = task.id in pendingSyncIds,
                onTaskClick = onTaskClick,
                onCheckedChange = { onTaskCheckedChange(task, it) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

/**
 * UX v2 filter bar: Group / Person / Status dropdowns, Sort menu and a compact
 * expandable search field. All filters AND together.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(
    availableGroups: List<String>,
    selectedGroup: String?,
    onSelectGroup: (String?) -> Unit,
    members: List<dev.boudy04.taskvault.data.source.network.MemberDto>,
    personFilter: Int?,
    onSelectPerson: (Int?) -> Unit,
    statusFilter: TasksFilterType,
    onSelectStatus: (TasksFilterType) -> Unit,
    sort: TasksSort,
    onSelectSort: (TasksSort) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchOpen by rememberSaveable { mutableStateOf(searchQuery.isNotEmpty()) }
    var groupExpanded by remember { mutableStateOf(false) }
    var personExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterMenu(
                label = selectedGroup ?: stringResource(R.string.filter_group),
                expanded = groupExpanded,
                onExpandedChange = { groupExpanded = it },
                modifier = Modifier.weight(1.2f)
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tag_filter_all)) },
                    onClick = {
                        onSelectGroup(null)
                        groupExpanded = false
                    }
                )
                availableGroups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group) },
                        onClick = {
                            onSelectGroup(if (selectedGroup == group) null else group)
                            groupExpanded = false
                        }
                    )
                }
            }
            FilterMenu(
                label = when (personFilter) {
                    null -> stringResource(R.string.person_anyone)
                    TasksViewModel.PERSON_UNASSIGNED ->
                        stringResource(R.string.person_unassigned)
                    else -> members.firstOrNull { it.id == personFilter }?.username
                        ?: stringResource(R.string.person_anyone)
                },
                expanded = personExpanded,
                onExpandedChange = { personExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.person_anyone)) },
                    onClick = {
                        onSelectPerson(null)
                        personExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.person_unassigned)) },
                    onClick = {
                        onSelectPerson(TasksViewModel.PERSON_UNASSIGNED)
                        personExpanded = false
                    }
                )
                members.forEach { member ->
                    DropdownMenuItem(
                        text = { Text(member.username) },
                        onClick = {
                            onSelectPerson(member.id)
                            personExpanded = false
                        }
                    )
                }
            }
            FilterMenu(
                label = stringResource(
                    when (statusFilter) {
                        ALL_TASKS -> R.string.filter_status
                        ACTIVE_TASKS -> R.string.nav_active
                        COMPLETED_TASKS -> R.string.nav_completed
                    }
                ),
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = it },
                modifier = Modifier.weight(0.9f)
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tag_filter_all)) },
                    onClick = {
                        onSelectStatus(ALL_TASKS)
                        statusExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.nav_active)) },
                    onClick = {
                        onSelectStatus(ACTIVE_TASKS)
                        statusExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.nav_completed)) },
                    onClick = {
                        onSelectStatus(COMPLETED_TASKS)
                        statusExpanded = false
                    }
                )
            }
            Box(modifier = Modifier.width(48.dp)) {
                IconButton(onClick = { sortExpanded = true }) {
                    Icon(
                        Icons.Filled.Sort,
                        contentDescription = stringResource(R.string.filter_sort)
                    )
                }
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false }
                ) {
                    TasksSort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        when (option) {
                                            TasksSort.NEAREST_DUE -> R.string.sort_nearest_due
                                            TasksSort.NEWEST -> R.string.sort_newest
                                            TasksSort.OLDEST -> R.string.sort_oldest
                                            TasksSort.PRIORITY -> R.string.sort_priority
                                        }
                                    )
                                )
                            },
                            onClick = {
                                onSelectSort(option)
                                sortExpanded = false
                            }
                        )
                    }
                }
            }
            IconButton(onClick = { searchOpen = !searchOpen }) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = stringResource(R.string.cd_search_toggle)
                )
            }
        }

        AnimatedVisibility(visible = searchOpen) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

/** Compact outlined-pill dropdown used across the filter bar. */
@Composable
private fun FilterMenu(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (androidx.compose.foundation.layout.ColumnScope.() -> Unit)
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.padding(horizontal = 2.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 9.dp)
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            content = content
        )
    }
}

@Composable
private fun TaskItem(
    task: Task,
    members: List<dev.boudy04.taskvault.data.source.network.MemberDto>,
    isUnsynced: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hasMetadata = task.tags.isNotEmpty() || task.dueAt != null || task.assigneeIds.isNotEmpty()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTaskClick(task) }
                .padding(all = 14.dp)
        ) {
            MiniCheckbox(
                checked = task.isCompleted,
                onCheckedChange = { checked ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(checked)
                },
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (task.isCompleted) 0.45f else 1f)
            ) {
                Text(
                    text = task.titleForList,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (task.isCompleted) {
                        TextDecoration.LineThrough
                    } else {
                        null
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (hasMetadata) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        task.tags.forEach { tag -> MetaChip(text = tag) }
                        task.dueAt?.let { iso -> DueChip(iso = iso, isActive = task.isActive) }
                        Spacer(Modifier.weight(1f))
                        AssigneeBadges(assigneeIds = task.assigneeIds, members = members)
                    }
                }
            }
            SyncBadge(isUnsynced = isUnsynced, modifier = Modifier.padding(start = 10.dp))
        }
    }
}

/** Tiny outlined pill for row metadata (group names). */
@Composable
private fun MetaChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** Due pill: amber while upcoming, red once overdue. */
@Composable
private fun DueChip(iso: String, isActive: Boolean) {
    // ponytail: cached per (dueAt, active); overdue staleness within a minute is acceptable
    val (overdue, dueText) = remember(iso, isActive) {
        val isOverdue = runCatching {
            java.time.Instant.parse(iso).isBefore(java.time.Instant.now()) && isActive
        }.getOrDefault(false)
        isOverdue to DueDates.format(iso).orEmpty()
    }
    val dueColor = if (overdue) Color(0xFFB3261E) else Color(0xFFFFB300)
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = dueColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = dueText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = dueColor,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/** 24dp circle checkbox: primary fill + check when selected, hairline ring otherwise. */
@Composable
private fun MiniCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .then(
                if (checked) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                }
            )
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** Sync state icon, vertically centered at the card's trailing edge. */
@Composable
private fun SyncBadge(isUnsynced: Boolean, modifier: Modifier = Modifier) {
    if (isUnsynced) {
        Icon(
            Icons.Filled.CloudOff,
            contentDescription = stringResource(R.string.cd_sync_pending),
            tint = Color(0xFFFFB300),
            modifier = modifier
                .size(18.dp)
                .semantics { contentDescription = "Waiting to sync" }
        )
    } else {
        Icon(
            Icons.Filled.CloudDone,
            contentDescription = stringResource(R.string.cd_sync_synced),
            tint = Color(0xFF38693C).copy(alpha = 0.6f),
            modifier = modifier.size(18.dp)
        )
    }
}

// ponytail: fixed muted palette hashed by username; per-user theming would need server colors
private val assigneeColors = listOf(
    Color(0xFF7986CB), Color(0xFF4DB6AC), Color(0xFFE57373),
    Color(0xFFFFB74D), Color(0xFF9575CD), Color(0xFF81C784),
)

/** Colored initial circles for assigned members; shows at most 3 plus a "+n". */
@Composable
private fun AssigneeBadges(
    assigneeIds: List<Int>,
    members: List<dev.boudy04.taskvault.data.source.network.MemberDto>,
    modifier: Modifier = Modifier
) {
    if (assigneeIds.isEmpty()) return

    val shown = assigneeIds.take(MAX_BADGES)
    val overflow = assigneeIds.size - shown.size

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        shown.forEach { id ->
            val name = members.firstOrNull { it.id == id }?.username
            // Offline / unknown member: fall back to a stable id-derived initial.
            val initial = (name?.take(1) ?: ('A' + (Math.abs(id) % 26)).toString()).uppercase()
            val bg = assigneeColors[Math.abs((name ?: id.toString()).hashCode()) % assigneeColors.size]
            val desc = stringResource(R.string.cd_assignee_badge, name ?: initial)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp)
                    .background(bg, CircleShape)
                    .semantics { contentDescription = desc }
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
        if (overflow > 0) {
            Text(
                text = stringResource(R.string.assignees_more, overflow),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

private const val MAX_BADGES = 3

@Composable
private fun RequestNotificationsIfNeeded() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* denial degrades silently: in-app dot remains */ }
    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
