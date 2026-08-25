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

package dev.boudy04.taskvault.addedittask

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.data.GROUP_PRESETS
import dev.boudy04.taskvault.data.TaskPriority
import dev.boudy04.taskvault.data.source.network.MemberDto
import dev.boudy04.taskvault.util.AddEditTaskTopAppBar
import dev.boudy04.taskvault.util.DueDates
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun AddEditTaskScreen(
    @StringRes topBarTitle: Int,
    onTaskUpdate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditTaskViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AddEditTaskTopAppBar(topBarTitle, onBack) },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = viewModel::saveTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Done, stringResource(id = R.string.cd_save_task))
            }
        }
    ) { paddingValues ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val priority by viewModel.priority.collectAsStateWithLifecycle()
        val dueAt by viewModel.dueAt.collectAsStateWithLifecycle()
        val tags by viewModel.tags.collectAsStateWithLifecycle()
        val tagSuggestions by viewModel.tagSuggestions.collectAsStateWithLifecycle()
        val members by viewModel.members.collectAsStateWithLifecycle()
        val assigneeIds by viewModel.assigneeIds.collectAsStateWithLifecycle()

        val haptic = LocalHapticFeedback.current
        LaunchedEffect(uiState.isTaskSaved) {
            if (uiState.isTaskSaved) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onTaskUpdate()
            }
        }

        AddEditTaskContent(
            loading = uiState.isLoading,
            title = uiState.title,
            description = uiState.description,
            onTitleChanged = viewModel::updateTitle,
            onDescriptionChanged = viewModel::updateDescription,
            priority = priority,
            onPriorityChanged = viewModel::updatePriority,
            dueAt = dueAt,
            onDueAtChanged = viewModel::updateDueAt,
            tags = tags,
            tagSuggestions = tagSuggestions.filter { it !in tags },
            onTagAdded = viewModel::addTag,
            onTagRemoved = viewModel::removeTag,
            members = members,
            assigneeIds = assigneeIds,
            onToggleAssignee = viewModel::toggleAssignee,
            onAssigneesSheetOpened = viewModel::reloadMembers,
            modifier = Modifier.padding(paddingValues)
        )

        // Check if the task is saved and call onTaskUpdate event
        LaunchedEffect(uiState.isTaskSaved) {
            if (uiState.isTaskSaved) {
                onTaskUpdate()
            }
        }

        // Check for user messages to display on the screen
        uiState.userMessage?.let { userMessage ->
            val snackbarText = stringResource(userMessage)
            LaunchedEffect(snackbarHostState, viewModel, userMessage, snackbarText) {
                snackbarHostState.showSnackbar(snackbarText)
                viewModel.snackbarMessageShown()
            }
        }
    }
}

@Composable
private fun AddEditTaskContent(
    loading: Boolean,
    title: String,
    description: String,
    onTitleChanged: (String) -> Unit,
    priority: TaskPriority,
    onPriorityChanged: (TaskPriority) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    dueAt: String?,
    onDueAtChanged: (String?) -> Unit,
    tags: List<String>,
    tagSuggestions: List<String>,
    onTagAdded: (String) -> Unit,
    onTagRemoved: (String) -> Unit,
    members: List<MemberDto>,
    assigneeIds: List<Int>,
    onToggleAssignee: (Int) -> Unit,
    onAssigneesSheetOpened: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshingState = rememberPullToRefreshState()
    if (loading) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = refreshingState,
            onRefresh = { /* DO NOTHING */ },
            content = { }
        )
    } else {
        Column(
            modifier
                .fillMaxWidth()
                .padding(all = dimensionResource(id = R.dimen.horizontal_margin))
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onSecondary
            )
            OutlinedTextField(
                value = title,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onTitleChanged,
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.title_hint),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall
                    .copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                colors = textFieldColors
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChanged,
                placeholder = { Text(stringResource(id = R.string.description_hint)) },
                modifier = Modifier
                    .height(350.dp)
                    .fillMaxWidth(),
                colors = textFieldColors
            )
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PriorityPicker(priority = priority, onPriorityChanged = onPriorityChanged)
                DuePicker(dueAt = dueAt, onDueAtChanged = onDueAtChanged)
                var showGroupsSheet by remember { mutableStateOf(false) }
                FieldRow(
                    label = stringResource(id = R.string.groups_label),
                    value = if (tags.isEmpty()) {
                        stringResource(id = R.string.groups_none)
                    } else {
                        tags.joinToString(", ")
                    },
                    onClick = { showGroupsSheet = true }
                )
                var showAssigneesSheet by remember { mutableStateOf(false) }
                val assigneeNames = members
                    .filter { it.id in assigneeIds }
                    .joinToString(", ") { it.username }
                FieldRow(
                    label = stringResource(id = R.string.assignees_label),
                    value = assigneeNames.ifEmpty { stringResource(id = R.string.assignees_none) },
                    onClick = {
                        onAssigneesSheetOpened()
                        showAssigneesSheet = true
                    }
                )
                if (showGroupsSheet) {
                    GroupsSheet(
                        selected = tags,
                        suggestions = tagSuggestions,
                        onAdd = onTagAdded,
                        onRemove = onTagRemoved,
                        onDismiss = { showGroupsSheet = false }
                    )
                }
                if (showAssigneesSheet) {
                    AssigneesSheet(
                        members = members,
                        selectedIds = assigneeIds,
                        onToggle = onToggleAssignee,
                        onDismiss = { showAssigneesSheet = false }
                    )
                }
            }
        }
    }
}

/**
 * Unified field row: muted label above value, hairline divider below, chevron
 * on tappable rows. 56dp min height per visual-polish spec.
 */
@Composable
private fun FieldRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp, bottom = 10.dp)
            )
            if (onClick != null) {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
    }
}

/**
 * Groups picker per UX v2: FilterChips for the presets plus every previously used
 * group; free-text entry is demoted to a single "Custom..." option at the bottom.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupsSheet(
    selected: List<String>,
    suggestions: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var showCustom by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (GROUP_PRESETS + suggestions + selected).distinct().forEach { group ->
                FilterChip(
                    selected = group in selected,
                    onClick = { if (group in selected) onRemove(group) else onAdd(group) },
                    label = { Text(group) },
                )
            }
        }
        TextButton(onClick = { showCustom = !showCustom }) {
            Text(stringResource(id = R.string.groups_custom))
        }
        if (showCustom) {
            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it },
                placeholder = { Text(stringResource(id = R.string.groups_custom_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (custom.isNotBlank()) onAdd(custom)
                        custom = ""
                        showCustom = false
                    }
                ),
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** Assignee picker: checkbox list of workspace members + Done button. */
@Composable
private fun AssigneesSheet(
    members: List<MemberDto>,
    selectedIds: List<Int>,
    onToggle: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(id = R.string.assignees_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            members.forEach { member ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(member.id) }
                        .padding(horizontal = 16.dp),
                ) {
                    Checkbox(
                        checked = member.id in selectedIds,
                        onCheckedChange = { onToggle(member.id) }
                    )
                    Text(member.username)
                }
            }
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(stringResource(id = R.string.sheet_done))
        }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * "Due" row + pick flow per R20: chips (Today / Tomorrow / Pick date…) then Material3
 * TimePicker; [Clear] removes the due. The ViewModel stores ISO-8601 UTC.
 */
@Composable
private fun DuePicker(
    dueAt: String?,
    onDueAtChanged: (String?) -> Unit,
) {
    var showChips by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }

    FieldRow(
        label = stringResource(id = R.string.due_label),
        value = DueDates.format(dueAt) ?: stringResource(id = R.string.due_none),
        onClick = { showChips = true }
    )

    if (showChips) {
        val selectedTime = dueAt?.let {
            runCatching { DueDates.toLocalDateTime(it).toLocalTime() }.getOrNull()
        } ?: DueDates.nextFullHour().toLocalTime()

        AlertDialog(
            onDismissRequest = { showChips = false },
            title = { Text(stringResource(id = R.string.due_label)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                val local = LocalDate.now().atTime(selectedTime)
                                onDueAtChanged(DueDates.toIso(local))
                                showChips = false
                            },
                            label = { Text(stringResource(id = R.string.due_today)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                val local = LocalDate.now().plusDays(1).atTime(selectedTime)
                                onDueAtChanged(DueDates.toIso(local))
                                showChips = false
                            },
                            label = { Text(stringResource(id = R.string.due_tomorrow)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                showChips = false
                                showDatePicker = true
                            },
                            label = { Text(stringResource(id = R.string.due_pick_date)) }
                        )
                    }
                    if (dueAt != null) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                onDueAtChanged(null)
                                showChips = false
                            },
                            label = { Text(stringResource(id = R.string.due_clear)) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChips = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickedDate = dateState.selectedDateMillis?.let { millis ->
                            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        } ?: LocalDate.now()
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val initial = dueAt?.let {
            runCatching { DueDates.toLocalDateTime(it).toLocalTime() }.getOrNull()
        } ?: DueDates.nextFullHour().toLocalTime()
        val timeState = rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(id = R.string.due_label)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val local = (pickedDate ?: LocalDate.now())
                            .atTime(timeState.hour, timeState.minute)
                        onDueAtChanged(DueDates.toIso(local))
                        showTimePicker = false
                    }
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriorityPicker(
    priority: TaskPriority,
    onPriorityChanged: (TaskPriority) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember {
        listOf(
            TaskPriority.LOW to R.string.priority_low,
            TaskPriority.MEDIUM to R.string.priority_medium,
            TaskPriority.HIGH to R.string.priority_high,
        )
    }
    val selectedLabel = stringResource(
        when (priority) {
            TaskPriority.LOW -> R.string.priority_low
            TaskPriority.MEDIUM -> R.string.priority_medium
            TaskPriority.HIGH -> R.string.priority_high
        }
    )
    Box {
        FieldRow(
            label = stringResource(id = R.string.priority_label),
            value = selectedLabel,
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, labelRes) ->
                DropdownMenuItem(
                    text = { Text(stringResource(id = labelRes)) },
                    onClick = {
                        onPriorityChanged(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
