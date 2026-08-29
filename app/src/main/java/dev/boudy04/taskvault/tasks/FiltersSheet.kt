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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.tasks.TasksFilterType.ACTIVE_TASKS
import dev.boudy04.taskvault.tasks.TasksFilterType.ALL_TASKS
import dev.boudy04.taskvault.tasks.TasksFilterType.COMPLETED_TASKS

/**
 * UX v3 consolidated Filters sheet: Group / Person / Status dropdowns, Sort
 * selector, Search field and Reset. Everything applies live.
 */
@Composable
internal fun FiltersSheet(
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
