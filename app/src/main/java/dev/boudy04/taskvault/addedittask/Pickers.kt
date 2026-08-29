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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.data.TaskPriority
import dev.boudy04.taskvault.util.DueDates
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * "Due" row + pick flow per R20: chips (Today / Tomorrow / Pick date…) then Material3
 * TimePicker; [Clear] removes the due. The ViewModel stores ISO-8601 UTC.
 */
@Composable
internal fun DuePicker(
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
internal fun PriorityPicker(
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
