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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.data.Task
import dev.boudy04.taskvault.settings.ThemeMode
import dev.boudy04.taskvault.util.SnackbarHostEffect
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
        SnackbarHostEffect(
            userMessage = uiState.userMessage,
            snackbarHostState = snackbarHostState,
            onMessageShown = viewModel::snackbarMessageShown,
        )

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
