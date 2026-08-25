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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.TextButton
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
import dev.boudy04.taskvault.data.TaskNote
import dev.boudy04.taskvault.settings.ThemeMode
import dev.boudy04.taskvault.tasks.TasksFilterType.ACTIVE_TASKS
import dev.boudy04.taskvault.tasks.TasksFilterType.ALL_TASKS
import dev.boudy04.taskvault.tasks.TasksFilterType.COMPLETED_TASKS
import dev.boudy04.taskvault.util.DueDates
import dev.boudy04.taskvault.util.TasksTopAppBar

/** Which list the segmented control shows; also drives the FAB create context. */
private enum class Tab { PERSONAL, TEAM }

@Composable
fun TasksScreen(
    @StringRes userMessage: Int,
    onAddTask: (isPersonal: Boolean) -> Unit,
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

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(Tab.TEAM) }
    // The row whose note sheet is open; null = closed.
    var notesTask by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TasksTopAppBar(
                openDrawer = openDrawer,
                onClearCompletedTasks = { viewModel.clearCompletedTasks() },
                onRefresh = { viewModel.refresh() },
                onSettingsClick = onSettingsClick,
                themeMode = themeMode,
                onToggleTheme = onCycleTheme
            )
        },
        floatingActionButton = {
            // Members create only LOCAL-ONLY personal tasks; admins also create team tasks.
            if (!uiState.isMember || currentTab == Tab.PERSONAL) {
                SmallFloatingActionButton(
                    onClick = { onAddTask(currentTab == Tab.PERSONAL) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, stringResource(id = R.string.add_task))
                }
            }
        }
    ) { paddingValues ->
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
            filtersOpen = uiState.filtersOpen,
            activeFilterCount = uiState.activeFilterCount,
            onOpenFilters = viewModel::openFilters,
            onCloseFilters = viewModel::closeFilters,
            onResetFilters = viewModel::resetFilters,
            currentTab = currentTab,
            onTabChange = { currentTab = it },
            isMember = uiState.isMember,
            sessionUserId = uiState.sessionUserId,
            sessionUsername = uiState.sessionUsername,
            onRefresh = viewModel::refresh,
            onTaskClick = onTaskClick,
            onTaskCheckedChange = viewModel::completeTask,
            onOpenNotes = { notesTask = it },
            modifier = Modifier.padding(paddingValues)
        )

        notesTask?.let { task ->
            NoteSheet(
                task = task,
                members = uiState.members,
                onAddNote = { body -> viewModel.addNote(task, body) },
                onDismiss = { notesTask = null }
            )
        }

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
    filtersOpen: Boolean,
    activeFilterCount: Int,
    onOpenFilters: () -> Unit,
    onCloseFilters: () -> Unit,
    onResetFilters: () -> Unit,
    currentTab: Tab,
    onTabChange: (Tab) -> Unit,
    isMember: Boolean,
    sessionUserId: Int,
    sessionUsername: String,
    onRefresh: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onTaskCheckedChange: (Task, Boolean) -> Unit,
    onOpenNotes: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val segmentedColors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                SegmentedButton(
                    selected = currentTab == Tab.PERSONAL,
                    onClick = { onTabChange(Tab.PERSONAL) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = segmentedColors
                ) {
                    Text(stringResource(R.string.section_personal))
                }
                SegmentedButton(
                    selected = currentTab == Tab.TEAM,
                    onClick = { onTabChange(Tab.TEAM) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = segmentedColors
                ) {
                    Text(stringResource(R.string.section_team))
                }
            }

            Spacer(Modifier.height(12.dp))
            // ONE chic entry point for every filter; the badge counts active ones.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.clickable(onClick = onOpenFilters)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 14.dp, end = 10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.filters_button),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 9.dp)
                        )
                        if (activeFilterCount > 0) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Text(
                                    text = activeFilterCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
            }

            if (isMember) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.member_banner, sessionUsername),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (currentTab == Tab.PERSONAL) {
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
                        showNotes = false,
                        checkEnabled = true,
                        onTaskClick = onTaskClick,
                        onTaskCheckedChange = onTaskCheckedChange,
                        onOpenNotes = onOpenNotes
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.team_explainer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                if (teamItems.isEmpty()) {
                    SectionEmpty(
                        headline = stringResource(R.string.empty_title_nothing),
                        subtext = stringResource(R.string.empty_team)
                    )
                } else {
                    val general = teamItems.filter { it.assigneeIds.isEmpty() }
                    val mine = teamItems.filter { sessionUserId in it.assigneeIds }
                    SectionHeader(stringResource(R.string.section_general), general.size)
                    TaskList(
                        tasks = general,
                        members = members,
                        pendingSyncIds = pendingSyncIds,
                        showNotes = true,
                        // Members flip status only on their own assigned tasks.
                        checkEnabled = !isMember,
                        onTaskClick = onTaskClick,
                        onTaskCheckedChange = onTaskCheckedChange,
                        onOpenNotes = onOpenNotes,
                        perRowCheckEnabled = { !isMember }
                    )
                    Spacer(Modifier.height(24.dp))
                    if (mine.isNotEmpty()) {
                        SectionHeader(stringResource(R.string.section_assigned_you), mine.size)
                        TaskList(
                            tasks = mine,
                            members = members,
                            pendingSyncIds = pendingSyncIds,
                            showNotes = true,
                            checkEnabled = true,
                            onTaskClick = onTaskClick,
                            onTaskCheckedChange = onTaskCheckedChange,
                            onOpenNotes = onOpenNotes,
                            perRowCheckEnabled = { !isMember || sessionUserId in it.assigneeIds }
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (filtersOpen) {
            FiltersSheet(
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
                onReset = onResetFilters,
                onDismiss = onCloseFilters
            )
        }
    }
}

@Composable
private fun SectionHeader(label: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionEmpty(headline: String, subtext: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
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
    showNotes: Boolean,
    checkEnabled: Boolean,
    onTaskClick: (Task) -> Unit,
    onTaskCheckedChange: (Task, Boolean) -> Unit,
    onOpenNotes: (Task) -> Unit,
    perRowCheckEnabled: (Task) -> Boolean = { checkEnabled }
) {
    Column {
        tasks.forEach { task ->
            TaskItem(
                task = task,
                members = members,
                isUnsynced = task.id in pendingSyncIds,
                showNotes = showNotes,
                checkEnabled = perRowCheckEnabled(task),
                onTaskClick = onTaskClick,
                onCheckedChange = { onTaskCheckedChange(task, it) },
                onOpenNotes = { onOpenNotes(task) }
            )
        }
    }
}

/**
 * UX v3 consolidated Filters sheet: Group / Person / Status dropdowns, Sort
 * selector, Search field and Reset. Everything applies live.
 */
@Composable
private fun FiltersSheet(
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
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var groupExpanded by remember { mutableStateOf(false) }
    var personExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.filters_button),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.filters_reset))
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            FilterMenu(
                label = selectedGroup ?: stringResource(R.string.filter_group),
                expanded = groupExpanded,
                onExpandedChange = { groupExpanded = it },
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
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
            FilterMenu(
                label = stringResource(
                    when (sort) {
                        TasksSort.NEAREST_DUE -> R.string.sort_nearest_due
                        TasksSort.NEWEST -> R.string.sort_newest
                        TasksSort.OLDEST -> R.string.sort_oldest
                        TasksSort.PRIORITY -> R.string.sort_priority
                    }
                ),
                expanded = sortExpanded,
                onExpandedChange = { sortExpanded = it },
                modifier = Modifier.fillMaxWidth()
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
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Compact outlined-pill dropdown used inside the Filters sheet. */
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
        modifier = modifier
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

/** Notes sheet: existing notes plus an inline composer; posts directly to the server. */
@Composable
private fun NoteSheet(
    task: Task,
    members: List<dev.boudy04.taskvault.data.source.network.MemberDto>,
    onAddNote: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.notes_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (task.notes.isEmpty()) {
                Text(
                    text = stringResource(R.string.note_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                task.notes.forEach { note ->
                    NoteRow(note = note, members = members)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(R.string.note_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        val body = draft.trim()
                        if (body.isNotEmpty()) {
                            onAddNote(body)
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank()
                ) {
                    Text(stringResource(R.string.note_add))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NoteRow(
    note: TaskNote,
    members: List<dev.boudy04.taskvault.data.source.network.MemberDto>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = members.firstOrNull { it.username == note.author }?.username ?: note.author,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = DueDates.relative(note.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = note.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun TaskItem(
    task: Task,
    members: List<dev.boudy04.taskvault.data.source.network.MemberDto>,
    isUnsynced: Boolean,
    showNotes: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTaskClick: (Task) -> Unit,
    onOpenNotes: () -> Unit,
    checkEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val latestNote = task.notes.firstOrNull()
    val hasMetadata = task.tags.isNotEmpty() || task.dueAt != null ||
        task.assigneeIds.isNotEmpty() || latestNote != null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTaskClick(task) }
                .padding(all = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniCheckbox(
                    checked = task.isCompleted,
                    enabled = checkEnabled,
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
                    latestNote?.let { note ->
                        Text(
                            text = "${note.author}: ${note.body}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                if (showNotes) {
                    IconButton(onClick = onOpenNotes, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.Notes,
                            contentDescription = stringResource(R.string.cd_open_notes),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                SyncBadge(isUnsynced = isUnsynced, modifier = Modifier.padding(start = 10.dp))
            }
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
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .then(
                if (checked) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                }
            )
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                enabled = enabled,
                onValueChange = onCheckedChange
            )
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
