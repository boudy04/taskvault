/*
 * Copyright 2019 The Android Open Source Project
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

package dev.boudy04.taskvault.addedittask

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.boudy04.taskvault.R
import dev.boudy04.taskvault.TodoDestinationsArgs
import dev.boudy04.taskvault.data.TaskPriority
import dev.boudy04.taskvault.data.TaskRepository
import dev.boudy04.taskvault.data.source.network.MemberDto
import dev.boudy04.taskvault.data.source.network.TaskApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UiState for the Add/Edit screen
 */
data class AddEditTaskUiState(
    val title: String = "",
    val description: String = "",
    val isTaskCompleted: Boolean = false,
    val isLoading: Boolean = false,
    val userMessage: Int? = null,
    val isTaskSaved: Boolean = false
)

/**
 * ViewModel for the Add/Edit screen.
 */
@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val api: TaskApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String? = savedStateHandle[TodoDestinationsArgs.TASK_ID_ARG]

    // A MutableStateFlow needs to be created in this ViewModel. The source of truth of the current
    // editable Task is the ViewModel, we need to mutate the UI state directly in methods such as
    // `updateTitle` or `updateDescription`
    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    private val _priority = MutableStateFlow(TaskPriority.MEDIUM)
    val priority: StateFlow<TaskPriority> = _priority.asStateFlow()

    /** ISO-8601 UTC due timestamp; null = no reminder. */
    private val _dueAt = MutableStateFlow<String?>(null)
    val dueAt: StateFlow<String?> = _dueAt.asStateFlow()

    /** Canonical (trim/lowercase) selected tags for the chip editor. */
    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    /** Up to 8 existing distinct tags from all tasks; drives the suggestion chips. */
    private val _tagSuggestions = MutableStateFlow<List<String>>(emptyList())
    val tagSuggestions: StateFlow<List<String>> = _tagSuggestions.asStateFlow()

    /** Workspace members for the assignee sheet; empty when offline. */
    private val _members = MutableStateFlow<List<MemberDto>>(emptyList())
    val members: StateFlow<List<MemberDto>> = _members.asStateFlow()

    /** Chosen assignee member ids. */
    private val _assigneeIds = MutableStateFlow<List<Int>>(emptyList())
    val assigneeIds: StateFlow<List<Int>> = _assigneeIds.asStateFlow()

    init {
        if (taskId != null) {
            loadTask(taskId)
        }
        viewModelScope.launch {
            _tagSuggestions.value = taskRepository.getAllTags().take(MAX_SUGGESTED_TAGS)
        }
        viewModelScope.launch {
            try {
                _members.value = api.listMembers()
            } catch (_: Exception) {
                // ponytail: offline = no assignable people in the sheet; retried on open
            }
        }
    }

    /** Re-fetches members so the assignee sheet can retry after an offline start. */
    fun reloadMembers() {
        viewModelScope.launch {
            try {
                _members.value = api.listMembers()
            } catch (_: Exception) {
            }
        }
    }

    fun toggleAssignee(memberId: Int) {
        _assigneeIds.value =
            if (memberId in _assigneeIds.value) _assigneeIds.value - memberId
            else _assigneeIds.value + memberId
    }

    /** Usernames for the chosen ids, resolved against [members] ("a, b"). */
    fun assigneeNames(): String =
        _members.value.filter { it.id in _assigneeIds.value }.joinToString(", ") { it.username }

    /** Commits a chip from raw input: commas stripped (a comma is the stored
     *  column's separator, so "a,b" as one chip would parse back as two tags),
     *  then trim + lowercase; drop empties/dupes. */
    fun addTag(raw: String) {
        val tag = raw.replace(",", "").trim().lowercase()
        if (tag.isEmpty()) return
        _tags.value = (_tags.value + tag).distinct()
    }

    fun removeTag(tag: String) {
        _tags.value = _tags.value - tag
    }

    private companion object {
        const val MAX_SUGGESTED_TAGS = 8
    }

    // Called when clicking on fab.
    fun saveTask() {
        if (uiState.value.title.isEmpty() || uiState.value.description.isEmpty()) {
            _uiState.update {
                it.copy(userMessage = R.string.empty_task_message)
            }
            return
        }

        if (taskId == null) {
            createNewTask()
        } else {
            updateTask()
        }
    }

    fun snackbarMessageShown() {
        _uiState.update {
            it.copy(userMessage = null)
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.update {
            it.copy(title = newTitle)
        }
    }

    fun updateDescription(newDescription: String) {
        _uiState.update {
            it.copy(description = newDescription)
        }
    }

    fun updatePriority(newPriority: TaskPriority) {
        _priority.value = newPriority
    }

    fun updateDueAt(newDueAt: String?) {
        _dueAt.value = newDueAt
    }

    private fun createNewTask() = viewModelScope.launch {
        taskRepository.createTask(
            uiState.value.title,
            uiState.value.description,
            _priority.value,
            _dueAt.value,
            _tags.value,
            _assigneeIds.value,
        )
        _uiState.update {
            it.copy(isTaskSaved = true)
        }
    }

    private fun updateTask() {
        if (taskId == null) {
            throw RuntimeException("updateTask() was called but task is new.")
        }
        viewModelScope.launch {
            taskRepository.updateTask(
                taskId,
                title = uiState.value.title,
                description = uiState.value.description,
                priority = _priority.value,
                dueAt = _dueAt.value,
                tags = _tags.value,
                assigneeIds = _assigneeIds.value,
            )
            _uiState.update {
                it.copy(isTaskSaved = true)
            }
        }
    }

    private fun loadTask(taskId: String) {
        _uiState.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            taskRepository.getTask(taskId).let { task ->
                if (task != null) {
                    _priority.value = task.priority
                    _dueAt.value = task.dueAt
                    _tags.value = task.tags
                    _assigneeIds.value = task.assigneeIds
                    _uiState.update {
                        it.copy(
                            title = task.title,
                            description = task.description,
                            isTaskCompleted = task.isCompleted,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                }
            }
        }
    }
}
